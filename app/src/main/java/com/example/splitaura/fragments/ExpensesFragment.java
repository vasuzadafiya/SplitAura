package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.splitaura.AddExpenseActivity;
import com.example.splitaura.GroupExpensesActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.ExpenseAdapter;
import com.example.splitaura.models.Expense;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList = new ArrayList<>();
    private FirebaseFirestore db;
    private LinearLayout placeholder;


    // Field to store the group ID
    private String groupId;

    // Firestore listener for real-time updates
    private ListenerRegistration expensesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        SwipeRefreshLayout swipeRefreshLayout;
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        // Retrieve groupId from the parent Activity's intent using the constant GROUP_ID.
        if (getActivity() != null && getActivity().getIntent() != null) {
            groupId = getActivity().getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID);
        }

        recyclerView = view.findViewById(R.id.recycler_view_expenses);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        ExtendedFloatingActionButton fabAddExpense = view.findViewById(R.id.fab_add_expense);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expenseAdapter = new ExpenseAdapter(expenseList);
        recyclerView.setAdapter(expenseAdapter);
        placeholder = view.findViewById(R.id.placeholder);

        db = FirebaseFirestore.getInstance();

        // Setup swipe-to-refresh; manually re-attach listener if needed
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (expensesListener != null) {
                expensesListener.remove();
            }
            fetchExpenses();
            swipeRefreshLayout.setRefreshing(false);
        });

        fabAddExpense.setOnClickListener(v -> {
            // Pass the same groupId to AddExpenseActivity using the constant key.
            Intent intent = new Intent(v.getContext(), AddExpenseActivity.class);
            intent.putExtra(GroupExpensesActivity.GROUP_ID, groupId);
            v.getContext().startActivity(intent);
        });

        return view;
    }

    // Start listening when the fragment becomes visible
    @Override
    public void onStart() {
        super.onStart();
        fetchExpenses();
    }

    // Stop listening to avoid memory leaks
    @Override
    public void onStop() {
        super.onStop();
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
        expensesListener = db.collection("groups")
                .document(groupId)
                .collection("expenses")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        expenseList.clear();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Expense expense = document.toObject(Expense.class);
                            expenseList.add(expense);
                        }
                        expenseAdapter.notifyDataSetChanged();

                        // Show placeholder if no expenses are found, hide it otherwise
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
