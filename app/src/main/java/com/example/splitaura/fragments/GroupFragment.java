package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.AddGroupActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.GroupAdapter;
import com.example.splitaura.models.GroupItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupFragment extends Fragment {

    private Button addGroup;
    private RecyclerView groupRecyclerView;
    private GroupAdapter groupAdapter;
    private List<GroupItem> groupList;

    public GroupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        // Initialize RecyclerView
        groupRecyclerView = view.findViewById(R.id.group_recyclerView);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize Group List
        groupList = new ArrayList<>();
        initializeGroupList();

        // Initialize and set GroupAdapter
        groupAdapter = new GroupAdapter(getContext(), groupList);
        groupRecyclerView.setAdapter(groupAdapter);

        addGroup = view.findViewById(R.id.activity_add_group);
        addGroup.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddGroupActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void initializeGroupList() {
        // Sample Data
        groupList.add(new GroupItem(
                "Office Trip", "May 23, 2024", 9235.34,
                R.drawable.ic_group,
                Arrays.asList(R.drawable.image, R.drawable.obwomens, R.drawable.obmensmall),
                12));

        groupList.add(new GroupItem(
                "Weekend Party", "Feb 10, 2024", 4850.50,
                R.drawable.ic_group,
                Arrays.asList(R.drawable.obmensmall, R.drawable.image, R.drawable.obwomensmall),
                8));
    }
}