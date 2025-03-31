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
import android.widget.LinearLayout;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final String PREFS_NAME = "FriendsCache";
    private static final String KEY_FRIENDS_DATA = "friends_data";
    private static final int MAX_GROUPS_TO_DISPLAY = 2;
    private static final int MAX_FRIENDS_TO_DISPLAY = 3;

    // Firestore listener registrations to remove when needed
    private final List<ListenerRegistration> firestoreListeners = new ArrayList<>();
    private ImageView profilePicture;
    // Firebase Tasks (note: Firebase Tasks cannot really be canceled, so we simply reset our references)
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView username;
    private TextView viewAllGroups;
    private TextView viewAllFriends;
    private RecyclerView groupRecyclerView;
    private RecyclerView friendRecyclerView;
    private TextView savingTextView;
    private CardView groupCardView;
    private CardView friendCardView;
    private FriendBalanceAdapter friendBalanceAdapter;
    private List<FriendBalance> friendBalanceList;
    private HomeViewModel homeViewModel;
    private ImageButton pay;
    private ImageButton request;
    private ImageButton addIncome;
    private ImageButton withdraw;

    private FirebaseFirestore db;
    private String currentUserId;
    private ImageButton history;
    private LinearLayout savingLayout;
    private Map<String, ExpenseManager> expenseManagers;
    private Map<String, String> friendNames;
    private Map<String, String> friendProfilePictures;
    private Map<String, Double> friendBalances;
    private SharedPreferences sharedPreferences;
    // Data structures for groups and friends
    private List<String> groupIds;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth;
        auth = FirebaseAuth.getInstance();
        currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize collections
        groupIds = new ArrayList<>();
        expenseManagers = new HashMap<>();
        friendNames = new HashMap<>();
        friendProfilePictures = new HashMap<>();
        friendBalances = new HashMap<>();

        // Initialize views and ViewModels
        initViews(rootView);
        initViewModels();
        setupUserInfo();
        setupFriendBalanceAdapter();
        fetchGroupData();
        loadCachedFriendsData();
        fetchFriendsData();
        setupClickListeners();

        return rootView;
    }

    private void initViews(View view) {
        // RecyclerViews and adapters
        groupRecyclerView = view.findViewById(R.id.group_recyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendRecyclerView = view.findViewById(R.id.friend_recyclerView);
        friendRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Other UI elements
        savingLayout = view.findViewById(R.id.saving_layout);
        username = view.findViewById(R.id.username_text);
        profilePicture = view.findViewById(R.id.profile_picture);
        viewAllGroups = view.findViewById(R.id.view_all_groups);
        viewAllFriends = view.findViewById(R.id.btn_view_all_friends);
        groupCardView = view.findViewById(R.id.group_cardview);
        friendCardView = view.findViewById(R.id.friend_cardview);
        savingTextView = view.findViewById(R.id.txt_saving);
        pay = view.findViewById(R.id.btn_pay);
        request = view.findViewById(R.id.btn_request_amount);
        withdraw = view.findViewById(R.id.btn_withdraw_saving);
        history = view.findViewById(R.id.btn_history);
        addIncome = view.findViewById(R.id.btn_add_income);
    }

    private void initViewModels() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    private void setupUserInfo() {
        // Observe and update username
        homeViewModel.getUsername().observe(getViewLifecycleOwner(), name -> username.setText((name != null && !name.isEmpty()) ? name : "Hello User\nManage your finance easily"));

        // Observe and update profile picture using Glide
        homeViewModel.getProfilePicUrl().observe(getViewLifecycleOwner(), picUrl -> {
            if (picUrl != null && !picUrl.isEmpty()) {
                Glide.with(this)
                        .load(picUrl)
                        .placeholder(R.drawable.obsmallmen2)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(profilePicture);
            }
        });

        // Observe saving amount
        homeViewModel.getSaving().observe(getViewLifecycleOwner(), saving -> {
                    savingLayout.setVisibility(View.VISIBLE);
                    savingTextView.setText("₹" + (String.format("%.2f", saving)));
                }
        );
    }

    private void setupFriendBalanceAdapter() {
        friendBalanceList = new ArrayList<>();
        friendBalanceAdapter = new FriendBalanceAdapter(getContext(), friendBalanceList);
        friendRecyclerView.setAdapter(friendBalanceAdapter);
    }

    private void setupClickListeners() {
        viewAllGroups.setOnClickListener(v -> navigateToGroupFragment());
        groupCardView.setOnClickListener(v -> navigateToGroupFragment());
        viewAllFriends.setOnClickListener(v -> navigateToManageFriends());
        friendCardView.setOnClickListener(v -> navigateToManageFriends());

        pay.setOnClickListener(v -> navigateToPaymentFragment());
        request.setOnClickListener(v -> navigateToPaymentFragment());
        history.setOnClickListener(v -> navigateToPaymentFragment());
        addIncome.setOnClickListener(v -> showSavingDialog());
        withdraw.setOnClickListener(v -> showSavingDialog());
    }

    private void navigateToGroupFragment() {
        if (getActivity() instanceof Homepage) {
            ((Homepage) getActivity()).navigateToAddFriendFragment();
        }
    }

    private void navigateToPaymentFragment() {
        if (getActivity() instanceof Homepage) {
            ((Homepage) getActivity()).navigateToPaymentFragments();
        }
    }

    private void navigateToManageFriends() {
        Intent intent = new Intent(getActivity(), ManageFriends.class);
        startActivity(intent);
    }

    private void showSavingDialog() {
        // Inflate and setup the saving dialog
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.manage_saving, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        EditText savingEditText = dialogView.findViewById(R.id.saving_edit_text);
        Button updateBtn = dialogView.findViewById(R.id.updateButton);

        updateBtn.setOnClickListener(v -> {
            String inputText = savingEditText.getText().toString().trim();
            if (!inputText.isEmpty()) {
                try {
                    double saving = Double.parseDouble(inputText);
                    db.collection("users").document(currentUserId)
                            .update("saving", saving)
                            .addOnSuccessListener(task -> {
                                Utill.showToast(getActivity(), "Saving Updated, Refresh page to update");
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Utill.showToast(getActivity(), "Can't update saving! try again later")
                            );
                } catch (NumberFormatException e) {
                    savingEditText.setError("Please Enter a valid number");
                    savingEditText.requestFocus();
                }
            } else {
                savingEditText.setError("Enter amount first!");
                savingEditText.requestFocus();
            }
        });
    }

    private void fetchGroupData() {
        GroupAdapter groupAdapter;
        List<GroupItem> groupList;
        // Initialize group list and adapter
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(getContext(), groupList);
        groupRecyclerView.setAdapter(groupAdapter);

        // Observe group data from GroupViewModel
        GroupViewModel groupViewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);
        groupViewModel.getGroupList().observe(getViewLifecycleOwner(), groups -> {
            List<GroupItem> limitedGroups = groups.size() > MAX_GROUPS_TO_DISPLAY
                    ? new ArrayList<>(groups.subList(0, MAX_GROUPS_TO_DISPLAY))
                    : groups;
            groupAdapter.setGroupList(limitedGroups);
        });
    }

    private void loadCachedFriendsData() {
        String friendsJson = sharedPreferences.getString(KEY_FRIENDS_DATA, null);
        if (friendsJson != null) {
            try {
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
            } catch (Exception e) {
                Log.e(TAG, "Error loading cached friends data: ", e);
                Toast.makeText(getContext(), "Error loading cached data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cacheFriendsData() {
        try {
            Gson gson = new Gson();
            List<FriendBalance> friendsToCache = new ArrayList<>(friendBalanceList.subList(0, Math.min(friendBalanceList.size(), MAX_FRIENDS_TO_DISPLAY)));
            String friendsJson = gson.toJson(friendsToCache);
            sharedPreferences.edit().putString(KEY_FRIENDS_DATA, friendsJson).apply();
            Log.d(TAG, "Cached friends data (limited to " + MAX_FRIENDS_TO_DISPLAY + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error caching friends data: ", e);
        }
    }

    private void fetchFriendsData() {
        // Reset any previous task reference (Firebase Tasks cannot truly be cancelled)
        groupIds.clear();
        expenseManagers.clear();

        db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    Set<String> uniqueFriendIds = new HashSet<>();

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
                        Toast.makeText(getContext(), "No friends found", Toast.LENGTH_SHORT).show();
                        friendBalanceList.clear();
                        friendBalanceAdapter.notifyDataSetChanged();
                        cacheFriendsData();
                        calculateFriendBalances();
                    } else {
                        fetchFriendDetails(new ArrayList<>(uniqueFriendIds));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching groups: ", e);
                    Toast.makeText(getContext(), "Error fetching friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchFriendDetails(List<String> friendIds) {
        // Reset previous fetch reference
        friendNames.clear();
        friendProfilePictures.clear();
        friendBalances.clear();
        // Initialize friend balances to zero
        for (String friendId : friendIds) {
            friendBalances.put(friendId, 0.0);
        }
        db.collection("users")
                .whereIn("uid", friendIds)
                .get()
                .addOnSuccessListener(query -> {
                    if (!isAdded() || getView() == null) {
                        Log.d(TAG, "Fragment not active, skipping friend details update");
                        return;
                    }
                    for (QueryDocumentSnapshot userDoc : query) {
                        String friendId = userDoc.getId();
                        String fname = userDoc.getString("fname");
                        String lname = userDoc.getString("lname");
                        String profilePic = userDoc.getString("profile_picture");

                        if (fname != null) {
                            String friendName;
                            // If last name exists and is non-empty, append its first character; otherwise, just use the first name.
                            if (lname != null && !lname.isEmpty()) {
                                friendName = fname + " " + lname.charAt(0) + ".";
                            } else {
                                friendName = fname;
                            }
                            friendNames.put(friendId, friendName);
                            friendProfilePictures.put(friendId, (profilePic != null) ? profilePic : "");
                        }
                    }
                    calculateFriendBalances();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching friend details: ", e);
                    Toast.makeText(getContext(), "Error fetching friend details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void calculateFriendBalances() {
        if (!isAdded() || getView() == null) {
            Log.d(TAG, "Fragment not active, skipping calculateFriendBalances");
            return;
        }
        final Map<String, ExpenseManager.UserExpenseData> aggregatedData = new HashMap<>();
        final int groupCount = expenseManagers.size();
        if (groupCount == 0) {
            updateFriendBalanceList();
            return;
        }

        final CountDownLatch latch = new CountDownLatch(groupCount);
        final Map<String, Boolean> groupDataReceived = new HashMap<>();

        for (String groupId : groupIds) {
            ExpenseManager expenseManager = expenseManagers.get(groupId);
            if (expenseManager != null) {
                expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), userDataMap -> {
                    if (!isAdded() || getView() == null) return;

                    if (userDataMap != null && !groupDataReceived.containsKey(groupId)) {
                        synchronized (aggregatedData) {
                            for (Map.Entry<String, ExpenseManager.UserExpenseData> entry : userDataMap.entrySet()) {
                                String userId = entry.getKey();
                                ExpenseManager.UserExpenseData newData = entry.getValue();
                                ExpenseManager.UserExpenseData existingData = aggregatedData.get(userId);
                                if (existingData == null) {
                                    aggregatedData.put(userId, new ExpenseManager.UserExpenseData(
                                            newData.totalSpent, newData.totalShare, newData.netBalance,
                                            new HashMap<>(newData.debts), new HashMap<>(newData.credits)
                                    ));
                                } else {
                                    existingData.totalSpent += newData.totalSpent;
                                    existingData.totalShare += newData.totalShare;
                                    existingData.netBalance = existingData.totalSpent - existingData.totalShare;
                                    newData.debts.forEach((k, v) -> existingData.debts.put(k,
                                            existingData.debts.getOrDefault(k, 0.0) + v));
                                    newData.credits.forEach((k, v) -> existingData.credits.put(k,
                                            existingData.credits.getOrDefault(k, 0.0) + v));
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
                // Wait for expense  data with timeout
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Timeout: Expense data not loaded within 5 seconds.");
                    return;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Error while waiting for expense data: ", e);
                Thread.currentThread().interrupt(); // Restore interrupted status
                return;
            }

            // Switch to UI thread to update UI
            handler.post(() -> {
                if (!isAdded() || getView() == null) {
                    Log.e(TAG, "Fragment not attached or view unavailable.");
                    return;
                }

                // Get current user expense data
                ExpenseManager.UserExpenseData currentUserData = aggregatedData.get(currentUserId);

                if (currentUserData == null) {
                    Log.w(TAG, "No expense data found for current user.");
                    updateFriendBalanceList();
                    return;
                }

                // Clear and calculate balances with friends
                friendBalances.clear();
                for (Map.Entry<String, String> friendEntry : friendNames.entrySet()) {
                    String friendId = friendEntry.getKey();
                    ExpenseManager.UserExpenseData friendData = aggregatedData.get(friendId);

                    double debtToFriend = friendData != null ? friendData.credits.getOrDefault(currentUserId, 0.0) : 0.0;
                    double creditFromFriend = currentUserData.credits.getOrDefault(friendId, 0.0);
                    double balanceWithFriend = creditFromFriend - debtToFriend;

                    friendBalances.put(friendId, balanceWithFriend);
                    Log.d(TAG, "Balance with friend " + friendId + ": " + balanceWithFriend);
                }

                // Update friend balance list after calculation
                updateFriendBalanceList();
            });
        }).start();

    }

    private void updateFriendBalanceList() {
        friendBalanceList.clear();
        for (Map.Entry<String, Double> entry : friendBalances.entrySet()) {
            String friendId = entry.getKey();
            double balance = entry.getValue();
            String friendName = friendNames.get(friendId);
            String profilePic = friendProfilePictures.get(friendId);
            if (friendName != null) {
                friendBalanceList.add(new FriendBalance(friendName, balance, profilePic));
            }
        }
        // Limit to max friends to display
        if (friendBalanceList.size() > MAX_FRIENDS_TO_DISPLAY) {
            friendBalanceList = new ArrayList<>(friendBalanceList.subList(0, MAX_FRIENDS_TO_DISPLAY));
        }
        friendBalanceAdapter.notifyDataSetChanged();
        cacheFriendsData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firestore listeners
        for (ListenerRegistration listener : firestoreListeners) {
            listener.remove();
        }
        firestoreListeners.clear();

        // Remove observers from expense managers and clear handlers
        for (ExpenseManager manager : expenseManagers.values()) {
            manager.getUserExpenseData().removeObservers(getViewLifecycleOwner());
            manager.removeListener();
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clear collections to avoid memory leaks
        expenseManagers.clear();
        groupIds.clear();
        friendNames.clear();
        friendProfilePictures.clear();
        friendBalances.clear();
    }
}
