package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendRequestAdapter;
import com.example.splitaura.models.FriendRequest;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class FriendRequestFragment extends Fragment {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserUid;

    public FriendRequestFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_friend_request, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);

        // Check if user is authenticated
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            setupRecyclerView();
        } else {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewFriendRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupRecyclerView() {
        // Build query for pending friend requests ordered by sentAt in descending order.
        Query friendRequestsQuery = db.collection("friend_requests")
                .whereEqualTo("receiver_uid", currentUserUid)
                .whereEqualTo("status", "Pending")
                .orderBy("sentAt", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<FriendRequest> options =
                new FirestoreRecyclerOptions.Builder<FriendRequest>()
                        .setQuery(friendRequestsQuery, FriendRequest.class)
                        .build();

        adapter = new FriendRequestAdapter(options);
        recyclerView.setAdapter(adapter);

        // Start listening once; omit onStart/onStop for a one‑time update.
        adapter.startListening();

        adapter.setOnRequestActionListener((DocumentSnapshot snapshot, FriendRequest request) -> {
            if (snapshot.exists()) {
                snapshot.getReference().update("status", "Accepted")
                        .addOnSuccessListener(unused -> {
                            // Retrieve sender and receiver UIDs from the friend request document
                            String senderUid = snapshot.getString("sender_uid");
                            String receiverUid = snapshot.getString("receiver_uid");

                            // Fetch sender details
                            db.collection("users").document(senderUid).get()
                                    .addOnSuccessListener(documentSnapshotSender -> {
                                        if (documentSnapshotSender.exists()) {
                                            String senderEmail = documentSnapshotSender.getString("email");
                                            String senderFname = documentSnapshotSender.getString("fname");
                                            String senderLname = documentSnapshotSender.getString("lname");
                                            String senderProfilePicture = documentSnapshotSender.getString("profile_picture");

                                            Map<String, Object> senderData = new HashMap<>();
                                            senderData.put("email", senderEmail);
                                            senderData.put("fname", senderFname);
                                            senderData.put("lname", senderLname);
                                            senderData.put("profile_picture", senderProfilePicture);

                                            // Fetch receiver details
                                            db.collection("users").document(receiverUid).get()
                                                    .addOnSuccessListener(documentSnapshotReceiver -> {
                                                        if (documentSnapshotReceiver.exists()) {
                                                            String receiverEmail = documentSnapshotReceiver.getString("email");
                                                            String receiverFname = documentSnapshotReceiver.getString("fname");
                                                            String receiverLname = documentSnapshotReceiver.getString("lname");
                                                            String receiverProfilePicture = documentSnapshotReceiver.getString("profile_picture");

                                                            Map<String, Object> receiverData = new HashMap<>();
                                                            receiverData.put("email", receiverEmail);
                                                            receiverData.put("fname", receiverFname);
                                                            receiverData.put("lname", receiverLname);
                                                            receiverData.put("profile_picture", receiverProfilePicture);

                                                            // Update both friend subcollections concurrently:
                                                            Task<Void> task1 = db.collection("users").document(senderUid)
                                                                    .collection("friends").document(receiverUid)
                                                                    .set(receiverData);
                                                            Task<Void> task2 = db.collection("users").document(receiverUid)
                                                                    .collection("friends").document(senderUid)
                                                                    .set(senderData);

                                                            Tasks.whenAll(task1, task2)
                                                                    .addOnSuccessListener(task -> {
                                                                        Toast.makeText(getContext(), "Friend request accepted and friends added", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(getContext(), "Error adding friends", Toast.LENGTH_SHORT).show();
                                                                        e.printStackTrace();
                                                                    });
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getContext(), "Error fetching receiver details", Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Error fetching sender details", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error accepting request", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        });
            }
        });
    }
}
