package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.MemberAdapter;
import com.example.splitaura.models.Friend;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupInfoFragment extends Fragment {
    private static final String TAG = "GroupInfoFragment";
    private TextView tvGroupTitle, tvDescriptionContent, tvCurrencyContent, tvCategoryContent;
    private Button btnAddMember;
    private RecyclerView recyclerViewMembers;
    private MemberAdapter memberAdapter;
    private String groupId;
    private FirebaseFirestore db;
    private ListenerRegistration groupListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_info, container, false);
        db = FirebaseFirestore.getInstance();
        groupId = getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID);

        tvGroupTitle = view.findViewById(R.id.tv_group_title);
        tvDescriptionContent = view.findViewById(R.id.tv_description_content);
        tvCurrencyContent = view.findViewById(R.id.tv_currency_content);
        tvCategoryContent = view.findViewById(R.id.tv_category_content);
        btnAddMember = view.findViewById(R.id.btn_add_member);
        recyclerViewMembers = view.findViewById(R.id.recycler_view_members);

        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        memberAdapter = new MemberAdapter(new ArrayList<>());
        recyclerViewMembers.setAdapter(memberAdapter);

        observeGroupData();

        btnAddMember.setOnClickListener(v -> Toast.makeText(getContext(), "Add Member clicked", Toast.LENGTH_SHORT).show());

        return view;
    }

    private void observeGroupData() {
        if (groupId == null) {
            Log.e(TAG, "Group ID is null");
            return;
        }

        groupListener = db.collection("groups").document(groupId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching group data: ", e);
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        tvGroupTitle.setText(doc.getString("title"));
                        tvDescriptionContent.setText(doc.getString("description"));
                        tvCurrencyContent.setText(doc.getString("currency"));
                        tvCategoryContent.setText(doc.getString("category"));

                        List<String> memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds != null) {
                            fetchGroupMembers(memberIds);
                        }
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
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error fetching members", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupListener != null) {
            groupListener.remove();
        }
    }
}