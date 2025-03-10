package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.splitaura.AddExpenseActivity;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.ExpenseAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpensesFragment extends Fragment {
    private static final String TAG = "ExpensesFragment";
    private RecyclerView recyclerView;
    private ExpenseAdapter expenseAdapter;
    private ExpenseManager expenseManager;
    private LinearLayout placeholder;
    private ProgressBar progressBar;
    private final Map<String, String> userNamesCache = new HashMap<>();
    private String groupId;
    private FirebaseFirestore db;
    private boolean userNamesFetched = false; // Track if names are loaded

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        groupId = getActivity() != null && getActivity().getIntent() != null
                ? getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID)
                : null;

        if (groupId == null || groupId.isEmpty()) {
            Log.e(TAG, "Group ID is null or empty");
            Toast.makeText(getContext(), "Invalid Group ID", Toast.LENGTH_SHORT).show();
            return view;
        }

        expenseManager = new ExpenseManager(groupId);
        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recycler_view_expenses);
        ExtendedFloatingActionButton fabAddExpense = view.findViewById(R.id.fab_add_expense);
        placeholder = view.findViewById(R.id.placeholder);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expenseAdapter = new ExpenseAdapter(new ArrayList<>());
        recyclerView.setAdapter(expenseAdapter);

        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddExpenseActivity.class);
            intent.putExtra(GroupExpensesActivity.GROUP_ID, groupId);
            startActivity(intent);
        });

        fetchUserNames(); // Fetch names first, then observe expenses

        return view;
    }

    private void fetchUserNames() {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    List<String> memberIds = (List<String>) doc.get("memberIds");
                    if (memberIds != null && !memberIds.isEmpty()) {
                        db.collection("users").whereIn(FieldPath.documentId(), memberIds)
                                .get()
                                .addOnSuccessListener(query -> {
                                    userNamesCache.clear();
                                    for (QueryDocumentSnapshot userDoc : query) {
                                        String fullName = (userDoc.getString("fname") != null ? userDoc.getString("fname") : "") + " " +
                                                (userDoc.getString("lname") != null ? userDoc.getString("lname") : "");
                                        userNamesCache.put(userDoc.getId(), fullName.trim());
                                    }
                                    Log.d(TAG, "Fetched " + userNamesCache.size() + " user names");
                                    userNamesFetched = true;
                                    observeExpenses(); // Start observing only after names are fetched
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to fetch user names", e);
                                    userNamesFetched = true; // Proceed even if failed
                                    observeExpenses(); // Fallback to UIDs if names fail
                                });
                    } else {
                        Log.w(TAG, "No memberIds found for group: " + groupId);
                        userNamesFetched = true;
                        observeExpenses();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch group members", e);
                    userNamesFetched = true; // Proceed with empty cache
                    observeExpenses();
                });
    }

    private void observeExpenses() {
        if (!userNamesFetched) {
            Log.d(TAG, "Waiting for user names before observing expenses");
            return; // Wait until names are fetched
        }

        expenseManager.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            Log.d(TAG, "Observed expense update with " + (expenses != null ? expenses.size() : 0) + " items");

            if (expenses == null || expenses.isEmpty()) {
                Log.w(TAG, "No expenses available for group: " + groupId);
                expenseAdapter.updateExpenses(new ArrayList<>());
                recyclerView.setVisibility(View.GONE);
                placeholder.setVisibility(View.VISIBLE);
                return;
            }

            List<ExpenseManager.Expense> updatedExpenses = new ArrayList<>();
            for (ExpenseManager.Expense expense : expenses) {
                String payerName = userNamesCache.getOrDefault(expense.getPaidBy(), expense.getPaidBy());
                expense.setPaidBy(payerName); // Set full name before passing to adapter
                updatedExpenses.add(expense);
            }

            expenseAdapter.updateExpenses(updatedExpenses);
            recyclerView.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
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