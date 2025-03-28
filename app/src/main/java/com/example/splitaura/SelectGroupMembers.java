package com.example.splitaura;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.ContactsAdapter;
import com.example.splitaura.adapters.SelectedContactsAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.models.Friend;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectGroupMembers extends BaseActivity {
    // Views
    private TextView heading;
    private ImageButton back;
    private RecyclerView rvContacts;
    private RecyclerView rvSelectedContacts;
    private EditText etSearch;
    private MaterialButton btnCancel;
    private MaterialButton btnSave;
    private LinearLayout placeholder;
    private LinearLayout buttonContainer;

    // Data variables
    private String title;
    private String description;
    private String currency;
    private String category;
    private String imageUrl;
    private final List<Friend> allFriends = new ArrayList<>();
    private final List<Friend> friendList = new ArrayList<>();
    private final List<Friend> selectedFriends = new ArrayList<>();

    private ContactsAdapter contactsAdapter;
    private SelectedContactsAdapter selectedContactsAdapter;

    // Cached Firebase instances
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_members);
        initializeViews();

        // Cache Firebase instances for reuse
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set status bar transparency for Lollipop and above
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // Setup the friends RecyclerView (vertical list)
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        contactsAdapter = new ContactsAdapter(friendList, selectedFriends, friend -> {
            // Toggle selection on click
            if (!selectedFriends.contains(friend)) {
                selectedFriends.add(friend);
            } else {
                selectedFriends.remove(friend);
            }
            // Show or hide the selected friends RecyclerView based on selection count
            rvSelectedContacts.setVisibility(selectedFriends.isEmpty() ? View.GONE : View.VISIBLE);
            contactsAdapter.notifyDataSetChanged();
            selectedContactsAdapter.notifyDataSetChanged();
        });
        rvContacts.setAdapter(contactsAdapter);

        // Setup the selected friends RecyclerView (horizontal list)
        rvSelectedContacts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedContactsAdapter = new SelectedContactsAdapter(selectedFriends);
        rvSelectedContacts.setAdapter(selectedContactsAdapter);

        // Load data from Firestore
        loadData();

        // Search functionality with optimized query processing
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        // Handle Cancel and Save button actions
        btnCancel.setOnClickListener(view -> finish());
        btnSave.setOnClickListener(view -> uploadDataToFirestore(title, description, currency, category, imageUrl, selectedFriends));
        back.setOnClickListener(v -> finish());

        // Add friend button
        Button addFriend = findViewById(R.id.placeholder_button);
        addFriend.setOnClickListener(v -> {
            Intent intent = new Intent(SelectGroupMembers.this, ManageFriends.class);
            intent.putExtra("SelectAddFriend", true);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        back = findViewById(R.id.back_navigation);
        heading = findViewById(R.id.header_title);
        rvContacts = findViewById(R.id.rvContacts);
        placeholder = findViewById(R.id.placeholder);
        buttonContainer = findViewById(R.id.buttonContainer);
        rvSelectedContacts = findViewById(R.id.rvSelectedContacts);
        etSearch = findViewById(R.id.etSearch);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        title = getIntent().getStringExtra("title");
        description = getIntent().getStringExtra("description");
        currency = getIntent().getStringExtra("currency");
        category = getIntent().getStringExtra("category");
        imageUrl = getIntent().getStringExtra("imageUrl");
    }

    // Filter friends based on search query with optimized lower-case conversion
    private void filterFriends(String query) {
        if (query == null) query = "";
        String lowerQuery = query.toLowerCase();
        // If query is empty, reset to the full list
        if (lowerQuery.isEmpty()) {
            contactsAdapter.updateList(new ArrayList<>(allFriends));
            return;
        }
        List<Friend> filteredList = new ArrayList<>();
        for (Friend friend : allFriends) {
            // Cache lower-case conversion for friend fields
            String fullName = friend.getFullName().toLowerCase();
            String email = friend.getEmail().toLowerCase();
            if (fullName.contains(lowerQuery) || email.contains(lowerQuery)) {
                filteredList.add(friend);
            }
        }
        contactsAdapter.updateList(filteredList);
    }

    // Load friend data from Firestore and manage view visibility dynamically
    private void loadData() {
        db.collection("users").document(currentUser.getUid())
                .collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Clear existing lists
                    allFriends.clear();
                    friendList.clear();

                    // Convert each document into a Friend object
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Friend friend = document.toObject(Friend.class);
                        if (friend != null) {
                            allFriends.add(friend);
                        }
                    }

                    // Update friendList and notify adapter
                    friendList.addAll(allFriends);
                    contactsAdapter.notifyDataSetChanged();

                    // Dynamic view handling: If no friends found, show placeholder and hide others.
                    if (friendList.isEmpty()) {
                        placeholder.setVisibility(View.VISIBLE);
                        heading.setVisibility(View.GONE);
                        rvContacts.setVisibility(View.GONE);
                        buttonContainer.setVisibility(View.GONE);
                        rvSelectedContacts.setVisibility(View.GONE);
                        etSearch.setVisibility(View.GONE);
                    } else {
                        placeholder.setVisibility(View.GONE);
                        heading.setVisibility(View.VISIBLE);
                        rvContacts.setVisibility(View.VISIBLE);
                        buttonContainer.setVisibility(View.VISIBLE);
                        etSearch.setVisibility(View.VISIBLE);
                        // Show selected contacts only if there are selections.
                        rvSelectedContacts.setVisibility(selectedFriends.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(SelectGroupMembers.this, "Error fetching friends: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadDataToFirestore(String title, String description, String currency, String category, String imageUrl, List<Friend> selectedFriends) {
        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String uid = documentSnapshot.getString("uid");
                String email = documentSnapshot.getString("email");
                String fname = documentSnapshot.getString("fname");
                String lname = documentSnapshot.getString("lname");
                String profilePicture = documentSnapshot.getString("profile_picture");
                String upi = documentSnapshot.getString("upi");

                Friend friend = new Friend(uid, email, fname, lname, profilePicture,upi);
                selectedFriends.add(friend); // Ensure current user is also added

                // Proceed with Firestore upload **after** user is added to the list
                uploadGroupToFirestore(title, description, currency, category, imageUrl, selectedFriends);
            } else {
                Toast.makeText(this, "Error fetching user data!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Firestore error: " + e.getMessage());
        });
    }

    private void uploadGroupToFirestore(String title, String description, String currency, String category, String imageUrl, List<Friend> selectedFriends) {
        List<String> getFriendIds = new ArrayList<>();
        for (Friend friend : selectedFriends) {
            getFriendIds.add(friend.getUid());
        }

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("title", title);
        groupData.put("description", description);
        groupData.put("currency", currency);
        groupData.put("category", category);
        groupData.put("imageUrl", imageUrl);
        groupData.put("members", selectedFriends);
        groupData.put("createdBy", currentUser.getUid());
        groupData.put("createdAt", new Date());
        groupData.put("amount", null);
        groupData.put("memberIds", getFriendIds);

        db.collection("groups")
                .add(groupData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firestore error: " + e.getMessage());
                });
    }

}
