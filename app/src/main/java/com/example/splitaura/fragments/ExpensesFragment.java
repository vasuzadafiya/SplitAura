package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.AddExpenseActivity;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.ExpenseAdapter;
import com.example.splitaura.dialogues.LoadingDialog;
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
    private final Map<String, String> userNamesCache = new HashMap<>();
    private String groupId;
    private FirebaseFirestore db;
    private LoadingDialog loadingDialog;
    private boolean userNamesFetched = false;

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
        loadingDialog = new LoadingDialog(getContext());

        recyclerView = view.findViewById(R.id.recycler_view_expenses);
        ExtendedFloatingActionButton fabAddExpense = view.findViewById(R.id.fab_add_expense);
        placeholder = view.findViewById(R.id.placeholder);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expenseAdapter = new ExpenseAdapter(getContext(), new ExpenseAdapter.OnExpenseActionListener() {
            @Override
            public void onEditExpense(ExpenseManager.Expense expense) {
                Intent intent = new Intent(getContext(), AddExpenseActivity.class);
                intent.putExtra(GroupExpensesActivity.GROUP_ID, groupId);
                intent.putExtra("EXPENSE_TITLE", expense.getTitle());
                intent.putExtra("EXPENSE_AMOUNT", expense.getAmount());
                intent.putExtra("EXPENSE_PAID_BY", expense.getPaidBy());
                intent.putExtra("EXPENSE_SPLIT_BY", expense.getSplitBy());
                intent.putExtra("EXPENSE_DATE", expense.getDate());
                intent.putExtra("EXPENSE_NOTES", expense.getNotes());
                intent.putExtra("EXPENSE_IMAGE_URL", expense.getImageUrl());
                intent.putExtra("EXPENSE_DESCRIPTION", expense.getDescription());
                intent.putExtra("EXPENSE_CATEGORIES", new ArrayList<>(expense.getCategories()));
                intent.putExtra("EXPENSE_PARTICIPANTS", new HashMap<>(expense.getParticipants()));
                intent.putExtra("EXPENSE_ID", expense.getExpenseId());
                startActivity(intent);
            }

            @Override
            public void onDeleteExpense(ExpenseManager.Expense expense) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Expense")
                        .setMessage("Are you sure you want to delete this expense?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            loadingDialog.show();
                            expenseManager.deleteExpense(expense.getExpenseId(),
                                    () -> {
                                        loadingDialog.hide();
                                        Toast.makeText(getContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                                    },
                                    () -> {
                                        loadingDialog.hide();
                                        Toast.makeText(getContext(), "Failed to delete expense", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        recyclerView.setAdapter(expenseAdapter);

        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddExpenseActivity.class);
            intent.putExtra(GroupExpensesActivity.GROUP_ID, groupId);
            startActivity(intent);
        });

        loadingDialog.show();
        fetchUserNames();

        return view;
    }

    private void fetchUserNames() {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    List<String> memberIds = (List<String>) doc.get("memberIds");
                    if (memberIds != null && !memberIds.isEmpty()) {
                        // Batch fetch user names to reduce Firestore calls
                        List<List<String>> batches = partitionList(memberIds, 10); // Firestore IN query limit is 10
                        fetchUserNamesInBatches(batches, 0);
                    } else {
                        Log.w(TAG, "No memberIds found for group: " + groupId);
                        userNamesFetched = true;
                        observeExpenses();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch group members", e);
                    userNamesFetched = true;
                    observeExpenses();
                });
    }

    private void fetchUserNamesInBatches(List<List<String>> batches, int currentBatchIndex) {
        if (currentBatchIndex >= batches.size()) {
            Log.d(TAG, "Fetched " + userNamesCache.size() + " user names");
            userNamesFetched = true;
            observeExpenses();
            return;
        }

        List<String> batch = batches.get(currentBatchIndex);
        db.collection("users").whereIn(FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot userDoc : query) {
                        String fullName = (userDoc.getString("fname") != null ? userDoc.getString("fname") : "") + " " +
                                (userDoc.getString("lname") != null ? userDoc.getString("lname") : "");
                        userNamesCache.put(userDoc.getId(), fullName.trim());
                    }
                    fetchUserNamesInBatches(batches, currentBatchIndex + 1);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user names for batch " + currentBatchIndex, e);
                    fetchUserNamesInBatches(batches, currentBatchIndex + 1);
                });
    }

    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private void observeExpenses() {
        if (!userNamesFetched) {
            Log.d(TAG, "Waiting for user names before observing expenses");
            return;
        }

        expenseManager.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            loadingDialog.hide();
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
                expense.setPaidBy(payerName);
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
        if (loadingDialog != null) {
            loadingDialog.hide();
            loadingDialog = null;
        }
        if (expenseManager != null) {
            expenseManager.removeListener();
            expenseManager = null;
        }
        recyclerView.setAdapter(null);
        expenseAdapter = null;
    }
}