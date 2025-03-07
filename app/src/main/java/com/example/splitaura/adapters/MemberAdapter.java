
package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.models.Friend;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Friend> membersList;

    public MemberAdapter(List<Friend> membersList) {
        this.membersList = membersList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_layout, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Friend friend = membersList.get(position);
        holder.friendNameTextView.setText(friend.getFname() + " " + friend.getLname());
        holder.friendEmailTextView.setText(friend.getEmail());
        holder.menuDots.setOnClickListener(v -> showPopupMenu(v, friend, position));

        Glide.with(holder.itemView.getContext())
                .load(friend.getProfile_picture())
                .circleCrop()
                .into(holder.friendProfileImageView);
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    public void updateData(List<Friend> newFriends) {
        this.membersList = newFriends;
        notifyDataSetChanged();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView friendProfileImageView;
        TextView friendNameTextView;
        TextView friendEmailTextView;
        TextView menuDots;


        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            friendProfileImageView = itemView.findViewById(R.id.ivProfile);
            friendNameTextView = itemView.findViewById(R.id.tvName);
            friendEmailTextView = itemView.findViewById(R.id.tvEmail);
            menuDots = itemView.findViewById(R.id.menu_dots);
            menuDots.setVisibility(View.VISIBLE);
        }
    }

    private void showPopupMenu(View view, Friend friend, int position) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add("View Profile");
        popup.getMenu().add("Remove Member");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String action = item.getTitle().toString();
                switch (action) {
                    case "View Profile":
                        Toast.makeText(view.getContext(), "View Profile for " + friend.getFname(), Toast.LENGTH_SHORT).show();
                        break;
                    case "Remove Member":
                        Toast.makeText(view.getContext(), "Remove Member " + friend.getFname(), Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });

        popup.show();
    }
}
