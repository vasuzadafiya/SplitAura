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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.splitaura.adapters.CategoryAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddExpenseActivity extends BaseActivity {

    // Firebase
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = mAuth.getCurrentUser();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // Views
    private ScrollView scrollView;
    private ImageView backButton, uploadIcon, imagePreview;
    private TextView categoryButton;
    private TextInputEditText titleEditText, amountEditText, descriptionEditText;
    private TextInputLayout titleInputLayout, amountInputLayout, descriptionInputLayout;
    private Button skipButton, nextButton;
    private ProgressBar progressBar;

    // Category selection
    private List<String> selectedCategories = new ArrayList<>();

    // Cloudinary
    private Uri imageUri;
    private String uploadedImageUrl;
    private Cloudinary cloudinary;

    // Group ID passed via intent (used throughout the activity)
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Retrieve the group ID from the intent once.
        groupId = getIntent().getStringExtra("groupId");

        // Initialize Cloudinary
        cloudinary = MediaManager.get().getCloudinary();

        // Initialize views
        initViews();

        // Setup listeners and spinners
        setupListeners();
        initializeSpinners();
    }

    private void initViews() {
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
        // Back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Skip button finishes the activity
        skipButton.setOnClickListener(v -> finish());

        // Next button validates fields and saves expense
        nextButton.setOnClickListener(v -> {
            if (validateFields()) {
                progressBar.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.GONE);
                if (imageUri != null) {
                    uploadImageToCloudinary(imageUri);
                } else {
                    saveExpense(null); // Save directly if no image selected
                }
            }
        });

        // Upload icon opens image picker
        uploadIcon.setOnClickListener(v -> openImagePicker());

        // Category button shows a custom dialog for selecting categories
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
        // Get field values
        String title = titleEditText.getText().toString().trim();
        String amount = amountEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        Spinner spinnerPaidBy = findViewById(R.id.spinner_paid_by);
        Spinner spinnerSplitBy = findViewById(R.id.spinner_split_by);

        String paidBy = spinnerPaidBy.getSelectedItem().toString();
        String splitBy = spinnerSplitBy.getSelectedItem().toString();

        // Build expense data map
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

        // Save expense data to Firestore using the dynamic groupId from the intent
        firestore.collection("users")
                .document(user.getUid())
                .collection("groups")
                .document(groupId) // Use the groupId obtained from the intent
                .collection("expenses")
                .add(expenseData)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddExpenseActivity.this, "Expense saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddExpenseActivity.this, "Failed to save expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(getClass().getSimpleName(), "Failed to save expense: " + e.getMessage());
                });
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        try {
            InputStream inputStream;
            inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                // For devices running Android Tiramisu and above, use readAllBytes()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    MediaManager.get().upload(inputStream.readAllBytes())
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) {
                                    // Upload started
                                }

                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) {
                                    // Update progress if needed
                                }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String imageUrl = (String) resultData.get("url");
                                    saveExpense(imageUrl);
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    Toast.makeText(AddExpenseActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) {
                                    // Retry logic if needed
                                }
                            })
                            .dispatch();
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "File not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "File not found: " + e.getMessage());
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
            imagePreview.setVisibility(View.VISIBLE);
        }
    }

    private void showCategoryDialog() {
        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
        builder.setView(dialogView);

        // Setup RecyclerView for category selection
        RecyclerView categoryRecyclerView = dialogView.findViewById(R.id.recyclerViewCategories);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        String[] categories = {"General", "Games", "Sports", "Music", "Movies", "Food", "Liquor", "Grocery", "Shop"};
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, selectedCategories);
        categoryRecyclerView.setAdapter(categoryAdapter);

        // Setup dialog buttons
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            if (!selectedCategories.isEmpty()) {
                String selectedCategoriesString = String.join(", ", selectedCategories);
                Toast.makeText(this, "Selected categories: " + selectedCategoriesString, Toast.LENGTH_SHORT).show();
                categoryButton.setText(selectedCategoriesString);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
}
