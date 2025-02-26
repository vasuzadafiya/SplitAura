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
import com.example.splitaura.models.Friend;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class FriendAdapter extends FirestoreRecyclerAdapter<Friend, FriendAdapter.FriendViewHolder> {

    private OnRemoveFriendListener removeListener;

    public FriendAdapter(@NonNull FirestoreRecyclerOptions<Friend> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull FriendViewHolder holder, int position, @NonNull Friend model) {
        // Combine first and last name.
        String fullName = model.getFname() + " " + model.getLname();
        holder.friendNameTextView.setText(fullName);
        holder.friendEmailTextView.setText(model.getEmail());

        // Load profile image with Glide.
        Glide.with(holder.itemView.getContext())
                .load(model.getProfile_picture())
                .circleCrop()
                .into(holder.friendProfileImageView);

        // Set the remove button click listener (if using it).
        holder.removeFriendButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(getSnapshots().getSnapshot(position), model);
            }
        });
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_layout, parent, false);
        return new FriendViewHolder(view);
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView friendProfileImageView;
        TextView friendNameTextView;
        TextView friendEmailTextView;
        ImageButton removeFriendButton;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            friendProfileImageView = itemView.findViewById(R.id.friendProfileImageView);
            friendNameTextView = itemView.findViewById(R.id.friendNameTextView);
            friendEmailTextView = itemView.findViewById(R.id.friendEmailTextView);
            removeFriendButton = itemView.findViewById(R.id.removeFriendButton);
        }
    }

    public interface OnRemoveFriendListener {
        void onRemove(DocumentSnapshot snapshot, Friend friend);
    }

    public void setOnRemoveFriendListener(OnRemoveFriendListener listener) {
        this.removeListener = listener;
    }
}
