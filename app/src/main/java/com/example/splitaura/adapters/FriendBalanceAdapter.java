package com.example.splitaura.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.FriendBalance;

import java.util.List;

public class FriendBalanceAdapter extends RecyclerView.Adapter<FriendBalanceAdapter.ViewHolder> {

    private Context context;
    private List<FriendBalance> friendBalanceList;

    public FriendBalanceAdapter(Context context, List<FriendBalance> friendBalanceList) {
        this.context = context;
        this.friendBalanceList = friendBalanceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendBalance friend = friendBalanceList.get(position);

        holder.friendName.setText(friend.getName());
        holder.friendBalance.setText(String.format("₹%.2f", friend.getBalance()));
        holder.profileImage.setImageResource(friend.getProfileImage());

        // Set color for negative balance (red) and positive balance (green)
        if (friend.getBalance() < 0) {
            holder.friendBalance.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.friendBalance.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    @Override
    public int getItemCount() {
        return friendBalanceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView friendName;
        TextView friendBalance;
        ImageView profileImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.userNameTextView);
            friendBalance = itemView.findViewById(R.id.sendRequestButton);
            profileImage = itemView.findViewById(R.id.userProfileImageView);
        }
    }
}

