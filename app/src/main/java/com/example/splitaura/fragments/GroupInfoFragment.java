package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.MemberAdapter;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.Friend;
import com.example.splitaura.viewmodels.GroupViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class GroupInfoFragment extends Fragment {
    private static final String TAG = "GroupInfoFragment";
    private static final String GROUPS = "groups";
    private static final String ERROR = "Error";

    private RecyclerView recyclerViewMembers;
    private TextView tvGroupTitle;
    private TextView tvDescriptionContent;
    private TextView tvCurrencyContent;
    private TextView tvCategoryContent;
    private TextView tvMenuDots;
    private MemberAdapter memberAdapter;
    private String groupId;
    private String currentUserId;
    private FirebaseFirestore db;
    private ListenerRegistration groupListener;
    private LoadingDialog loadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_info, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        groupId = getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID);
        loadingDialog = new LoadingDialog(requireContext());

        initViews(view);
        setupRecyclerView();
        setupMenuDots();
        observeGroupData();

        return view;
    }

    private void initViews(View view) {
        tvGroupTitle = view.findViewById(R.id.tv_group_title);
        tvDescriptionContent = view.findViewById(R.id.tv_description_content);
        tvCurrencyContent = view.findViewById(R.id.tv_currency_content);
        tvCategoryContent = view.findViewById(R.id.tv_category_content);
        tvMenuDots = view.findViewById(R.id.tv_menu_dots);
        recyclerViewMembers = view.findViewById(R.id.recycler_view_members);
    }

    private void setupRecyclerView() {
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        memberAdapter = new MemberAdapter(new ArrayList<>(), getContext(),
                requireActivity().getSupportFragmentManager());
        recyclerViewMembers.setAdapter(memberAdapter);
    }

    private void setupMenuDots() {
        tvMenuDots.setOnClickListener(v -> showGroupOptionsMenu());
    }

    private void showGroupOptionsMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), tvMenuDots);
        popupMenu.getMenu().add("Delete Group");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Delete Group")) {
                verifyUserIsCreator();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void verifyUserIsCreator() {
        loadingDialog.show();
        db.collection(GROUPS).document(groupId)
                .get()
                .addOnSuccessListener(document -> {
                    loadingDialog.hide();
                    if (document.exists()) {
                        String creatorId = document.getString("createdBy");
                        if (creatorId != null && creatorId.equals(currentUserId)) {
                            showDeleteConfirmationDialog();
                        } else {
                            showPermissionDeniedDialog();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    loadingDialog.hide();
                    showErrorDialog("Error verifying permissions", e.getMessage());
                });
    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setTitle("Delete Group?")
                .setMessage("This will permanently delete the group and all its data.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    loadingDialog.show();
                    deleteGroup();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setTitle("Permission Denied")
                .setMessage("Only the group creator can delete this group.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showErrorDialog(String title, String message) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void deleteGroup() {
        WriteBatch batch = db.batch();
        batch.delete(db.collection(GROUPS).document(groupId));

        db.collection(GROUPS).document(groupId).collection("expenses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }
                    commitDeletionBatch(batch);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching expenses: ", e);
                    commitDeletionBatch(batch);
                });
    }

    private void commitDeletionBatch(WriteBatch batch) {
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.hide();
                    notifyHomeFragmentAndNavigate();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.hide();
                    handleDeletionFailure(e);
                });
    }

    private void notifyHomeFragmentAndNavigate() {
        // Notify HomeFragment through ViewModel
        if (getActivity() != null) {
            GroupViewModel groupViewModel = new ViewModelProvider(getActivity()).get(GroupViewModel.class);
            groupViewModel.setGroupDeleted(true);
        }

        // Navigate back
        requireActivity().finish();
    }

    private void handleDeletionFailure(Exception e) {
        String errorMsg = "Failed to delete group";

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) e;

            if (firestoreEx.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                errorMsg = "You don't have permission to delete this group";
            } else if (firestoreEx.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                errorMsg = "Network error - please check your connection";
            }
        }

        showErrorDialog("Deletion Failed", errorMsg);
    }


    private void observeGroupData() {
        if (groupId == null) {
            showErrorDialog(ERROR, "Group ID not found");
            return;
        }

        loadingDialog.show();
        groupListener = db.collection(GROUPS).document(groupId)
                .addSnapshotListener((doc, e) -> {
                    loadingDialog.hide();
                    if (e != null) {
                        showErrorDialog(ERROR, "Failed to load group data");
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        updateGroupUI(doc);
                        fetchMembers(doc);
                    }
                });
    }

    private void updateGroupUI(DocumentSnapshot doc) {
        tvGroupTitle.setText(doc.getString("title") != null ?
                doc.getString("title") : "Untitled Group");
        tvDescriptionContent.setText(doc.getString("description") != null ?
                doc.getString("description") : "No description");
        tvCurrencyContent.setText(doc.getString("currency") != null ?
                doc.getString("currency") : "INR");
        tvCategoryContent.setText(doc.getString("category") != null ?
                doc.getString("category") : "General");
    }

    private void fetchMembers(DocumentSnapshot doc) {
        List<String> memberIds = (List<String>) doc.get("memberIds");
        if (memberIds == null || memberIds.isEmpty()) {
            memberAdapter.updateData(new ArrayList<>());
            return;
        }

        loadingDialog.show();
        db.collection("users").whereIn(FieldPath.documentId(), memberIds)
                .get()
                .addOnSuccessListener(query -> {
                    loadingDialog.hide();
                    List<Friend> friends = new ArrayList<>();
                    for (QueryDocumentSnapshot userDoc : query) {
                        friends.add(new Friend(
                                userDoc.getId(),
                                userDoc.getString("email"),
                                userDoc.getString("fname"),
                                userDoc.getString("lname"),
                                userDoc.getString("profile_picture"),
                                userDoc.getString("upi")
                        ));
                    }
                    memberAdapter.updateData(friends);
                })
                .addOnFailureListener(e -> {
                    loadingDialog.hide();
                    showErrorDialog(ERROR, "Failed to load members");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupListener != null) {
            groupListener.remove();
        }
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
    }
}