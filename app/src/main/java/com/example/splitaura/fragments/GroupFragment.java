package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.example.splitaura.AddGroupActivity;
import com.example.splitaura.R;

public class GroupFragment extends Fragment {

    private Button addGroup;

    public GroupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        addGroup = view.findViewById(R.id.activity_add_group);
        addGroup.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddGroupActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
