package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.splitaura.Homepage;
import com.example.splitaura.ManageFriends;
import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendBalanceAdapter;
import com.example.splitaura.adapters.GroupAdapter;
import com.example.splitaura.models.FriendBalance;
import com.example.splitaura.models.GroupItem;
import com.example.splitaura.viewmodels.GroupViewModel;
import com.example.splitaura.viewmodels.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView username, viewAllGroups;
    private ImageView profilePicture;
    private HomeViewModel homeViewModel;
    private List<GroupItem> groupList;
    private GroupAdapter groupAdapter;
    private RecyclerView groupRecyclerView;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        initViewModels();
        setupUserInfo();
        setupFriendBalanceAdapter(view);
        fetchGroupData();
        navigatetoGroupGragment();

        return view;
    }

    private void navigatetoGroupGragment() {
        viewAllGroups.setOnClickListener(v -> {
            if (getActivity() instanceof Homepage) {
                ((Homepage) getActivity()).navigateToAddFriendFragment();
            }
        });
    }

    // Initialize view references and set up RecyclerViews
    private void initViews(View view) {
        groupRecyclerView = view.findViewById(R.id.group_recyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        username = view.findViewById(R.id.username_text);
        profilePicture = view.findViewById(R.id.profile_picture);
        viewAllGroups = view.findViewById(R.id.view_all_groups);
    }

    // Initialize ViewModels
    private void initViewModels() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    // Observe LiveData for user info (username and profile picture)
    private void setupUserInfo() {
        homeViewModel.getUsername().observe(getViewLifecycleOwner(), name -> {
            if (name != null && !name.isEmpty()) {
                username.setText(name);
            } else {
                username.setText("Hello User\nManage your finance easily");
            }
        });

        homeViewModel.getProfilePicUrl().observe(getViewLifecycleOwner(), picUrl -> {
            if (picUrl != null && !picUrl.isEmpty()) {
                Glide.with(this)
                        .load(picUrl)
                        .placeholder(R.drawable.obmensmall)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(profilePicture);
            }
        });
    }

    // Set up the Friend Balance adapter with sample data
    private void setupFriendBalanceAdapter(View view) {
        RecyclerView friendRecycleView = view.findViewById(R.id.friend_recyclerView);
        friendRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<FriendBalance> friendBalanceList = new ArrayList<>();
        friendBalanceList.add(new FriendBalance("Vasu Z.", -45.69, R.drawable.image));
        friendBalanceList.add(new FriendBalance("Sahil H.", 120.50, R.drawable.obwomens));
        friendBalanceList.add(new FriendBalance("Tushar C.", -230.75, R.drawable.obmensmall));
        friendBalanceList.add(new FriendBalance("Bhargav Z.", 90.00, R.drawable.obwomensmall));

        FriendBalanceAdapter friendBalanceAdapter = new FriendBalanceAdapter(getContext(), friendBalanceList);
        friendRecycleView.setAdapter(friendBalanceAdapter);
    }

    // Fetch groups using a shared GroupViewModel and update the adapter
    private void fetchGroupData() {
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(getContext(), groupList);
        groupRecyclerView.setAdapter(groupAdapter);

        GroupViewModel groupViewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);
        groupViewModel.getGroupList().observe(getViewLifecycleOwner(), groups -> {
            groupAdapter.setGroupList(groups);
        });
    }
}
