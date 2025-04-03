package com.example.splitaura.adapters;

import android.content.Intent;
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
import java.util.List;
import java.util.Map;

public class OweAdapter extends RecyclerView.Adapter<OweAdapter.OweViewHolder> {
    private static final String TAG = "OweAdapter";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<OweRelationship> oweRelationships;
    private final String currentUserId;
    private final Map<String, String> userNamesCache = Collections.synchronizedMap(new HashMap<>());

    public OweAdapter(List<OweRelationship> oweRelationships) {
        this.oweRelationships = oweRelationships != null ? new ArrayList<>(oweRelationships) : new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        fetchUserNames();
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

        // Set default values.
        holder.debtorName.setText(debtorId.equals(currentUserId) ? "You" : userNamesCache.getOrDefault(debtorId, "Loading..."));
        holder.creditorText.setText(creditorId.equals(currentUserId) ? "You" : userNamesCache.getOrDefault(creditorId, "Loading..."));
        holder.oweAmount.setText(String.format("₹%.2f", owe.getAmount()));

        holder.menuDots.setOnClickListener(v -> showPopupMenu(v, owe));
    }

    @Override
    public int getItemCount() {
        return oweRelationships.size();
    }

    public void updateOweRelationships(List<OweRelationship> newOweRelationships) {
        this.oweRelationships.clear();
        this.oweRelationships.addAll(newOweRelationships != null ? newOweRelationships : new ArrayList<>());
        fetchUserNames();
        notifyDataSetChanged();
    }

    /**
     * Fetches user names from Firestore for any user IDs in the owe relationships that are not already cached.
     */
    private void fetchUserNames() {
        List<String> userIdsToFetch = new ArrayList<>();
        for (OweRelationship owe : oweRelationships) {
            if (!userNamesCache.containsKey(owe.getDebtor()) && !owe.getDebtor().equals(currentUserId)) {
                userIdsToFetch.add(owe.getDebtor());
            }
            if (!userNamesCache.containsKey(owe.getCreditor()) && !owe.getCreditor().equals(currentUserId)) {
                userIdsToFetch.add(owe.getCreditor());
            }
        }
        if (userIdsToFetch.isEmpty()) return;

        db.collection("users").whereIn(FieldPath.documentId(), userIdsToFetch)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String fname = doc.getString("fname");
                        String lname = doc.getString("lname");
                        String fullName = ((fname != null ? fname : "") + " " + (lname != null ? lname : "")).trim();
                        if (fullName.isEmpty()) {
                            fullName = "User " + doc.getId();
                        }
                        userNamesCache.put(doc.getId(), fullName);
                    }
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> { /* Log error if needed */ });
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
        TextView creditorText;
        TextView oweAmount;
        TextView menuDots;

        public OweViewHolder(@NonNull View itemView) {
            super(itemView);
            debtorName = itemView.findViewById(R.id.debtor_name);
            creditorText = itemView.findViewById(R.id.creditor_text);
            oweAmount = itemView.findViewById(R.id.owe_amount);
            menuDots = itemView.findViewById(R.id.menu_dots);
        }
    }
}