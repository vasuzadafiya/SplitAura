package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.AddExpenseActivity;
import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.ExpenseAdapter;
import com.example.splitaura.models.Expense;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExpenseAdapter expenseAdapter;
    private final List<Expense> expenseList = new ArrayList<>();
    private FirebaseFirestore db;
    private LinearLayout placeholder;
    private final Map<String, String> userNamesCache = new HashMap<>(); // Cache for user names

    // Field to store the group ID
    private String groupId;

    // Firestore listener for real-time updates
    private ListenerRegistration expensesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        // Retrieve groupId from the parent Activity's intent
        if (getActivity() != null && getActivity().getIntent() != null) {
            groupId = getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID);
        }

        recyclerView = view.findViewById(R.id.recycler_view_expenses);
        ExtendedFloatingActionButton fabAddExpense = view.findViewById(R.id.fab_add_expense);
        placeholder = view.findViewById(R.id.placeholder);

        // Initialize RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expenseAdapter = new ExpenseAdapter(expenseList);
        recyclerView.setAdapter(expenseAdapter);

        db = FirebaseFirestore.getInstance();

        // Set up FAB click listener
        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AddExpenseActivity.class);
            intent.putExtra(GroupExpensesActivity.GROUP_ID, groupId);
            v.getContext().startActivity(intent);
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchExpenses();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Remove Firestore listener to avoid memory leaks
        if (expensesListener != null) {
            expensesListener.remove();
            expensesListener = null;
        }
    }

    private void fetchExpenses() {
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid Group ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch group members and cache their names
        db.collection("groups")
                .document(groupId)
                .get()
                .addOnSuccessListener(groupDocument -> {
                    if (groupDocument.exists()) {
                        List<String> memberIds = (List<String>) groupDocument.get("memberIds");
                        if (memberIds != null && !memberIds.isEmpty()) {
                            fetchUserNames(memberIds, this::fetchExpensesWithUserNames);
                        } else {
                            Toast.makeText(getContext(), "No members found in the group", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Group not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load group details", Toast.LENGTH_SHORT).show();
                    Log.e("ExpensesFragment", "Error fetching group details", e);
                });
    }

    private void fetchUserNames(List<String> memberIds, Runnable onComplete) {
        db.collection("users")
                .whereIn(FieldPath.documentId(), memberIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userNamesCache.clear(); // Clear the cache before populating
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getId();
                            String fname = document.getString("fname");
                            String lname = document.getString("lname");

                            // Concatenate fname and lname to form the full name
                            String fullName = (fname != null ? fname : "") + " " + (lname != null ? lname : "");
                            userNamesCache.put(userId, fullName.trim()); // Trim to remove extra spaces
                        }
                        onComplete.run(); // Execute the callback
                    } else {
                        Toast.makeText(getContext(), "Failed to fetch user names", Toast.LENGTH_SHORT).show();
                        Log.e("ExpensesFragment", "Error fetching user names", task.getException());
                    }
                });
    }

    private void fetchExpensesWithUserNames() {
        expensesListener = db.collection("groups")
                .document(groupId)
                .collection("expenses")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show();
                        Log.e("ExpensesFragment", "Error fetching expenses", e);
                        return;
                    }

                    if (querySnapshot != null) {
                        expenseList.clear();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Expense expense = document.toObject(Expense.class);

                            // Replace the paidBy ID with the cached user name
                            String payerName = userNamesCache.get(expense.getPaidBy());
                            if (payerName != null) {
                                expense.setPaidBy(payerName); // Update the paidBy field with the user's name
                            }

                            expenseList.add(expense);
                        }

                        // Notify adapter of data changes
                        expenseAdapter.notifyDataSetChanged();

                        // Show/hide placeholder based on data availability
                        if (expenseList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            placeholder.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            placeholder.setVisibility(View.GONE);
                        }
                    }
                });
    }
}