package com.example.splitaura.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.Homepage;
import com.example.splitaura.ManageFriends;
import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendBalanceAdapter;
import com.example.splitaura.adapters.GroupAdapter;
import com.example.splitaura.models.FriendBalance;
import com.example.splitaura.models.GroupItem;
import com.example.splitaura.viewmodels.GroupViewModel;
import com.example.splitaura.viewmodels.HomeViewModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final String PREFS_NAME = "FriendsCache";
    private static final String KEY_FRIENDS_DATA = "friends_data";
    private static final int MAX_GROUPS_TO_DISPLAY = 2; // Limit to 2 groups
    private static final int MAX_FRIENDS_TO_DISPLAY = 3; // Limit to 3 friends

    private TextView username, viewAllGroups, viewAllFriends;
    private ImageView profilePicture;
    private HomeViewModel homeViewModel;
    private List<GroupItem> groupList;
    private GroupAdapter groupAdapter;
    private RecyclerView groupRecyclerView;
    private RecyclerView friendRecyclerView;
    private FriendBalanceAdapter friendBalanceAdapter;
    private List<FriendBalance> friendBalanceList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private List<String> groupIds;
    private Map<String, ExpenseManager> expenseManagers;
    private Map<String, String> friendNames;
    private Map<String, String> friendProfilePictures;
    private Map<String, Double> friendBalances;
    private SharedPreferences sharedPreferences;
    private Task<?> fetchGroupsTask;
    private Task<?> fetchFriendsTask;
    private List<ListenerRegistration> firestoreListeners;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isDataFresh = false;
    private CardView groupCardView;
    private CardView friendCardView;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firestore and FirebaseAuth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not authenticated. Please log in.", Toast.LENGTH_LONG).show();
            return view;
        }

        // Initialize SharedPreferences for caching
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize collections
        groupIds = new ArrayList<>();
        expenseManagers = new HashMap<>();
        friendNames = new HashMap<>();
        friendProfilePictures = new HashMap<>();
        friendBalances = new HashMap<>();
        firestoreListeners = new ArrayList<>();

        initViews(view);
        initViewModels();
        setupUserInfo();
        setupFriendBalanceAdapter(view);
        fetchGroupData();
        loadCachedFriendsData();
        fetchFriendsData();
        navigatetoGroupFragment();
        navigatetoManageFriends();

        return view;
    }

    private void navigatetoManageFriends() {
        viewAllFriends.setOnClickListener(v -> {
            manageFriends();
        });
        friendCardView.setOnClickListener(v -> {
            manageFriends();
        });
    }

    private void navigatetoGroupFragment() {
        viewAllGroups.setOnClickListener(v -> {
            if (getActivity() instanceof Homepage) {
                ((Homepage) getActivity()).navigateToAddFriendFragment();
            }
        });
        groupCardView.setOnClickListener(v -> {
            if (getActivity() instanceof Homepage) {
                ((Homepage) getActivity()).navigateToAddFriendFragment();
            }
        });
    }

    private void manageFriends() {
        Intent intent = new Intent(getActivity(), ManageFriends.class);
        startActivity(intent);
    }

    private void initViews(View view) {
        groupRecyclerView = view.findViewById(R.id.group_recyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        friendRecyclerView = view.findViewById(R.id.friend_recyclerView);
        friendRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        username = view.findViewById(R.id.username_text);
        profilePicture = view.findViewById(R.id.profile_picture);
        viewAllGroups = view.findViewById(R.id.view_all_groups);
        viewAllFriends = view.findViewById(R.id.btn_view_all_friends);
        groupCardView = view.findViewById(R.id.group_cardview);
        friendCardView = view.findViewById(R.id.friend_cardview);
    }

    private void initViewModels() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    private void setupUserInfo() {
        try {
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
                            .placeholder(R.drawable.obsmallmen2)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(profilePicture);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up user info: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error loading user info", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupFriendBalanceAdapter(View view) {
        friendBalanceList = new ArrayList<>();
        friendBalanceAdapter = new FriendBalanceAdapter(getContext(), friendBalanceList);
        friendRecyclerView.setAdapter(friendBalanceAdapter);
    }

    private void loadCachedFriendsData() {
        try {
            String friendsJson = sharedPreferences.getString(KEY_FRIENDS_DATA, null);
            if (friendsJson != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<FriendBalance>>() {}.getType();
                List<FriendBalance> cachedFriends = gson.fromJson(friendsJson, type);
                if (cachedFriends != null) {
                    friendBalanceList.clear();
                    // Limit to the first 3 friends from the cache
                    friendBalanceList.addAll(cachedFriends.subList(0, Math.min(cachedFriends.size(), MAX_FRIENDS_TO_DISPLAY)));
                    friendBalanceAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded friends data from cache (limited to " + MAX_FRIENDS_TO_DISPLAY + ")");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cached friends data: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error loading cached data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cacheFriendsData() {
        try {
            Gson gson = new Gson();
            // Cache only the first 3 friends
            List<FriendBalance> friendsToCache = new ArrayList<>(friendBalanceList.subList(0, Math.min(friendBalanceList.size(), MAX_FRIENDS_TO_DISPLAY)));
            String friendsJson = gson.toJson(friendsToCache);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_FRIENDS_DATA, friendsJson);
            editor.apply();
            Log.d(TAG, "Cached friends data (limited to " + MAX_FRIENDS_TO_DISPLAY + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error caching friends data: ", e);
        }
    }

    private void fetchFriendsData() {
        // Cancel any existing fetch task
        if (fetchGroupsTask != null && !fetchGroupsTask.isComplete()) {
            fetchGroupsTask.isCanceled(); // Use cancel(true) instead of isCanceled()
            Log.d(TAG, "Canceled previous fetchGroupsTask");
        }

        try {
            fetchGroupsTask = db.collection("groups")
                    .whereArrayContains("memberIds", currentUserId)
                    .get()
                    .addOnSuccessListener(query -> {
                        try {
                            Set<String> uniqueFriendIds = new HashSet<>();
                            groupIds.clear();
                            expenseManagers.clear();

                            for (QueryDocumentSnapshot groupDoc : query) {
                                String groupId = groupDoc.getId();
                                groupIds.add(groupId);
                                expenseManagers.put(groupId, new ExpenseManager(groupId));

                                List<String> memberIds = (List<String>) groupDoc.get("memberIds");
                                if (memberIds != null) {
                                    for (String memberId : memberIds) {
                                        if (!memberId.equals(currentUserId)) {
                                            uniqueFriendIds.add(memberId);
                                        }
                                    }
                                }
                            }

                            if (uniqueFriendIds.isEmpty()) {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "No friends found", Toast.LENGTH_SHORT).show();
                                }
                                friendBalanceList.clear();
                                friendBalanceAdapter.notifyDataSetChanged();
                                cacheFriendsData();
                                return;
                            }

                            fetchFriendDetails(new ArrayList<>(uniqueFriendIds));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing groups data: ", e);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error processing friends data", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching groups: ", e);
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Error fetching friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error initiating fetchFriendsData: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error fetching friends", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchFriendDetails(List<String> friendIds) {
        // Cancel any existing fetch task
        if (fetchFriendsTask != null && !fetchFriendsTask.isComplete()) {
            fetchFriendsTask.isCanceled(); // Use cancel(true) instead of isCanceled()
            Log.d(TAG, "Canceled previous fetchFriendsTask");
        }

        try {
            fetchFriendsTask = db.collection("users")
                    .whereIn("uid", friendIds)
                    .get()
                    .addOnSuccessListener(query -> {
                        try {
                            friendNames.clear();
                            friendProfilePictures.clear();
                            friendBalances.clear();

                            for (String friendId : friendIds) {
                                friendBalances.put(friendId, 0.0);
                            }

                            for (QueryDocumentSnapshot userDoc : query) {
                                String friendId = userDoc.getId();
                                String fname = userDoc.getString("fname");
                                String lname = userDoc.getString("lname");
                                String profilePicture = userDoc.getString("profile_picture");

                                if (fname == null || lname == null) {
                                    continue;
                                }

                                String friendName = fname + " " + lname.charAt(0) + ".";
                                friendNames.put(friendId, friendName);
                                friendProfilePictures.put(friendId, profilePicture != null ? profilePicture : "");
                            }

                            calculateFriendBalances();
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing friend details: ", e);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error processing friend details", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching friend details: ", e);
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Error fetching friend details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error initiating fetchFriendDetails: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error fetching friend details", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void calculateFriendBalances() {
        for (String groupId : groupIds) {
            ExpenseManager expenseManager = expenseManagers.get(groupId);
            if (expenseManager != null) {
                try {
                    // Use Fragment lifecycle instead of view lifecycle
                    expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), userDataMap -> {
                        try {
                            if (userDataMap == null) {
                                return;
                            }

                            ExpenseManager.UserExpenseData currentUserData = userDataMap.get(currentUserId);
                            if (currentUserData == null) {
                                return;
                            }

                            for (String friendId : friendNames.keySet()) {
                                ExpenseManager.UserExpenseData friendData = userDataMap.get(friendId);
                                if (friendData != null) {
                                    double currentNetBalance = currentUserData.netBalance;
                                    double friendNetBalance = friendData.netBalance;
                                    double balanceWithFriend = currentNetBalance - friendNetBalance;

                                    double existingBalance = friendBalances.getOrDefault(friendId, 0.0);
                                    friendBalances.put(friendId, existingBalance + balanceWithFriend);
                                }
                            }

                            updateFriendBalanceList();
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user expense data: ", e);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error calculating balances", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error observing user expense data: ", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error calculating balances", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void updateFriendBalanceList() {
        try {
            friendBalanceList.clear();
            for (Map.Entry<String, Double> entry : friendBalances.entrySet()) {
                String friendId = entry.getKey();
                double balance = entry.getValue();
                String friendName = friendNames.get(friendId);
                String profilePicture = friendProfilePictures.get(friendId);
                if (friendName != null) {
                    friendBalanceList.add(new FriendBalance(friendName, balance, profilePicture));
                }
            }
            // Limit to the first 3 friends
            if (friendBalanceList.size() > MAX_FRIENDS_TO_DISPLAY) {
                friendBalanceList = new ArrayList<>(friendBalanceList.subList(0, MAX_FRIENDS_TO_DISPLAY));
            }
            friendBalanceAdapter.notifyDataSetChanged();
            cacheFriendsData();
            isDataFresh = true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating friend balance list: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error updating friend list", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchGroupData() {
        try {
            groupList = new ArrayList<>();
            groupAdapter = new GroupAdapter(getContext(), groupList);
            groupRecyclerView.setAdapter(groupAdapter);

            GroupViewModel groupViewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);
            groupViewModel.getGroupList().observe(getViewLifecycleOwner(), groups -> {
                // Limit to the first 2 groups
                List<GroupItem> limitedGroups = groups.size() > MAX_GROUPS_TO_DISPLAY
                        ? new ArrayList<>(groups.subList(0, MAX_GROUPS_TO_DISPLAY))
                        : groups;
                groupAdapter.setGroupList(limitedGroups);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error fetching group data: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error loading groups", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel Firestore tasks
        if (fetchGroupsTask != null && !fetchGroupsTask.isComplete()) {
            fetchGroupsTask.isCanceled(); // Use cancel(true) instead of isCanceled()
        }
        if (fetchFriendsTask != null && !fetchFriendsTask.isComplete()) {
            fetchFriendsTask.isCanceled(); // Use cancel(true) instead of isCanceled()
        }

        // Remove Firestore listeners
        for (ListenerRegistration listener : firestoreListeners) {
            listener.remove();
        }
        firestoreListeners.clear();

        // Remove LiveData observers
        for (ExpenseManager manager : expenseManagers.values()) {
            manager.getUserExpenseData().removeObservers(this);
            manager.removeListener();
        }

        // Clear handler callbacks
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Ensure all resources are released
        expenseManagers.clear();
        groupIds.clear();
        friendNames.clear();
        friendProfilePictures.clear();
        friendBalances.clear();
    }
}