package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.models.User;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserAdapter extends FirestoreRecyclerAdapter<User, UserAdapter.UserViewHolder> {

    private OnUserRequestClickListener listener;
    // Flag to control which UI mode to show
    private boolean isAddMode = true; // default: show "Add" button

    public UserAdapter(@NonNull FirestoreRecyclerOptions<User> options) {
        super(options);
    }

    // Allow toggling mode (if ever needed)
    public void setAddMode(boolean mode) {
        this.isAddMode = mode;
        notifyDataSetChanged();
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull User model) {
        // Set full name with proper capitalization.
        holder.userNameTextView.setText(capitalizeFirst(model.getFullName()));

        // Load profile picture using Glide with circle crop.
        if (model.getProfile_picture() != null && !model.getProfile_picture().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(model.getProfile_picture())
                    .circleCrop()
                    .into(holder.userProfileImageView);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .into(holder.userProfileImageView);
        }

        // Always force Add mode for this adapter.
        if (isAddMode) {
            holder.showAddButton();
        } else {
            holder.showAcceptRejectButtons();
        }

        // Set listener for the Add button.
        holder.sendRequestButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSendRequest(getSnapshots().getSnapshot(position));
            }
        });
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item_layout, parent, false);
        return new UserViewHolder(view);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImageView;
        TextView userNameTextView;
        Button sendRequestButton;
        ImageButton accept, reject;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            sendRequestButton = itemView.findViewById(R.id.sendRequestButton);
            accept = itemView.findViewById(R.id.btn_accept);
            reject = itemView.findViewById(R.id.btn_reject);
        }

        // Show only the Add button.
        public void showAddButton() {
            sendRequestButton.setVisibility(View.VISIBLE);
            accept.setVisibility(View.GONE);
            reject.setVisibility(View.GONE);
        }

        // (Optional) Method to show Accept/Reject buttons.
        public void showAcceptRejectButtons() {
            sendRequestButton.setVisibility(View.GONE);
            accept.setVisibility(View.VISIBLE);
            reject.setVisibility(View.VISIBLE);
        }
    }

    public interface OnUserRequestClickListener {
        void onSendRequest(DocumentSnapshot snapshot);
    }

    public void setOnUserRequestClickListener(OnUserRequestClickListener listener) {
        this.listener = listener;
    }

    public static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
