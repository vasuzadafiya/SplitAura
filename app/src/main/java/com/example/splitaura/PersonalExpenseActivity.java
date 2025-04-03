package com.example.splitaura;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

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
import java.util.TreeMap;

public class PersonalExpenseActivity extends BaseActivity {
    // Constants
    private static final String EXPENSE = "expenses";
    private static final String USERS = "users";
    private static final String TIME_STAMP = "timestamp";
    private static final String TAG = "PersonalExpenseActivity";
    private static final String USER_ID = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    private static final String GENERAL = "General";
    // Used to store selections across dialogs
    private final List<String> selectedCategories = new ArrayList<>();
    private Spinner paymentMethod;
    private EditText searchBox;
    private TextView totalAmount;
    private PersonalExpenseAdapter adapter;
    // UI components
    private TextView categorySelector;
    private ListenerRegistration expenseListenerRegistration;
    // Data
    private List<PersonalExpense> allExpenses = new ArrayList<>();
    private String selectedMethod; // Payment method selection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.personal_expense);

        initLoadingDialog();
        setupRecyclerView();
        setupSearchBox();
        setupFilterSpinner();
        setupButtons();
    }

    //region UI Setup Methods
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        TextView chartButton = findViewById(R.id.chart_button);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PersonalExpenseAdapter(this, new ArrayList<>(), this::openUpdateExpenseDialog);
        recyclerView.setAdapter(adapter);
        chartButton.setOnClickListener(v -> showChart());
    }

    private void setupSearchBox() {
        searchBox = findViewById(R.id.search_box);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterExpenses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                //not used
            }
        });
    }

    private void setupFilterSpinner() {
        Spinner filterSpinner = findViewById(R.id.filter_spinner);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFilter = parent.getItemAtPosition(position).toString();
                applyDateFilter(selectedFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                applyDateFilter("All");
            }
        });
    }

    private void setupButtons() {
        Button addExpenseButton = findViewById(R.id.fab_add_expense);
        addExpenseButton.setOnClickListener(v -> addExpense());
        totalAmount = findViewById(R.id.totalTextView);
    }

    private DialogData createExpenseDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.add_expense_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
        return new DialogData(dialog, dialogView);
    }

    // Initializes common controls in the expense dialog
    private void initializeExpenseDialog(View dialogView) {
        categorySelector = dialogView.findViewById(R.id.category_selector);
        paymentMethod = dialogView.findViewById(R.id.spinner_currency);
        categorySelector.setOnClickListener(v -> showCategoryDialog());
        setupPaymentMethodSpinner();
    }

    //region Expense Dialogs
    private void openUpdateExpenseDialog(PersonalExpense expense) {
        DialogData dialogData = createExpenseDialog();
        View dialogView = dialogData.view;
        // Get references
        TextView heading = dialogView.findViewById(R.id.heading);
        EditText titleEditText = dialogView.findViewById(R.id.title_edit_text);
        EditText amountEditText = dialogView.findViewById(R.id.amount_edit_text);
        EditText notesEditText = dialogView.findViewById(R.id.notes_edit_text);
        Button nextButton = dialogView.findViewById(R.id.next_button);

        // Initialize common UI elements
        initializeExpenseDialog(dialogView);

        // Populate fields with existing expense data
        titleEditText.setText(expense.getTitle());
        amountEditText.setText(String.valueOf(expense.getAmount()));
        notesEditText.setText(expense.getNotes());
        heading.setText("Update Expense");
        nextButton.setText("Update");

        nextButton.setOnClickListener(v -> {
            String updatedTitle = titleEditText.getText().toString().trim();
            String updatedAmountText = amountEditText.getText().toString().trim();
            String updatedNotes = notesEditText.getText().toString().trim();

            if (updatedTitle.isEmpty() || updatedAmountText.isEmpty()) {
                showToast("Title and amount are required");
                return;
            }
            double updatedAmount;
            try {
                updatedAmount = Double.parseDouble(updatedAmountText);
            } catch (NumberFormatException e) {
                showToast("Please enter a valid amount");
                return;
            }
            // Prepare updated data
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("title", updatedTitle);
            updatedData.put("amount", updatedAmount);
            updatedData.put("notes", updatedNotes);
            updatedData.put(TIME_STAMP, Timestamp.now());
            updatedData.put("paymentMethod", selectedMethod);
            if (!selectedCategories.isEmpty()) {
                updatedData.put("categories", new ArrayList<>(selectedCategories));
            }
            // Update Firestore document
            FirebaseFirestore.getInstance()
                    .collection(USERS).document(USER_ID)
                    .collection(EXPENSE).document(expense.getDocumentId())
                    .update(updatedData)
                    .addOnSuccessListener(aVoid -> {
                        showToast("Expense updated successfully");
                        dialogData.dialog.dismiss();
                    })
                    .addOnFailureListener(e -> showToast("Update failed: " + e.getMessage()));
            selectedCategories.clear();
        });
    }

    private void addExpense() {
        DialogData dialogData = createExpenseDialog();
        View dialogView = dialogData.view;
        // Get references
        EditText titleEditText = dialogView.findViewById(R.id.title_edit_text);
        EditText amountEditText = dialogView.findViewById(R.id.amount_edit_text);
        EditText notesEditText = dialogView.findViewById(R.id.notes_edit_text);
        Button nextButton = dialogView.findViewById(R.id.next_button);

        // Initialize common UI elements
        initializeExpenseDialog(dialogView);

        nextButton.setOnClickListener(v -> {
            String titleText = titleEditText.getText().toString().trim();
            String amountText = amountEditText.getText().toString().trim();
            String notesText = notesEditText.getText().toString().trim();

            if (titleText.isEmpty() || amountText.isEmpty()) {
                showToast("Title and amount are required");
                return;
            }
            double amountValue;
            try {
                amountValue = Double.parseDouble(amountText);
            } catch (NumberFormatException e) {
                showToast("Please enter a valid amount");
                return;
            }
            if (selectedCategories.isEmpty()) {
                showToast("Please select at least one category");
                return;
            }
            // Prepare expense data
            Map<String, Object> expense = new HashMap<>();
            expense.put("title", titleText);
            expense.put("amount", amountValue);
            expense.put("notes", notesText.isEmpty() ? "-" : notesText);
            expense.put(TIME_STAMP, Timestamp.now());
            expense.put("paymentMethod", selectedMethod);
            expense.put("categories", new ArrayList<>(selectedCategories));

            // Add expense to Firestore
            FirebaseFirestore.getInstance()
                    .collection(USERS).document(USER_ID)
                    .collection(EXPENSE).add(expense)
                    .addOnSuccessListener(documentReference -> {
                        showToast("Expense added successfully");
                        dialogData.dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        showToast("Failed to add expense");
                        Log.e(TAG, "Error adding expense: " + e.getMessage());
                    });
            selectedCategories.clear();
        });
    }

    // Common method to process Firestore snapshots
    private void processExpenseSnapshot(QuerySnapshot snapshots) {
        List<PersonalExpense> expenses = new ArrayList<>();
        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            PersonalExpense expense = doc.toObject(PersonalExpense.class);
            if (expense != null) {
                expense.setDocumentId(doc.getId());
                expenses.add(expense);
            }
        }
        allExpenses = expenses;
        filterExpenses(searchBox.getText().toString());
    }

    // Fetch expenses with an optional cutoff date (for "All", "7 days", etc.)
    private void fetchFilteredExpenses(Date cutoffDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection(USERS).document(USER_ID).collection(EXPENSE);
        if (cutoffDate != null) {
            query = query.whereGreaterThanOrEqualTo(TIME_STAMP, new Timestamp(cutoffDate));
        }
        if (expenseListenerRegistration != null) {
            expenseListenerRegistration.remove();
        }
        expenseListenerRegistration = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                showToast("Error fetching expenses");
                Log.e(TAG, "Error fetching expenses: " + error.getMessage());
                return;
            }
            if (snapshots != null) {
                processExpenseSnapshot(snapshots);
            }
        });
    }

    // Overloaded method for fetching expenses within a custom date range
    private void fetchFilteredExpenses(Date startDate, Date endDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection(USERS).document(USER_ID).collection(EXPENSE)
                .whereGreaterThanOrEqualTo(TIME_STAMP, new Timestamp(startDate))
                .whereLessThanOrEqualTo(TIME_STAMP, new Timestamp(endDate));
        if (expenseListenerRegistration != null) {
            expenseListenerRegistration.remove();
        }
        expenseListenerRegistration = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                showToast("Error fetching expenses");
                Log.e(TAG, "Error fetching expenses: " + error.getMessage());
                return;
            }
            if (snapshots != null) {
                processExpenseSnapshot(snapshots);
            }
        });
    }

    // Filters expenses by matching title or notes with the query
    private void filterExpenses(String query) {
        List<PersonalExpense> filteredList;
        if (query == null || query.trim().isEmpty()) {
            filteredList = new ArrayList<>(allExpenses);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = new ArrayList<>();
            for (PersonalExpense expense : allExpenses) {
                if ((expense.getTitle() != null && expense.getTitle().toLowerCase().contains(lowerCaseQuery))
                        || (expense.getNotes() != null && expense.getNotes().toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(expense);
                }
            }
        }
        adapter.updateExpenses(filteredList);
        double total = 0.0;
        for (PersonalExpense expense : filteredList) {
            total += expense.getAmount();
        }
        totalAmount.setText("Total: " + String.format("%.2f", total));
    }

    // Applies date filter based on the selected option
    private void applyDateFilter(String filterOption) {
        long currentTime = System.currentTimeMillis();
        switch (filterOption) {
            case "7 days":
                fetchFilteredExpenses(new Date(currentTime - (7L * 24 * 60 * 60 * 1000)));
                break;
            case "15 days":
                fetchFilteredExpenses(new Date(currentTime - (15L * 24 * 60 * 60 * 1000)));
                break;
            case "30 days":
                fetchFilteredExpenses(new Date(currentTime - (30L * 24 * 60 * 60 * 1000)));
                break;
            case "Custom":
                showCustomDateRangePicker();
                break;
            case "All":
            default:
                fetchFilteredExpenses(null);
                break;
        }
    }

    //region Miscellaneous Helpers
    private void initLoadingDialog() {
        Dialog loadingDialog;
        loadingDialog = new Dialog(this, R.style.CustomLoadingDialog);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setCancelable(false);
        loadingDialog.setContentView(R.layout.loadingbar_layout);
        LottieAnimationView animationView = loadingDialog.findViewById(R.id.animation_view);
        animationView.playAnimation();
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setLayout(
                    MATCH_PARENT, MATCH_PARENT);
            loadingDialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            loadingDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
    //endregion

    @Override
    protected void onStop() {
        super.onStop();
        if (expenseListenerRegistration != null) {
            expenseListenerRegistration.remove();
            expenseListenerRegistration = null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    //region Dialog Creation Helpers
    // Helper container for dialog and its inflated view
    private static class DialogData {
        AlertDialog dialog;
        View view;

        DialogData(AlertDialog dialog, View view) {
            this.dialog = dialog;
            this.view = view;
        }
    }
}