package com.example.splitaura.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.models.Friend;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private final List<Friend> friends;
    private final List<Friend> selectedFriends;
    private final OnItemClickListener listener;

    public ContactsAdapter(List<Friend> friends, List<Friend> selectedFriends, OnItemClickListener listener) {
        this.friends = friends;
        this.selectedFriends = selectedFriends;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_item_layout, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {
        Friend friend = friends.get(position);
        boolean isSelected = selectedFriends.contains(friend);
        holder.bind(friend, listener, isSelected);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    // Update the adapter's list and refresh
    public void updateList(List<Friend> newList) {
        friends.clear();
        friends.addAll(newList);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(Friend friend);
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName;
        TextView tvEmail;

        public ContactViewHolder(View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
        }

        public void bind(final Friend friend, final OnItemClickListener listener, boolean isSelected) {
            tvName.setText(friend.getFullName());
            tvEmail.setText(friend.getEmail());
            Glide.with(itemView.getContext())
                    .load(friend.getProfile_picture())
                    .circleCrop()
                    .placeholder(R.drawable.obmensmall)
                    .into(ivProfile);
            // Change background color using CardView if available
            CardView cardView = itemView.findViewById(R.id.cardView);
            if (cardView != null) {
                // Use the default card color (e.g., Color.WHITE) or your theme's background color
                cardView.setCardBackgroundColor(isSelected ? Color.LTGRAY : Color.WHITE);
            } else {
                itemView.setBackgroundColor(isSelected ? Color.LTGRAY : Color.TRANSPARENT);
            }

            itemView.setOnClickListener(view -> listener.onItemClick(friend));
        }
    }
}
