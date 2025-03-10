package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.google.firebase.auth.FirebaseAuth;

public class TotalsFragment extends Fragment {
    private TextView tvGroupSpending, tvYouPaid, tvYourShare;
    private ExpenseManager expenseManager;
    private String groupId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_totals, container, false);

        groupId = getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID);
        expenseManager = new ExpenseManager(groupId);

        tvGroupSpending = view.findViewById(R.id.tv_group_spending);
        tvYouPaid = view.findViewById(R.id.tv_you_paid);
        tvYourShare = view.findViewById(R.id.tv_your_share);

        observeTotals();

        return view;
    }

    private void observeTotals() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), data -> {
            double totalGroupSpending = data.values().stream()
                    .mapToDouble(userData -> userData.totalSpent)
                    .sum();
            ExpenseManager.UserExpenseData userData = data.get(currentUserId);

            if (userData != null) {
                tvGroupSpending.setText(String.format("₹%.2f", totalGroupSpending));
                tvYouPaid.setText(String.format("₹%.2f", userData.totalSpent));
                tvYourShare.setText(String.format("₹%.2f", userData.totalShare));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        expenseManager.removeListener();
    }
}