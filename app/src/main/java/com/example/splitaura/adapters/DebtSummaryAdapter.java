package com.example.splitaura.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.DebtSummary;

import java.util.List;

public class DebtSummaryAdapter extends RecyclerView.Adapter<DebtSummaryAdapter.ViewHolder> {
    private final Context context;
    private final List<DebtSummary> debtSummaries;
    private String selectedFriendId;
    private final OnDebtSelectedListener listener;

    public DebtSummaryAdapter(Context context, List<DebtSummary> debtSummaries, OnDebtSelectedListener listener) {
        this.context = context;
        this.debtSummaries = debtSummaries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DebtSummary summary = debtSummaries.get(position);
        holder.text1.setText(summary.getFriendName());
        String amountText = summary.getAmount() > 0 ? "Owes you ₹" + String.format("%.2f", summary.getAmount()) :
                summary.getAmount() < 0 ? "You owe ₹" + String.format("%.2f", Math.abs(summary.getAmount())) : "Settled";
        holder.text2.setText(amountText);
        holder.itemView.setBackgroundColor(summary.getFriendId().equals(selectedFriendId) ? Color.LTGRAY : Color.WHITE);
        holder.itemView.setOnClickListener(v -> {
            selectedFriendId = summary.getFriendId();
            notifyDataSetChanged();
            listener.onDebtSelected(summary);
        });
    }

    @Override
    public int getItemCount() {
        return debtSummaries.size();
    }

    public DebtSummary getSelectedDebt() {
        for (DebtSummary summary : debtSummaries) {
            if (summary.getFriendId().equals(selectedFriendId)) {
                return summary;
            }
        }
        return null;
    }

    public void setSelectedFriendId(String friendId) {
        this.selectedFriendId = friendId;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }

    public interface OnDebtSelectedListener {
        void onDebtSelected(DebtSummary debtSummary);
    }
}