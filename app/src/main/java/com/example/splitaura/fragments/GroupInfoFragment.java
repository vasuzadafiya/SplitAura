package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.MemberAdapter;
import com.example.splitaura.models.Friend;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupInfoFragment extends Fragment {

    private static final String TAG = "GroupInfoFragment";
    private TextView tvGroupTitle, tvDescriptionContent, tvCurrencyContent, tvCategoryContent;
    private Button btnAddMember;
    private RecyclerView recyclerViewMembers;
    private MemberAdapter memberAdapter;
    private String groupId;
    private FirebaseFirestore db;
    private ListenerRegistration groupListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        fetchGroupData();

        btnAddMember.setOnClickListener(v -> Toast.makeText(getContext(), "Add Member clicked", Toast.LENGTH_SHORT).show());

        return view;
    }

    private void fetchGroupData() {
        if (groupId == null) {
            Log.e(TAG, "Group ID is null");
            return;
        }

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvGroupTitle.setText(documentSnapshot.getString("title"));
                        tvDescriptionContent.setText(documentSnapshot.getString("description"));
                        tvCurrencyContent.setText(documentSnapshot.getString("currency"));
                        tvCategoryContent.setText(documentSnapshot.getString("category"));
                        fetchGroupMembers();
                    } else {
                        Log.e(TAG, "Group document does not exist");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching group data: ", e));
    }

    private void fetchGroupMembers() {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> membersList = (List<Map<String, Object>>) documentSnapshot.get("members");
                        List<Friend> friends = new ArrayList<>();

                        if (membersList != null) {
                            for (Map<String, Object> member : membersList) {
                                Friend friend = new Friend();
                                friend.setUid((String) member.get("uid"));
                                friend.setEmail((String) member.get("email"));
                                friend.setFname((String) member.get("fname"));
                                friend.setLname((String) member.get("lname"));
                                friend.setProfile_picture((String) member.get("profile_picture"));
                                friends.add(friend);
                            }
                        }

                        // Use MemberAdapter instead of FirestoreRecyclerAdapter
                        memberAdapter.updateData(friends);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error fetching group members", Toast.LENGTH_SHORT).show());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupListener != null) {
            groupListener.remove();
        }
    }
}
