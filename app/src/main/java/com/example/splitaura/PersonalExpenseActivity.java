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
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.CategoryAdapter;
import com.example.splitaura.adapters.PersonalExpenseAdapter;
import com.example.splitaura.models.PersonalExpense;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PersonalExpenseActivity extends AppCompatActivity {

    private TextView categorySelector;
    private Spinner paymentMethod;
    private final List<String> selectedCategories = new ArrayList<>();
    private String selectedMethod; // Payment method selection
    private EditText searchBox;
    private TextView chartButton;

    //    You have to calculate and show total amount dynamically to the user
    private TextView totalAmount;

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
        chartButton = findViewById(R.id.chart_button);

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
        totalAmount = findViewById(R.id.totalTextView);

        chartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewCharts();
            }
        });
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
        expenseListenerRegistration = query.addSnapshotListener((QuerySnapshot queryDocumentSnapshots, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
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
                if ((expense.getTitle() != null && expense.getTitle().toLowerCase().contains(lowerCaseQuery)) || (expense.getNotes() != null && expense.getNotes().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(expense);
                }
            }
        }
        adapter.updateExpenses(filteredList);

        // Calculate the total amount from the filtered expenses
        double total = 0.0;
        for (PersonalExpense expense : filteredList) {
            total += expense.getAmount();
        }
        totalAmount.setText("Total: " + String.format("%.2f", total));
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

            if (selectedCategories.isEmpty()) {
                Toast.makeText(PersonalExpenseActivity.this, "Please select at least one category", Toast.LENGTH_SHORT).show();
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
            db.collection("users").document(userId).collection("expenses").add(expense).addOnSuccessListener(documentReference -> {
                Toast.makeText(PersonalExpenseActivity.this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(PersonalExpenseActivity.this, "Failed to add expense", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error adding expense: " + e.getMessage());
            });
        });
    }

    private void viewCharts() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.charts, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        // Get chart views from layout
        PieChart paymentMethodChart = dialogView.findViewById(R.id.pieChartPaymentMethod);
        PieChart categoryChart = dialogView.findViewById(R.id.pieChartCategory);
        BarChart datewiseChart = dialogView.findViewById(R.id.barChartDatewise);

        /*
         * 1. Payment Method Chart: Count occurrences for each method (Cash, Credit Card, Debit Card, UPI, Net Banking)
         */
        Map<String, Integer> paymentMethodCount = new HashMap<>();
        String[] paymentMethods = {"Cash", "Credit Card", "Debit Card", "UPI", "Net Banking"};
        // Initialize counts
        for (String method : paymentMethods) {
            paymentMethodCount.put(method, 0);
        }
        // Aggregate counts from all expenses
        for (PersonalExpense expense : allExpenses) {
            String method = expense.getPaymentMethod();
            if (method != null && paymentMethodCount.containsKey(method)) {
                paymentMethodCount.put(method, paymentMethodCount.get(method) + 1);
            }
        }
        // Create PieEntries for non-zero counts
        List<com.github.mikephil.charting.data.PieEntry> paymentEntries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : paymentMethodCount.entrySet()) {
            if (entry.getValue() > 0) {
                paymentEntries.add(new com.github.mikephil.charting.data.PieEntry(entry.getValue(), entry.getKey()));
            }
        }
        com.github.mikephil.charting.data.PieDataSet paymentDataSet = new com.github.mikephil.charting.data.PieDataSet(paymentEntries, "Payment Methods");
        paymentDataSet.setColors(com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS);
        com.github.mikephil.charting.data.PieData paymentData = new com.github.mikephil.charting.data.PieData(paymentDataSet);
        paymentMethodChart.setData(paymentData);

        // Formatting Payment Method Chart
        paymentMethodChart.getDescription().setEnabled(false);
        paymentMethodChart.setDrawEntryLabels(true);
        paymentMethodChart.setEntryLabelColor(Color.BLACK);
        paymentMethodChart.setEntryLabelTextSize(12f);
        paymentMethodChart.setCenterText("Methods");
        paymentMethodChart.setCenterTextSize(16f);
        paymentMethodChart.animateY(1000);
        paymentMethodChart.invalidate(); // refresh
        paymentMethodChart.getLegend().setEnabled(false);

        /*
         * 2. Category Spending Chart: Sum total amount spent per category.
         * Expected categories: "General", "Games", "Sports", "Music", "Movies", "Food", "Liquor", "Grocery", "Shop"
         * If an expense does not have any category, it will be added to "General".
         */
        Map<String, Double> categorySpending = new HashMap<>();
        String[] categories = {"General", "Games", "Sports", "Music", "Movies", "Food", "Liquor", "Grocery", "Shop"};
        // Initialize spending with 0 for each category
        for (String cat : categories) {
            categorySpending.put(cat, 0.0);
        }
        // Aggregate spending from all expenses
        for (PersonalExpense expense : allExpenses) {
            List<String> cats = expense.getCategories();
            if (cats != null && !cats.isEmpty()) {
                for (String cat : cats) {
                    if (categorySpending.containsKey(cat)) {
                        categorySpending.put(cat, categorySpending.get(cat) + expense.getAmount());
                    } else {
                        // If category doesn't match expected ones, add its amount to "General"
                        categorySpending.put("General", categorySpending.get("General") + expense.getAmount());
                    }
                }
            } else {
                // No category provided, add to "Other" (or General based on your needs)
                categorySpending.put("General", categorySpending.get("General") + expense.getAmount());
            }
        }
        // Create PieEntries for categories with spending > 0
        List<com.github.mikephil.charting.data.PieEntry> categoryEntries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categorySpending.entrySet()) {
            if (entry.getValue() > 0) {
                categoryEntries.add(new com.github.mikephil.charting.data.PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        }
        com.github.mikephil.charting.data.PieDataSet categoryDataSet = new com.github.mikephil.charting.data.PieDataSet(categoryEntries, "Category Spending");
        categoryDataSet.setColors(com.github.mikephil.charting.utils.ColorTemplate.COLORFUL_COLORS);
        com.github.mikephil.charting.data.PieData categoryData = new com.github.mikephil.charting.data.PieData(categoryDataSet);
        categoryChart.setData(categoryData);

        // Formatting Category Chart
        categoryChart.getDescription().setEnabled(false);
        categoryChart.setDrawEntryLabels(true);
        categoryChart.setEntryLabelColor(Color.BLACK);
        categoryChart.setEntryLabelTextSize(12f);
        categoryChart.setCenterText("Spending");
        categoryChart.setCenterTextSize(16f);
        categoryChart.animateY(1000);
        categoryChart.getLegend().setEnabled(false);
        categoryChart.invalidate();


        // Use a TreeMap with Date as key for proper chronological sorting.
// If you want to group by day only, consider truncating the time component.
        Map<Date, Double> dateSpending = new TreeMap<>();

// SimpleDateFormat for display purpose.
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

        for (PersonalExpense expense : allExpenses) {
            // Convert Firebase Timestamp to Date
            Date fullDate = expense.getTimestamp().toDate();

            // Optional: Truncate the time portion so that expenses on the same day are grouped together.
            // This can be done using Calendar:
            Calendar cal = Calendar.getInstance();
            cal.setTime(fullDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date dateOnly = cal.getTime();

            // Sum up spending for each day
            Double currentAmount = dateSpending.get(dateOnly);
            if (currentAmount == null) {
                dateSpending.put(dateOnly, expense.getAmount());
            } else {
                dateSpending.put(dateOnly, currentAmount + expense.getAmount());
            }
        }

// Prepare BarEntries and X-axis labels from the sorted TreeMap
        List<com.github.mikephil.charting.data.BarEntry> barEntries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Date, Double> entry : dateSpending.entrySet()) {
            barEntries.add(new com.github.mikephil.charting.data.BarEntry(index, entry.getValue().floatValue()));
            // Format the Date object into a label.
            dateLabels.add(sdf.format(entry.getKey()));
            index++;
        }

        com.github.mikephil.charting.data.BarDataSet barDataSet = new com.github.mikephil.charting.data.BarDataSet(barEntries, "Datewise Spending");
        int color = ContextCompat.getColor(this, R.color.f_pass_color);
        barDataSet.setColor(color);

        com.github.mikephil.charting.data.BarData barData = new com.github.mikephil.charting.data.BarData(barDataSet);
        datewiseChart.setData(barData);

// Formatting Datewise Chart
        datewiseChart.getDescription().setEnabled(false);
        datewiseChart.animateY(1000);

// Set up the X-axis formatter with the date labels
        com.github.mikephil.charting.components.XAxis xAxis = datewiseChart.getXAxis();
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dateLabels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);

// Format the left Y-axis and disable the right Y-axis
        com.github.mikephil.charting.components.YAxis leftAxis = datewiseChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f);
        com.github.mikephil.charting.components.YAxis rightAxis = datewiseChart.getAxisRight();
        rightAxis.setEnabled(false);

        datewiseChart.getLegend().setEnabled(false);

// Set visible range to 5 bars and enable dragging
        datewiseChart.setVisibleXRangeMaximum(5f);
        datewiseChart.setDragEnabled(true);

// Scroll the view to show the last 5 (most recent) data points
        if (barEntries.size() > 5) {
            datewiseChart.moveViewToX(barEntries.size() - 5);
        } else {
            datewiseChart.moveViewToX(0f);
        }

        datewiseChart.invalidate();

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
