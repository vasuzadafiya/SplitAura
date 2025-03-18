package com.example.splitaura.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.splitaura.LoginActivity;
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
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private static final String CACHE_PREFS = "ProfileCache";
    private static final String USER_DATA_KEY = "user_data";
    private static final String FRIENDS_LIST_KEY = "friends_list";
    private static final String GROUPS_LIST_KEY = "groups_list";
    private static final String CACHE_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private static final String ARG_USER_UID = "user_uid"; // Argument key for target user's UID

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button editProfileButton, logoutButton, addFriendButton;
    private ImageView profileImage;
    private TextView userName, userEmail, userMobile, userGender, userUpi, friendsCount, groupsCount;
    private final Map<String, Friend> friendCache = new HashMap<>();
    private final List<Friend> friendsList = new ArrayList<>();
    private final Map<String, String> groupCache = new HashMap<>();
    private final List<String> groupsList = new ArrayList<>();

    private SharedPreferences cachePrefs;
    private Gson gson = new Gson();
    private String targetUid; // UID of the profile being displayed
    private String currentUserUid; // UID of the logged-in user

    public ProfileFragment() {
        // Required empty public constructor
    }

    // Factory method to create instance with target UID
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

        // Get the target UID from arguments, default to current user if not provided
        currentUserUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (getArguments() != null && getArguments().containsKey(ARG_USER_UID)) {
            targetUid = getArguments().getString(ARG_USER_UID);
        } else {
            targetUid = currentUserUid; // Default to current user's profile
        }
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

        // Set initial "Loading..." placeholders
        setLoadingPlaceholders();

        // Load data (from cache or Firestore)
        loadCachedData();

        setupButtons();

        return view;
    }

    private void setLoadingPlaceholders() {
        userName.setText("Loading...");
        userEmail.setText("Email: Loading...");
        userMobile.setText("Mobile: Loading...");
        userGender.setText("Gender: Loading...");
        userUpi.setText("UPI: Loading...");
        friendsCount.setText("Loading...");
        groupsCount.setText("Loading...");
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
                Map<String, String> userData = gson.fromJson(userDataJson, new TypeToken<Map<String, String>>(){}.getType());
                userName.setText(userData.getOrDefault("fullName", "Unknown User"));
                userEmail.setText("Email: " + userData.getOrDefault("email", "Not provided"));
                userMobile.setText("Mobile: " + userData.getOrDefault("mobile", "Not provided"));
                userGender.setText("Gender: " + userData.getOrDefault("gender", "Not provided"));
                userUpi.setText("UPI: " + userData.getOrDefault("upi", "Not provided"));
                String profilePicture = userData.get("profilePicture");
                if (profilePicture != null && !profilePicture.isEmpty()) {
                    Glide.with(this)
                            .load(profilePicture)
                            .apply(new RequestOptions()
                                    .placeholder(R.drawable.obsmallmen2)
                                    .error(R.drawable.obsmallmen2)
                                    .circleCrop())
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.obsmallmen2);
                }
            } else {
                fetchUserData();
                return;
            }

            if (friendsListJson != null) {
                Type friendListType = new TypeToken<List<Friend>>(){}.getType();
                friendsList.clear();
                friendsList.addAll(gson.fromJson(friendsListJson, friendListType));
                friendsCount.setText(String.valueOf(friendsList.size()));
            } else {
                fetchUserData();
                return;
            }

            if (groupsListJson != null) {
                Type groupListType = new TypeToken<List<String>>(){}.getType();
                groupsList.clear();
                groupsList.addAll(gson.fromJson(groupsListJson, groupListType));
                groupsCount.setText(String.valueOf(groupsList.size()));
            } else {
                fetchUserData();
                return;
            }
        } else {
            fetchUserData();
        }
    }

    private void fetchUserData() {
        if (targetUid == null) {
            Toast.makeText(getContext(), "Invalid user profile", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("users").document(targetUid);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String firstName = document.getString("fname");
                    String lastName = document.getString("lname");
                    String email = document.getString("email");
                    String mobile = document.getString("mobile");
                    String gender = document.getString("gender");
                    String profilePicture = document.getString("profile_picture");
                    String upi = document.getString("upi");

                    String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                    userName.setText(fullName.trim().isEmpty() ? "Unknown User" : fullName.trim());
                    userEmail.setText("Email: " + (email != null ? email : "Not provided"));
                    userMobile.setText("Mobile: " + (mobile != null ? mobile : "Not provided"));
                    userGender.setText("Gender: " + (gender != null ? gender : "Not provided"));
                    userUpi.setText("UPI: " + (upi != null ? upi : "Not provided"));

                    if (profilePicture != null && !profilePicture.isEmpty()) {
                        Glide.with(this)
                                .load(profilePicture)
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.obsmallmen2)
                                        .error(R.drawable.obsmallmen2)
                                        .circleCrop())
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.obsmallmen2);
                    }

                    if (targetUid.equals(currentUserUid)) {
                        Map<String, String> userData = new HashMap<>();
                        userData.put("fullName", fullName.trim().isEmpty() ? "Unknown User" : fullName.trim());
                        userData.put("email", email != null ? email : "Not provided");
                        userData.put("mobile", mobile != null ? mobile : "Not provided");
                        userData.put("gender", gender != null ? gender : "Not provided");
                        userData.put("upi", upi != null ? upi : "Not provided");
                        userData.put("profilePicture", profilePicture != null ? profilePicture : "");
                        String userDataJson = gson.toJson(userData);
                        cachePrefs.edit().putString(USER_DATA_KEY, userDataJson).apply();
                    }

                    fetchFriendsAndGroups(targetUid);
                } else {
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Exception e = task.getException();
                Toast.makeText(getContext(), "Failed to load profile: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            }
            updateButtonVisibility();
        });
    }

    private void fetchFriendsAndGroups(String uid) {
        // Fetch friends
        final AtomicInteger friendQueriesCompleted = new AtomicInteger(0);
        final AtomicInteger totalFriendQueries = new AtomicInteger(0);
        final AtomicInteger friendDetailsFetched = new AtomicInteger(0);

        Query friendQuery1 = db.collection("friend_requests")
                .whereEqualTo("sender_uid", uid)
                .whereEqualTo("status", "Accepted");
        Query friendQuery2 = db.collection("friend_requests")
                .whereEqualTo("receiver_uid", uid)
                .whereEqualTo("status", "Accepted");

        friendQuery1.get().addOnSuccessListener(queryDocumentSnapshots -> {
            Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " friend requests as sender");
            if (!queryDocumentSnapshots.isEmpty()) {
                totalFriendQueries.addAndGet(queryDocumentSnapshots.size());
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String friendId = doc.getString("receiver_uid");
                    Log.d(TAG, "Sender friend ID: " + friendId);
                    if (friendId != null && !friendId.equals(uid)) {
                        fetchFriendDetails(friendId, friendDetailsFetched, totalFriendQueries.get());
                    }
                }
            }
            friendQueriesCompleted.incrementAndGet();
            checkFriendsCountUpdate(friendQueriesCompleted.get(), friendDetailsFetched.get(), totalFriendQueries.get());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load friends (sender): " + e.getMessage(), e);
            friendQueriesCompleted.incrementAndGet();
            checkFriendsCountUpdate(friendQueriesCompleted.get(), friendDetailsFetched.get(), totalFriendQueries.get());
        });

        friendQuery2.get().addOnSuccessListener(queryDocumentSnapshots -> {
            Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " friend requests as receiver");
            if (!queryDocumentSnapshots.isEmpty()) {
                totalFriendQueries.addAndGet(queryDocumentSnapshots.size());
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String friendId = doc.getString("sender_uid");
                    Log.d(TAG, "Receiver friend ID: " + friendId);
                    if (friendId != null && !friendId.equals(uid)) {
                        fetchFriendDetails(friendId, friendDetailsFetched, totalFriendQueries.get());
                    }
                }
            }
            friendQueriesCompleted.incrementAndGet();
            checkFriendsCountUpdate(friendQueriesCompleted.get(), friendDetailsFetched.get(), totalFriendQueries.get());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load friends (receiver): " + e.getMessage(), e);
            friendQueriesCompleted.incrementAndGet();
            checkFriendsCountUpdate(friendQueriesCompleted.get(), friendDetailsFetched.get(), totalFriendQueries.get());
        });

        // Fetch groups (split into two queries to comply with security rules)
        Set<String> groupIds = new HashSet<>();
        AtomicInteger groupQueriesCompleted = new AtomicInteger(0);
        int totalGroupQueries = 2;

        // Query 1: Groups where the user is the creator
        db.collection("groups")
                .whereEqualTo("createdBy", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " groups where user is creator");
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String groupId = doc.getId();
                        groupIds.add(groupId);
                    }
                    groupQueriesCompleted.incrementAndGet();
                    if (groupQueriesCompleted.get() == totalGroupQueries) {
                        fetchGroupDetails(groupIds);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load groups where user is creator: " + e.getMessage(), e);
                    groupQueriesCompleted.incrementAndGet();
                    if (groupQueriesCompleted.get() == totalGroupQueries) {
                        fetchGroupDetails(groupIds);
                    }
                });

        // Query 2: Groups where the user is in memberIds
        db.collection("groups")
                .whereArrayContains("memberIds", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " groups where user is a member");
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String groupId = doc.getId();
                        groupIds.add(groupId);
                    }
                    groupQueriesCompleted.incrementAndGet();
                    if (groupQueriesCompleted.get() == totalGroupQueries) {
                        fetchGroupDetails(groupIds);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load groups where user is a member: " + e.getMessage(), e);
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
            Log.d(TAG, "No groups found for user UID: " + targetUid);
            Log.d(TAG, "User authenticated: " + (FirebaseAuth.getInstance().getCurrentUser() != null));
            groupsCount.setText("0");
            if (targetUid.equals(currentUserUid)) {
                String groupsListJson = gson.toJson(groupsList);
                cachePrefs.edit().putString(GROUPS_LIST_KEY, groupsListJson).apply();
            }
            return;
        }

        AtomicInteger groupsProcessed = new AtomicInteger(0);
        for (String groupId : groupIds) {
            db.collection("groups").document(groupId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String groupName = doc.getString("name");
                            List<String> memberIds = (List<String>) doc.get("memberIds");
                            Log.d(TAG, "Group ID: " + groupId + ", Name: " + groupName + ", Member IDs: " + (memberIds != null ? memberIds.toString() : "null"));
                            groupCache.put(groupId, groupName != null ? groupName : "Unknown Group");
                            groupsList.add(groupId);
                        }
                        if (groupsProcessed.incrementAndGet() == groupIds.size()) {
                            groupsCount.setText(String.valueOf(groupsList.size()));
                            Log.d(TAG, "Updated groupsCount to: " + groupsList.size());
                            if (targetUid.equals(currentUserUid)) {
                                String groupsListJson = gson.toJson(groupsList);
                                cachePrefs.edit().putString(GROUPS_LIST_KEY, groupsListJson).apply();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch group details for " + groupId + ": " + e.getMessage(), e);
                        if (groupsProcessed.incrementAndGet() == groupIds.size()) {
                            groupsCount.setText(String.valueOf(groupsList.size()));
                            Log.d(TAG, "Updated groupsCount to: " + groupsList.size());
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
            Log.d(TAG, "Cached friend added: " + cachedFriend.getFullName());
            friendDetailsFetched.incrementAndGet();
            checkFriendsCountUpdate(2, friendDetailsFetched.get(), totalFriendQueries);
        } else {
            db.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            Friend friend = new Friend(
                                    userDoc.getString("uid"),
                                    userDoc.getString("email"),
                                    userDoc.getString("fname"),
                                    userDoc.getString("lname"),
                                    userDoc.getString("profile_picture")
                            );
                            friendCache.put(friendId, friend);
                            if (!friendsList.contains(friend)) {
                                friendsList.add(friend);
                            }
                            Log.d(TAG, "New friend added: " + friend.getFullName());
                        } else {
                            Log.w(TAG, "Friend user not found: " + friendId);
                        }
                        friendDetailsFetched.incrementAndGet();
                        checkFriendsCountUpdate(2, friendDetailsFetched.get(), totalFriendQueries);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load friend details for " + friendId + ": " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Failed to load friend: " + friendId, Toast.LENGTH_SHORT).show();
                        friendDetailsFetched.incrementAndGet();
                        checkFriendsCountUpdate(2, friendDetailsFetched.get(), totalFriendQueries);
                    });
        }
    }

    private void checkFriendsCountUpdate(int completedQueries, int friendDetailsFetched, int totalFriendQueries) {
        Log.d(TAG, "Completed Queries: " + completedQueries + ", Friend Details Fetched: " + friendDetailsFetched + ", Total: " + totalFriendQueries);
        if (completedQueries == 2 && (totalFriendQueries == 0 || friendDetailsFetched >= totalFriendQueries)) {
            friendsCount.setText(String.valueOf(friendsList.size()));
            Log.d(TAG, "Final friends count: " + friendsList.size());
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
        if (targetUid == null || currentUserUid == null || targetUid.equals(currentUserUid)) {
            Toast.makeText(getContext(), "Cannot send friend request to yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("friend_requests")
                .whereEqualTo("sender_uid", currentUserUid)
                .whereEqualTo("receiver_uid", targetUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Map<String, Object> friendRequest = new HashMap<>();
                        friendRequest.put("sender_uid", currentUserUid);
                        friendRequest.put("receiver_uid", targetUid);
                        friendRequest.put("status", "Pending");
                        friendRequest.put("timestamp", System.currentTimeMillis());

                        db.collection("friend_requests")
                                .add(friendRequest)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getContext(), "Friend request sent!", Toast.LENGTH_SHORT).show();
                                    addFriendButton.setEnabled(false);
                                    addFriendButton.setText("Request Sent");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to send friend request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(getContext(), "Friend request already sent!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking friend request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        cachePrefs.edit().clear().apply();
        mAuth.signOut();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}