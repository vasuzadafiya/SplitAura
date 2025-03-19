package com.example.splitaura;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.splitaura.adapters.CategoryAdapter;
import com.example.splitaura.adapters.PersonalExpenseAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.models.PersonalExpense;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.datepicker.MaterialDatePicker;
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
import java.util.Objects;
import java.util.TreeMap;

public class PersonalExpenseActivity extends BaseActivity {
    private static final String EXPENSE = "expenses";
    private static final String USERS = "users";
    private static final String TIME_STAMP = "timestamp";
    private static final String GENERAL = "General";
    private static final String USER_ID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

    private TextView categorySelector;
    private Spinner paymentMethod;
    private final List<String> selectedCategories = new ArrayList<>();
    private String selectedMethod; // Payment method selection
    private EditText searchBox;

    //    You have to calculate and show total amount dynamically to the user
    private TextView totalAmount;

    private PersonalExpenseAdapter adapter;
    private List<PersonalExpense> allExpenses = new ArrayList<>();

    // Store the Firestore listener registration
    private ListenerRegistration expenseListenerRegistration;

    // Loading Dialog
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.personal_expense);

        // Initialize Loading Dialog
        initLoadingDialog();

        // Setup RecyclerView and adapter
        RecyclerView recyclerView;
        TextView chartButton;
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

        chartButton.setOnClickListener(v -> showChart());
    }

    private void initLoadingDialog() {
        loadingDialog = new Dialog(this, R.style.CustomLoadingDialog);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setCancelable(false);
        loadingDialog.setContentView(R.layout.loadingbar_layout);

        LottieAnimationView animationView = loadingDialog.findViewById(R.id.animation_view);
        animationView.playAnimation();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setLayout(-1, -1);
            loadingDialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            );
            loadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    public void showLoadingDialog() {
        if (loadingDialog != null && !loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    public void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
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
// 1. Update applyDateFilter() to handle "Custom" selection
    private void applyDateFilter(String filterOption) {
        long currentTime = System.currentTimeMillis();
        Date cutoffDate;
        switch (filterOption) {
            case "7 days":
                cutoffDate = new Date(currentTime - (7L * 24 * 60 * 60 * 1000));
                fetchFilteredExpenses(cutoffDate);
                break;
            case "15 days":
                cutoffDate = new Date(currentTime - (15L * 24 * 60 * 60 * 1000));
                fetchFilteredExpenses(cutoffDate);
                break;
            case "30 days":
                cutoffDate = new Date(currentTime - (30L * 24 * 60 * 60 * 1000));
                fetchFilteredExpenses(cutoffDate);
                break;
            case "Custom":
                // Show date range selector dialog.
                showCustomDateRangePicker();
                break;
            case "All":
            default:
                fetchFilteredExpenses(null);
                break;
        }
    }

    // 2. Add a method to show custom date range picker dialogs
    private void showCustomDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select Date Range");
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = builder.build();

        dateRangePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            // selection.first and selection.second are the start and end in milliseconds.
            Date startDate = new Date(selection.first);
            Date endDate = new Date(selection.second);
            fetchFilteredExpenses(startDate, endDate);
        });
    }

    // 3. Overload fetchFilteredExpenses to handle a custom date range.
    private void fetchFilteredExpenses(Date startDate, Date endDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Build Firestore query with both start and end date restrictions.
        Query query = db.collection(USERS).document(userId).collection(EXPENSE)
                .whereGreaterThanOrEqualTo(TIME_STAMP, new Timestamp(startDate))
                .whereLessThanOrEqualTo(TIME_STAMP, new Timestamp(endDate));

        // Remove any existing listener
        if (expenseListenerRegistration != null) {
            expenseListenerRegistration.remove();
        }

        // Attach realtime listener for the custom range
        expenseListenerRegistration = query.addSnapshotListener((QuerySnapshot queryDocumentSnapshots,
                                                                 com.google.firebase.firestore.FirebaseFirestoreException error) -> {
            if (error != null) {
                Toast.makeText(PersonalExpenseActivity.this, "Error fetching expenses", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching expenses: " + error.getMessage());
                return;
            }
            List<PersonalExpense> expenses = new ArrayList<>();
            assert queryDocumentSnapshots != null;
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


    // Fetches expenses from Firestore with an optional date cutoff
    private void fetchFilteredExpenses(Date cutoffDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Build Firestore query
        Query query = db.collection(USERS).document(userId).collection(EXPENSE);
        if (cutoffDate != null) {
            Timestamp cutoffTimestamp = new Timestamp(cutoffDate);
            query = query.whereGreaterThanOrEqualTo(TIME_STAMP, cutoffTimestamp);
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
            assert queryDocumentSnapshots != null;
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
        String totals = "Total: ".concat(String.format("%.2f", total));
        totalAmount.setText(totals);
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

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Create expense object with required fields and optional notes/categories
            Map<String, Object> expense = new HashMap<>();
            expense.put("title", titleText);
            expense.put("amount", amountValue);
            if (!notesText.isEmpty()) {
                expense.put("notes", notesText);
            }
            expense.put(TIME_STAMP, Timestamp.now());
            expense.put("paymentMethod", selectedMethod);
            if (!selectedCategories.isEmpty()) {
                expense.put("categories", new ArrayList<>(selectedCategories));
            }

            // Write expense to Firestore under user's expenses subcollection
            db.collection(USERS).document(USER_ID).collection(EXPENSE).add(expense).addOnSuccessListener(documentReference -> {
                Toast.makeText(PersonalExpenseActivity.this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(PersonalExpenseActivity.this, "Failed to add expense", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error adding expense: " + e.getMessage());
            });
        });
    }

    private void showChart() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.charts, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        categoryChart(dialogView);
        paymentMethodChart(dialogView);
        datewiseChart(dialogView);

    }

    private void datewiseChart(View dialogView) {
        BarChart datewiseChart = dialogView.findViewById(R.id.barChartDatewise);
        Map<Date, Double> dateSpending = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

        // Aggregate spending by day (truncating the time component)
        for (PersonalExpense expense : allExpenses) {
            Date dateOnly = truncateDate(expense.getTimestamp().toDate());
            dateSpending.merge(dateOnly, expense.getAmount(), Double::sum);
        }

        // Prepare BarEntries and X-axis labels from the sorted map
        List<com.github.mikephil.charting.data.BarEntry> barEntries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Date, Double> entry : dateSpending.entrySet()) {
            barEntries.add(new com.github.mikephil.charting.data.BarEntry(index, entry.getValue().floatValue()));
            dateLabels.add(sdf.format(entry.getKey()));
            index++;
        }

        com.github.mikephil.charting.data.BarDataSet barDataSet =
                new com.github.mikephil.charting.data.BarDataSet(barEntries, "Datewise Spending");
        int color = ContextCompat.getColor(this, R.color.f_pass_color);
        barDataSet.setColor(color);

        datewiseChart.setData(new com.github.mikephil.charting.data.BarData(barDataSet));
        datewiseChart.getDescription().setEnabled(false);
        datewiseChart.animateY(1000);

        // Configure X-axis with date labels
        com.github.mikephil.charting.components.XAxis xAxis = datewiseChart.getXAxis();
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dateLabels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);

        // Configure left Y-axis and disable right Y-axis
        com.github.mikephil.charting.components.YAxis leftAxis = datewiseChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f);
        datewiseChart.getAxisRight().setEnabled(false);

        datewiseChart.getLegend().setEnabled(false);
        datewiseChart.setVisibleXRangeMaximum(5f);
        datewiseChart.setDragEnabled(true);
        datewiseChart.moveViewToX(barEntries.size() > 5 ? barEntries.size() - 5f : 0f);
        datewiseChart.invalidate();
    }

    private void paymentMethodChart(View dialogView) {
        PieChart paymentMethodChart = dialogView.findViewById(R.id.pieChartPaymentMethod);
        Map<String, Integer> paymentMethodCount = new HashMap<>();
        String[] methods = {"Cash", "Credit Card", "Debit Card", "UPI", "Net Banking"};

        // Initialize counts
        for (String method : methods) {
            paymentMethodCount.put(method, 0);
        }

        // Aggregate counts from all expenses using merge
        for (PersonalExpense expense : allExpenses) {
            String method = expense.getPaymentMethod();
            if (method != null && paymentMethodCount.containsKey(method)) {
                paymentMethodCount.merge(method, 1, Integer::sum);
            }
        }

        // Create PieEntries for non-zero counts
        List<com.github.mikephil.charting.data.PieEntry> paymentEntries = new ArrayList<>();
        paymentMethodCount.forEach((key, value) -> {
            if (value > 0) {
                paymentEntries.add(new com.github.mikephil.charting.data.PieEntry(value, key));
            }
        });

        com.github.mikephil.charting.data.PieDataSet paymentDataSet =
                new com.github.mikephil.charting.data.PieDataSet(paymentEntries, "Payment Methods");
        paymentDataSet.setColors(com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS);

        paymentMethodChart.setData(new com.github.mikephil.charting.data.PieData(paymentDataSet));
        paymentMethodChart.getDescription().setEnabled(false);
        paymentMethodChart.setDrawEntryLabels(true);
        paymentMethodChart.setEntryLabelColor(Color.BLACK);
        paymentMethodChart.setEntryLabelTextSize(12f);
        paymentMethodChart.setCenterText("Methods");
        paymentMethodChart.setCenterTextSize(16f);
        paymentMethodChart.animateY(1000);
        paymentMethodChart.getLegend().setEnabled(false);
        paymentMethodChart.invalidate();
    }

    private void categoryChart(View dialogView) {
        PieChart categoryChart = dialogView.findViewById(R.id.pieChartCategory);
        Map<String, Double> categorySpending = new HashMap<>();
        String[] categories = {GENERAL, "Games", "Sports", "Music", "Movies", "Food", "Liquor", "Grocery", "Shop"};

        // Prepopulate each expected category with 0 spending
        for (String cat : categories) {
            categorySpending.put(cat, 0.0);
        }

        // Aggregate spending; if an expense's category doesn't match, add its amount to GENERAL.
        for (PersonalExpense expense : allExpenses) {
            List<String> expCats = expense.getCategories();
            if (expCats != null && !expCats.isEmpty()) {
                for (String cat : expCats) {
                    if (categorySpending.containsKey(cat)) {
                        categorySpending.merge(cat, expense.getAmount(), Double::sum);
                    } else {
                        categorySpending.merge(GENERAL, expense.getAmount(), Double::sum);
                    }
                }
            } else {
                categorySpending.merge(GENERAL, expense.getAmount(), Double::sum);
            }
        }

        // Create PieEntries for categories with spending > 0
        List<com.github.mikephil.charting.data.PieEntry> categoryEntries = new ArrayList<>();
        categorySpending.forEach((key, value) -> {
            if (value > 0) {
                categoryEntries.add(new com.github.mikephil.charting.data.PieEntry(value.floatValue(), key));
            }
        });

        com.github.mikephil.charting.data.PieDataSet categoryDataSet =
                new com.github.mikephil.charting.data.PieDataSet(categoryEntries, "Category Spending");
        categoryDataSet.setColors(com.github.mikephil.charting.utils.ColorTemplate.COLORFUL_COLORS);

        categoryChart.setData(new com.github.mikephil.charting.data.PieData(categoryDataSet));
        categoryChart.getDescription().setEnabled(false);
        categoryChart.setDrawEntryLabels(true);
        categoryChart.setEntryLabelColor(Color.BLACK);
        categoryChart.setEntryLabelTextSize(12f);
        categoryChart.setCenterText("Spending");
        categoryChart.setCenterTextSize(16f);
        categoryChart.animateY(1000);
        categoryChart.getLegend().setEnabled(false);
        categoryChart.invalidate();
    }


    // Helper method to truncate the time portion from a Date.
    private Date truncateDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
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