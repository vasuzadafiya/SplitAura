package com.example.splitaura.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.Balance;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalanceAdapter extends RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder> {
    private static final int VIEW_TYPE_POSITIVE = 0;
    private static final int VIEW_TYPE_NEGATIVE = 1;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Balance> balances;
    private final Map<String, String> userNamesCache = new HashMap<>();

    public BalanceAdapter(List<Balance> balances) {
        this.balances = balances != null ? new ArrayList<>(balances) : new ArrayList<>();
        fetchUserNames();
    }

    @Override
    public int getItemViewType(int position) {
        Balance balance = balances.get(position);
        return balance.getAmount() >= 0 ? VIEW_TYPE_POSITIVE : VIEW_TYPE_NEGATIVE;
    }

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_POSITIVE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.balance_item_positive, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.balance_item_negative, parent, false);
        }
        return new BalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        Balance balance = balances.get(position);
        String userId = balance.getName();

        // Display "Loading..." or the cached name
        String displayName = userNamesCache.getOrDefault(userId, "Loading...");
        holder.nameTextView.setText(displayName);

        // Format the balance amount with sign and currency symbol
        String amountText;
        if (balance.getAmount() >= 0) {
            amountText = String.format("+ ₹%.2f", balance.getAmount());
            holder.amountTextView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.background_green));
        } else {
            amountText = String.format("- ₹%.2f", Math.abs(balance.getAmount()));
            holder.amountTextView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.background_red));
        }
        holder.amountTextView.setText(amountText);
    }

    @Override
    public int getItemCount() {
        return balances.size();
    }

    public void updateBalances(List<Balance> newBalances) {
        this.balances.clear();
        this.balances.addAll(newBalances != null ? newBalances : new ArrayList<>());
        fetchUserNames();
        notifyDataSetChanged();
    }

    private void fetchUserNames() {
        if (balances.isEmpty()) return;

        List<String> userIds = new ArrayList<>();
        for (Balance balance : balances) {
            String userId = balance.getName();
            if (!userNamesCache.containsKey(userId)) {
                userIds.add(userId);
            }
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
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("BalanceAdapter", "Failed to fetch user names", e));
    }

    public static class BalanceViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView amountTextView;

        public BalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.balance_name);
            amountTextView = itemView.findViewById(R.id.balance_amount);
        }
    }
}
