package com.example.splitaura;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.FriendRequestAdapter;
import com.example.splitaura.models.FriendRequest;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ReceivedFriendRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_friend_requests);

        recyclerView = findViewById(R.id.recyclerViewFriendRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Query friend requests where the current user is the receiver and status is "Pending"
        Query query = db.collection("friend_requests")
                .whereEqualTo("receiver_uid", currentUserUid)
                .whereEqualTo("status", "Pending")
                .orderBy("sentAt", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<FriendRequest> options = new FirestoreRecyclerOptions.Builder<FriendRequest>()
                .setQuery(query, FriendRequest.class)
                .build();

        adapter = new FriendRequestAdapter(options);
        recyclerView.setAdapter(adapter);

        adapter.setOnRequestActionListener((DocumentSnapshot snapshot, FriendRequest request) -> {
            // Update the friend request status to "accepted"
            snapshot.getReference().update("status", "accepted")
                    .addOnSuccessListener(unused ->
                            Toast.makeText(ReceivedFriendRequestsActivity.this, "Friend request accepted", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(ReceivedFriendRequestsActivity.this, "Error accepting request", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
