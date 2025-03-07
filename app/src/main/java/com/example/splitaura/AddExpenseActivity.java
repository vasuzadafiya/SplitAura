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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.splitaura.adapters.CategoryAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddExpenseActivity extends BaseActivity {

    // Firebase
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();


    private ImageView backButton;
    private ImageView uploadIcon;
    private ImageView imagePreview;
    private TextView categoryButton;
    private TextInputEditText titleEditText;
    private TextInputEditText amountEditText;
    private TextInputEditText descriptionEditText;
    private TextInputLayout titleInputLayout;
    private TextInputLayout amountInputLayout;
    private Button skipButton;
    private Button nextButton;
    private ProgressBar progressBar;
    private Spinner spinnerPaidBy;
    private Spinner spinnerSplitBy;

    // Category selection
    private List<String> selectedCategories = new ArrayList<>();

    // Cloudinary
    private Uri imageUri;

    // Group ID passed via intent
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Retrieve the group ID from the intent once.
        groupId = getIntent().getStringExtra("groupId");


        // Initialize views and spinners
        initViews();
        initializeSpinners();

        // Setup listeners
        setupListeners();
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        imagePreview = findViewById(R.id.imagePreview);
        titleEditText = findViewById(R.id.title_edit_text);
        amountEditText = findViewById(R.id.amount_edit_text);
        descriptionEditText = findViewById(R.id.notes_edit_text);
        titleInputLayout = findViewById(R.id.title_input_layout);
        amountInputLayout = findViewById(R.id.amount_input_layout);
        findViewById(R.id.notes_input_layout);
        categoryButton = findViewById(R.id.category_spinner);
        uploadIcon = findViewById(R.id.uploadIcon);
        skipButton = findViewById(R.id.skip_btn);
        nextButton = findViewById(R.id.next_button);
        progressBar = findViewById(R.id.progressBar);

        // Cache spinners
        spinnerPaidBy = findViewById(R.id.spinner_paid_by);
        spinnerSplitBy = findViewById(R.id.spinner_split_by);
    }

    private void initializeSpinners() {
        List<String> paidByList = List.of("Me", "Friend 1", "Friend 2");
        ArrayAdapter<String> paidByAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paidByList);
        paidByAdapter.setDropDownViewResource(R.layout.spinner_dropdown_list);
        spinnerPaidBy.setAdapter(paidByAdapter);

        List<String> splitByList = List.of("Equally", "By Amount", "Custom");
        ArrayAdapter<String> splitByAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, splitByList);
        splitByAdapter.setDropDownViewResource(R.layout.spinner_dropdown_list);
        spinnerSplitBy.setAdapter(splitByAdapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        skipButton.setOnClickListener(v -> finish());

        nextButton.setOnClickListener(v -> {
            if (validateFields()) {
                progressBar.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.GONE);
                if (imageUri != null) {
                    uploadImageToCloudinary(imageUri);
                } else {
                    saveExpense(null); // Save expense without image
                }
            }
        });

        uploadIcon.setOnClickListener(v -> openImagePicker());
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

        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveExpense(String imageUrl) {
        String title = titleEditText.getText().toString().trim();
        String amount = amountEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        String paidBy = spinnerPaidBy.getSelectedItem().toString();
        String splitBy = spinnerSplitBy.getSelectedItem().toString();

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("title", title);
        expenseData.put("amount", amount);
        expenseData.put("notes", description);
        expenseData.put("categories", selectedCategories);
        expenseData.put("paidBy", paidBy);
        expenseData.put("splitBy", splitBy);
        expenseData.put("date", System.currentTimeMillis());

        if (imageUrl != null) {
            expenseData.put("imageUrl", imageUrl);
        }

        firestore.collection("groups")
                .document(groupId)
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
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            if (inputStream != null) {
                byte[] imageBytes = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        ? inputStream.readAllBytes()
                        : getBytesFromInputStream(inputStream);

                MediaManager.get().upload(imageBytes)
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {
                                // Upload started
                            }

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {
                                // Optionally update progress
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
        } catch (IOException e) {
            Toast.makeText(this, "File not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "File not found: " + e.getMessage());
        }
    }

    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
        builder.setView(dialogView);

        RecyclerView categoryRecyclerView = dialogView.findViewById(R.id.recyclerViewCategories);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, selectedCategories);
        categoryRecyclerView.setAdapter(categoryAdapter);

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
