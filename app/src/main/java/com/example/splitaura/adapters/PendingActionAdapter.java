package com.example.splitaura.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PendingActionAdapter extends RecyclerView.Adapter<PendingActionAdapter.ViewHolder> {
    private final Context context;
    private final List<Notification> pendingActions = new ArrayList<>();
    private final OnActionListener listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public PendingActionAdapter(Context context, OnActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pending_action_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = pendingActions.get(position);

        // Fetch sender's name from Firestore
        db.collection("users").document(notification.getSenderId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String senderName = documentSnapshot.exists() ?
                            (documentSnapshot.getString("fname") + " " + (documentSnapshot.getString("lname") != null ? documentSnapshot.getString("lname") : "")) : "Unknown User";
                    holder.tvDetails.setText("From: " + senderName);
                })
                .addOnFailureListener(e -> {
                    holder.tvDetails.setText("From: Unknown User");
                    Log.e("PendingActionAdapter", "Failed to fetch sender name: " + e.getMessage());
                });

        // Set action type and status
        holder.tvType.setText("Type: " + notification.getActionType());
        holder.tvStatus.setText("Status: " + notification.getActionStatus());

        // Style status and control button visibility
        boolean isPending = "PENDING".equals(notification.getActionStatus());
        switch (notification.getActionStatus().toUpperCase(Locale.ROOT)) {
            case "PENDING":
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                break;
            case "CONFIRMED":
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
                holder.btnAccept.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                break;
            case "REJECTED":
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));
                holder.btnAccept.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                break;
            default:
                holder.tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                holder.btnAccept.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                break;
        }

        // Enable buttons only for pending actions
        holder.btnAccept.setEnabled(isPending);
        holder.btnReject.setEnabled(isPending);

        // Set button click listeners
        holder.btnAccept.setOnClickListener(v -> {
            if (isPending) {
                listener.onAction(notification, true);
            }
        });
        holder.btnReject.setOnClickListener(v -> {
            if (isPending) {
                listener.onAction(notification, false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pendingActions.size();
    }

    public void updatePendingActions(List<Notification> newActions) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NotificationDiffCallback(pendingActions, newActions));
        pendingActions.clear();
        pendingActions.addAll(newActions);
        diffResult.dispatchUpdatesTo(this);
        Log.d("PendingActionAdapter", "Pending actions updated, new count: " + pendingActions.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDetails, tvType, tvStatus;
        Button btnAccept, btnReject;
        ImageView ivPendingIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvDetails = itemView.findViewById(R.id.tv_pending_details);
            tvType = itemView.findViewById(R.id.tv_pending_type);
            tvStatus = itemView.findViewById(R.id.tv_pending_status);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
            ivPendingIcon = itemView.findViewById(R.id.iv_pending_icon);
        }
    }

    public interface OnActionListener {
        void onAction(Notification notification, boolean accept);
    }

    static class NotificationDiffCallback extends DiffUtil.Callback {
        private final List<Notification> oldList;
        private final List<Notification> newList;

        NotificationDiffCallback(List<Notification> oldList, List<Notification> newList) {
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
            return oldList.get(oldItemPosition).getActionId().equals(newList.get(newItemPosition).getActionId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Notification oldNotification = oldList.get(oldItemPosition);
            Notification newNotification = newList.get(newItemPosition);
            return oldNotification.getActionId().equals(newNotification.getActionId()) &&
                    oldNotification.getActionStatus().equals(newNotification.getActionStatus()) &&
                    oldNotification.getActionType().equals(newNotification.getActionType()) &&
                    oldNotification.getSenderId().equals(newNotification.getSenderId()) &&
                    oldNotification.getReceiverId().equals(newNotification.getReceiverId());
        }
    }
}