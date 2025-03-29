package com.example.splitaura.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.example.splitaura.utils.Utill;
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
    private static final int MAX_GROUPS_TO_DISPLAY = 2;
    private static final int MAX_FRIENDS_TO_DISPLAY = 3;

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
    private ImageButton pay;
    private ImageButton request;
    private ImageButton addIncome;
    private ImageButton withdraw;
    private ImageButton history;
    private TextView savingTextView;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;


        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        groupIds = new ArrayList<>();
        expenseManagers = new HashMap<>();
        friendNames = new HashMap<>();
        friendProfilePictures = new HashMap<>();
        friendBalances = new HashMap<>();
        firestoreListeners = new ArrayList<>();
        pay = view.findViewById(R.id.btn_pay);
        request = view.findViewById(R.id.btn_request_amount);
        withdraw = view.findViewById(R.id.btn_withdraw_saving);
        history = view.findViewById(R.id.btn_history);
        addIncome = view.findViewById(R.id.btn_add_income);
        savingTextView = view.findViewById(R.id.txt_saving);

        initViews(view);
        initViewModels();
        setupUserInfo();
        setupFriendBalanceAdapter(view);
        fetchGroupData();
        loadCachedFriendsData();
        fetchFriendsData();
        manageClick();

        return view;
    }

    private void manageClick() {
        pay.setOnClickListener(v -> navigateTopaymentFragment());
        request.setOnClickListener(v -> navigateTopaymentFragment());
        history.setOnClickListener(v -> navigateTopaymentFragment());
        addIncome.setOnClickListener(v -> updateSaving());
        withdraw.setOnClickListener(v -> updateSaving());
        getSaving();


    }

    private void updateSaving() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.manage_saving, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();

        EditText savingedt = dialogView.findViewById(R.id.saving_edit_text);
        Button update = dialogView.findViewById(R.id.updateButton);

        update.setOnClickListener(v -> {
            String inputText = savingedt.getText().toString().trim();

            // Check if input is not empty and is a valid number
            if (!inputText.isEmpty()) {
                try {
                    double saving = Double.parseDouble(inputText);

                    // Update the database with the new saving amount
                    db.collection("users").document(currentUserId)
                            .update("saving", saving)
                            .addOnSuccessListener(task -> {
                                Utill.showToast(getActivity(), "Saving Updated");
                                dialog.dismiss(); // Dismiss the dialog after successful update
                            })
                            .addOnFailureListener(e -> {
                                Utill.showToast(getActivity(), "Something went wrong with saving");
                            });
                } catch (NumberFormatException e) {
                    Utill.showToast(getActivity(), "Please enter a valid number");
                }
            } else {
                Utill.showToast(getActivity(), "Enter a valid saving amount");
            }
        });
    }

    private void getSaving() {
        homeViewModel.getSaving().observe(getViewLifecycleOwner(), saving -> {
            savingTextView.setText(String.format("%.2f", saving));
        });
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

        viewAllGroups.setOnClickListener(v -> navigateToGroupFragment());
        groupCardView.setOnClickListener(v -> navigateToGroupFragment());
        viewAllFriends.setOnClickListener(v -> navigateToManageFriends());
        friendCardView.setOnClickListener(v -> navigateToManageFriends());
    }

    private void navigateToGroupFragment() {
        if (getActivity() instanceof Homepage) {
            ((Homepage) getActivity()).navigateToAddFriendFragment();
        }
    }

    private void navigateTopaymentFragment() {
        if (getActivity() instanceof Homepage) {
            ((Homepage) getActivity()).navigateToPaymentFragments();
        }
    }

    private void navigateToManageFriends() {
        Intent intent = new Intent(getActivity(), ManageFriends.class);
        startActivity(intent);
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
                Type type = new TypeToken<List<FriendBalance>>() {
                }.getType();
                List<FriendBalance> cachedFriends = gson.fromJson(friendsJson, type);
                if (cachedFriends != null) {
                    friendBalanceList.clear();
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
        if (fetchGroupsTask != null && !fetchGroupsTask.isComplete()) {
            fetchGroupsTask.isCanceled();
            Log.d(TAG, "Canceled previous fetchGroupsTask");
        }

        fetchGroupsTask = db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
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
                        calculateFriendBalances();
                        return;
                    }

                    fetchFriendDetails(new ArrayList<>(uniqueFriendIds));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching groups: ", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error fetching friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchFriendDetails(List<String> friendIds) {
        if (fetchFriendsTask != null && !fetchFriendsTask.isComplete()) {
            fetchFriendsTask.isCanceled();
            Log.d(TAG, "Canceled previous fetchFriendsTask");
        }

        fetchFriendsTask = db.collection("users")
                .whereIn("uid", friendIds)
                .get()
                .addOnSuccessListener(query -> {
                    if (!isAdded() || getView() == null) {
                        Log.d(TAG, "Skipping fetchFriendDetails callback: Fragment not active");
                        return;
                    }

                    friendNames.clear();
                    friendProfilePictures.clear();
                    friendBalances.clear();

                    for (String friendId : friendIds) {
                        friendBalances.put(friendId, 0.0); // Initialize balance
                    }

                    for (QueryDocumentSnapshot userDoc : query) {
                        String friendId = userDoc.getId();
                        String fname = userDoc.getString("fname");
                        String lname = userDoc.getString("lname");
                        String profilePicture = userDoc.getString("profile_picture");

                        if (fname == null || lname == null) continue;

                        String friendName = fname + " " + lname.charAt(0) + ".";
                        friendNames.put(friendId, friendName);
                        friendProfilePictures.put(friendId, profilePicture != null ? profilePicture : "");
                    }

                    calculateFriendBalances();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching friend details: ", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error fetching friend details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateFriendBalances() {
        if (!isAdded() || getView() == null) {
            Log.d(TAG, "Skipping calculateFriendBalances: View not available");
            return;
        }

        Map<String, ExpenseManager.UserExpenseData> aggregatedData = new HashMap<>();
        int groupCount = expenseManagers.size();
        if (groupCount == 0) {
            updateFriendBalanceList();
            return;
        }

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(groupCount);
        Map<String, Boolean> groupDataReceived = new HashMap<>();

        for (String groupId : groupIds) {
            ExpenseManager expenseManager = expenseManagers.get(groupId);
            if (expenseManager != null) {
                expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), userDataMap -> {
                    if (!isAdded() || getView() == null) {
                        Log.d(TAG, "Skipping LiveData update for group " + groupId + ": View not available");
                        return;
                    }

                    if (userDataMap != null && !groupDataReceived.containsKey(groupId)) {
                        synchronized (aggregatedData) {
                            for (Map.Entry<String, ExpenseManager.UserExpenseData> entry : userDataMap.entrySet()) {
                                String userId = entry.getKey();
                                ExpenseManager.UserExpenseData newData = entry.getValue();
                                ExpenseManager.UserExpenseData existingData = aggregatedData.get(userId);
                                if (existingData == null) {
                                    aggregatedData.put(userId, new ExpenseManager.UserExpenseData(
                                            newData.totalSpent, newData.totalShare, newData.netBalance,
                                            new HashMap<>(newData.debts), new HashMap<>(newData.credits)));
                                } else {
                                    existingData.totalSpent += newData.totalSpent;
                                    existingData.totalShare += newData.totalShare;
                                    existingData.netBalance = existingData.totalSpent - existingData.totalShare;
                                    for (Map.Entry<String, Double> debt : newData.debts.entrySet()) {
                                        existingData.debts.put(debt.getKey(),
                                                existingData.debts.getOrDefault(debt.getKey(), 0.0) + debt.getValue());
                                    }
                                    for (Map.Entry<String, Double> credit : newData.credits.entrySet()) {
                                        existingData.credits.put(credit.getKey(),
                                                existingData.credits.getOrDefault(credit.getKey(), 0.0) + credit.getValue());
                                    }
                                }
                            }
                            groupDataReceived.put(groupId, true);
                        }
                        latch.countDown();
                    }
                });
            } else {
                latch.countDown();
            }
        }

        new Thread(() -> {
            try {
                latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                handler.post(() -> {
                    if (!isAdded() || getView() == null) {
                        Log.d(TAG, "Skipping balance update: Fragment not active");
                        return;
                    }

                    if (aggregatedData.isEmpty()) {
                        updateFriendBalanceList();
                        return;
                    }

                    ExpenseManager.UserExpenseData currentUserData = aggregatedData.get(currentUserId);
                    if (currentUserData == null) {
                        updateFriendBalanceList();
                        return;
                    }

                    friendBalances.clear();
                    for (String friendId : friendNames.keySet()) {
                        ExpenseManager.UserExpenseData friendData = aggregatedData.get(friendId);
                        double balanceWithFriend = 0.0;
                        if (friendData != null) {
                            double debtToFriend = friendData.credits.getOrDefault(currentUserId, 0.0);
                            double creditFromFriend = currentUserData.credits.getOrDefault(friendId, 0.0);
                            balanceWithFriend = creditFromFriend - debtToFriend;
                            Log.d(TAG, "Friend " + friendId + " balance: " + balanceWithFriend +
                                    " (debtTo: " + debtToFriend + ", creditFrom: " + creditFromFriend + ")");
                        }
                        friendBalances.put(friendId, balanceWithFriend);
                    }

                    updateFriendBalanceList();
                });
            } catch (InterruptedException e) {
                Log.e(TAG, "Error waiting for expense data: ", e);
                handler.post(() -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error calculating balances", Toast.LENGTH_SHORT).show();
                    }
                    updateFriendBalanceList();
                });
            }
        }).start();
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
        if (fetchGroupsTask != null && !fetchGroupsTask.isComplete()) {
            fetchGroupsTask.isCanceled();
        }
        if (fetchFriendsTask != null && !fetchFriendsTask.isComplete()) {
            fetchFriendsTask.isCanceled();
        }

        for (ListenerRegistration listener : firestoreListeners) {
            listener.remove();
        }
        firestoreListeners.clear();

        for (ExpenseManager manager : expenseManagers.values()) {
            manager.getUserExpenseData().removeObservers(getViewLifecycleOwner());
            manager.removeListener();
        }

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        expenseManagers.clear();
        groupIds.clear();
        friendNames.clear();
        friendProfilePictures.clear();
        friendBalances.clear();
    }
}