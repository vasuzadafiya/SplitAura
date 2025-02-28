package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        holder.amountTextView.setText(String.format("%s%.2f rs", balance.getAmount() >= 0 ? "+" : "", balance.getAmount()));
        // Set blue for positive balances and red for negative balances
        holder.amountTextView.setBackgroundColor(balance.getAmount() >= 0 ?
                ContextCompat.getColor(holder.itemView.getContext(), R.color.custom_green) :
                ContextCompat.getColor(holder.itemView.getContext(), R.color.custom_red));
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