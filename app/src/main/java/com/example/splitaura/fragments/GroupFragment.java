package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class GroupFragment extends Fragment {

    GroupAdapter groupAdapter;
    ImageView profilePic;
    TextView sizeofGroup;
    LinearLayout placeholder;
    RecyclerView groupRecyclerView;

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
        placeholder = view.findViewById(R.id.group_placeholder2);

        // Initialize group list and
        List<GroupItem> groupList;
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(getContext(), groupList);
        groupRecyclerView.setAdapter(groupAdapter);
    }

    // Observe group data from the shared GroupViewModel and update the count
    private void observeGroupData() {
        GroupViewModel groupViewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);
        groupViewModel.getGroupList().observe(getViewLifecycleOwner(), groups -> {
            groupAdapter.setGroupList(groups);
            sizeofGroup.setText(getString(R.string.you_are_in) + groups.size() + " " + getString(R.string.groups));

            // Show/hide placeholder based on group count
            if (groups.isEmpty()) {
                placeholder.setVisibility(View.VISIBLE);
                groupRecyclerView.setVisibility(View.GONE);
            } else {
                groupRecyclerView.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
            }
        });
    }


    private void setupProfilePic() {
        HomeViewModel homeViewModel;
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.getProfilePicUrl().observe(getViewLifecycleOwner(), picUrl -> {
            if (picUrl != null && !picUrl.isEmpty()) {
                Glide.with(this)
                        .load(picUrl)
                        .placeholder(R.drawable.obsmallmen2)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(profilePic);
            }
        });
    }

    private void setupAddGroupButton(View view) {
        MaterialButton addGroup;
        addGroup = view.findViewById(R.id.activity_add_group);
        addGroup.setOnClickListener(v -> {

            Intent intent = new Intent(getActivity(), AddGroupActivity.class);
            startActivity(intent);
        });
    }
}
