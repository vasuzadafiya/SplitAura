package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    public UserAdapter(@NonNull FirestoreRecyclerOptions<User> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull User model) {
        // Display the full name (fname + " " + lname)
        holder.userNameTextView.setText(capitalizeFirst(model.getFullName()));

        // Load the profile picture using Glide with a circle crop transformation
        if (model.getProfile_picture() != null && !model.getProfile_picture().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(model.getProfile_picture())
                    .circleCrop() // This makes the image circular
                    .into(holder.userProfileImageView);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .into(holder.userProfileImageView);
        }

        // Handle the Send Request button click
        holder.sendRequestButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSendRequest(getSnapshots().getSnapshot(position));
            }
        });
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_layout, parent, false);
        return new UserViewHolder(view);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImageView;
        TextView userNameTextView;
        Button sendRequestButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImageView = itemView.findViewById(R.id.userProfileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            sendRequestButton = itemView.findViewById(R.id.sendRequestButton);
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
