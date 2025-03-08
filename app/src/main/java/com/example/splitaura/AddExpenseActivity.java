package com.example.splitaura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.splitaura.adapters.CategoryAdapter;
import com.example.splitaura.adapters.ContactsAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.models.Friend;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddExpenseActivity extends BaseActivity {

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private ImageView backButton;
    private ImageView uploadIcon;
    private TextView categoryButton;
    private TextView paidByButton; // Replaced spinner with TextView/Button
    private View uploadImageLayout;
    private TextInputEditText titleEditText;
    private TextInputEditText amountEditText;
    private TextInputEditText descriptionEditText;
    private TextInputLayout titleInputLayout;
    private TextInputLayout amountInputLayout;
    private Button skipButton;
    private Button nextButton;
    private ProgressBar progressBar;
    private ContactsAdapter adapter;

    // Category selection
    private List<String> selectedCategories = new ArrayList<>();

    // Cloudinary
    private Uri selectedImageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher;


    // Group data
    private String groupId;
    private List<Friend> groupMembers = new ArrayList<>();
    private Friend selectedPayer; // Store the selected payer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        groupId = getIntent().getStringExtra("groupId");
        initViews();
        setupListeners();
        setupImagePickerLauncher();
        loadGroupMembers(); // Load group members on start
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        titleEditText = findViewById(R.id.title_edit_text);
        amountEditText = findViewById(R.id.amount_edit_text);
        descriptionEditText = findViewById(R.id.notes_edit_text);
        titleInputLayout = findViewById(R.id.title_input_layout);
        amountInputLayout = findViewById(R.id.amount_input_layout);
        categoryButton = findViewById(R.id.category_spinner);
        uploadIcon = findViewById(R.id.uploadIcon);
        skipButton = findViewById(R.id.skip_btn);
        uploadImageLayout = findViewById(R.id.uploadImageLayout);
        nextButton = findViewById(R.id.next_button);
        progressBar = findViewById(R.id.progressBar);
        paidByButton = findViewById(R.id.spinner_paid_by); // Replace spinner_paid_by with a TextView/Button in layout
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        skipButton.setOnClickListener(v -> finish());

        uploadImageLayout.setOnClickListener(v -> openImagePicker());

        nextButton.setOnClickListener(v -> {
            if (validateFields()) {
                progressBar.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.GONE);
                if (selectedImageUri != null) {
                    uploadImageToCloudinary(selectedImageUri);
                } else {
                    saveExpense(null);
                }
            }
        });

        uploadIcon.setOnClickListener(v -> openImagePicker());
        categoryButton.setOnClickListener(v -> showCategoryDialog());
        paidByButton.setOnClickListener(v -> showPayerSelectionDialog());
    }

    private void loadGroupMembers() {
        firestore.collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> memberIds = (List<String>) documentSnapshot.get("memberIds");
                        if (memberIds != null) {
                            groupMembers.clear();
                            for (String memberId : memberIds) {
                                // Fetch member details (optional, or use UID directly)
                                firestore.collection("users").document(memberId)
                                        .get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String uid = userDoc.getString("uid");
                                                String email = userDoc.getString("email");
                                                String fname = userDoc.getString("fname");
                                                String lname = userDoc.getString("lname");
                                                String profilePicture = userDoc.getString("profile_picture");
                                                Friend member = new Friend(uid, email, fname, lname, profilePicture);
                                                groupMembers.add(member);
                                                // Removed automatic selection of current user
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e("AddExpenseActivity", "Error fetching member: " + e.getMessage()));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load group members: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showPayerSelectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null); // Create this layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
        builder.setView(dialogView);

        RecyclerView rvParticipants = dialogView.findViewById(R.id.recyclerViewCategories);
        TextView tvTitle = dialogView.findViewById(R.id.category_dialog_title);
        tvTitle.setText("Payer");

        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        List<Friend> tempSelected = new ArrayList<>(); // Start with an empty list, no pre-selection

        // Initialize the adapter here
        adapter = new ContactsAdapter(groupMembers, tempSelected, friend -> {
            tempSelected.clear();
            tempSelected.add(friend);
            // After modifying the list, notify the adapter that the data has changed
            adapter.notifyDataSetChanged();
        });

        rvParticipants.setAdapter(adapter); // Set the adapter after it's initialized

        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            if (!tempSelected.isEmpty()) {
                selectPayer(tempSelected.get(0)); // Set the selected payer
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a payer", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void selectPayer(Friend payer) {
        selectedPayer = payer;
        paidByButton.setText(payer.getFullName()); // Display payer's name
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

        if (selectedPayer == null) {
            Toast.makeText(this, "Please select who paid", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveExpense(String imageUrl) {
        String title = titleEditText.getText().toString().trim();
        String amount = amountEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date());

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("title", title);
        expenseData.put("amount", amount);
        expenseData.put("notes", description);
        expenseData.put("categories", selectedCategories);
        expenseData.put("paidBy", selectedPayer.getUid()); // Store payer's UID
        expenseData.put("splitBy", "Equally"); // Hardcoded for now; expand later if needed
        expenseData.put("date", formattedDate); // Store the formatted date

        if (imageUrl != null) {
            expenseData.put("imageUrl", imageUrl);
        }

        firestore.collection("groups")
                .document(groupId)
                .collection("expenses")
                .add(expenseData)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Expense saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to save expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            public void onStart(String requestId) {}
                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {}
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
                            public void onReschedule(String requestId, ErrorInfo error) {}
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
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    selectedImageUri = data.getData();
                    uploadIcon.setImageURI(selectedImageUri);
                }
            }
        });
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
                categoryButton.setText(selectedCategoriesString);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
}