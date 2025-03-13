package com.example.splitaura;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.CategoryAdapter;
import com.example.splitaura.adapters.PersonalExpenseAdapter;
import com.example.splitaura.models.PersonalExpense;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonalExpenseActivity extends AppCompatActivity {

    private TextView categorySelector;
    private Spinner paymentMethod;
    private final List<String> selectedCategories = new ArrayList<>();
    private String selectedMethod; // Payment method selection
    private EditText searchBox;

    private PersonalExpenseAdapter adapter;
    private List<PersonalExpense> allExpenses = new ArrayList<>();

    // Store the Firestore listener registration
    private ListenerRegistration expenseListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.personal_expense);

        // Setup RecyclerView and adapter
        RecyclerView recyclerView;
        recyclerView = findViewById(R.id.recycler_view);

        Spinner filterSpinner;
        filterSpinner = findViewById(R.id.filter_spinner);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PersonalExpenseAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Initialize and setup search box listener
        searchBox = findViewById(R.id.search_box);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterExpenses(searchBox.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });

        // Set up the filter spinner listener
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFilter = parent.getItemAtPosition(position).toString();
                applyDateFilter(selectedFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default to "All" if nothing is selected
                applyDateFilter("All");
            }
        });

        // Load expenses initially (the spinner listener will trigger data load)

        Button addExpenseButton = findViewById(R.id.fab_add_expense);
        addExpenseButton.setOnClickListener(view -> addExpense());
    }

    // Remove the listener when the activity stops (or you can call this when the user logs out)
    @Override
    protected void onStop() {
        super.onStop();
        if (expenseListenerRegistration != null) {
            expenseListenerRegistration.remove();
            expenseListenerRegistration = null;
        }
    }

    // Applies the date filter based on spinner selection
    private void applyDateFilter(String filterOption) {
        long currentTime = System.currentTimeMillis();
        Date cutoffDate;
        switch (filterOption) {
            case "7 days":
                cutoffDate = new Date(currentTime - (7L * 24 * 60 * 60 * 1000));
                break;
            case "15 days":
                cutoffDate = new Date(currentTime - (15L * 24 * 60 * 60 * 1000));
                break;
            case "30 days":
                cutoffDate = new Date(currentTime - (30L * 24 * 60 * 60 * 1000));
                break;
            case "All":
            default:
                cutoffDate = null; // No date restriction
                break;
        }
        fetchFilteredExpenses(cutoffDate);
    }

    // Fetches expenses from Firestore with an optional date cutoff
    private void fetchFilteredExpenses(Date cutoffDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Build Firestore query
        Query query = db.collection("users").document(userId).collection("expenses");
        if (cutoffDate != null) {
            Timestamp cutoffTimestamp = new Timestamp(cutoffDate);
            query = query.whereGreaterThanOrEqualTo("timestamp", cutoffTimestamp);
        }

        // Remove any existing listener
        if (expenseListenerRegistration != null) {
            expenseListenerRegistration.remove();
        }

        // Attach realtime listener
        expenseListenerRegistration = query.addSnapshotListener((QuerySnapshot queryDocumentSnapshots,
                                                                 com.google.firebase.firestore.FirebaseFirestoreException error) -> {
            if (error != null) {
                Toast.makeText(PersonalExpenseActivity.this, "Error fetching expenses", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching expenses: " + error.getMessage());
                return;
            }
            List<PersonalExpense> expenses = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                PersonalExpense expense = doc.toObject(PersonalExpense.class);
                if (expense != null) {
                    expense.setDocumentId(doc.getId());
                    expenses.add(expense);
                }
            }
            // Save full list for search filtering
            allExpenses = expenses;
            filterExpenses(searchBox.getText().toString());
        });
    }

    // Filters expenses by matching the query with title or notes
    private void filterExpenses(String query) {
        List<PersonalExpense> filteredList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(allExpenses);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (PersonalExpense expense : allExpenses) {
                if ((expense.getTitle() != null && expense.getTitle().toLowerCase().contains(lowerCaseQuery)) ||
                        (expense.getNotes() != null && expense.getNotes().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(expense);
                }
            }
        }
        adapter.updateExpenses(filteredList);
    }

    private void addExpense() {
        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.add_expense_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        // Get references to input fields
        EditText titleEditText = dialogView.findViewById(R.id.title_edit_text);
        EditText amountEditText = dialogView.findViewById(R.id.amount_edit_text);
        EditText notesEditText = dialogView.findViewById(R.id.notes_edit_text);
        Button nextButton = dialogView.findViewById(R.id.next_button);
        categorySelector = dialogView.findViewById(R.id.category_selector);
        paymentMethod = dialogView.findViewById(R.id.spinner_currency);

        // Set up category selector click listener to show category selection dialog
        categorySelector.setOnClickListener(v -> showCategoryDialog());
        // Set up payment method spinner
        setupPaymentMethodSpinner();

        nextButton.setOnClickListener(v -> {
            String titleText = titleEditText.getText().toString().trim();
            String amountText = amountEditText.getText().toString().trim();
            String notesText = notesEditText.getText().toString().trim(); // optional field

            // Validate required fields: title and amount
            if (titleText.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(PersonalExpenseActivity.this, "Title and amount are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse amount safely
            double amountValue;
            try {
                amountValue = Double.parseDouble(amountText);
            } catch (NumberFormatException e) {
                Toast.makeText(PersonalExpenseActivity.this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ensure user is authenticated
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(PersonalExpenseActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Create expense object with required fields and optional notes/categories
            Map<String, Object> expense = new HashMap<>();
            expense.put("title", titleText);
            expense.put("amount", amountValue);
            if (!notesText.isEmpty()) {
                expense.put("notes", notesText);
            }
            expense.put("timestamp", Timestamp.now());
            expense.put("paymentMethod", selectedMethod);
            if (!selectedCategories.isEmpty()) {
                expense.put("categories", new ArrayList<>(selectedCategories));
            }

            // Write expense to Firestore under user's expenses subcollection
            db.collection("users").document(userId).collection("expenses")
                    .add(expense)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(PersonalExpenseActivity.this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(PersonalExpenseActivity.this, "Failed to add expense", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error adding expense: " + e.getMessage());
                    });
        });
    }

    private void showCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        RecyclerView categoryRecyclerView = dialogView.findViewById(R.id.recyclerViewCategories);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, selectedCategories);
        categoryRecyclerView.setAdapter(categoryAdapter);

        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button okButton = dialogView.findViewById(R.id.ok_button);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            if (!selectedCategories.isEmpty()) {
                // Set the category selector text to a comma-separated list of categories
                categorySelector.setText(String.join(", ", selectedCategories));
                dialog.dismiss();
            } else {
                Toast.makeText(PersonalExpenseActivity.this, "Select at least one category", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void setupPaymentMethodSpinner() {
        String[] paymentMethods = {"Cash", "Credit Card", "Debit Card", "UPI", "Net Banking"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paymentMethods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentMethod.setAdapter(adapter);

        paymentMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMethod = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMethod = paymentMethods[0];
            }
        });
    }
}
