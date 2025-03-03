package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.models.GroupData;
import com.example.splitaura.R;
import com.example.splitaura.adapters.MemberAdapter;
import com.example.splitaura.adapters.MemberData;

import java.util.Arrays;
import java.util.List;

public class GroupInfoFragment extends Fragment {

    private TextView tvGroupTitle, tvDescription, tvDescriptionContent, tvCurrency, tvCurrencyContent, tvCategory, tvCategoryContent;
    private ImageView ivDropdownArrow;
    private Button btnAddMember;
    private RecyclerView recyclerViewMembers;
    private MemberAdapter memberAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_info, container, false);

        tvGroupTitle = view.findViewById(R.id.tv_group_title);
        tvDescription = view.findViewById(R.id.tv_description);
        tvDescriptionContent = view.findViewById(R.id.tv_description_content);
        tvCurrency = view.findViewById(R.id.tv_currency);
        tvCurrencyContent = view.findViewById(R.id.tv_currency_content);
        tvCategory = view.findViewById(R.id.tv_category);
        tvCategoryContent = view.findViewById(R.id.tv_category_content);
        btnAddMember = view.findViewById(R.id.btn_add_member);
        recyclerViewMembers = view.findViewById(R.id.recycler_view_members);

        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        memberAdapter = new MemberAdapter(getStaticMembers());
        recyclerViewMembers.setAdapter(memberAdapter);

        setStaticGroupData();

        btnAddMember.setOnClickListener(v -> Toast.makeText(getContext(), "Add Member clicked", Toast.LENGTH_SHORT).show());

        return view;
    }

    private void setStaticGroupData() {
        GroupData group = new GroupData(
                "Trip to Paris",
                "Vacationing with college friends in Paris.",
                "INR",
                "Trip",
                getStaticMembers()
        );

        tvGroupTitle.setText(group.getTitle());
        tvDescriptionContent.setText(group.getDescription());
        tvCurrencyContent.setText(group.getCurrency());
        tvCategoryContent.setText(group.getCategory());
    }

    private List<MemberData> getStaticMembers() {
        return Arrays.asList(
                new MemberData("Alexa Hershey", "alexa.hershey123@gmail.com", R.drawable.ic_profile),
                new MemberData("Alfaza Schluessler", "alfaza.schluessler@yahoo.com", R.drawable.ic_profile),
                new MemberData("Augustina Midgett", "augustina.midgett45@gmail.com", R.drawable.ic_profile)
        );
    }
}