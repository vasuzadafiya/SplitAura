package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.splitaura.R;
import com.example.splitaura.models.OweRelationship;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OweAdapter extends RecyclerView.Adapter<OweAdapter.OweViewHolder> {
    private List<OweRelationship> oweRelationships;
    private final String currentUserId;
    private final Map<String, String> userNamesCache = new HashMap<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public OweAdapter(List<OweRelationship> oweRelationships) {
        this.oweRelationships = oweRelationships != null ? new ArrayList<>(oweRelationships) : new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchUserNames(); // Pre-fetch user names
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

        String debtorName = userNamesCache.getOrDefault(debtorId, debtorId.equals(currentUserId) ? "You" : debtorId);
        String creditorName = userNamesCache.getOrDefault(creditorId, creditorId.equals(currentUserId) ? "You" : creditorId);

        holder.debtorName.setText(debtorName);
        holder.creditorText.setText(creditorName);
        holder.oweAmount.setText(String.format("₹%.2f", owe.getAmount()));

        holder.menuDots.setOnClickListener(v -> showPopupMenu(v, owe, position));
    }

    @Override
    public int getItemCount() {
        return oweRelationships.size();
    }

    public static class OweViewHolder extends RecyclerView.ViewHolder {
        TextView debtorName, oweText, creditorText, oweAmount, menuDots;

        public OweViewHolder(@NonNull View itemView) {
            super(itemView);
            debtorName = itemView.findViewById(R.id.debtor_name);
            oweText = itemView.findViewById(R.id.owe_text);
            creditorText = itemView.findViewById(R.id.creditor_text);
            oweAmount = itemView.findViewById(R.id.owe_amount);
            menuDots = itemView.findViewById(R.id.menu_dots);
        }
    }

    public void updateOweRelationships(List<OweRelationship> newOweRelationships) {
        this.oweRelationships.clear();
        this.oweRelationships.addAll(newOweRelationships != null ? newOweRelationships : new ArrayList<>());
        fetchUserNames(); // Refresh names
        notifyDataSetChanged();
    }

    private void fetchUserNames() {
        if (oweRelationships.isEmpty()) return;

        List<String> userIds = new ArrayList<>();
        for (OweRelationship owe : oweRelationships) {
            if (!userNamesCache.containsKey(owe.getDebtor())) userIds.add(owe.getDebtor());
            if (!userNamesCache.containsKey(owe.getCreditor())) userIds.add(owe.getCreditor());
        }

        if (userIds.isEmpty()) return;

        db.collection("users").whereIn(FieldPath.documentId(), userIds)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String fullName = (doc.getString("fname") != null ? doc.getString("fname") : "") + " " +
                                (doc.getString("lname") != null ? doc.getString("lname") : "");
                        userNamesCache.put(doc.getId(), fullName.trim());
                    }
                    notifyDataSetChanged(); // Refresh UI with updated names
                });
    }

    private void showPopupMenu(View view, OweRelationship owe, int position) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        String debtorId = owe.getDebtor();
        String creditorId = owe.getCreditor();
        String debtorName = userNamesCache.getOrDefault(debtorId, debtorId);
        String creditorName = userNamesCache.getOrDefault(creditorId, creditorId);

        if (debtorId.equals(currentUserId)) {
            popup.getMenu().add("Pay Now");
            popup.getMenu().add("Mark as Paid");
        } else if (creditorId.equals(currentUserId)) {
            popup.getMenu().add("Request Payment");
            popup.getMenu().add("Mark as Settled");
        }

        popup.setOnMenuItemClickListener(item -> {
            String action = item.getTitle().toString();
            switch (action) {
                case "Pay Now":
                    Toast.makeText(view.getContext(), "Pay Now clicked for " + debtorName + " owing ₹" + owe.getAmount(), Toast.LENGTH_SHORT).show();
                    // TODO: Implement payment logic
                    break;
                case "Mark as Paid":
                    Toast.makeText(view.getContext(), "Marked as Paid: " + debtorName + " to " + creditorName, Toast.LENGTH_SHORT).show();
                    // TODO: Add logic to update Firestore (e.g., remove debt from calculation)
                    break;
                case "Request Payment":
                    Toast.makeText(view.getContext(), "Request Payment clicked for " + debtorName + " owing ₹" + owe.getAmount(), Toast.LENGTH_SHORT).show();
                    // TODO: Implement notification/request logic
                    break;
                case "Mark as Settled":
                    Toast.makeText(view.getContext(), "Marked as Settled: " + debtorName + " to " + creditorName, Toast.LENGTH_SHORT).show();
                    // TODO: Add logic to update Firestore (e.g., remove credit from calculation)
                    break;
            }
            return true;
        });

        popup.show();
    }
}