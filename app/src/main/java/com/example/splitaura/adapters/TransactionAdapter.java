package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.TransactionItem;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionItem> transactionList;

    public TransactionAdapter(List<TransactionItem> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionItem item = transactionList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvAmount.setText(item.getAmount());
        holder.tvPaidBy.setText(item.getPaidBy());
        holder.tvDate.setText(item.getDate());
        holder.ivIcon.setImageResource(item.getIconResId());
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvAmount, tvPaidBy, tvDate;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.category_icon);
            tvTitle = itemView.findViewById(R.id.expense_name);
            tvAmount = itemView.findViewById(R.id.expense_amount);
            tvPaidBy = itemView.findViewById(R.id.expense_paid_by);
            tvDate = itemView.findViewById(R.id.expense_date);
        }
    }
}
