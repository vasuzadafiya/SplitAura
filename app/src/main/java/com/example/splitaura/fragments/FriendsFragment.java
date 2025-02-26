package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.ManageFriends;
import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendAdapter;
import com.example.splitaura.models.Friend;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    //    constant Keys...
    static final String STATUS_KEY = "status";

    private RecyclerView recyclerView;
    private FriendAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserUid;
    private LinearLayout placeholder;

    public FriendsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewFriends);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        placeholder = view.findViewById(R.id.placeholder);
        Button addFriend;
        addFriend = view.findViewById(R.id.btn_add_friends);

        // Set click listener for the Add Friends button in the placeholder
        addFriend.setOnClickListener(v -> {
            if (getActivity() instanceof ManageFriends) {
                ((ManageFriends) getActivity()).navigateToAddFriendFragment();
            }
        });

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            setupRecyclerView();
        } else {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    private void setupRecyclerView() {
        Query query = db.collection("users")
                .document(currentUserUid)
                .collection("friends");

        FirestoreRecyclerOptions<Friend> options = new FirestoreRecyclerOptions.Builder<Friend>()
                .setQuery(query, Friend.class)
                .build();

        adapter = new FriendAdapter(options);
        recyclerView.setAdapter(adapter);

        // Listen for real-time changes and toggle placeholder visibility
        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                return;
            }

            if (value != null && value.isEmpty()) {
                // No friends: show placeholder, hide list
                placeholder.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                // Friends exist: hide placeholder, show list
                placeholder.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        // Set up the remove friend listener
        adapter.setOnRemoveFriendListener((DocumentSnapshot snapshot, Friend friend) -> {
            String friendUid = snapshot.getId();
            Task<Void> deleteFromCurrentUser = snapshot.getReference().delete();
            Task<Void> deleteFromFriend = db.collection("users")
                    .document(friendUid)
                    .collection("friends")
                    .document(currentUserUid)
                    .delete();

            Tasks.whenAll(deleteFromCurrentUser, deleteFromFriend)
                    .addOnSuccessListener(unused -> {
                        Task<QuerySnapshot> query1 = db.collection("friend_requests")
                                .whereEqualTo("sender_uid", currentUserUid)
                                .whereEqualTo("receiver_uid", friendUid)
                                .whereEqualTo(STATUS_KEY, "Accepted")
                                .get();
                        Task<QuerySnapshot> query2 = db.collection("friend_requests")
                                .whereEqualTo("sender_uid", friendUid)
                                .whereEqualTo("receiver_uid", currentUserUid)
                                .whereEqualTo(STATUS_KEY, "Accepted")
                                .get();

                        Tasks.whenAllSuccess(query1, query2)
                                .addOnSuccessListener(results -> {
                                    List<Task<Void>> updateTasks = new ArrayList<>();
                                    for (Object res : results) {
                                        QuerySnapshot qs = (QuerySnapshot) res;
                                        for (DocumentSnapshot doc : qs.getDocuments()) {
                                            updateTasks.add(doc.getReference().update(STATUS_KEY, "Removed"));
                                        }
                                    }
                                    Tasks.whenAll(updateTasks)
                                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Friend removed but error updating friend request", Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Friend removed but error querying friend requests", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error removing friend", Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
