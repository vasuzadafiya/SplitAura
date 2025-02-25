package com.example.splitaura;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class AddExpenseActivity extends AppCompatActivity {

    // Views
    private ImageView backButton, uploadIcon;
    private TextView titleNewGroup, uploadText;
    private TextInputEditText titleEditText, amountEditText, descriptionEditText;
    private TextInputLayout titleInputLayout, amountInputLayout, descriptionInputLayout;
    private Spinner spinnerCategory, spinnerPaidBy, spinnerSplitBy;
    private Button skipButton, nextButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_expense);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        }); // Replace with your layout name

        // Initialize Views
        backButton = findViewById(R.id.back_button);
        titleNewGroup = findViewById(R.id.title_new_group);
        titleEditText = findViewById(R.id.title_edit_text);
        amountEditText = findViewById(R.id.amount_edit_text);
        descriptionEditText = findViewById(R.id.description_edit_text);
        titleInputLayout = findViewById(R.id.title_input_layout);
        amountInputLayout = findViewById(R.id.amount_input_layout);
        descriptionInputLayout = findViewById(R.id.description_input_layout);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerPaidBy = findViewById(R.id.spinner_paid_by);
        spinnerSplitBy = findViewById(R.id.spinner_split_by);
        uploadIcon = findViewById(R.id.uploadIcon);
        uploadText = findViewById(R.id.uploadText);
        skipButton = findViewById(R.id.skip_btn);
        nextButton = findViewById(R.id.next_button);
        progressBar = findViewById(R.id.progressBar);

        // Set up listeners
        setupListeners();
        intializeSpinners();
    }

    private void intializeSpinners() {
        // Static values for category spinner
        List<String> categoryList = new ArrayList<>();
        categoryList.add("Food");
        categoryList.add("Travel");
        categoryList.add("Accommodation");
        categoryList.add("Shopping");

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Static values for paid by spinner
        List<String> paidByList = new ArrayList<>();
        paidByList.add("Me");
        paidByList.add("Friend 1");
        paidByList.add("Friend 2");

        ArrayAdapter<String> paidByAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paidByList);
        paidByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaidBy.setAdapter(paidByAdapter);

        // Static values for split by spinner
        List<String> splitByList = new ArrayList<>();
        splitByList.add("Equally");
        splitByList.add("By Amount");
        splitByList.add("Custom");

        ArrayAdapter<String> splitByAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, splitByList);
        splitByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSplitBy.setAdapter(splitByAdapter);
    }

    private void setupListeners() {
        // Back button click
        backButton.setOnClickListener(v -> {
            // Handle back button press
            onBackPressed();
        });

        // Skip button
        skipButton.setOnClickListener(v -> {
            // Handle skip/cancel action
            finish(); // Or perform another action
        });

        // Next button
        nextButton.setOnClickListener(v -> {
            // Handle the next action, like saving data
            if (validateFields()) {
                progressBar.setVisibility(View.VISIBLE);
                saveExpense();
            }
        });

        // Upload Icon Click (if image upload is required)
        uploadIcon.setOnClickListener(v -> {
            // Open file picker or image picker
            openImagePicker();
        });
    }

    private boolean validateFields() {
        // Validate input fields
        if (titleEditText.getText().toString().trim().isEmpty()) {
            titleInputLayout.setError("Title is required");
            return false;
        } else {
            titleInputLayout.setError(null);
        }

        if (amountEditText.getText().toString().trim().isEmpty()) {
            amountInputLayout.setError("Amount is required");
            return false;
        } else {
            amountInputLayout.setError(null);
        }

        if (descriptionEditText.getText().toString().trim().isEmpty()) {
            descriptionInputLayout.setError("Description is required");
            return false;
        } else {
            descriptionInputLayout.setError(null);
        }

        // Additional validations for Spinners if needed
        return true;
    }

    private void saveExpense() {
        // Retrieve data from the input fields
        String title = titleEditText.getText().toString().trim();
        String amount = amountEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String paidBy = spinnerPaidBy.getSelectedItem().toString();
        String splitBy = spinnerSplitBy.getSelectedItem().toString();

        // Perform the save operation, like sending data to Firestore or a local database
        // This is just a placeholder for now
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(AddExpenseActivity.this, "Expense saved successfully!", Toast.LENGTH_SHORT).show();
            // Clear fields or navigate to another activity
        }, 2000); // Simulating a network/database delay
    }

    private void openImagePicker() {
        // Launch image picker or file chooser to upload image
        // Handle image upload logic (using Cloudinary, Imgur, or local storage)
    }
}
