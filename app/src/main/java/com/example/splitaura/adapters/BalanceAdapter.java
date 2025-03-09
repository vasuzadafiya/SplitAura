package com.example.splitaura.adapters;

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

import java.util.List;

public class BalanceAdapter extends RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder> {

    private List<Balance> balances;

    public BalanceAdapter(List<Balance> balances) {
        this.balances = balances;
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
        holder.nameTextView.setText(balance.getName());

        // Format the balance amount
        String amountText = String.format("%.2f rs", Math.abs(balance.getAmount()));
        holder.amountTextView.setText(amountText);
        holder.amountTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

        // Get the ConstraintLayout.LayoutParams for the name and amount TextViews
        ConstraintLayout.LayoutParams nameParams = (ConstraintLayout.LayoutParams) holder.nameTextView.getLayoutParams();
        ConstraintLayout.LayoutParams amountParams = (ConstraintLayout.LayoutParams) holder.amountTextView.getLayoutParams();

        // Reset constraints
        nameParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
        nameParams.endToStart = ConstraintLayout.LayoutParams.UNSET;
        nameParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
        amountParams.startToEnd = ConstraintLayout.LayoutParams.UNSET;
        amountParams.endToStart = ConstraintLayout.LayoutParams.UNSET;

        if (balance.getAmount() >= 0) {
            // Positive balance: name on the left, amount on the right (green)
            holder.amountTextView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.background_green));

            // Align name to the left
            nameParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            nameParams.endToStart = holder.amountTextView.getId();
            nameParams.horizontalBias = 0.5f; // Centered horizontally

            // Align amount to the right
            amountParams.startToEnd = holder.nameTextView.getId();
            amountParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            amountParams.horizontalBias = 0.5f; // Centered horizontally
        } else {
            // Negative balance: name on the right, amount on the left (red)
            holder.amountTextView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.background_red));

            // Align name to the right
            nameParams.startToEnd = holder.amountTextView.getId();
            nameParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            nameParams.horizontalBias = 0.5f; // Centered horizontally

            // Align amount to the left
            amountParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            amountParams.endToStart = holder.nameTextView.getId();
            amountParams.horizontalBias = 0.5f; // Centered horizontally
        }

        // Apply the updated layout parameters
        holder.nameTextView.setLayoutParams(nameParams);
        holder.amountTextView.setLayoutParams(amountParams);
    }

    @Override
    public int getItemCount() {
        return balances != null ? balances.size() : 0;
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
        balances = newBalances;
        notifyDataSetChanged();
    }
}