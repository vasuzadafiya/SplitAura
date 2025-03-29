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
import android.widget.LinearLayout;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AddExpenseActivity extends BaseActivity {
    private static final String TAG = "AddExpenseActivity";
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Executor executor = Executors.newSingleThreadExecutor();
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
    private LinearLayout customSplitSettingsButton;
    private ProgressBar progressBar;
    private ContactsAdapter adapter;

    // Data
    private final List<Friend> selectedParticipants = new ArrayList<>();
    private final List<String> selectedCategories = new ArrayList<>();
    private Uri selectedImageUri;
    private String groupId;
    private Friend selectedPayer;
    private String expenseId;
    private String existingImageUrl;

    // Local cache
    private final Map<String, Friend> friendCache = new HashMap<>();
    private final List<Friend> groupMembers = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Loading Dialog
    private LoadingDialog loadingDialog;
    private boolean isLoadingGroupMembers = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        loadingDialog = new LoadingDialog(this);

        try {
            groupId = getIntent().getStringExtra(GroupExpensesActivity.GROUP_ID);
            if (groupId == null || groupId.isEmpty()) {
                throw new IllegalArgumentException("Invalid Group ID");
            }

            expenseManager = new ExpenseManager(groupId);
            initViews();
            setupListeners();
            setupImagePickerLauncher();

            expenseId = getIntent().getStringExtra("EXPENSE_ID");
            if (expenseId != null) {
                nextButton.setText("Update");
                showLoadingDialog();
                loadDataForEditMode();
            } else {
                preloadGroupMembers();
            }
        } catch (Exception e) {
            hideLoadingDialog();
            Log.e(TAG, "Initialization error", e);
            Toast.makeText(this, "Error initializing activity", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadDataForEditMode() {
        executor.execute(() -> {
            // Load group members
            firestore.collection("groups").document(groupId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> memberIds = (List<String>) documentSnapshot.get("memberIds");
                            if (memberIds != null && !memberIds.isEmpty()) {
                                fetchGroupMembersInBatches(memberIds, 0, () -> {
                                    Log.d(TAG, "Group members loaded for edit mode");
                                });
                            }
                        }
                    });

            // Load expense data
            firestore.collection("groups").document(groupId)
                    .collection("expenses").document(expenseId).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            runOnUiThread(() -> populateFieldsFromDocument(document));
                        }
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> {
                            hideLoadingDialog();
                            Toast.makeText(this, "Error loading expense data", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
        });
    }

    private void preloadGroupMembers() {
        if (friendCache.isEmpty() && !isLoadingGroupMembers) {
            isLoadingGroupMembers = true;
            executor.execute(() -> {
                firestore.collection("groups").document(groupId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                List<String> memberIds = (List<String>) documentSnapshot.get("memberIds");
                                if (memberIds != null && !memberIds.isEmpty()) {
                                    fetchGroupMembersInBatches(memberIds, 0, () -> {
                                        isLoadingGroupMembers = false;
                                        Log.d(TAG, "Group members preloaded successfully");
                                    });
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            isLoadingGroupMembers = false;
                            Log.e(TAG, "Failed to preload group members", e);
                        });
            });
        }
    }

    private void populateFieldsFromDocument(DocumentSnapshot document) {
        try {
            String title = document.getString("title");
            String amount = document.getString("amount");
            String paidBy = document.getString("paidBy");
            String splitBy = document.getString("splitBy");
            String notes = document.getString("notes");
            existingImageUrl = document.getString("imageUrl");
            List<String> categories = (List<String>) document.get("categories");
            Map<String, Double> participants = (Map<String, Double>) document.get("participants");

            titleEditText.setText(title);
            amountEditText.setText(amount);
            descriptionEditText.setText(notes != null ? notes : "");

            if (categories != null && !categories.isEmpty()) {
                selectedCategories.addAll(categories);
                categoryButton.setText(String.join(", ", selectedCategories));
            }

            if (splitBy != null) {
                String[] splitOptions = getResources().getStringArray(R.array.split_by_options);
                for (int i = 0; i < splitOptions.length; i++) {
                    if (splitOptions[i].equals(splitBy)) {
                        splitBySpinner.setSelection(i);
                        break;
                    }
                }
            }

            if (splitBy != null && splitBy.equals("Custom") && participants != null) {
                selectedParticipants.clear();
                for (String participantId : participants.keySet()) {
                    Friend participant = friendCache.get(participantId);
                    if (participant != null) {
                        selectedParticipants.add(participant);
                    }
                }
            }

            if (paidBy != null) {
                selectedPayer = friendCache.get(paidBy);
                if (selectedPayer != null) {
                    paidByButton.setText(selectedPayer.getFullName());
                }
            }

            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                Picasso.get().load(existingImageUrl).into(uploadIcon);
            }

            hideLoadingDialog();
        } catch (Exception e) {
            hideLoadingDialog();
            Log.e(TAG, "Error populating fields from document", e);
            Toast.makeText(this, "Error loading expense data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchGroupMembersInBatches(List<String> memberIds, int currentBatchIndex, Runnable onComplete) {
        if (memberIds == null || memberIds.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        List<List<String>> batches = partitionList(memberIds, 10);
        if (currentBatchIndex >= batches.size()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        List<String> batch = batches.get(currentBatchIndex);
        firestore.collection("users").whereIn(FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot userDoc : query) {
                        if (userDoc.exists()) {
                            Friend friend = new Friend(
                                    userDoc.getString("uid"),
                                    userDoc.getString("email"),
                                    userDoc.getString("fname"),
                                    userDoc.getString("lname"),
                                    userDoc.getString("profile_picture"),
                                    userDoc.getString("upi")
                            );
                            friendCache.put(userDoc.getId(), friend);
                            groupMembers.add(friend);
                        }
                    }
                    fetchGroupMembersInBatches(memberIds, currentBatchIndex + 1, onComplete);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user batch " + currentBatchIndex, e);
                    fetchGroupMembersInBatches(memberIds, currentBatchIndex + 1, onComplete);
                });
    }

    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
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
        customSplitSettingsButton = findViewById(R.id.custom_split_settings);
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
                loadingDialog.show();
                nextButton.setVisibility(View.GONE);
                if (selectedImageUri != null) {
                    uploadImageToCloudinary(selectedImageUri);
                } else {
                    saveExpense(existingImageUrl);
                }
            }
        });

        uploadIcon.setOnClickListener(v -> openImagePicker());
        categoryButton.setOnClickListener(v -> showCategoryDialog());
        paidByButton.setOnClickListener(v -> {
            if (groupMembers.isEmpty()) {
                showLoadingDialog();
                loadGroupMembers();
                Toast.makeText(this, "Loading group members, please wait...", Toast.LENGTH_SHORT).show();
            } else {
                showPayerSelectionDialog();
            }
        });

        splitBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                customSplitSettingsButton.setVisibility(selectedOption.equals("Custom") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                customSplitSettingsButton.setVisibility(View.GONE);
            }
        });

        customSplitSettingsButton.setOnClickListener(v -> {
            if (splitBySpinner.getSelectedItem().toString().equals("Custom")) {
                if (groupMembers.isEmpty()) {
                    showLoadingDialog();
                    loadGroupMembers();
                    Toast.makeText(this, "Loading group members, please wait...", Toast.LENGTH_SHORT).show();
                } else {
                    showParticipantSelectionDialog();
                }
            } else {
                Toast.makeText(this, "Please select 'Custom' split first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPayerSelectionDialog() {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error showing payer selection dialog", e);
            Toast.makeText(this, "Error showing payer selection", Toast.LENGTH_SHORT).show();
        }
    }

    private void showParticipantSelectionDialog() {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FullScreenDialog);
            builder.setView(dialogView);

            RecyclerView rvParticipants = dialogView.findViewById(R.id.recyclerViewCategories);
            TextView tvTitle = dialogView.findViewById(R.id.category_dialog_title);
            tvTitle.setText("Select Participants");

            rvParticipants.setLayoutManager(new LinearLayoutManager(this));
            List<Friend> tempSelectedParticipants = new ArrayList<>(selectedParticipants);

            adapter = new ContactsAdapter(groupMembers, tempSelectedParticipants, friend -> {
                if (tempSelectedParticipants.contains(friend)) {
                    tempSelectedParticipants.remove(friend);
                } else {
                    tempSelectedParticipants.add(friend);
                }
                adapter.notifyDataSetChanged();
            });
            rvParticipants.setAdapter(adapter);

            Button cancelButton = dialogView.findViewById(R.id.cancel_button);
            Button okButton = dialogView.findViewById(R.id.ok_button);
            AlertDialog dialog = builder.create();

            cancelButton.setOnClickListener(v -> dialog.dismiss());
            okButton.setOnClickListener(v -> {
                selectedParticipants.clear();
                selectedParticipants.addAll(tempSelectedParticipants);
                if (selectedParticipants.isEmpty()) {
                    Toast.makeText(this, "No participants selected. Switching to Equal split.", Toast.LENGTH_SHORT).show();
                    splitBySpinner.setSelection(0);
                }
                dialog.dismiss();
            });
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing participant selection dialog", e);
            Toast.makeText(this, "Error showing participant selection", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateFields() {
        try {
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
            if (Double.parseDouble(amountStr) > 10000.00) {
                amountInputLayout.setError("Amount cannot exceed 10000");
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

            String splitBy = splitBySpinner.getSelectedItem().toString();
            if (splitBy.equals("Custom") && selectedParticipants.isEmpty()) {
                Toast.makeText(this, "Please select participants for custom split", Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating fields", e);
            Toast.makeText(this, "Validation error", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void saveExpense(String imageUrl) {
        try {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String title = titleEditText.getText().toString().trim();
            String amount = amountEditText.getText().toString().trim();
            String notes = descriptionEditText.getText().toString().trim();
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            if (selectedPayer == null) {
                throw new IllegalStateException("Payer not selected");
            }

            executor.execute(() -> {
                Map<String, Object> expenseData = new HashMap<>();
                expenseData.put("title", title);
                expenseData.put("amount", amount);
                expenseData.put("notes", notes);
                expenseData.put("categories", selectedCategories);
                expenseData.put("paidBy", selectedPayer.getUid());
                expenseData.put("date", date);
                expenseData.put("timestamp", FieldValue.serverTimestamp());

                if (expenseId == null || !getIntent().hasExtra("EXPENSE_ADDED_BY")) {
                    expenseData.put("addedBy", currentUserId);
                }

                String splitBy = splitBySpinner.getSelectedItem().toString();
                expenseData.put("splitBy", splitBy);

                Map<String, Double> participants = calculateParticipantShares(amount, splitBy);
                if (participants == null) {
                    runOnUiThread(() -> {
                        loadingDialog.hide();
                        nextButton.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Error calculating participant shares", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                expenseData.put("participants", participants);

                if (imageUrl != null) {
                    expenseData.put("imageUrl", imageUrl);
                }

                runOnUiThread(() -> {
                    if (expenseId != null) {
                        updateExistingExpense(expenseData);
                    } else {
                        createNewExpense(expenseData);
                    }
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error saving expense", e);
            loadingDialog.hide();
            nextButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Error saving expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, Double> calculateParticipantShares(String amountStr, String splitBy) {
        try {
            double amountDouble = Double.parseDouble(amountStr);
            Map<String, Double> participants = new HashMap<>();

            if (splitBy.equals("Custom")) {
                if (selectedParticipants.isEmpty()) return null;
                double splitAmount = amountDouble / selectedParticipants.size();
                for (Friend participant : selectedParticipants) {
                    participants.put(participant.getUid(), splitAmount);
                }
            } else if (splitBy.equals("Equally")) {
                if (groupMembers.isEmpty()) return null;
                double splitAmount = amountDouble / groupMembers.size();
                for (Friend member : groupMembers) {
                    participants.put(member.getUid(), splitAmount);
                }
            }
            return participants;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid amount format", e);
            runOnUiThread(() -> amountInputLayout.setError("Invalid amount"));
            return null;
        }
    }

    private void updateExistingExpense(Map<String, Object> expenseData) {
        firestore.collection("groups").document(groupId).collection("expenses")
                .document(expenseId)
                .set(expenseData)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.hide();
                    Toast.makeText(this, "Expense updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.hide();
                    nextButton.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Failed to update expense", Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewExpense(Map<String, Object> expenseData) {
        expenseManager.addExpense(expenseData, () -> {
            loadingDialog.hide();
            Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show();
            finish();
        }, () -> {
            loadingDialog.hide();
            nextButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show();
        });
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
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker", e);
            Toast.makeText(this, "Error opening image picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    uploadIcon.setImageURI(selectedImageUri);
                }
            }
        });
    }

    private void showCategoryDialog() {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error showing category dialog", e);
            Toast.makeText(this, "Error showing categories", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadingDialog() {
        if (loadingDialog != null && !loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
    }

    private void loadGroupMembers() {
        if (isLoadingGroupMembers) return;
        isLoadingGroupMembers = true;
        firestore.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> memberIds = (List<String>) documentSnapshot.get("memberIds");
                        if (memberIds != null && !memberIds.isEmpty()) {
                            fetchGroupMembersInBatches(memberIds, 0, () -> {
                                isLoadingGroupMembers = false;
                                hideLoadingDialog();
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    isLoadingGroupMembers = false;
                    hideLoadingDialog();
                    Log.e(TAG, "Failed to load group members", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (expenseManager != null) {
                expenseManager.removeListener();
            }
            hideLoadingDialog();
            loadingDialog = null;
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
}