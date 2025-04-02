package com.example.splitaura.adapters;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.Homepage;
import com.example.splitaura.R;
import com.example.splitaura.models.OweRelationship;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class OweAdapter extends RecyclerView.Adapter<OweAdapter.OweViewHolder> {
    private static final String TAG = "OweAdapter";
    private static final int MAX_BATCH_SIZE = 10; // Firestore's whereIn limit
    private static final long DEBOUNCE_DELAY_MS = 300; // Delay for batch processing

    private final List<OweRelationship> oweRelationships;
    private final String currentUserId;
    private final Map<String, String> userNamesCache = Collections.synchronizedMap(new HashMap<>());
    private final Set<String> pendingUserIds = Collections.synchronizedSet(new HashSet<>());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor backgroundExecutor = Executors.newFixedThreadPool(2);
    private final Runnable batchFetchRunnable = this::processPendingUserIds;

    public OweAdapter(List<OweRelationship> oweRelationships) {
        this.oweRelationships = oweRelationships != null ? new ArrayList<>(oweRelationships) : new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        scheduleInitialFetch();
    }

    @NonNull
    @Override
    public OweViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.owe_item, parent, false);
        return new OweViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OweViewHolder holder, int position) {
        OweRelationship owe = oweRelationships.get(position);
        String debtorId = owe.getDebtor();
        String creditorId = owe.getCreditor();

        // Set default values first
        holder.debtorName.setText(debtorId.equals(currentUserId) ? "You" : "Loading...");
        holder.creditorText.setText(creditorId.equals(currentUserId) ? "You" : "Loading...");
        holder.oweAmount.setText(String.format("₹%.2f", owe.getAmount()));

        // Check cache and update if available
        updateNameIfCached(holder.debtorName, debtorId);
        updateNameIfCached(holder.creditorText, creditorId);

        // Trigger fetch if not in cache and not already pending
        scheduleFetchIfNeeded(debtorId);
        scheduleFetchIfNeeded(creditorId);

        holder.menuDots.setOnClickListener(v -> showPopupMenu(v, owe));
    }

    @Override
    public int getItemCount() {
        return oweRelationships.size();
    }

    public void updateOweRelationships(List<OweRelationship> newOweRelationships) {
        this.oweRelationships.clear();
        this.oweRelationships.addAll(newOweRelationships != null ? newOweRelationships : new ArrayList<>());
        scheduleInitialFetch();
        notifyDataSetChanged();
    }

    private void scheduleInitialFetch() {
        backgroundExecutor.execute(() -> {
            synchronized (pendingUserIds) {
                pendingUserIds.clear();
                for (OweRelationship owe : oweRelationships) {
                    if (!userNamesCache.containsKey(owe.getDebtor())) {
                        pendingUserIds.add(owe.getDebtor());
                    }
                    if (!userNamesCache.containsKey(owe.getCreditor())) {
                        pendingUserIds.add(owe.getCreditor());
                    }
                }
            }
            scheduleBatchProcessing();
        });
    }

    private void scheduleFetchIfNeeded(String userId) {
        if (userNamesCache.containsKey(userId) || userId.equals(currentUserId)) {
            return;
        }

        backgroundExecutor.execute(() -> {
            synchronized (pendingUserIds) {
                if (!pendingUserIds.contains(userId)) {
                    pendingUserIds.add(userId);
                    scheduleBatchProcessing();
                }
            }
        });
    }

    private void scheduleBatchProcessing() {
        // Remove any pending calls to avoid duplicate processing
        mainHandler.removeCallbacks(batchFetchRunnable);
        // Schedule with debounce delay
        mainHandler.postDelayed(batchFetchRunnable, DEBOUNCE_DELAY_MS);
    }

    private void processPendingUserIds() {
        backgroundExecutor.execute(() -> {
            List<String> userIdsToFetch;
            synchronized (pendingUserIds) {
                if (pendingUserIds.isEmpty()) {
                    return;
                }
                userIdsToFetch = new ArrayList<>(pendingUserIds);
                pendingUserIds.clear();
            }

            // Split into batches of MAX_BATCH_SIZE
            for (int i = 0; i < userIdsToFetch.size(); i += MAX_BATCH_SIZE) {
                int end = Math.min(i + MAX_BATCH_SIZE, userIdsToFetch.size());
                List<String> batch = userIdsToFetch.subList(i, end);
                fetchUserNamesBatch(new ArrayList<>(batch));
            }
        });
    }

    private void fetchUserNamesBatch(List<String> userIds) {
        if (userIds.isEmpty()) {
            return;
        }

        db.collection("users")
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .addOnSuccessListener(query -> {
                    synchronized (userNamesCache) {
                        for (QueryDocumentSnapshot doc : query) {
                            String fullName = formatUserName(doc);
                            userNamesCache.put(doc.getId(), fullName);
                        }
                    }
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user names for batch: " + userIds, e);
                    // Retry failed IDs (you might want to add exponential backoff in production)
                    synchronized (pendingUserIds) {
                        pendingUserIds.addAll(userIds);
                    }
                    scheduleBatchProcessing();
                });
    }

    private String formatUserName(QueryDocumentSnapshot doc) {
        String fname = doc.getString("fname");
        String lname = doc.getString("lname");
        if (fname == null) fname = "";
        if (lname == null) lname = "";
        return (fname + " " + lname).trim();
    }

    private void updateNameIfCached(TextView textView, String userId) {
        if (userId.equals(currentUserId)) {
            textView.setText("You");
            return;
        }

        String cachedName = userNamesCache.get(userId);
        if (cachedName != null) {
            textView.setText(cachedName);
        }
    }

    private void showPopupMenu(View view, OweRelationship owe) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        String menuText = "";

        if (owe.getDebtor().equals(currentUserId)) {
            menuText = "Pay Now";
        } else if (owe.getCreditor().equals(currentUserId)) {
            menuText = "Request Payment";
        }

        if (!menuText.isEmpty()) {
            popup.getMenu().add(menuText);
        }

        popup.setOnMenuItemClickListener(item -> {
            navigateToPaymentFragment(view);
            return true;
        });

        popup.show();
    }

    private void navigateToPaymentFragment(View view) {
        Intent intent = new Intent(view.getContext(), Homepage.class);
        intent.putExtra("navigateToPayment", true);
        view.getContext().startActivity(intent);
    }

    public static class OweViewHolder extends RecyclerView.ViewHolder {
        TextView debtorName;
        TextView oweText;
        TextView creditorText;
        TextView oweAmount;
        TextView menuDots;

        public OweViewHolder(@NonNull View itemView) {
            super(itemView);
            debtorName = itemView.findViewById(R.id.debtor_name);
            oweText = itemView.findViewById(R.id.owe_text);
            creditorText = itemView.findViewById(R.id.creditor_text);
            oweAmount = itemView.findViewById(R.id.owe_amount);
            menuDots = itemView.findViewById(R.id.menu_dots);
        }
    }
}