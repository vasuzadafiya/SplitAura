package com.example.splitaura.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.splitaura.AddExpenseActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.ExpenseAdapter;
import com.example.splitaura.models.Expense;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList = new ArrayList<>();
    private FirebaseFirestore db;
    private String groupId;// You'll pass the groupId based on the selected group
    private ExtendedFloatingActionButton fabAddExpense;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_expenses);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        fabAddExpense = view.findViewById(R.id.fab_add_expense);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expenseAdapter = new ExpenseAdapter(expenseList);
        recyclerView.setAdapter(expenseAdapter);

        db = FirebaseFirestore.getInstance();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Perform your refresh operation, like fetching new data from Firestore
            refreshData(); // Your method to reload data

            // When refresh is complete, hide the loading spinner

        });
        fabAddExpense.setOnClickListener(v -> {
            v.getContext().startActivity(new Intent(v.getContext(), AddExpenseActivity.class));
        });
        // Fetch the group ID based on your logic, for now, hardcoded
        groupId = "group_1";

        fetchExpenses();

        return view;
    }

    private void refreshData() {
        fetchExpenses();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void fetchExpenses() {
        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("groups")
                .document(groupId)
                .collection("expenses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        expenseList.clear(); // Clear the list before adding new data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Expense expense = document.toObject(Expense.class);
                            expenseList.add(expense);
                        }
                        expenseAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
