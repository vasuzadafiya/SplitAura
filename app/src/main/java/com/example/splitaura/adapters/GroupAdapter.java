package com.example.splitaura.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.models.GroupItem;

import java.util.List;
import java.util.Random;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private final Context context;
    private List<GroupItem> groupList;
    private final Random random = new Random();

    // Define a static array for the background colors.
    private static final int[] COLORS = {
            Color.parseColor("#F4FFEB"),
            Color.parseColor("#EBF8FF"),
            Color.parseColor("#F1F1F1"),
            Color.parseColor("#F4FFEB"),
            Color.parseColor("#FFEBEB"),
    };

    public GroupAdapter(Context context, List<GroupItem> groupList) {
        this.context = context;
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.group_item_layout, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupItem group = groupList.get(position);

        holder.groupName.setText(group.getGroupName());
        holder.groupDate.setText(group.getDate());
        holder.groupAmount.setText("₹ " + String.format("%.2f", group.getAmount()));

        // Load group image from URL using Glide
        Glide.with(context)
                .load(group.getGroupImageUrl())
                .placeholder(R.drawable.ic_group)
                .circleCrop()
                .into(holder.groupImage);

        // Load member images dynamically using Glide from URLs
        List<String> memberImages = group.getMemberImagesUrls();
        if (!memberImages.isEmpty()) {
            holder.member1.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(memberImages.get(0))
                    .placeholder(R.drawable.obmensmall)
                    .circleCrop()
                    .into(holder.member1);
        }
        if (memberImages.size() > 1) {
            holder.member2.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(memberImages.get(1))
                    .placeholder(R.drawable.obwomens)
                    .circleCrop()
                    .into(holder.member2);
        }
        if (memberImages.size() > 2) {
            holder.member3.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(memberImages.get(2))
                    .placeholder(R.drawable.obwomensmall)
                    .circleCrop()
                    .into(holder.member3);
        }
        if (group.getMoreMembersCount() > 0) {
            holder.moreMembers.setVisibility(View.VISIBLE);
            holder.moreMembers.setText("+" + group.getMoreMembersCount() + " more");
        }
        // Set a random background color for the CardView
        int randomColor = COLORS[random.nextInt(COLORS.length)];
        holder.cardView.setCardBackgroundColor(randomColor);

        holder.viewDetails.setOnClickListener(v -> {
            // Handle View Details Click as needed
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    // Setter method to update the list
    public void setGroupList(List<GroupItem> groupList) {
        this.groupList = groupList;
        notifyDataSetChanged();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView groupName;
        TextView groupDate;
        TextView groupAmount;
        TextView moreMembers;
        ImageView groupImage;
        ImageView member1;
        ImageView member2;
        ImageView member3;
        Button viewDetails;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            groupName = itemView.findViewById(R.id.group_name);
            groupDate = itemView.findViewById(R.id.group_date);
            groupAmount = itemView.findViewById(R.id.group_amount);
            groupImage = itemView.findViewById(R.id.group_image);
            member1 = itemView.findViewById(R.id.member1);
            member2 = itemView.findViewById(R.id.member2);
            member3 = itemView.findViewById(R.id.member3);
            moreMembers = itemView.findViewById(R.id.more_members);
            viewDetails = itemView.findViewById(R.id.view_details_btn);
        }
    }
}
