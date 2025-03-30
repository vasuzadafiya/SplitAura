package com.example.splitaura.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.splitaura.R;
import com.example.splitaura.dialogues.LoadingDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditProfileDialogFragment extends DialogFragment {

    private static final String TAG = "EditProfileDialogFragment";
    private static final String LOADING = "Loading...";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView editProfileImage;
    private TextInputEditText editFirstNameEditText;
    private TextInputEditText editLastNameEditText;
    private TextInputEditText editUpiIdEditText; // New field for UPI ID
    private TextInputEditText editMobileEditText;
    private RadioGroup genderRadioGroup;
    private RadioButton radioMale;
    private RadioButton radioFemale;
    private RadioButton radioOther;
    private Button changeProfilePictureButton;
    private Button saveProfileButton;
    private Button cancelButton;
    private ProgressBar saveProgressBar;

    private String currentProfilePictureUrl;
    private Uri selectedImageUri;

    // Loading Dialog
    private LoadingDialog loadingDialog;

    // ActivityResultLauncher for image selection
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    selectedImageUri = result;
                    Glide.with(this)
                            .load(selectedImageUri)
                            .apply(new RequestOptions()
                                    .placeholder(R.drawable.obsmallmen2)
                                    .error(R.drawable.obsmallmen2)
                                    .circleCrop())
                            .into(editProfileImage);
                }
            }
    );

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_edit_profile, null);

        // Initialize views
        editProfileImage = view.findViewById(R.id.editProfileImage);
        editFirstNameEditText = view.findViewById(R.id.editFirstNameEditText);
        editLastNameEditText = view.findViewById(R.id.editLastNameEditText);
        editUpiIdEditText = view.findViewById(R.id.editEmailEditText); // Initialize UPI ID field
        editMobileEditText = view.findViewById(R.id.editMobileEditText);
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup);
        radioMale = view.findViewById(R.id.radioMale);
        radioFemale = view.findViewById(R.id.radioFemale);
        radioOther = view.findViewById(R.id.radioOther);
        changeProfilePictureButton = view.findViewById(R.id.changeProfilePictureButton);
        saveProfileButton = view.findViewById(R.id.saveProfileButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        saveProgressBar = view.findViewById(R.id.saveProgressBar);

        // Initialize Loading Dialog
        loadingDialog = new LoadingDialog(requireContext());

        // Set initial "Loading..." placeholder
        setLoadingPlaceholders();

        // Fetch and display current user data
        fetchUserData();

        // Set up button listeners
        setupButtons();

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return dialog;
    }

    private void setLoadingPlaceholders() {
        editFirstNameEditText.setText(LOADING);
        editLastNameEditText.setText(LOADING);
        editUpiIdEditText.setText(LOADING);
        editMobileEditText.setText(LOADING);
    }

    private void fetchUserData() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (uid == null) {
            showSafeToast("User not logged in");
            dismiss();
            return;
        }

        DocumentReference userRef = db.collection("users").document(uid);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String firstName = document.getString("fname");
                    String lastName = document.getString("lname");
                    String upiId = document.getString("upi");
                    String mobile = document.getString("mobile");
                    String gender = document.getString("gender");
                    String profilePicture = document.getString("profile_picture");

                    // Update views with actual data
                    editFirstNameEditText.setText(firstName != null ? firstName : "");
                    editLastNameEditText.setText(lastName != null ? lastName : "");
                    editUpiIdEditText.setText(upiId != null ? upiId : ""); // Set UPI ID
                    editMobileEditText.setText(mobile != null ? mobile : "");

                    // Set gender radio button
                    if (gender != null) {
                        if (gender.equalsIgnoreCase("Male")) {
                            radioMale.setChecked(true);
                        } else if (gender.equalsIgnoreCase("Female")) {
                            radioFemale.setChecked(true);
                        } else if (gender.equalsIgnoreCase("Other")) {
                            radioOther.setChecked(true);
                        }
                    }

                    // Load profile picture
                    currentProfilePictureUrl = profilePicture;
                    if (profilePicture != null && !profilePicture.isEmpty()) {
                        Glide.with(this)
                                .load(profilePicture)
                                .apply(new RequestOptions()
                                        .placeholder(R.drawable.obsmallmen2)
                                        .error(R.drawable.obsmallmen2)
                                        .circleCrop())
                                .into(editProfileImage);
                    } else {
                        editProfileImage.setImageResource(R.drawable.obsmallmen2);
                    }
                } else {
                    showSafeToast("User data not found");
                    dismiss();
                }
            } else {
                Exception e = task.getException();
                showSafeToast("Failed to load profile: " + (e != null ? e.getMessage() : "Unknown error"));
                dismiss();
            }
        });
    }

    private void setupButtons() {
//        launch image picker
        changeProfilePictureButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        saveProfileButton.setOnClickListener(v -> {
            saveProgressBar.setVisibility(View.VISIBLE); // Show ProgressBar
            saveProfileButton.setEnabled(false); // Disable save button during processing
            saveProfileChanges();
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void saveProfileChanges() {
        String firstName = editFirstNameEditText.getText().toString().trim();
        String lastName = editLastNameEditText.getText().toString().trim();
        String mobile = editMobileEditText.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(firstName)) {
            editFirstNameEditText.setError("First name is required");
            editFirstNameEditText.requestFocus();
            saveProgressBar.setVisibility(View.GONE); // Hide ProgressBar on error
            saveProfileButton.setEnabled(true); // Re-enable save button
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            editLastNameEditText.setError("Last name is required");
            editLastNameEditText.requestFocus();
            saveProgressBar.setVisibility(View.GONE); // Hide ProgressBar on error
            saveProfileButton.setEnabled(true); // Re-enable save button
            return;
        }

        if (!TextUtils.isEmpty(mobile) && !android.util.Patterns.PHONE.matcher(mobile).matches()) {
            editMobileEditText.setError("Enter a valid mobile number");
            editMobileEditText.requestFocus();
            saveProgressBar.setVisibility(View.GONE); // Hide ProgressBar on error
            saveProfileButton.setEnabled(true); // Re-enable save button
            return;
        }

        // Show Loading Dialog
        loadingDialog.show();

        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        // Handle profile picture upload if a new image is selected
        if (selectedImageUri != null) {
            uploadProfilePictureToCloudinary(uid, userRef);
        } else {
            updateUserData(userRef, currentProfilePictureUrl);
        }
    }

    private String getSelectedGender() {
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) return null; // No selection
        RadioButton selectedRadioButton = genderRadioGroup.findViewById(selectedId);
        return selectedRadioButton.getText().toString();
    }

    private void uploadProfilePictureToCloudinary(String uid, DocumentReference userRef) {
        String publicId = "profile_images/" + uid + "_" + UUID.randomUUID().toString();
        Map<String, String> options = new HashMap<>();
        options.put("public_id", publicId);
        options.put("overwrite", "true");
        options.put("resource_type", "image");

        MediaManager.get().upload(selectedImageUri)
                .option("public_id", publicId)
                .option("overwrite", true)
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Optional: Handle progress if needed
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newProfilePictureUrl = (String) resultData.get("secure_url");
                        currentProfilePictureUrl = newProfilePictureUrl;
                        updateUserData(userRef, newProfilePictureUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        loadingDialog.hide(); // Hide Loading Dialog on error
                        showSafeToast("Failed to upload image: " + error.getDescription());
                        saveProgressBar.setVisibility(View.GONE); // Hide ProgressBar on error
                        saveProfileButton.setEnabled(true); // Re-enable save button
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Handle reschedule if needed
                    }
                }).dispatch();
    }

    private void updateUserData(DocumentReference userRef, String profilePictureUrl) {
        String firstName = editFirstNameEditText.getText().toString().trim();
        String lastName = editLastNameEditText.getText().toString().trim();
        String upiId = editUpiIdEditText.getText().toString().trim(); // Get UPI ID
        String mobile = editMobileEditText.getText().toString().trim();
        String gender = getSelectedGender();

        // Prepare updated data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("fname", firstName);
        updatedData.put("lname", lastName);
        updatedData.put("upi", upiId); // Add UPI ID
        updatedData.put("mobile", TextUtils.isEmpty(mobile) ? null : mobile);
        updatedData.put("gender", gender);
        updatedData.put("profile_picture", profilePictureUrl);

        // Update Firestore
        userRef.update(updatedData).addOnCompleteListener(task -> {
            loadingDialog.hide(); // Hide Loading Dialog
            saveProgressBar.setVisibility(View.GONE); // Hide ProgressBar
            saveProfileButton.setEnabled(true); // Re-enable save button

            if (task.isSuccessful()) {
                showSafeToast("Profile updated successfully");

                // Update display name in Firebase Auth
                String fullName = firstName + " " + lastName;
                mAuth.getCurrentUser().updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build())
                        .addOnCompleteListener(displayNameTask -> {
                            if (displayNameTask.isSuccessful()) {
                                Log.d(TAG, "User display name updated in Firebase Auth");
                            } else {
                                Log.e(TAG, "Failed to update display name in Firebase Auth: " + displayNameTask.getException().getMessage());
                            }
                        });

                // Trigger refresh in ProfileFragment
                refreshParentFragment();

                // Ensure dialog closes after animation
                animateAndDismiss();
            } else {
                Exception e = task.getException();
                showSafeToast("Failed to update profile: " + (e != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    private void showSafeToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "Cannot show Toast: Fragment is not attached or context is null. Message: " + message);
        }
    }

    private void refreshParentFragment() {
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof ProfileFragment) {
            ((ProfileFragment) parentFragment).refreshData();
        }
    }

    private void animateAndDismiss() {
        if (getDialog() == null || getDialog().getWindow() == null) {
            dismiss(); // Fallback if view hierarchy is invalid
            return;
        }

        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500); // 500ms fade-out duration
        fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
                //not used
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                if (isAdded()) {
                    dismiss();
                }
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {
                //not used
            }
        });

        getDialog().getWindow().getDecorView().startAnimation(fadeOut);

        // Fallback: Dismiss after animation duration if animation listener fails
        new Handler().postDelayed(() -> {
            if (isAdded()) {
                dismiss();
            }
        }, 550); // Slightly longer than animation duration to ensure completion
    }
}