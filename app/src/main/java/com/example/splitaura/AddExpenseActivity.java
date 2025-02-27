package com.example.splitaura;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.splitaura.adapters.CategoryAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddExpenseActivity extends AppCompatActivity {

    // Views
    private ScrollView scrollView;
    private ImageView backButton, uploadIcon, imagePreview;
    private TextView categoryButton;
    private TextInputEditText titleEditText, amountEditText, descriptionEditText;
    private TextInputLayout titleInputLayout, amountInputLayout, descriptionInputLayout;
    private Button skipButton, nextButton;
    private ProgressBar progressBar;
    private List<String> selectedCategories = new ArrayList<>();

    // Cloudinary Image URI
    private Uri imageUri;
    private String uploadedImageUrl;

    // Cloudinary configuration
    private Cloudinary cloudinary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Initialize Cloudinary
        cloudinary = MediaManager.get().getCloudinary();

        // Initialize Views
        scrollView = findViewById(R.id.scrollview);
        backButton = findViewById(R.id.back_button);
        imagePreview = findViewById(R.id.imagePreview);
        titleEditText = findViewById(R.id.title_edit_text);
        amountEditText = findViewById(R.id.amount_edit_text);
        descriptionEditText = findViewById(R.id.notes_edit_text);
        titleInputLayout = findViewById(R.id.title_input_layout);
        amountInputLayout = findViewById(R.id.amount_input_layout);
        descriptionInputLayout = findViewById(R.id.notes_input_layout);
        categoryButton = findViewById(R.id.category_spinner);
        uploadIcon = findViewById(R.id.uploadIcon);
        skipButton = findViewById(R.id.skip_btn);
        nextButton = findViewById(R.id.next_button);
        progressBar = findViewById(R.id.progressBar);

        setupListeners();
        initializeSpinners();
    }

    private void initializeSpinners() {
        // Paid By Spinner
        Spinner spinnerPaidBy = findViewById(R.id.spinner_paid_by);
        List<String> paidByList = List.of("Me", "Friend 1", "Friend 2");
        ArrayAdapter<String> paidByAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paidByList);
        paidByAdapter.setDropDownViewResource(R.layout.spinner_dropdown_list);
        spinnerPaidBy.setAdapter(paidByAdapter);

        // Split By Spinner
        Spinner spinnerSplitBy = findViewById(R.id.spinner_split_by);
        List<String> splitByList = List.of("Equally", "By Amount", "Custom");
        ArrayAdapter<String> splitByAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, splitByList);
        splitByAdapter.setDropDownViewResource(R.layout.spinner_dropdown_list);
        spinnerSplitBy.setAdapter(splitByAdapter);
    }

    private void setupListeners() {
        // Back button click
        backButton.setOnClickListener(v -> onBackPressed());

        // Skip button
        skipButton.setOnClickListener(v -> finish());

        // Next button to validate fields and save expense
        nextButton.setOnClickListener(v -> {
            if (validateFields()) {
                progressBar.setVisibility(View.VISIBLE);
                if (imageUri != null) {
                    uploadImageToCloudinary(imageUri);
                } else {
                    saveExpense(null); // No image, save directly
                }
            }
        });

        // Upload Icon Click
        uploadIcon.setOnClickListener(v -> openImagePicker());

        // Category button click to show custom AlertDialog
        categoryButton.setOnClickListener(v -> showCategoryDialog());
    }

    private boolean validateFields() {
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
            descriptionInputLayout.setError("Notes are required");
            return false;
        } else {
            descriptionInputLayout.setError(null);
        }

        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveExpense(String imageUrl) {
        // Fetch values
        String title = titleEditText.getText().toString().trim();
        String amount = amountEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        Spinner spinnerPaidBy = findViewById(R.id.spinner_paid_by);
        Spinner spinnerSplitBy = findViewById(R.id.spinner_split_by);

        String paidBy = spinnerPaidBy.getSelectedItem().toString();
        String splitBy = spinnerSplitBy.getSelectedItem().toString();

        // Create a new expense map
        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("title", title);
        expenseData.put("amount", amount);
        expenseData.put("notes", description);
        expenseData.put("categories", selectedCategories);
        expenseData.put("paidBy", paidBy);
        expenseData.put("splitBy", splitBy);
        if (imageUrl != null) {
            expenseData.put("imageUrl", imageUrl);
        }

        // Firestore instance
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Save expense data to Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();  // Get the UID of the current authenticated user
            firestore.collection("users")
                    .document(userId)  // Use the actual user’s UID here
                    .collection("groups")
                    .document("group_1")  // Replace with the actual group ID
                    .collection("expenses")
                    .add(expenseData).addOnSuccessListener(documentReference -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddExpenseActivity.this, "Expense saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddExpenseActivity.this, "Failed to save expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);  // Get InputStream from Uri
            if (inputStream != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    MediaManager.get().upload(inputStream.readAllBytes())
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) {
                                    // Upload started
                                }

                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) {
                                    // Handle progress
                                }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String imageUrl = (String) resultData.get("url");
                                    saveExpense(imageUrl);  // Proceed to save with image URL
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    Toast.makeText(AddExpenseActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) {
                                    // Retry upload
                                }
                            })
                            .dispatch();
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "File not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(String.valueOf(this),"File not found :" + e.getMessage());
        }
    }


    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imagePreview.setImageURI(imageUri);
            imagePreview.setVisibility(View.VISIBLE);// Show image preview
        }
    }

    private String getRealPathFromUri(Uri uri) {
        // Logic to get the real file path from URI (depends on how the image is chosen)
        return uri.getPath();  // Placeholder (implement actual logic)
    }

    private void showCategoryDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
        builder.setView(dialogView);

        // RecyclerView for categories
        RecyclerView categoryRecyclerView = dialogView.findViewById(R.id.recyclerViewCategories);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Data for categories
        String[] categories = {"General", "Games", "Sports", "Music", "Movies", "Food", "Liquor", "Grocery", "Shop"};
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, selectedCategories);

        // Set the adapter
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Handle cancel and ok button clicks
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button okButton = dialogView.findViewById(R.id.ok_button);

        // Create and show dialog
        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            if (!selectedCategories.isEmpty()) {
                String selectedCategoriesString = String.join(", ", selectedCategories); // Join categories
                Toast.makeText(this, "Selected categories: " + selectedCategoriesString, Toast.LENGTH_SHORT).show();
                categoryButton.setText(selectedCategoriesString); // Display selected categories on button
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
}
