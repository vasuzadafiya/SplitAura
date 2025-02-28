package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
    //    constants
    private static final String USER = "users";
    private static final String FRIEND_REQUESTS = "friend_requests";
    private static final String EMAIL = "email";
    private static final String FNAME = "fname";
    private static final String LNAME = "lname";
    private static final String PROFILE_PICTURE = "profile_picture";
    private static final String UID = "uid";

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private String currentUserUid;
    // This view is displayed when no friend requests are found.
    private LinearLayout placeholder;

    public FriendRequestFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_request, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            setRecyclerView();
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewFriendRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Ensure your layout contains a view with the id "placeholder"
        placeholder = view.findViewById(R.id.placeholder);
    }


    private void setRecyclerView() {
        // Build query for pending friend requests
        Query friendRequestsQuery = db.collection(FRIEND_REQUESTS)
                .whereEqualTo("receiver_uid", currentUserUid)
                .whereEqualTo("status", "Pending");

        // One-time query to count documents and show a toast.
        friendRequestsQuery.get().addOnSuccessListener(querySnapshot -> initializeViews(querySnapshot.size())).addOnFailureListener(e -> Toast.makeText(requireContext(), "Error fetching friend requests", Toast.LENGTH_SHORT).show());

        // Set up adapter options
        FirestoreRecyclerOptions<FriendRequest> options =
                new FirestoreRecyclerOptions.Builder<FriendRequest>()
                        .setQuery(friendRequestsQuery, FriendRequest.class)
                        .build();

        FriendRequestAdapter adapter;
        adapter = new FriendRequestAdapter(options);
        recyclerView.setAdapter(adapter);

        // Start listening (one-time load, not continuous realtime updates)
        adapter.startListening();

        // Existing acceptance logic remains unchanged.
        adapter.setOnRequestActionListener((DocumentSnapshot snapshot, FriendRequest request) -> {
            if (snapshot.exists()) {
                snapshot.getReference().update("status", "Accepted")
                        .addOnSuccessListener(unused -> {
                            String senderUid = snapshot.getString("sender_uid");
                            String receiverUid = snapshot.getString("receiver_uid");

                            assert senderUid != null;
                            db.collection(USER).document(senderUid).get()
                                    .addOnSuccessListener(documentSnapshotSender -> {
                                        if (documentSnapshotSender.exists()) {
                                            String senderEmail = documentSnapshotSender.getString(EMAIL);
                                            String senderFname = documentSnapshotSender.getString(FNAME);
                                            String senderLname = documentSnapshotSender.getString(LNAME);
                                            String senderProfilePicture = documentSnapshotSender.getString(PROFILE_PICTURE);

                                            Map<String, Object> senderData = new HashMap<>();
                                            senderData.put(EMAIL, senderEmail);
                                            senderData.put(FNAME, senderFname);
                                            senderData.put(LNAME, senderLname);
                                            senderData.put(PROFILE_PICTURE, senderProfilePicture);
                                            senderData.put(UID, senderUid);

                                            assert receiverUid != null;
                                            db.collection(USER).document(receiverUid).get()
                                                    .addOnSuccessListener(documentSnapshotReceiver -> {
                                                        if (documentSnapshotReceiver.exists()) {
                                                            String receiverEmail = documentSnapshotReceiver.getString(EMAIL);
                                                            String receiverFname = documentSnapshotReceiver.getString(FNAME);
                                                            String receiverLname = documentSnapshotReceiver.getString(LNAME);
                                                            String receiverProfilePicture = documentSnapshotReceiver.getString(PROFILE_PICTURE);

                                                            Map<String, Object> receiverData = new HashMap<>();
                                                            receiverData.put(EMAIL, receiverEmail);
                                                            receiverData.put(FNAME, receiverFname);
                                                            receiverData.put(LNAME, receiverLname);
                                                            receiverData.put(PROFILE_PICTURE, receiverProfilePicture);
                                                            receiverData.put(UID, receiverUid);

                                                            Task<Void> task1 = db.collection(USER).document(senderUid)
                                                                    .collection("friends").document(receiverUid)
                                                                    .set(receiverData);
                                                            Task<Void> task2 = db.collection(USER).document(receiverUid)
                                                                    .collection("friends").document(senderUid)
                                                                    .set(senderData);

                                                            Tasks.whenAll(task1, task2)
                                                                    .addOnSuccessListener(task -> Toast.makeText(requireContext(), "Friend request accepted and friends added", Toast.LENGTH_SHORT).show())
                                                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error adding friends", Toast.LENGTH_SHORT).show());
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error fetching receiver details", Toast.LENGTH_SHORT).show());
                                        }
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error fetching sender details", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error accepting request", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void initializeViews(int count) {
        if (count == 0) {
            placeholder.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            placeholder.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

}
