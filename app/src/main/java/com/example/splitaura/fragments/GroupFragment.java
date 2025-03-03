package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.splitaura.AddGroupActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.GroupAdapter;
import com.example.splitaura.models.GroupItem;
import com.example.splitaura.viewmodels.GroupViewModel;
import com.example.splitaura.viewmodels.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class GroupFragment extends Fragment {

    private Button addGroup;
    private RecyclerView groupRecyclerView;
    private GroupAdapter groupAdapter;
    private List<GroupItem> groupList;
    private ImageView profilePic;
    private HomeViewModel homeViewModel;
    private TextView sizeofGroup;

    public GroupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);
        initViews(view);
        setupProfilePic();
        observeGroupData();
        setupAddGroupButton(view);
        return view;
    }

    private void initViews(View view) {
        groupRecyclerView = view.findViewById(R.id.group_recyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        profilePic = view.findViewById(R.id.profile_picture);
        sizeofGroup = view.findViewById(R.id.number_of_groups);  // Initialize the TextView here

        // Initialize group list and adapter
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(getContext(), groupList);
        groupRecyclerView.setAdapter(groupAdapter);
    }

    // Observe group data from the shared GroupViewModel and update the count
    private void observeGroupData() {
        GroupViewModel groupViewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);
        groupViewModel.getGroupList().observe(getViewLifecycleOwner(), groups -> {
            groupAdapter.setGroupList(groups);
            sizeofGroup.setText("You are in " + groups.size() + " groups");
        });
    }
    private void setupProfilePic() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.getProfilePicUrl().observe(getViewLifecycleOwner(), picUrl -> {
            if (picUrl != null && !picUrl.isEmpty()) {
                Glide.with(this)
                        .load(picUrl)
                        .placeholder(R.drawable.obmensmall)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(profilePic);
            }
        });
    }

    private void setupAddGroupButton(View view) {
        addGroup = view.findViewById(R.id.activity_add_group);
        addGroup.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddGroupActivity.class);
            startActivity(intent);
        });
    }
}
