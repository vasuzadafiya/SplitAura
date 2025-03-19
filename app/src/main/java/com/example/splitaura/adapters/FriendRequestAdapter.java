package com.example.splitaura.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.models.FriendRequest;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class FriendRequestAdapter extends FirestoreRecyclerAdapter<FriendRequest, FriendRequestAdapter.FriendRequestViewHolder> {

    private static final String TAG = "FriendRequestAdapter";

    private OnRequestActionListener listener;

    public FriendRequestAdapter(@NonNull FirestoreRecyclerOptions<FriendRequest> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position, @NonNull FriendRequest model) {
        try {
            // Set sender's name with null check
            String senderName = model.getSenderName();
            holder.userNameTextView.setText(senderName != null ? senderName : "Unknown User");

            // Load sender's profile picture with null check
            String profilePicture = model.getSenderProfilePicture();
            if (profilePicture != null && !profilePicture.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(profilePicture)
                        .circleCrop()
                        .placeholder(R.drawable.obsmallmen2) // Fallback image
                        .error(R.drawable.obsmallmen2) // Fallback on error
                        .into(holder.userProfileImageView);
            } else {
                holder.userProfileImageView.setImageResource(R.drawable.obsmallmen2);
            }

            // Store the snapshot ID for stable reference
            holder.snapshotId = getSnapshots().getSnapshot(position).getId();

            // Always force Accept/Reject mode
            holder.showAcceptRejectButtons();

            // Set listeners for Accept and Reject
            holder.accept.setOnClickListener(v -> {
                try {
                    // Disable buttons to prevent multiple clicks
                    holder.accept.setEnabled(false);
                    holder.reject.setEnabled(false);

                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= getItemCount()) {
                        Log.w(TAG, "Invalid position on accept click: " + currentPosition + ", item count: " + getItemCount());
                        Toast.makeText(holder.itemView.getContext(), "Friend request no longer available", Toast.LENGTH_SHORT).show();
                        holder.accept.setEnabled(true);
                        holder.reject.setEnabled(true);
                        return;
                    }

                    DocumentSnapshot snapshot = getSnapshots().getSnapshot(currentPosition);
                    FriendRequest request = snapshot.toObject(FriendRequest.class);
                    if (request == null) {
                        Log.e(TAG, "Failed to parse FriendRequest at position: " + currentPosition);
                        Toast.makeText(holder.itemView.getContext(), "Error processing friend request", Toast.LENGTH_SHORT).show();
                        holder.accept.setEnabled(true);
                        holder.reject.setEnabled(true);
                        return;
                    }

                    if (listener != null) {
                        listener.onAccept(snapshot, request);
                    } else {
                        Log.w(TAG, "OnRequestActionListener is null on accept click");
                        Toast.makeText(holder.itemView.getContext(), "Action not available", Toast.LENGTH_SHORT).show();
                        holder.accept.setEnabled(true);
                        holder.reject.setEnabled(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in accept click handler at position: " + position, e);
                    Toast.makeText(holder.itemView.getContext(), "Error accepting friend request", Toast.LENGTH_SHORT).show();
                    holder.accept.setEnabled(true);
                    holder.reject.setEnabled(true);
                }
            });

            holder.reject.setOnClickListener(v -> {
                try {
                    // Disable buttons to prevent multiple clicks
                    holder.accept.setEnabled(false);
                    holder.reject.setEnabled(false);

                    int currentPosition = holder.getAdapterPosition();
                    if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= getItemCount()) {
                        Log.w(TAG, "Invalid position on reject click: " + currentPosition + ", item count: " + getItemCount());
                        Toast.makeText(holder.itemView.getContext(), "Friend request no longer available", Toast.LENGTH_SHORT).show();
                        holder.accept.setEnabled(true);
                        holder.reject.setEnabled(true);
                        return;
                    }

                    DocumentSnapshot snapshot = getSnapshots().getSnapshot(currentPosition);
                    FriendRequest request = snapshot.toObject(FriendRequest.class);
                    if (request == null) {
                        Log.e(TAG, "Failed to parse FriendRequest at position: " + currentPosition);
                        Toast.makeText(holder.itemView.getContext(), "Error processing friend request", Toast.LENGTH_SHORT).show();
                        holder.accept.setEnabled(true);
                        holder.reject.setEnabled(true);
                        return;
                    }

                    if (listener != null) {
                        listener.onReject(snapshot, request);
                    } else {
                        Log.w(TAG, "OnRequestActionListener is null on reject click");
                        Toast.makeText(holder.itemView.getContext(), "Action not available", Toast.LENGTH_SHORT).show();
                        holder.accept.setEnabled(true);
                        holder.reject.setEnabled(true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in reject click handler at position: " + position, e);
                    Toast.makeText(holder.itemView.getContext(), "Error rejecting friend request", Toast.LENGTH_SHORT).show();
                    holder.accept.setEnabled(true);
                    holder.reject.setEnabled(true);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position: " + position, e);
            Toast.makeText(holder.itemView.getContext(), "Error displaying friend request", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.user_item_layout, parent, false);
            return new FriendRequestViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error creating view holder", e);
            throw new RuntimeException("Failed to create view holder", e);
        }
    }

    public class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImageView;
        TextView userNameTextView;
        MaterialButton sendRequestButton; // remains in layout but hidden in this adapter
        ImageButton accept;
        ImageButton reject;
        String snapshotId; // Store the snapshot ID for stable reference

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
                userNameTextView = itemView.findViewById(R.id.userNameTextView);
                sendRequestButton = itemView.findViewById(R.id.sendRequestButton);
                accept = itemView.findViewById(R.id.btn_accept);
                reject = itemView.findViewById(R.id.btn_reject);

                // Null checks for view bindings
                if (userProfileImageView == null || userNameTextView == null || sendRequestButton == null ||
                        accept == null || reject == null) {
                    Log.e(TAG, "One or more views not found in user_item_layout");
                    throw new IllegalStateException("Required views not found in user_item_layout");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing FriendRequestViewHolder", e);
                throw new RuntimeException("Failed to initialize FriendRequestViewHolder", e);
            }
        }

        // Show Accept/Reject buttons and hide the Add button
        public void showAcceptRejectButtons() {
            try {
                sendRequestButton.setVisibility(View.GONE);
                accept.setVisibility(View.VISIBLE);
                reject.setVisibility(View.VISIBLE);
                accept.setEnabled(true);
                reject.setEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, "Error showing accept/reject buttons", e);
            }
        }
    }

    public interface OnRequestActionListener {
        void onAccept(DocumentSnapshot snapshot, FriendRequest request);
        void onReject(DocumentSnapshot snapshot, FriendRequest request);
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void onError(@NonNull Exception e) {
        super.onError((FirebaseFirestoreException) e);
        Log.e(TAG, "FirestoreRecyclerAdapter error", e);
        // Notify the user of the error
        if (getSnapshots().getSnapshot(0) != null) { // Check if there's at least one item to get context
            Toast.makeText(getSnapshots().getSnapshot(0).getReference().getFirestore().getApp().getApplicationContext(),
                    "Error loading friend requests", Toast.LENGTH_SHORT).show();
        }
    }
}
