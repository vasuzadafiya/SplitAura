package com.example.splitaura.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionPaymentAdapter extends RecyclerView.Adapter<TransactionPaymentAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;
    private final Context context;

    public TransactionPaymentAdapter(Context context) {
        this.context = context;
        this.transactionList = new ArrayList<>();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_item, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.tvFriendName.setText(transaction.getFriendName());
        holder.tvAmount.setText(String.format("₹%.2f", transaction.getAmount()));
        holder.tvDate.setText(transaction.getDate());
        holder.tvStatus.setText(transaction.getStatus());
        holder.tvTransactionId.setText("Transaction ID: " + transaction.getTransactionId());
        holder.tvUpiApp.setText("UPI App: " + transaction.getUpiApp());

        // Dynamic styling based on status
        switch (transaction.getStatus()) {
            case "SUCCESS":
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.black));
                break;
            case "PENDING":
            case "REQUESTED": // Added to match PaymentFragment
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.black));
                break;
            default: // FAILED or other
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light));
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));
                break;
        }

        // Click listener for transaction details
        holder.itemView.setOnClickListener(v ->
                Toast.makeText(context, "Transaction ID: " + transaction.getTransactionId(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TransactionDiffCallback(transactionList, newTransactions));
        transactionList.clear();
        transactionList.addAll(newTransactions);
        diffResult.dispatchUpdatesTo(this);
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvFriendName, tvAmount, tvDate, tvStatus, tvTransactionId, tvUpiApp;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFriendName = itemView.findViewById(R.id.tv_friend_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTransactionId = itemView.findViewById(R.id.tv_transaction_id);
            tvUpiApp = itemView.findViewById(R.id.tv_upi_app);
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
            return oldList.get(oldItemPosition).getTransactionId().equals(newList.get(newItemPosition).getTransactionId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Transaction oldTransaction = oldList.get(oldItemPosition);
            Transaction newTransaction = newList.get(newItemPosition);
            return oldTransaction.getAmount() == newTransaction.getAmount() &&
                    oldTransaction.getStatus().equals(newTransaction.getStatus()) &&
                    oldTransaction.getFriendName().equals(newTransaction.getFriendName()) &&
                    oldTransaction.getDate().equals(newTransaction.getDate()) &&
                    oldTransaction.getUpiApp().equals(newTransaction.getUpiApp());
        }
    }
}