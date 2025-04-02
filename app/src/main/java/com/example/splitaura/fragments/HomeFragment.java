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

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String PREFS_NAME = "FriendsCache";
    private static final String KEY_FRIENDS_DATA = "friends_data";
    private static final int MAX_GROUPS_TO_DISPLAY = 2;
    private static final int MAX_FRIENDS_TO_DISPLAY = 4; // Changed to show 4 friends
    private static final int EXPENSE_DATA_TIMEOUT_SECONDS = 5;

    private final List<ListenerRegistration> firestoreListeners = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ExpenseManager> expenseManagers = new HashMap<>();
    private final Map<String, String> friendNames = new HashMap<>();
    private final Map<String, String> friendProfilePictures = new HashMap<>();
    private final Map<String, Double> friendBalances = new HashMap<>();

    private ImageView profilePicture;
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
    private ImageButton payButton;
    private ImageButton requestButton;
    private ImageButton addIncomeButton;
    private ImageButton withdrawButton;
    private FirebaseFirestore db;
    private String currentUserId;
    private ImageButton historyButton;
    private LinearLayout savingLayout;
    private SharedPreferences sharedPreferences;
    private List<String> groupIds;
    private GroupViewModel groupViewModel;
    private ListenerRegistration groupsListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        if (getContext() != null) {
            sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        initializeCollections();
        initializeViews(rootView);
        initializeViewModels();
        setupUserInfo();
        setupFriendBalanceAdapter();
        setupClickListeners();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchInitialData();
    }

    private void initializeCollections() {
        groupIds = new ArrayList<>();
    }

    private void initializeViews(View rootView) {
        try {
            profilePicture = rootView.findViewById(R.id.profile_picture);
            username = rootView.findViewById(R.id.username_text);
            savingLayout = rootView.findViewById(R.id.saving_layout);
            savingTextView = rootView.findViewById(R.id.txt_saving);

            // Initialize RecyclerViews
            groupRecyclerView = rootView.findViewById(R.id.group_recyclerView);
            groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            friendRecyclerView = rootView.findViewById(R.id.friend_recyclerView);
            friendRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            // Initialize buttons and clickable views
            viewAllGroups = rootView.findViewById(R.id.view_all_groups);
            viewAllFriends = rootView.findViewById(R.id.btn_view_all_friends);
            groupCardView = rootView.findViewById(R.id.group_cardview);
            friendCardView = rootView.findViewById(R.id.friend_cardview);
            payButton = rootView.findViewById(R.id.btn_pay);
            requestButton = rootView.findViewById(R.id.btn_request_amount);
            withdrawButton = rootView.findViewById(R.id.btn_withdraw_saving);
            historyButton = rootView.findViewById(R.id.btn_history);
            addIncomeButton = rootView.findViewById(R.id.btn_add_income);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error initializing UI components", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeViewModels() {
        try {
            homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
            groupViewModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);

            // Observe group deletion events
            groupViewModel.getGroupDeleted().observe(getViewLifecycleOwner(), deleted -> {
                if (deleted) {
                    refreshGroups();
                    groupViewModel.setGroupDeleted(false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ViewModels", e);
        }
    }

    private void setupUserInfo() {
        try {
            homeViewModel.getUsername().observe(getViewLifecycleOwner(), name -> {
                if (name != null && !name.isEmpty()) {
                    username.setText(name);
                } else {
                    username.setText(getString(R.string.default_username));
                }
            });

            homeViewModel.getProfilePicUrl().observe(getViewLifecycleOwner(), picUrl -> {
                if (picUrl != null && !picUrl.isEmpty() && isAdded()) {
                    Glide.with(this)
                            .load(picUrl)
                            .placeholder(R.drawable.obsmallmen2)
                            .error(R.drawable.obsmallmen2)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(profilePicture);
                }
            });

            homeViewModel.getSaving().observe(getViewLifecycleOwner(), saving -> {
                if (isAdded()) {
                    savingLayout.setVisibility(View.VISIBLE);
                    savingTextView.setText(getString(R.string.currency_format, saving));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up user info", e);
        }
    }

    private void setupFriendBalanceAdapter() {
        try {
            friendBalanceList = new ArrayList<>();
            friendBalanceAdapter = new FriendBalanceAdapter(requireContext(), friendBalanceList);
            friendRecyclerView.setAdapter(friendBalanceAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up friend balance adapter", e);
        }
    }

    private void setupClickListeners() {
        try {
            viewAllGroups.setOnClickListener(v -> navigateToGroupFragment());
            groupCardView.setOnClickListener(v -> navigateToGroupFragment());
            viewAllFriends.setOnClickListener(v -> navigateToManageFriends());
            friendCardView.setOnClickListener(v -> navigateToManageFriends());

            payButton.setOnClickListener(v -> navigateToPaymentFragment());
            requestButton.setOnClickListener(v -> navigateToPaymentFragment());
            historyButton.setOnClickListener(v -> navigateToPaymentFragment());
            addIncomeButton.setOnClickListener(v -> showSavingDialog(true));
            withdrawButton.setOnClickListener(v -> showSavingDialog(false));
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    private void fetchInitialData() {
        loadCachedFriendsData();
        fetchGroupData();
        fetchFriendsData();
    }

    private void fetchGroupData() {
        try {
            List<GroupItem> groupList = new ArrayList<>();
            GroupAdapter groupAdapter = new GroupAdapter(requireContext(), groupList);
            groupRecyclerView.setAdapter(groupAdapter);

            groupsListener = db.collection("groups")
                    .whereArrayContains("memberIds", currentUserId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Listen failed.", error);
                            showErrorMessage(R.string.error_fetching_groups);
                            return;
                        }

                        if (value == null) {
                            Log.w(TAG, "Groups query returned null");
                            return;
                        }

                        List<GroupItem> updatedGroups = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                GroupItem group = parseGroupDocument(doc);
                                if (group != null) {
                                    updatedGroups.add(group);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing group document: " + doc.getId(), e);
                            }
                        }

                        updateGroupList(updatedGroups, groupAdapter);
                    });

            firestoreListeners.add(groupsListener);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching group data", e);
            showErrorMessage(R.string.error_fetching_groups);
        }
    }

    private GroupItem parseGroupDocument(DocumentSnapshot doc) {
        try {
            String groupId = doc.getId();
            String title = Objects.requireNonNull(doc.getString("title"), getString(R.string.untitled_group));
            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0.0;
            String imageUrl = doc.getString("imageUrl");
            String date = doc.getTimestamp("createdAt") != null
                    ? formatDate(doc.getTimestamp("createdAt").toDate())
                    : "";

            List<String> memberImagesUrls = new ArrayList<>();
            List<Map<String, Object>> members = (List<Map<String, Object>>) doc.get("members");
            if (members != null) {
                for (Map<String, Object> member : members) {
                    String profilePic = (String) member.get("profile_picture");
                    if (profilePic != null) {
                        memberImagesUrls.add(profilePic);
                    }
                }
            }

            int moreMembersCount = Math.max(0, memberImagesUrls.size() - 3);
            if (memberImagesUrls.size() > 3) {
                memberImagesUrls = new ArrayList<>(memberImagesUrls.subList(0, 3));
            }

            return new GroupItem(
                    groupId,
                    title,
                    date,
                    amount,
                    imageUrl,
                    memberImagesUrls,
                    moreMembersCount,
                    new HashMap<>()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error parsing group document: " + doc.getId(), e);
            return null;
        }
    }

    private void updateGroupList(List<GroupItem> updatedGroups, GroupAdapter adapter) {
        handler.post(() -> {
            if (!isAdded()) return;

            try {
                List<GroupItem> limitedGroups = updatedGroups.size() > MAX_GROUPS_TO_DISPLAY
                        ? new ArrayList<>(updatedGroups.subList(0, MAX_GROUPS_TO_DISPLAY))
                        : new ArrayList<>(updatedGroups);

                adapter.setGroupList(limitedGroups);

                viewAllGroups.setVisibility(updatedGroups.size() > MAX_GROUPS_TO_DISPLAY
                        ? View.VISIBLE : View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Error updating group list", e);
            }
        });
    }

    private void loadCachedFriendsData() {
        if (sharedPreferences == null || !isAdded()) return;

        try {
            String friendsJson = sharedPreferences.getString(KEY_FRIENDS_DATA, null);
            if (friendsJson != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<FriendBalance>>() {}.getType();
                List<FriendBalance> cachedFriends = gson.fromJson(friendsJson, type);

                if (cachedFriends != null && !cachedFriends.isEmpty()) {
                    friendBalanceList.clear();
                    friendBalanceList.addAll(cachedFriends.subList(0,
                            Math.min(cachedFriends.size(), MAX_FRIENDS_TO_DISPLAY)));
                    friendBalanceAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded friends data from cache");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cached friends data", e);
            sharedPreferences.edit().remove(KEY_FRIENDS_DATA).apply();
        }
    }

    private void cacheFriendsData() {
        if (sharedPreferences == null || !isAdded() || friendBalanceList == null) return;

        try {
            Gson gson = new Gson();
            List<FriendBalance> friendsToCache = new ArrayList<>(friendBalanceList);
            String friendsJson = gson.toJson(friendsToCache);
            sharedPreferences.edit()
                    .putString(KEY_FRIENDS_DATA, friendsJson)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error caching friends data", e);
        }
    }

    private void fetchFriendsData() {
        if (!isAdded() || currentUserId == null) return;

        try {
            groupIds.clear();
            expenseManagers.clear();

            db.collection("groups")
                    .whereArrayContains("memberIds", currentUserId)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!isAdded()) return;

                        Set<String> uniqueFriendIds = new HashSet<>();

                        for (QueryDocumentSnapshot groupDoc : query) {
                            try {
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
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing group document", e);
                            }
                        }

                        if (uniqueFriendIds.isEmpty()) {
                            Log.d(TAG, "No friends found in groups");
                            updateEmptyFriendState();
                        } else {
                            fetchFriendDetails(new ArrayList<>(uniqueFriendIds));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching groups", e);
                        showErrorMessage(R.string.error_fetching_friends);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchFriendsData", e);
            showErrorMessage(R.string.error_fetching_friends);
        }
    }

    private void updateEmptyFriendState() {
        handler.post(() -> {
            if (!isAdded()) return;

            friendBalanceList.clear();
            friendBalanceAdapter.notifyDataSetChanged();
            cacheFriendsData();

            viewAllFriends.setVisibility(View.GONE);
        });
    }

    private void fetchFriendDetails(List<String> friendIds) {
        if (!isAdded() || friendIds.isEmpty()) return;

        try {
            friendNames.clear();
            friendProfilePictures.clear();
            friendBalances.clear();

            for (String friendId : friendIds) {
                friendBalances.put(friendId, 0.0);
            }

            db.collection("users")
                    .whereIn("uid", friendIds)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!isAdded()) return;

                        for (QueryDocumentSnapshot userDoc : query) {
                            try {
                                String friendId = userDoc.getId();
                                String fname = userDoc.getString("fname");
                                String lname = userDoc.getString("lname");
                                String profilePic = userDoc.getString("profile_picture");

                                if (fname != null && lname != null) {
                                    String friendName = fname + " " + lname.charAt(0) + ".";
                                    friendNames.put(friendId, friendName);
                                    friendProfilePictures.put(friendId,
                                            profilePic != null ? profilePic : "");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing friend document", e);
                            }
                        }

                        calculateFriendBalances();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching friend details", e);
                        showErrorMessage(R.string.error_fetching_friends);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchFriendDetails", e);
            showErrorMessage(R.string.error_fetching_friends);
        }
    }

    private void calculateFriendBalances() {
        if (!isAdded() || expenseManagers.isEmpty()) {
            updateFriendBalanceList();
            return;
        }

        final Map<String, ExpenseManager.UserExpenseData> aggregatedData = new HashMap<>();
        final CountDownLatch latch = new CountDownLatch(expenseManagers.size());
        final Map<String, Boolean> groupDataReceived = new HashMap<>();

        for (String groupId : groupIds) {
            ExpenseManager expenseManager = expenseManagers.get(groupId);
            if (expenseManager != null) {
                expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), userDataMap -> {
                    if (!isAdded()) return;

                    if (userDataMap != null && !groupDataReceived.containsKey(groupId)) {
                        synchronized (aggregatedData) {
                            for (Map.Entry<String, ExpenseManager.UserExpenseData> entry : userDataMap.entrySet()) {
                                String userId = entry.getKey();
                                ExpenseManager.UserExpenseData newData = entry.getValue();
                                ExpenseManager.UserExpenseData existingData = aggregatedData.get(userId);

                                if (existingData == null) {
                                    aggregatedData.put(userId, new ExpenseManager.UserExpenseData(
                                            newData.totalSpent,
                                            newData.totalShare,
                                            newData.netBalance,
                                            new HashMap<>(newData.debts),
                                            new HashMap<>(newData.credits)
                                    ));
                                } else {
                                    existingData.totalSpent += newData.totalSpent;
                                    existingData.totalShare += newData.totalShare;
                                    existingData.netBalance = existingData.totalSpent - existingData.totalShare;

                                    newData.debts.forEach((k, v) ->
                                            existingData.debts.merge(k, v, Double::sum));

                                    newData.credits.forEach((k, v) ->
                                            existingData.credits.merge(k, v, Double::sum));
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
                if (!latch.await(EXPENSE_DATA_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Timeout: Expense data not loaded within " +
                            EXPENSE_DATA_TIMEOUT_SECONDS + " seconds");
                    return;
                }

                handler.post(() -> {
                    if (!isAdded()) return;
                    processAggregatedData(aggregatedData);
                });
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for expense data", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void processAggregatedData(Map<String, ExpenseManager.UserExpenseData> aggregatedData) {
        try {
            if (aggregatedData == null || !aggregatedData.containsKey(currentUserId)) {
                Log.w(TAG, "No expense data found for current user");
                updateFriendBalanceList();
                return;
            }

            ExpenseManager.UserExpenseData currentUserData = aggregatedData.get(currentUserId);
            friendBalances.clear();

            // Calculate balances with all friends
            for (Map.Entry<String, String> friendEntry : friendNames.entrySet()) {
                String friendId = friendEntry.getKey();
                ExpenseManager.UserExpenseData friendData = aggregatedData.get(friendId);

                // Calculate net balance between current user and this friend
                double netBalance = 0.0;

                if (friendData != null) {
                    // What friend owes to current user (current user's credit)
                    double friendOwesYou = currentUserData.credits.getOrDefault(friendId, 0.0);
                    // What current user owes to friend (current user's debt)
                    double youOweFriend = friendData.credits.getOrDefault(currentUserId, 0.0);

                    netBalance = friendOwesYou - youOweFriend;
                }

                friendBalances.put(friendId, netBalance);
            }

            updateFriendBalanceList();
        } catch (Exception e) {
            Log.e(TAG, "Error processing aggregated data", e);
            showErrorMessage(R.string.error_calculating_balances);
        }
    }

    private void updateFriendBalanceList() {
        if (!isAdded()) return;

        try {
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

            // Sort by absolute balance (highest first)
            friendBalanceList.sort((a, b) ->
                    Double.compare(Math.abs(b.getBalance()), Math.abs(a.getBalance())));

            // Limit to MAX_FRIENDS_TO_DISPLAY (now 4)
            if (friendBalanceList.size() > MAX_FRIENDS_TO_DISPLAY) {
                friendBalanceList = new ArrayList<>(
                        friendBalanceList.subList(0, MAX_FRIENDS_TO_DISPLAY));
            }

            friendBalanceAdapter.notifyDataSetChanged();
            cacheFriendsData();

            viewAllFriends.setVisibility(friendBalances.size() > MAX_FRIENDS_TO_DISPLAY
                    ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Error updating friend balance list", e);
        }
    }

    private void showSavingDialog(boolean isDeposit) {
        if (!isAdded() || getActivity() == null) return;

        try {
            View dialogView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.manage_saving, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            TextView title = dialogView.findViewById(R.id.textView);
            EditText savingEditText = dialogView.findViewById(R.id.saving_edit_text);
            Button actionButton = dialogView.findViewById(R.id.updateButton);

            if (isDeposit) {
                title.setText(R.string.deposit_savings);
                actionButton.setText(R.string.deposit);
            } else {
                title.setText(R.string.withdraw_savings);
                actionButton.setText(R.string.withdraw);
            }

            actionButton.setOnClickListener(v -> {
                String inputText = savingEditText.getText().toString().trim();
                if (inputText.isEmpty()) {
                    savingEditText.setError(getString(R.string.error_empty_amount));
                    savingEditText.requestFocus();
                    return;
                }

                try {
                    double amount = Double.parseDouble(inputText);
                    if (amount <= 0) {
                        savingEditText.setError(getString(R.string.error_invalid_amount));
                        savingEditText.requestFocus();
                        return;
                    }

                    updateSavings(amount, isDeposit, dialog);
                } catch (NumberFormatException e) {
                    savingEditText.setError(getString(R.string.error_invalid_number));
                    savingEditText.requestFocus();
                }
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing saving dialog", e);
            showErrorMessage(R.string.error_processing_request);
        }
    }

    private void updateSavings(double amount, boolean isDeposit, AlertDialog dialog) {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    double currentSaving = documentSnapshot.getDouble("saving") != null
                            ? documentSnapshot.getDouble("saving") : 0.0;

                    double newSaving = isDeposit
                            ? currentSaving + amount
                            : currentSaving - amount;

                    if (!isDeposit && newSaving < 0) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    R.string.error_insufficient_savings,
                                    Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    db.collection("users").document(currentUserId)
                            .update("saving", newSaving)
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                if (isAdded()) {
                                    String message = isDeposit
                                            ? getString(R.string.deposit_success, amount)
                                            : getString(R.string.withdraw_success, amount);
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update saving", e);
                                if (isAdded()) {
                                    Toast.makeText(getContext(),
                                            R.string.error_updating_savings,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch current saving", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                R.string.error_fetching_savings,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToGroupFragment() {
        try {
            if (getActivity() instanceof Homepage) {
                ((Homepage) getActivity()).navigateToAddFriendFragment();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to group fragment", e);
            showErrorMessage(R.string.error_navigation);
        }
    }

    private void navigateToPaymentFragment() {
        try {
            if (getActivity() instanceof Homepage) {
                ((Homepage) getActivity()).navigateToPaymentFragments();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to payment fragment", e);
            showErrorMessage(R.string.error_navigation);
        }
    }

    private void navigateToManageFriends() {
        try {
            Intent intent = new Intent(getActivity(), ManageFriends.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to manage friends", e);
            showErrorMessage(R.string.error_navigation);
        }
    }

    private void refreshGroups() {
        handler.post(() -> {
            if (isAdded()) {
                fetchGroupData();
            }
        });
    }

    private void showErrorMessage(int stringResId) {
        if (isAdded() && getContext() != null) {
            handler.post(() ->
                    Toast.makeText(getContext(), stringResId, Toast.LENGTH_SHORT).show());
        }
    }

    public static String formatDate(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupResources();
    }

    private void cleanupResources() {
        if (groupsListener != null) {
            groupsListener.remove();
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

        if (groupIds != null) {
            groupIds.clear();
        }

        friendNames.clear();
        friendProfilePictures.clear();
        friendBalances.clear();
    }
}