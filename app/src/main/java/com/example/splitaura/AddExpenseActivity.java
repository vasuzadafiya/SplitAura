package com.example.splitaura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.splitaura.adapters.CategoryAdapter;
import com.example.splitaura.adapters.ContactsAdapter;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.Friend;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddExpenseActivity extends BaseActivity {
    private static final String TAG = "AddExpenseActivity";
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ExpenseManager expenseManager;

    // UI Elements
    private ImageView backButton;
    private ImageView uploadIcon;
    private Spinner splitBySpinner;
    private TextView categoryButton;
    private TextView paidByButton;
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

    // Data
    private final List<Friend> selectedParticipants = new ArrayList<>();
    private final List<String> selectedCategories = new ArrayList<>();
    private Uri selectedImageUri;
    private String groupId;
    private Friend selectedPayer;

    // Local cache (simplified, replace with Room in production)
    private final Map<String, Friend> friendCache = new HashMap<>();
    private final List<Friend> groupMembers = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Loading Dialog
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null || groupId.isEmpty()) {
            Log.e(TAG, "Group ID is null or empty");
            Toast.makeText(this, "Invalid Group ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize LoadingDialog
        loadingDialog = new LoadingDialog(this);

        expenseManager = new ExpenseManager(groupId);
        initViews();
        setupListeners();
        setupImagePickerLauncher();
        loadGroupMembers();
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
        splitBySpinner = findViewById(R.id.spinner_split_by);
        paidByButton = findViewById(R.id.spinner_paid_by);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
        skipButton.setOnClickListener(v -> finish());
        uploadImageLayout.setOnClickListener(v -> openImagePicker());
        nextButton.setOnClickListener(v -> {
            if (validateFields()) {
                // Show loading dialog instead of progress bar
                loadingDialog.show();
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

        splitBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                if (selectedOption.equals("Custom")) {
                    showParticipantSelectionDialog();
                } else {
                    selectedParticipants.clear();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* No action needed */ }
        });
    }

    private void loadGroupMembers() {
        firestore.collection("groups").document(groupId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> memberIds = (List<String>) documentSnapshot.get("memberIds");
                if (memberIds != null) {
                    groupMembers.clear();
                    for (String memberId : memberIds) {
                        Friend cachedFriend = friendCache.get(memberId);
                        if (cachedFriend != null) {
                            groupMembers.add(cachedFriend);
                        } else {
                            firestore.collection("users").document(memberId).get().addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    Friend friend = new Friend(userDoc.getString("uid"), userDoc.getString("email"), userDoc.getString("fname"), userDoc.getString("lname"), userDoc.getString("profile_picture"));
                                    friendCache.put(memberId, friend);
                                    groupMembers.add(friend);
                                    Log.d(TAG, "Loaded group member: " + friend.getFullName());
                                }
                            }).addOnFailureListener(e -> Log.e(TAG, "Failed to load user " + memberId, e));
                        }
                    }
                    Log.d(TAG, "Loaded " + groupMembers.size() + " group members");
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load group members", e);
            Toast.makeText(this, "Failed to load group members", Toast.LENGTH_SHORT).show();
        });
    }

    private void showPayerSelectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
        builder.setView(dialogView);

        RecyclerView rvParticipants = dialogView.findViewById(R.id.recyclerViewCategories);
        TextView tvTitle = dialogView.findViewById(R.id.category_dialog_title);
        tvTitle.setText("Payer");

        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        List<Friend> tempSelected = new ArrayList<>();

        adapter = new ContactsAdapter(groupMembers, tempSelected, friend -> {
            tempSelected.clear();
            tempSelected.add(friend);
            adapter.notifyDataSetChanged();
        });
        rvParticipants.setAdapter(adapter);

        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            if (!tempSelected.isEmpty()) {
                selectedPayer = tempSelected.get(0);
                paidByButton.setText(selectedPayer.getFullName());
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a payer", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void showParticipantSelectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
        builder.setView(dialogView);

        RecyclerView rvParticipants = dialogView.findViewById(R.id.recyclerViewCategories);
        TextView tvTitle = dialogView.findViewById(R.id.category_dialog_title);
        tvTitle.setText("Select Participants");

        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactsAdapter(groupMembers, selectedParticipants, friend -> {
            if (selectedParticipants.contains(friend)) {
                selectedParticipants.remove(friend);
            } else {
                selectedParticipants.add(friend);
            }
            adapter.notifyDataSetChanged();
        });
        rvParticipants.setAdapter(adapter);

        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            if (selectedParticipants.isEmpty()) {
                Toast.makeText(this, "Please select at least one participant", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private boolean validateFields() {
        if (titleEditText.getText().toString().trim().isEmpty()) {
            titleInputLayout.setError("Title is required");
            return false;
        }
        titleInputLayout.setError(null);

        String amountStr = amountEditText.getText().toString().trim();
        if (amountStr.isEmpty()) {
            amountInputLayout.setError("Amount is required");
            return false;
        }
        try {
            Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            amountInputLayout.setError("Invalid amount");
            return false;
        }
        amountInputLayout.setError(null);

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
        String notes = descriptionEditText.getText().toString().trim();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("title", title);
        expenseData.put("amount", amount);
        expenseData.put("notes", notes);
        expenseData.put("categories", selectedCategories);
        expenseData.put("paidBy", selectedPayer.getUid());
        expenseData.put("date", date);
        expenseData.put("timestamp", FieldValue.serverTimestamp());

        String splitBy = splitBySpinner.getSelectedItem().toString();
        expenseData.put("splitBy", splitBy);

        Map<String, Double> participants = new HashMap<>();
        double amountDouble = Double.parseDouble(amount);
        if (splitBy.equals("Custom")) {
            if (selectedParticipants.isEmpty()) {
                Toast.makeText(this, "No participants selected for custom split", Toast.LENGTH_SHORT).show();
                loadingDialog.hide();
                nextButton.setVisibility(View.VISIBLE);
                return;
            }
            double splitAmount = amountDouble / selectedParticipants.size();
            for (Friend participant : selectedParticipants) {
                participants.put(participant.getUid(), splitAmount);
            }
        } else if (splitBy.equals("Equally")) {
            if (groupMembers.isEmpty()) {
                Toast.makeText(this, "No group members available for equal split", Toast.LENGTH_SHORT).show();
                loadingDialog.hide();
                nextButton.setVisibility(View.VISIBLE);
                return;
            }
            double splitAmount = amountDouble / groupMembers.size();
            for (Friend member : groupMembers) {
                participants.put(member.getUid(), splitAmount);
            }
        }
        expenseData.put("participants", participants);

        if (imageUrl != null) {
            expenseData.put("imageUrl", imageUrl);
        }

        Log.d(TAG, "Saving expense: " + expenseData);
        expenseManager.addExpense(expenseData, () -> {
            Log.d(TAG, "Expense saved successfully");
            loadingDialog.hide();
            Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show();
            finish();
        }, () -> {
            Log.e(TAG, "Failed to save expense");
            loadingDialog.hide();
            nextButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expenseManager != null) {
            expenseManager.removeListener();
        }
        // Ensure loading dialog is dismissed to prevent memory leaks
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            if (inputStream != null) {
                byte[] imageBytes = getBytesFromInputStream(inputStream);
                MediaManager.get().upload(imageBytes).callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Image upload started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Not used
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        Log.d(TAG, "Image uploaded successfully: " + resultData.get("url"));
                        saveExpense((String) resultData.get("url"));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Image upload failed: " + error.getDescription());
                        Toast.makeText(AddExpenseActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        loadingDialog.hide();
                        nextButton.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Not used
                    }
                }).dispatch();
            }
        } catch (IOException e) {
            Log.e(TAG, "File error during image upload", e);
            Toast.makeText(this, "File error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadingDialog.hide();
            nextButton.setVisibility(View.VISIBLE);
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
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                uploadIcon.setImageURI(selectedImageUri);
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
                categoryButton.setText(String.join(", ", selectedCategories));
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Select at least one category", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
}