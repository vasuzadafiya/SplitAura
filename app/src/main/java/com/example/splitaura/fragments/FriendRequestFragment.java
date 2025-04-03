package com.example.splitaura.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.R;
import com.example.splitaura.adapters.FriendRequestAdapter;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.FriendRequest;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class FriendRequestFragment extends Fragment {
    // Constants for Firestore collection/field names.
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
    private LinearLayout placeholder;
    private LoadingDialog dialog;

    public FriendRequestFragment() {
        // Required empty public constructor.
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
        placeholder = view.findViewById(R.id.placeholder);
        dialog = new LoadingDialog(requireContext());
    }

    private void setRecyclerView() {
        // Query for pending friend requests.
        Query friendRequestsQuery = db.collection(FRIEND_REQUESTS)
                .whereEqualTo("receiver_uid", currentUserUid)
                .whereEqualTo("status", "Pending");

        FirestoreRecyclerOptions<FriendRequest> options =
                new FirestoreRecyclerOptions.Builder<FriendRequest>()
                        .setQuery(friendRequestsQuery, FriendRequest.class)
                        .build();

        // Override onDataChanged() to update the UI based on the current data count.
        FriendRequestAdapter adapter = new FriendRequestAdapter(options) {
            @Override
            public void onDataChanged() {
                if (getItemCount() == 0) {
                    placeholder.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    placeholder.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.setOnRequestActionListener(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(DocumentSnapshot snapshot, FriendRequest request) {
                dialog.show();
                if (!snapshot.exists()) {
                    dialog.hide();
                    return;
                }

                // Update request status to "Accepted".
                snapshot.getReference().update("status", "Accepted")
                        .addOnSuccessListener(unused -> {
                            String senderUid = snapshot.getString("sender_uid");
                            String receiverUid = snapshot.getString("receiver_uid");

                            if (senderUid == null || receiverUid == null) {
                                Toast.makeText(requireContext(), "Invalid sender or receiver", Toast.LENGTH_SHORT).show();
                                dialog.hide();
                                return;
                            }

                            // Fetch sender details.
                            db.collection(USER).document(senderUid).get()
                                    .addOnSuccessListener(documentSnapshotSender -> {
                                        if (!documentSnapshotSender.exists()) {
                                            Toast.makeText(requireContext(), "Sender details not found", Toast.LENGTH_SHORT).show();
                                            dialog.hide();
                                            return;
                                        }
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

                                        // Fetch receiver details.
                                        db.collection(USER).document(receiverUid).get()
                                                .addOnSuccessListener(documentSnapshotReceiver -> {
                                                    if (!documentSnapshotReceiver.exists()) {
                                                        Toast.makeText(requireContext(), "Receiver details not found", Toast.LENGTH_SHORT).show();
                                                        dialog.hide();
                                                        return;
                                                    }
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

                                                    // Add each other as friends.
                                                    Task<Void> task1 = db.collection(USER).document(senderUid)
                                                            .collection("friends").document(receiverUid)
                                                            .set(receiverData);
                                                    Task<Void> task2 = db.collection(USER).document(receiverUid)
                                                            .collection("friends").document(senderUid)
                                                            .set(senderData);

                                                    Tasks.whenAll(task1, task2)
                                                            .addOnSuccessListener(task -> {
                                                                Toast.makeText(requireContext(),
                                                                        "Friend request accepted and friends added", Toast.LENGTH_SHORT).show();
                                                                // Refresh fragment.
                                                                getParentFragmentManager().beginTransaction()
                                                                        .detach(FriendRequestFragment.this)
                                                                        .attach(FriendRequestFragment.this)
                                                                        .commit();
                                                                dialog.hide();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(requireContext(),
                                                                        "Error adding friends", Toast.LENGTH_SHORT).show();
                                                                dialog.hide();
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(requireContext(),
                                                            "Error fetching receiver details", Toast.LENGTH_SHORT).show();
                                                    dialog.hide();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(),
                                                "Error fetching sender details", Toast.LENGTH_SHORT).show();
                                        dialog.hide();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Error accepting request", Toast.LENGTH_SHORT).show();
                            dialog.hide();
                        });
            }

            @Override
            public void onReject(DocumentSnapshot snapshot, FriendRequest request) {
                showRejectRequestDialog(snapshot, request);
            }
        });
    }

    /**
     * Displays a confirmation dialog before rejecting a friend request.
     * If confirmed, the request document is deleted.
     */
    private void showRejectRequestDialog(DocumentSnapshot snapshot, FriendRequest request) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_remove_friend, null);
        ImageView profileImageView = dialogView.findViewById(R.id.ivDialogProfile);
        TextView nameTextView = dialogView.findViewById(R.id.tvDialogName);
        TextView alertTextView = dialogView.findViewById(R.id.tvDialogAlertText);
        MaterialButton cancelButton = dialogView.findViewById(R.id.btnCancel);
        MaterialButton rejectButton = dialogView.findViewById(R.id.btnRemove);
        rejectButton.setText("Reject");

        // Load the sender's profile image (from the friend request).
        Glide.with(getContext())
                .load(request.getSenderProfilePicture())
                .circleCrop()
                .into(profileImageView);

        // Set the sender’s name.
        nameTextView.setText(request.getSenderName());
        // Set a custom alert text for rejection.
        alertTextView.setText("Are you sure you want to reject this friend request?");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme);
        builder.setView(dialogView);
        AlertDialog confirmDialog = builder.create();
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        cancelButton.setOnClickListener(v -> confirmDialog.dismiss());
        rejectButton.setOnClickListener(v -> {
            dialog.show();
            confirmDialog.dismiss();
            // Delete the friend request document.
            snapshot.getReference().delete()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Friend request rejected", Toast.LENGTH_SHORT).show();
                        // Refresh fragment.
                        getParentFragmentManager().beginTransaction()
                                .detach(FriendRequestFragment.this)
                                .attach(FriendRequestFragment.this)
                                .commit();
                        dialog.hide();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error rejecting friend request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.hide();
                    });
        });

        confirmDialog.show();
    }
}
