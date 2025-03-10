package com.example.splitaura.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
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
    private List<Balance> balances;
    private final Map<String, String> userNamesCache = new HashMap<>(); // Cache for user names
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public BalanceAdapter(List<Balance> balances) {
        this.balances = balances != null ? new ArrayList<>(balances) : new ArrayList<>();
        fetchUserNames(); // Pre-fetch user names
    }

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.balance_item, parent, false);
        return new BalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        Balance balance = balances.get(position);
        String userId = balance.getName();

        // Display "Loading..." or a placeholder until the user name is fetched
        String displayName = userNamesCache.getOrDefault(userId, "Loading...");
        holder.nameTextView.setText(displayName);

        // Format the balance amount with currency symbol and sign
        String amountText;
        if (balance.getAmount() >= 0) {
            amountText = String.format("+ ₹%.2f", balance.getAmount()); // Add plus sign for positive amounts
        } else {
            amountText = String.format("- ₹%.2f", Math.abs(balance.getAmount())); // Add minus sign for negative amounts
        }
        holder.amountTextView.setText(amountText);
        holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

        // Configure layout based on balance sign
        ConstraintLayout.LayoutParams nameParams = (ConstraintLayout.LayoutParams) holder.nameTextView.getLayoutParams();
        ConstraintLayout.LayoutParams amountParams = (ConstraintLayout.LayoutParams) holder.amountTextView.getLayoutParams();

        // Reset constraints
        nameParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
        nameParams.endToStart = ConstraintLayout.LayoutParams.UNSET;
        nameParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
        amountParams.startToEnd = ConstraintLayout.LayoutParams.UNSET;
        amountParams.endToStart = ConstraintLayout.LayoutParams.UNSET;

        if (balance.getAmount() >= 0) {
            holder.amountTextView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.background_green));
            nameParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            nameParams.endToStart = holder.amountTextView.getId();
            amountParams.startToEnd = holder.nameTextView.getId();
            amountParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        } else {
            holder.amountTextView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.background_red));
            nameParams.startToEnd = holder.amountTextView.getId();
            nameParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            amountParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            amountParams.endToStart = holder.nameTextView.getId();
        }

        holder.nameTextView.setLayoutParams(nameParams);
        holder.amountTextView.setLayoutParams(amountParams);
    }

    @Override
    public int getItemCount() {
        return balances.size();
    }

    public static class BalanceViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, amountTextView;

        public BalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.balance_name);
            amountTextView = itemView.findViewById(R.id.balance_amount);
        }
    }

    public void updateBalances(List<Balance> newBalances) {
        this.balances.clear();
        this.balances.addAll(newBalances != null ? newBalances : new ArrayList<>());
        fetchUserNames(); // Refresh names
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
                    notifyDataSetChanged(); // Refresh UI with updated names
                })
                .addOnFailureListener(e -> {
                    // Handle failure (e.g., log the error)
                    Log.e("BalanceAdapter", "Failed to fetch user names", e);
                });
    }
}