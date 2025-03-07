package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.splitaura.R;
import com.example.splitaura.models.Friend;
import java.util.List;
import com.bumptech.glide.Glide;

public class SelectedContactsAdapter extends RecyclerView.Adapter<SelectedContactsAdapter.SelectedContactViewHolder> {

    private final List<Friend> selectedFriends;

    public SelectedContactsAdapter(List<Friend> selectedFriends) {
        this.selectedFriends = selectedFriends;
    }

    @NonNull
    @Override
    public SelectedContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_contact, parent, false);
        return new SelectedContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SelectedContactViewHolder holder, int position) {
        Friend friend = selectedFriends.get(position);
        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(friend.getProfile_picture())
                .circleCrop()
                .placeholder(R.drawable.obsmallmen2)
                .into(holder.ivProfile);
    }

    @Override
    public int getItemCount() {
        return selectedFriends.size();
    }

    public static class SelectedContactViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;

        public SelectedContactViewHolder(View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
        }
    }
}
