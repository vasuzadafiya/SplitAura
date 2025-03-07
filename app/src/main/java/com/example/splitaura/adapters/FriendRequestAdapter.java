package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.models.FriendRequest;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

public class FriendRequestAdapter extends FirestoreRecyclerAdapter<FriendRequest, FriendRequestAdapter.FriendRequestViewHolder> {

    private OnRequestActionListener listener;

    public FriendRequestAdapter(@NonNull FirestoreRecyclerOptions<FriendRequest> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position, @NonNull FriendRequest model) {
        // Set sender's name.
        holder.userNameTextView.setText(model.getSenderName());
        // Load sender's profile picture.
        Glide.with(holder.itemView.getContext())
                .load(model.getSenderProfilePicture())
                .circleCrop()
                .into(holder.userProfileImageView);

        // Always force Accept/Reject mode.
        holder.showAcceptRejectButtons();

        // Set listeners for Accept and Reject.
        holder.accept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(getSnapshots().getSnapshot(position), model);
            }
        });
        holder.reject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(getSnapshots().getSnapshot(position), model);
            }
        });
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item_layout, parent, false);
        return new FriendRequestViewHolder(view);
    }

    public class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImageView;
        TextView userNameTextView;
        MaterialButton sendRequestButton; // remains in layout but hidden in this adapter
        ImageButton accept;
        ImageButton reject;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            sendRequestButton = itemView.findViewById(R.id.sendRequestButton);
            accept = itemView.findViewById(R.id.btn_accept);
            reject = itemView.findViewById(R.id.btn_reject);
        }

        // Show Accept/Reject buttons and hide the Add button.
        public void showAcceptRejectButtons() {
            sendRequestButton.setVisibility(View.GONE);
            accept.setVisibility(View.VISIBLE);
            reject.setVisibility(View.VISIBLE);
        }

    }

    public interface OnRequestActionListener {
        void onAccept(DocumentSnapshot snapshot, FriendRequest request);
        void onReject(DocumentSnapshot snapshot, FriendRequest request);
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }
}
