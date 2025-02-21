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

import com.example.splitaura.R;
import com.example.splitaura.models.GroupItem;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private Context context;
    private List<GroupItem> groupList;

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
        holder.groupImage.setImageResource(group.getGroupImage());

        // Set member images dynamically
        List<Integer> memberImages = group.getMemberImages();
        holder.member1.setImageResource(memberImages.get(0));
        holder.member2.setImageResource(memberImages.get(1));
        holder.member3.setImageResource(memberImages.get(2));

        holder.moreMembers.setText("+" + group.getMoreMembersCount() + " more");

        // Set random background color for CardView
        int[] colors = {
                Color.parseColor("#F4FFEB"),
                Color.parseColor("#EBF8FF"),
                Color.parseColor("#F1F1F1"),
                Color.parseColor("#F4FFEB"),
                Color.parseColor("#FFEBEB"),
        };

        AtomicReference<Random> random = new AtomicReference<>(new Random());
        int randomColor = colors[random.get().nextInt(colors.length)];
        holder.cardView.setCardBackgroundColor(randomColor); // Set background color to CardView

        holder.viewDetails.setOnClickListener(v -> {
            // Handle View Details Click
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
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
