package com.example.splitaura.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.ManageFriends;
import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendAdapter;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.Friend;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    // Constant key used for updating status.
    static final String STATUS_KEY = "status";
    private RecyclerView recyclerView;
    private FriendAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserUid;
    private LinearLayout placeholder;
    private LoadingDialog loadingDialog;

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
        Button addFriend = view.findViewById(R.id.btn_add_friends);
        loadingDialog = new LoadingDialog(requireContext());

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

        // Toggle placeholder visibility based on friend list existence.
        query.addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null && value.isEmpty()) {
                placeholder.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                placeholder.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        // Instead of immediately removing the friend, show the custom alert dialog.
        adapter.setOnRemoveFriendListener(this::showRemoveFriendDialog);
    }

    // Builds and shows the custom alert dialog.
    private void showRemoveFriendDialog(DocumentSnapshot snapshot, Friend friend) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_remove_friend, null);
        ImageView profileImageView = dialogView.findViewById(R.id.ivDialogProfile);
        TextView nameTextView = dialogView.findViewById(R.id.tvDialogName);
        TextView alertTextView = dialogView.findViewById(R.id.tvDialogAlertText);
        MaterialButton cancelButton = dialogView.findViewById(R.id.btnCancel);
        MaterialButton removeButton = dialogView.findViewById(R.id.btnRemove);

        // Load profile image using Glide.
        Glide.with(getContext())
                .load(friend.getProfile_picture())
                .circleCrop()
                .into(profileImageView);

        // Set the friend’s full name.
        String fullName = friend.getFname() + " " + friend.getLname();
        nameTextView.setText(fullName);
        // Set a custom alert text.
        alertTextView.setText("Are you sure you want to remove this friend?");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        removeButton.setOnClickListener(v -> {
            // Show loading dialog and dismiss the alert dialog.
            loadingDialog.show();
            dialog.dismiss();
            removeFriend(snapshot);
        });

        dialog.show();
    }

    private void removeFriend(DocumentSnapshot snapshot) {
        String friendUid = snapshot.getId();
        // Delete friend records from both users
        Task<Void> deleteFromCurrentUser = snapshot.getReference().delete();
        Task<Void> deleteFromFriend = db.collection("users")
                .document(friendUid)
                .collection("friends")
                .document(currentUserUid)
                .delete();

        Tasks.whenAll(deleteFromCurrentUser, deleteFromFriend)
                .continueWithTask(task -> {
                    // Query friend requests in both directions (assuming they are in "Accepted" status)
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
                    return Tasks.whenAllSuccess(query1, query2);
                })
                .continueWithTask(task -> {
                    // Remove friend request documents completely
                    List<Object> results = task.getResult();
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (Object res : results) {
                        QuerySnapshot qs = (QuerySnapshot) res;
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            deleteTasks.add(doc.getReference().delete());
                        }
                    }
                    return Tasks.whenAll(deleteTasks);
                })
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Friend removed", Toast.LENGTH_SHORT).show();
                    loadingDialog.hide();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error removing friend: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingDialog.hide();
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
