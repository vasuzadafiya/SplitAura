package com.example.splitaura.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.adapters.UserAdapter;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class AddFriendFragment extends Fragment {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserAdapter adapter;
    private RecyclerView recyclerView;
    private EditText searchView;
    private LoadingDialog dialog;
    private FirebaseAuth mAuth;

    public AddFriendFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_friend, container, false);

        searchView = view.findViewById(R.id.searchView);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        dialog = new LoadingDialog(requireContext());

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

        return view;
    }

    private void loadUsers(String searchText) {
        String currentUid = mAuth.getCurrentUser().getUid();
        Query query;

        if (searchText.isEmpty()) {
            query = db.collection("users")
                    .whereNotEqualTo(FieldPath.documentId(), currentUid);
        } else {
            query = db.collection("users")
                    .whereNotEqualTo(FieldPath.documentId(), currentUid)
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
            String receiverUid = snapshot.getId();
            String receiverFullName = snapshot.getString("fname") + " " + snapshot.getString("lname");
            String senderUid = mAuth.getCurrentUser().getUid();

            db.collection("users")
                    .document(senderUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String senderFname = documentSnapshot.getString("fname");
                            String senderLname = documentSnapshot.getString("lname");
                            String senderName = senderFname + " " + senderLname;
                            String senderProfilePicture = documentSnapshot.getString("profile_picture");

                            sendFriendRequest(senderUid, senderName, senderProfilePicture, receiverUid, receiverFullName);
                            dialog.hide();
                        } else {
                            dialog.hide();
                            Toast.makeText(getContext(), "Sender details not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        dialog.hide();
                        Toast.makeText(getContext(), "Error retrieving sender info", Toast.LENGTH_SHORT).show();
                    });
        });

        adapter.startListening();
    }

    private void sendFriendRequest(String senderUid, String senderName, String senderProfilePicture, String receiverUid, String receiverFullName) {
        db.collection("friend_requests")
                .whereEqualTo("sender_uid", senderUid)
                .whereEqualTo("receiver_uid", receiverUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Map<String, Object> requestData = new HashMap<>();
                        requestData.put("sender_uid", senderUid);
                        requestData.put("senderName", senderName);
                        requestData.put("senderProfilePicture", senderProfilePicture);
                        requestData.put("receiver_uid", receiverUid);
                        requestData.put("status", "Pending");
                        requestData.put("sentAt", FieldValue.serverTimestamp());

                        db.collection("friend_requests")
                                .add(requestData)
                                .addOnSuccessListener(documentReference -> {
                                    dialog.hide();
                                    Toast.makeText(getContext(), "Request sent to " + receiverFullName, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    dialog.hide();
                                    Toast.makeText(getContext(), "Error sending friend request", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        dialog.hide();
                        Toast.makeText(getContext(), "Friend request already sent!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.hide();
                    Toast.makeText(getContext(), "Error checking existing friend requests", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}
