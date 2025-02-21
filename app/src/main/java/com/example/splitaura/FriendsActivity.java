//package com.example.splitaura;
//
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.splitaura.adapters.UserAdapter;
//import com.example.splitaura.admin.BaseActivity;
//import com.example.splitaura.dialogues.LoadingDialog;
//import com.example.splitaura.models.User;
//import com.firebase.ui.firestore.FirestoreRecyclerOptions;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.FieldValue;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.Query;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class FriendsActivity extends BaseActivity {
//
//    private FirebaseFirestore db = FirebaseFirestore.getInstance();
//    private UserAdapter adapter;
//    private RecyclerView recyclerView;
//    private EditText searchView;
//    private LoadingDialog dialog;
//    private FirebaseAuth mAuth;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.friends_activity);
//
//        searchView = findViewById(R.id.searchView);
//        recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        mAuth = FirebaseAuth.getInstance();
//
//        // Initialize your LoadingDialog (ensure it's not null)
//        dialog = new LoadingDialog(this);
//
//        // Load all users initially
//        loadUsers("");
//
//        // Listen for text changes for dynamic search
//        searchView.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                // No action needed
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                loadUsers(s.toString());
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                // No action needed
//            }
//        });
//    }
//
//    private void loadUsers(String searchText) {
//        Query query;
//        if (searchText.isEmpty()) {
//            query = db.collection("users");
//        } else {
//            // Make sure the search text is lowercase to match the stored keywords
//            query = db.collection("users")
//                    .whereArrayContains("searchKeywords", searchText.toLowerCase());
//        }
//
//        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
//                .setQuery(query, User.class)
//                .build();
//
//        if (adapter != null) {
//            adapter.stopListening();
//        }
//
//        adapter = new UserAdapter(options);
//        recyclerView.setAdapter(adapter);
//
//        adapter.setOnUserRequestClickListener(snapshot -> {
//            dialog.show();
//            // Use snapshot.getId() as the receiver's UID.
//            String receiverUid = snapshot.getId();
//            sendFriendRequest(mAuth.getCurrentUser().getUid(), receiverUid);
//            String fullName = snapshot.getString("fname") + " " + snapshot.getString("lname");
//            Toast.makeText(FriendsActivity.this, "Request sent to " + fullName, Toast.LENGTH_SHORT).show();
//        });
//
//        adapter.startListening();
//    }
//
//    private void sendFriendRequest(String senderUid, String receiverUid) {
//        // Build the friend request data
//        Map<String, Object> requestData = new HashMap<>();
//        requestData.put("sender_uid", senderUid);
//        requestData.put("receiver_uid", receiverUid);
//        requestData.put("status", "Pending");
//        requestData.put("sentAt", FieldValue.serverTimestamp());
//
//        // Add the document to the friend_requests collection
//        db.collection("friend_requests")
//                .add(requestData)
//                .addOnSuccessListener(documentReference -> {
//                    dialog.hide();
//                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    dialog.hide();
//                    Toast.makeText(this, "Error sending friend request", Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (adapter != null) adapter.startListening();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (adapter != null) adapter.stopListening();
//    }
//}
package com.example.splitaura;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.UserAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class FriendsActivity extends BaseActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserAdapter adapter;
    private RecyclerView recyclerView;
    private EditText searchView;
    private LoadingDialog dialog;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friends_activity);

        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();

        // Initialize your LoadingDialog (ensure it's properly implemented)
        dialog = new LoadingDialog(this);

        // Load all users initially
        loadUsers("");

        // Listen for text changes for dynamic search
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadUsers(String searchText) {
        Query query;
        if (searchText.isEmpty()) {
            query = db.collection("users");
        } else {
            // Make sure the search text is lowercase to match stored keywords
            query = db.collection("users")
                    .whereArrayContains("searchKeywords", searchText.toLowerCase());
        }

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new UserAdapter(options);
        recyclerView.setAdapter(adapter);

        adapter.setOnUserRequestClickListener(snapshot -> {
            dialog.show();
            // Use snapshot.getId() as the receiver's UID.
            String receiverUid = snapshot.getId();
            // Retrieve current sender details (replace these with your actual profile details)
            String senderUid = mAuth.getCurrentUser().getUid();
            String senderName = snapshot.getString("fname") + " " + snapshot.getString("lname");
            String senderProfilePicture = snapshot.getString("profile_picture");

            sendFriendRequest(senderUid, senderName, senderProfilePicture, receiverUid);
            String fullName = snapshot.getString("fname") + " " + snapshot.getString("lname");
            Toast.makeText(FriendsActivity.this, "Request sent to " + fullName, Toast.LENGTH_SHORT).show();
        });

        adapter.startListening();
    }

    private void sendFriendRequest(String senderUid, String senderName, String senderProfilePicture, String receiverUid) {
        // Build the friend request data
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("sender_uid", senderUid);
        requestData.put("senderName", senderName);
        requestData.put("senderProfilePicture", senderProfilePicture);
        requestData.put("receiver_uid", receiverUid);
        requestData.put("status", "Pending");
        requestData.put("sentAt", FieldValue.serverTimestamp());

        // Add the document to the friend_requests collection
        db.collection("friend_requests")
                .add(requestData)
                .addOnSuccessListener(documentReference -> {
                    dialog.hide();
                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    dialog.hide();
                    Toast.makeText(this, "Error sending friend request", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}
