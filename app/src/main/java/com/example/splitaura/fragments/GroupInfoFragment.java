package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.MemberAdapter;
import com.example.splitaura.models.Friend;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupInfoFragment extends Fragment {
    private static final String TAG = "GroupInfoFragment";

    private ViewPager2 viewPager;
    private RecyclerView recyclerViewMembers;
    private TextView tvGroupTitle, tvDescriptionContent, tvCurrencyContent, tvCategoryContent;
    private MemberAdapter memberAdapter;
    private String groupId;
    private FirebaseFirestore db;
    private ListenerRegistration groupListener;

    public GroupInfoFragment(ViewPager2 viewPager) {
        this.viewPager = viewPager;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_info, container, false);

        // Initialize Firestore and group ID
        db = FirebaseFirestore.getInstance();
        groupId = getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID);

        // Initialize UI components
        tvGroupTitle = view.findViewById(R.id.tv_group_title);
        tvDescriptionContent = view.findViewById(R.id.tv_description_content);
        tvCurrencyContent = view.findViewById(R.id.tv_currency_content);
        tvCategoryContent = view.findViewById(R.id.tv_category_content);
        recyclerViewMembers = view.findViewById(R.id.recycler_view_members);
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize MemberAdapter with ViewPager2
        memberAdapter = new MemberAdapter(new ArrayList<>(), getContext(), requireActivity().getSupportFragmentManager(), viewPager);
        recyclerViewMembers.setAdapter(memberAdapter);

        // Fetch and observe group data
        observeGroupData();

        return view;
    }

    private void observeGroupData() {
        if (groupId == null) {
            Log.e(TAG, "Group ID is null");
            Toast.makeText(getContext(), "Group ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        groupListener = db.collection("groups").document(groupId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching group data: ", e);
                        Toast.makeText(getContext(), "Error fetching group data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        // Fetch group details
                        String title = doc.getString("title");
                        String description = doc.getString("description");
                        String currency = doc.getString("currency");
                        String category = doc.getString("category");

                        // Update UI with group details
                        if (title != null) {
                            tvGroupTitle.setText(title);
                        } else {
                            tvGroupTitle.setText("Untitled Group");
                        }

                        if (description != null) {
                            tvDescriptionContent.setText(description);
                        } else {
                            tvDescriptionContent.setText("No description available");
                        }

                        if (currency != null) {
                            tvCurrencyContent.setText(currency);
                        } else {
                            tvCurrencyContent.setText("INR"); // Default currency
                        }

                        if (category != null) {
                            tvCategoryContent.setText(category);
                        } else {
                            tvCategoryContent.setText("General");
                        }

                        // Fetch group members
                        List<String> memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds != null && !memberIds.isEmpty()) {
                            fetchGroupMembers(memberIds);
                        } else {
                            memberAdapter.updateData(new ArrayList<>());
                            Toast.makeText(getContext(), "No members in this group", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Group not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchGroupMembers(List<String> memberIds) {
        db.collection("users").whereIn(FieldPath.documentId(), memberIds)
                .get()
                .addOnSuccessListener(query -> {
                    List<Friend> friends = new ArrayList<>();
                    for (QueryDocumentSnapshot userDoc : query) {
                        Friend friend = new Friend();
                        friend.setUid(userDoc.getId());
                        friend.setEmail(userDoc.getString("email"));
                        friend.setFname(userDoc.getString("fname"));
                        friend.setLname(userDoc.getString("lname"));
                        friend.setProfile_picture(userDoc.getString("profile_picture"));
                        friends.add(friend);
                    }
                    memberAdapter.updateData(friends);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching members: ", e);
                    Toast.makeText(getContext(), "Error fetching members: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupListener != null) {
            groupListener.remove();
            groupListener = null;
        }
    }
}