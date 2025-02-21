package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.models.FriendRequest;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class FriendRequestAdapter extends FirestoreRecyclerAdapter<FriendRequest, FriendRequestAdapter.FriendRequestViewHolder> {

    private OnRequestActionListener listener;

    public FriendRequestAdapter(@NonNull FirestoreRecyclerOptions<FriendRequest> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position, @NonNull FriendRequest model) {
        // Set the sender's name
        holder.userNameTextView.setText(model.getSenderName());
        // Load the sender's profile image (using circleCrop for a circular image)
        Glide.with(holder.itemView.getContext())
                .load(model.getSenderProfilePicture())
                .circleCrop()
                .into(holder.userProfileImageView);

        // Set the Accept button listener
        holder.sendRequestButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(getSnapshots().getSnapshot(position), model);
            }
        });
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_layout, parent, false);
        return new FriendRequestViewHolder(view);
    }

    public class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImageView;
        TextView userNameTextView;
        MaterialButton sendRequestButton;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            sendRequestButton = itemView.findViewById(R.id.sendRequestButton);
        }
    }

    public interface OnRequestActionListener {
        void onAccept(DocumentSnapshot snapshot, FriendRequest request);
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }
}
