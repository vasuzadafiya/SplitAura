package com.example.splitaura.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionPaymentAdapter extends RecyclerView.Adapter<TransactionPaymentAdapter.ViewHolder> {
    private final Context context;
    private final List<Transaction> transactions = new ArrayList<>();

    public TransactionPaymentAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        holder.tvFriendName.setText(transaction.getDescription());
        holder.tvAmount.setText(String.format("₹%.2f", transaction.getAmount()));
        holder.tvDate.setText(transaction.getRecordedAt());
        holder.tvMethod.setText("Method: " + transaction.getMethod());
        holder.tvStatus.setText(transaction.getPaymentStatus());

        // Set status icon and background color
        switch (transaction.getPaymentStatus()) {
            case "SUCCESS":
                holder.ivStatusIcon.setImageResource(R.drawable.ic_transaction_success);
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "PENDING":
                holder.ivStatusIcon.setImageResource(R.drawable.ic_transaction_pending);
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "FAILED":
                holder.ivStatusIcon.setImageResource(R.drawable.ic_transaction_failed);
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));
                break;
            default:
                holder.ivStatusIcon.setImageResource(R.drawable.ic_transaction_pending);
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TransactionDiffCallback(transactions, newTransactions));
        transactions.clear();
        transactions.addAll(newTransactions);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendName, tvAmount, tvDate, tvMethod, tvStatus;
        ImageView ivStatusIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvFriendName = itemView.findViewById(R.id.tv_friend_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvMethod = itemView.findViewById(R.id.tv_method);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
        }
    }

    static class TransactionDiffCallback extends DiffUtil.Callback {
        private final List<Transaction> oldList;
        private final List<Transaction> newList;

        TransactionDiffCallback(List<Transaction> oldList, List<Transaction> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getPaymentId().equals(newList.get(newItemPosition).getPaymentId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Transaction oldTransaction = oldList.get(oldItemPosition);
            Transaction newTransaction = newList.get(newItemPosition);
            return oldTransaction.getPaymentId().equals(newTransaction.getPaymentId()) &&
                    oldTransaction.getPaymentStatus().equals(newTransaction.getPaymentStatus()) &&
                    oldTransaction.getAmount() == newTransaction.getAmount() &&
                    oldTransaction.getDescription().equals(newTransaction.getDescription());
        }
    }
}