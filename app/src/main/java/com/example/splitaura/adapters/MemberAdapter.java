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

import com.example.splitaura.R;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<MemberData> members;

    public MemberAdapter(List<MemberData> members) {
        this.members = members;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberData member = members.get(position);
        holder.tvMemberName.setText(member.getName());
        holder.tvMemberEmail.setText(member.getEmail());
        holder.ivProfilePicture.setImageResource(member.getProfileResourceId());

        holder.menuDots.setOnClickListener(v -> showPopupMenu(v, member, position));
    }

    @Override
    public int getItemCount() {
        return members != null ? members.size() : 0;
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePicture;
        TextView tvMemberName, tvMemberEmail, menuDots;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.iv_profile_picture);
            tvMemberName = itemView.findViewById(R.id.tv_member_name);
            tvMemberEmail = itemView.findViewById(R.id.tv_member_email);
            menuDots = itemView.findViewById(R.id.menu_dots);
        }
    }

    public void updateMembers(List<MemberData> newMembers) {
        members = newMembers;
        notifyDataSetChanged();
    }

    private void showPopupMenu(View view, MemberData member, int position) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenu().add("View Profile");
        popup.getMenu().add("Remove Member");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String action = item.getTitle().toString();
                switch (action) {
                    case "View Profile":
                        Toast.makeText(view.getContext(), "View Profile for " + member.getName(), Toast.LENGTH_SHORT).show();
                        break;
                    case "Remove Member":
                        Toast.makeText(view.getContext(), "Remove Member " + member.getName(), Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });

        popup.show();
    }
}