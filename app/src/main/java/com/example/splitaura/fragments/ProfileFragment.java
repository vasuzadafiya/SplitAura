package com.example.splitaura.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.splitaura.Homepage;
import com.example.splitaura.LoginActivity;
import com.example.splitaura.ManageFriends;
import com.example.splitaura.R;
import com.example.splitaura.models.Friend;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileFragment extends Fragment {
    private static final String CACHE_PREFS = "ProfileCache";
    private static final String USER_DATA_KEY = "user_data";
    private static final String FRIENDS_LIST_KEY = "friends_list";
    private static final String GROUPS_LIST_KEY = "groups_list";
    private static final String CACHE_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_DURATION_MS = TimeUnit.DAYS.toMillis(1);// 24 hours
    private static final String ARG_USER_UID = "user_uid";
    private static final String GROUPS = "groups";
    private static final String LOADING = "Loading...";
    private static final String UNKNOWN_USER = "Unknown User";
    private static final String EMAIL = "email";
    private static final String STATUS = "status";
    private static final String NOT_PROVIDED = "Not provided";
    private static final String MOBILE = "mobile";
    private static final String GENDER = "gender";
    private static final String FRIEND_REQUESTS = "friend_requests";
    private static final String SENDER_UID = "sender_uid";
    private static final String RECEIVER_UID = "receiver_uid";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button editProfileButton;
    private Button logoutButton;
    private Button addFriendButton;
    private ImageView profileImage;
    private TextView userName;
    private TextView userEmail;
    private TextView userMobile;
    private TextView userGender;
    private TextView userUpi;
    private TextView friendsCount;
    private TextView groupsCount;
    private final Map<String, Friend> friendCache = new HashMap<>();
    private final List<Friend> friendsList = new ArrayList<>();
    private final Map<String, String> groupCache = new HashMap<>();
    private final List<String> groupsList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private CardView groupCard;
    private CardView friendCard;

    private SharedPreferences cachePrefs;
    private final Gson gson = new Gson();
    private String targetUid;
    private String currentUserUid;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String userUid) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID, userUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        cachePrefs = requireContext().getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);

        currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        targetUid = getArguments() != null && getArguments().containsKey(ARG_USER_UID) ? getArguments().getString(ARG_USER_UID) : currentUserUid;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        editProfileButton = view.findViewById(R.id.editProfileButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        addFriendButton = view.findViewById(R.id.addFriendButton);
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        userMobile = view.findViewById(R.id.userMobile);
        userGender = view.findViewById(R.id.userGender);
        userUpi = view.findViewById(R.id.userUpi);
        friendsCount = view.findViewById(R.id.friendsCount);
        groupsCount = view.findViewById(R.id.groupsCount);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        groupCard = view.findViewById(R.id.group_cardview);
        friendCard = view.findViewById(R.id.friend_cardview);

        setLoadingPlaceholders();
        loadCachedData();

        setupButtons();
        setRefresh();
        cardListener();

        return view;
    }

    private void cardListener() {
        groupCard.setOnClickListener(v -> {
            if (getActivity() instanceof Homepage) {
                ((Homepage) getActivity()).navigateToAddFriendFragment();
            }
        });

        friendCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ManageFriends.class);
            startActivity(intent);
        });
    }

    private void setRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setLoadingPlaceholders() {
        userName.setText(LOADING);
        userEmail.setText("Email: Loading...");
        userMobile.setText("Mobile: Loading...");
        userGender.setText("Gender: Loading...");
        userUpi.setText("UPI: Loading...");
        friendsCount.setText(LOADING);
        groupsCount.setText(LOADING);
    }

    public void refreshData() {
        friendCache.clear();
        friendsList.clear();
        groupCache.clear();
        groupsList.clear();

        cachePrefs.edit().clear().apply();
        setLoadingPlaceholders();
        loadCachedData();
    }

    private void loadCachedData() {
        long lastCacheTime = cachePrefs.getLong(CACHE_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCacheTime < CACHE_DURATION_MS && targetUid.equals(currentUserUid)) {
            String userDataJson = cachePrefs.getString(USER_DATA_KEY, null);
            String friendsListJson = cachePrefs.getString(FRIENDS_LIST_KEY, null);
            String groupsListJson = cachePrefs.getString(GROUPS_LIST_KEY, null);

            if (userDataJson != null) {
                Map<String, String> userData = gson.fromJson(userDataJson, new TypeToken<Map<String, String>>() {
                }.getType());
                userName.setText(userData.getOrDefault("fullName", UNKNOWN_USER));
                userEmail.setText("Email: " + userData.getOrDefault(EMAIL, NOT_PROVIDED));
                userMobile.setText("Mobile: " + userData.getOrDefault(MOBILE, NOT_PROVIDED));
                userGender.setText("Gender: " + userData.getOrDefault(GENDER, NOT_PROVIDED));
                userUpi.setText("UPI: " + userData.getOrDefault("upi", NOT_PROVIDED));
                String profilePicture = userData.get("profilePicture");
                if (profilePicture != null && !profilePicture.isEmpty()) {
                    Glide.with(this).load(profilePicture).apply(new RequestOptions().placeholder(R.drawable.obsmallmen2).error(R.drawable.obsmallmen2).circleCrop()).into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.obsmallmen2);
                }
            } else {
                fetchUserData();
                return;
            }

            if (friendsListJson != null) {
                Type friendListType = new TypeToken<List<Friend>>() {
                }.getType();
                friendsList.clear();
                friendsList.addAll(gson.fromJson(friendsListJson, friendListType));
                friendsCount.setText(String.valueOf(friendsList.size()));
            } else {
                fetchUserData();
                return;
            }

            if (groupsListJson != null) {
                Type groupListType = new TypeToken<List<String>>() {
                }.getType();
                groupsList.clear();
                groupsList.addAll(gson.fromJson(groupsListJson, groupListType));
                groupsCount.setText(String.valueOf(groupsList.size()));
            } else {
                fetchUserData();
            }
        } else {
            fetchUserData();
        }
    }

    private void fetchUserData() {
        if (targetUid == null) return;

        DocumentReference userRef = db.collection("users").document(targetUid);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String firstName = document.getString("fname");
                    String lastName = document.getString("lname");
                    String email = document.getString(EMAIL);
                    String mobile = document.getString(MOBILE);
                    String gender = document.getString(GENDER);
                    String profilePicture = document.getString("profile_picture");
                    String upi = document.getString("upi");

                    String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                    userName.setText(fullName.trim().isEmpty() ? UNKNOWN_USER : fullName.trim());
                    userEmail.setText("Email: " + (email != null ? email : NOT_PROVIDED));
                    userMobile.setText("Mobile: " + (mobile != null ? mobile : NOT_PROVIDED));
                    userGender.setText("Gender: " + (gender != null ? gender : NOT_PROVIDED));
                    userUpi.setText("UPI: " + (upi != null ? upi : NOT_PROVIDED));

                    if (profilePicture != null && !profilePicture.isEmpty()) {
                        Glide.with(this).load(profilePicture).apply(new RequestOptions().placeholder(R.drawable.obsmallmen2).error(R.drawable.obsmallmen2).circleCrop()).into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.obsmallmen2);
                    }

                    if (targetUid.equals(currentUserUid)) {
                        Map<String, String> userData = new HashMap<>();
                        userData.put("fullName", fullName.trim().isEmpty() ? UNKNOWN_USER : fullName.trim());
                        userData.put(EMAIL, email != null ? email : NOT_PROVIDED);
                        userData.put(MOBILE, mobile != null ? mobile : NOT_PROVIDED);
                        userData.put(GENDER, gender != null ? gender : NOT_PROVIDED);
                        userData.put("upi", upi != null ? upi : NOT_PROVIDED);
                        userData.put("profilePicture", profilePicture != null ? profilePicture : "");
                        String userDataJson = gson.toJson(userData);
                        cachePrefs.edit().putString(USER_DATA_KEY, userDataJson).apply();
                    }

                    fetchFriendsAndGroups(targetUid);
                }
            }
            updateButtonVisibility();
        });
    }

    private void fetchFriendsAndGroups(String uid) {
        final AtomicInteger friendQueriesCompleted = new AtomicInteger(0);
        final AtomicInteger totalFriendQueries = new AtomicInteger(0);
        final AtomicInteger friendDetailsFetched = new AtomicInteger(0);

        Query friendQuery1 = db.collection(FRIEND_REQUESTS).whereEqualTo(SENDER_UID, uid).whereEqualTo(STATUS, "Accepted");
        Query friendQuery2 = db.collection(FRIEND_REQUESTS).whereEqualTo(RECEIVER_UID, uid).whereEqualTo(STATUS, "Accepted");

        friendQuery1.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                totalFriendQueries.addAndGet(queryDocumentSnapshots.size());
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String friendId = doc.getString(RECEIVER_UID);
                    if (friendId != null && !friendId.equals(uid)) {
                        fetchFriendDetails(friendId, friendDetailsFetched, totalFriendQueries.get());
                    }
                }
            }
            friendQueriesCompleted.incrementAndGet();
            checkFriendsCountUpdate(friendQueriesCompleted.get(), friendDetailsFetched.get(), totalFriendQueries.get());
        });

        friendQuery2.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                totalFriendQueries.addAndGet(queryDocumentSnapshots.size());
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String friendId = doc.getString(SENDER_UID);
                    if (friendId != null && !friendId.equals(uid)) {
                        fetchFriendDetails(friendId, friendDetailsFetched, totalFriendQueries.get());
                    }
                }
            }
            friendQueriesCompleted.incrementAndGet();
            checkFriendsCountUpdate(friendQueriesCompleted.get(), friendDetailsFetched.get(), totalFriendQueries.get());
        });

        Set<String> groupIds = new HashSet<>();
        AtomicInteger groupQueriesCompleted = new AtomicInteger(0);
        int totalGroupQueries = 2;

        db.collection(GROUPS).whereEqualTo("createdBy", uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                groupIds.add(doc.getId());
            }
            groupQueriesCompleted.incrementAndGet();
            if (groupQueriesCompleted.get() == totalGroupQueries) {
                fetchGroupDetails(groupIds);
            }
        });

        db.collection(GROUPS).whereArrayContains("memberIds", uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                groupIds.add(doc.getId());
            }
            groupQueriesCompleted.incrementAndGet();
            if (groupQueriesCompleted.get() == totalGroupQueries) {
                fetchGroupDetails(groupIds);
            }
        });
    }

    private void fetchGroupDetails(Set<String> groupIds) {
        groupsList.clear();
        groupCache.clear();

        if (groupIds.isEmpty()) {
            groupsCount.setText("0");
            if (targetUid.equals(currentUserUid)) {
                String groupsListJson = gson.toJson(groupsList);
                cachePrefs.edit().putString(GROUPS_LIST_KEY, groupsListJson).apply();
            }
            return;
        }

        AtomicInteger groupsProcessed = new AtomicInteger(0);
        for (String groupId : groupIds) {
            db.collection(GROUPS).document(groupId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String groupName = doc.getString("name");
                    groupCache.put(groupId, groupName != null ? groupName : "Unknown Group");
                    groupsList.add(groupId);
                }
                if (groupsProcessed.incrementAndGet() == groupIds.size()) {
                    groupsCount.setText(String.valueOf(groupsList.size()));
                    if (targetUid.equals(currentUserUid)) {
                        String groupsListJson = gson.toJson(groupsList);
                        cachePrefs.edit().putString(GROUPS_LIST_KEY, groupsListJson).apply();
                    }
                }
            });
        }
    }

    private void fetchFriendDetails(String friendId, AtomicInteger friendDetailsFetched, int totalFriendQueries) {
        Friend cachedFriend = friendCache.get(friendId);
        if (cachedFriend != null) {
            if (!friendsList.contains(cachedFriend)) {
                friendsList.add(cachedFriend);
            }
            friendDetailsFetched.incrementAndGet();
            checkFriendsCountUpdate(2, friendDetailsFetched.get(), totalFriendQueries);
        } else {
            db.collection("users").document(friendId).get().addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    Friend friend = new Friend(userDoc.getString("uid"), userDoc.getString(EMAIL), userDoc.getString("fname"), userDoc.getString("lname"), userDoc.getString("profile_picture"), userDoc.getString("upi"));
                    friendCache.put(friendId, friend);
                    if (!friendsList.contains(friend)) {
                        friendsList.add(friend);
                    }
                }
                friendDetailsFetched.incrementAndGet();
                checkFriendsCountUpdate(2, friendDetailsFetched.get(), totalFriendQueries);
            });
        }
    }

    private void checkFriendsCountUpdate(int completedQueries, int friendDetailsFetched, int totalFriendQueries) {
        if (completedQueries == 2 && (totalFriendQueries == 0 || friendDetailsFetched >= totalFriendQueries)) {
            friendsCount.setText(String.valueOf(friendsList.size()));
            if (targetUid.equals(currentUserUid)) {
                String friendsListJson = gson.toJson(friendsList);
                cachePrefs.edit().putString(FRIENDS_LIST_KEY, friendsListJson).apply();
                cachePrefs.edit().putLong(CACHE_TIMESTAMP, System.currentTimeMillis()).apply();
            }
        }
    }

    private void setupButtons() {
        if (targetUid != null && currentUserUid != null) {
            if (targetUid.equals(currentUserUid)) {
                editProfileButton.setVisibility(View.VISIBLE);
                logoutButton.setVisibility(View.VISIBLE);
                addFriendButton.setVisibility(View.GONE);
            } else {
                editProfileButton.setVisibility(View.GONE);
                logoutButton.setVisibility(View.GONE);
                addFriendButton.setVisibility(View.VISIBLE);
            }

            editProfileButton.setOnClickListener(v -> {
                EditProfileDialogFragment editProfileDialog = new EditProfileDialogFragment();
                editProfileDialog.show(getChildFragmentManager(), "EditProfileDialog");
            });

            logoutButton.setOnClickListener(v -> logout());

            addFriendButton.setOnClickListener(v -> sendFriendRequest());
        }
    }

    private void updateButtonVisibility() {
        if (targetUid != null && currentUserUid != null) {
            if (targetUid.equals(currentUserUid)) {
                editProfileButton.setVisibility(View.VISIBLE);
                logoutButton.setVisibility(View.VISIBLE);
                addFriendButton.setVisibility(View.GONE);
            } else {
                editProfileButton.setVisibility(View.GONE);
                logoutButton.setVisibility(View.GONE);
                addFriendButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void sendFriendRequest() {
        if (targetUid == null || currentUserUid == null || targetUid.equals(currentUserUid)) return;

        db.collection(FRIEND_REQUESTS).whereEqualTo(SENDER_UID, currentUserUid).whereEqualTo(RECEIVER_UID, targetUid).get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                Map<String, Object> friendRequest = new HashMap<>();
                friendRequest.put(SENDER_UID, currentUserUid);
                friendRequest.put(RECEIVER_UID, targetUid);
                friendRequest.put(STATUS, "Pending");
                friendRequest.put("timestamp", System.currentTimeMillis());

                db.collection(FRIEND_REQUESTS).add(friendRequest).addOnSuccessListener(documentReference -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Friend request sent!", Toast.LENGTH_SHORT).show();
                    }
                    addFriendButton.setEnabled(false);
                    addFriendButton.setText("Request Sent");
                });
            } else if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Friend request already sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        cachePrefs.edit().clear().apply();
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}