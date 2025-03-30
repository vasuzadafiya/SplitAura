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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OweAdapter extends RecyclerView.Adapter<OweAdapter.OweViewHolder> {
    private final List<OweRelationship> oweRelationships;
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

        holder.menuDots.setOnClickListener(v -> showPopupMenu(v, owe));
    }

    @Override
    public int getItemCount() {
        return oweRelationships.size();
    }

    private void fetchUserNames() {
        if (oweRelationships.isEmpty()) return;

        List<String> userIds = new ArrayList<>();
        for (OweRelationship owe : oweRelationships) {
            if (!userNamesCache.containsKey(owe.getDebtor())) userIds.add(owe.getDebtor());
            if (!userNamesCache.containsKey(owe.getCreditor())) userIds.add(owe.getCreditor());
        }

        if (userIds.isEmpty()) return;

        db.collection("users").whereIn(FieldPath.documentId(), userIds).get().addOnSuccessListener(query -> {
            for (QueryDocumentSnapshot doc : query) {
                String fullName = (doc.getString("fname") != null ? doc.getString("fname") : "") + " " + (doc.getString("lname") != null ? doc.getString("lname") : "");
                userNamesCache.put(doc.getId(), fullName.trim());
            }
            notifyDataSetChanged(); // Refresh UI with updated names
        });
    }

    public void updateOweRelationships(List<OweRelationship> newOweRelationships) {
        this.oweRelationships.clear();
        this.oweRelationships.addAll(newOweRelationships != null ? newOweRelationships : new ArrayList<>());
        fetchUserNames(); // Refresh names
        notifyDataSetChanged();
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

        // All menu items trigger the same action
        popup.setOnMenuItemClickListener(item -> {
            navigateToPaymentFragment(view);
            return true;
        });

        popup.show();
    }

    private void navigateToPaymentFragment(View view) {
        // Create an intent to launch HomeActivity
        Intent intent = new Intent(view.getContext(), Homepage.class);
        // Pass a flag to navigate to the Payment fragment
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