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
import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendBalanceAdapter;
import com.example.splitaura.adapters.GroupAdapter;
import com.example.splitaura.models.FriendBalance;
import com.example.splitaura.models.GroupItem;
import com.example.splitaura.viewmodels.HomeViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView username;
    private ImageView profilePicture;
    private HomeViewModel homeViewModel;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView groupRecyclerView = view.findViewById(R.id.group_recyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RecyclerView friendRecycleView = view.findViewById(R.id.friend_recyclerView);
        friendRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));

        username = view.findViewById(R.id.username_text);
        profilePicture = view.findViewById(R.id.profile_picture);

        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Observe and update UI with username
        homeViewModel.getUsername().observe(getViewLifecycleOwner(), name -> {
            if (name != null && !name.isEmpty()) {
                username.setText(name);  // Ensure username is set correctly
            } else {
                username.setText("Hello User\nManage your finance easily");  // Fallback if empty
            }
        });


        // Observe and update profile picture (cached)
        homeViewModel.getProfilePicUrl().observe(getViewLifecycleOwner(), picUrl -> {
            if (picUrl != null && !picUrl.isEmpty()) {
                Glide.with(this)
                        .load(picUrl)
                        .placeholder(R.drawable.obmensmall) // Default image while loading
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache image for offline use
                        .into(profilePicture);
            }
        });

        // Sample Group Data (Static)
        List<GroupItem> groupList = new ArrayList<>();
        groupList.add(new GroupItem(
                "Office Trip", "May 23, 2024", 9235.34,
                R.drawable.ic_group,
                Arrays.asList(R.drawable.image, R.drawable.obwomens, R.drawable.obmensmall),
                12));

        groupList.add(new GroupItem(
                "Weekend Party", "Feb 10, 2024", 4850.50,
                R.drawable.ic_group,
                Arrays.asList(R.drawable.obmensmall, R.drawable.image, R.drawable.obwomensmall),
                8));

        GroupAdapter adapter = new GroupAdapter(getContext(), groupList);
        groupRecyclerView.setAdapter(adapter);

        // Sample Friend Balance Data (Static)
        List<FriendBalance> friendBalanceList = new ArrayList<>();
        friendBalanceList.add(new FriendBalance("Vasu Z.", -45.69, R.drawable.image));
        friendBalanceList.add(new FriendBalance("Sahil H.", 120.50, R.drawable.obwomens));
        friendBalanceList.add(new FriendBalance("Tushar C.", -230.75, R.drawable.obmensmall));
        friendBalanceList.add(new FriendBalance("Bhargav Z.", 90.00, R.drawable.obwomensmall));

        FriendBalanceAdapter friendBalanceAdapter = new FriendBalanceAdapter(getContext(), friendBalanceList);
        friendRecycleView.setAdapter(friendBalanceAdapter);

        return view;
    }
}
