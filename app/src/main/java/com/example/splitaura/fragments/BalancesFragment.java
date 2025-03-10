package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.BalanceAdapter;
import com.example.splitaura.adapters.OweAdapter;
import com.example.splitaura.models.Balance;
import com.example.splitaura.models.OweRelationship;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BalancesFragment extends Fragment {
    private static final String TAG = "BalancesFragment";
    private RecyclerView recyclerViewBalances, recyclerViewOwing;
    private BalanceAdapter balanceAdapter;
    private OweAdapter oweAdapter;
    private ExpenseManager expenseManager;
    private String groupId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balances, container, false);

        // Retrieve groupId and validate
        groupId = getActivity() != null && getActivity().getIntent() != null
                ? getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID)
                : null;

        if (groupId == null || groupId.isEmpty()) {
            Log.e(TAG, "Group ID is null or empty");
            Toast.makeText(getContext(), "Invalid Group ID", Toast.LENGTH_SHORT).show();
            return view; // Early return to avoid crashes
        }

        // Initialize ExpenseManager
        expenseManager = new ExpenseManager(groupId);

        // Set up RecyclerViews
        recyclerViewBalances = view.findViewById(R.id.recycler_view_balances);
        recyclerViewOwing = view.findViewById(R.id.recycler_view_owing);

        recyclerViewBalances.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewOwing.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapters with empty lists
        balanceAdapter = new BalanceAdapter(new ArrayList<>());
        oweAdapter = new OweAdapter(new ArrayList<>());
        recyclerViewBalances.setAdapter(balanceAdapter);
        recyclerViewOwing.setAdapter(oweAdapter);

        // Start observing balances
        observeBalances();

        return view;
    }

    private void observeBalances() {
        expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), data -> {
            if (data == null || data.isEmpty()) {
                Log.w(TAG, "No expense data available for group: " + groupId);
                balanceAdapter.updateBalances(new ArrayList<>());
                oweAdapter.updateOweRelationships(new ArrayList<>());
                return;
            }

            List<Balance> balances = new ArrayList<>();
            List<OweRelationship> oweRelationships = new ArrayList<>();

            for (Map.Entry<String, ExpenseManager.UserExpenseData> entry : data.entrySet()) {
                String userId = entry.getKey();
                ExpenseManager.UserExpenseData userData = entry.getValue();

                // Add balance for each user
                balances.add(new Balance(userId, userData.netBalance));

                // Add owe relationships for debts
                for (Map.Entry<String, Double> debt : userData.debts.entrySet()) {
                    oweRelationships.add(new OweRelationship(userId, debt.getKey(), debt.getValue()));
                }
            }

            // Update adapters with new data
            balanceAdapter.updateBalances(balances);
            oweAdapter.updateOweRelationships(oweRelationships);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (expenseManager != null) {
            expenseManager.removeListener();
        }
    }
}