package com.example.splitaura;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class AddGroupActivity extends AppCompatActivity {

    private static final String TAG = "AddGroupActivity";

    private ImageView backButton;
    private View uploadImageLayout;
    private ImageView uploadIcon;
    private TextInputEditText titleEditText;
    private TextInputEditText descriptionEditText;
    private Spinner currencySpinner;
    private Button categoryTripButton;
    private Button categoryFamilyButton;
    private Button categoryProjectButton;
    private Button categoryCoupleButton;
    private Button categoryEventButton;
    private Button categoryOtherButton;
    private Button cancelButton;
    private Button continueButton;

    private String selectedCurrency = "";
    private String selectedCategory = "";
    private Uri selectedImageUri;
    private String uploadedImageUrl; // Store the URL of the uploaded image

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private StorageReference storageRef;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_group);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_add_group), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference().child("images"); // Create an "images" folder in Firebase Storage
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Set up currency spinner
        setupCurrencySpinner();

        // Set up image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedImageUri = data.getData();
                            uploadIcon.setImageURI(selectedImageUri);
                            Log.d(TAG, "Selected Image URI: " + selectedImageUri);
                        }
                    }
                }
        );
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        uploadImageLayout = findViewById(R.id.uploadImageLayout);
        uploadIcon = findViewById(R.id.uploadIcon);
        titleEditText = findViewById(R.id.et_title);
        descriptionEditText = findViewById(R.id.et_description);
        currencySpinner = findViewById(R.id.spinner_currency);
        categoryTripButton = findViewById(R.id.btn_category_trip);
        categoryFamilyButton = findViewById(R.id.btn_category_family);
        categoryProjectButton = findViewById(R.id.btn_category_project);
        categoryCoupleButton = findViewById(R.id.btn_category_couple);
        categoryEventButton = findViewById(R.id.btn_category_event);
        categoryOtherButton = findViewById(R.id.btn_category_other);
        cancelButton = findViewById(R.id.skip_btn);
        continueButton = findViewById(R.id.next_button);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        uploadImageLayout.setOnClickListener(v -> openImagePicker());

        categoryTripButton.setOnClickListener(v -> selectCategory(categoryTripButton, "Trip"));
        categoryFamilyButton.setOnClickListener(v -> selectCategory(categoryFamilyButton, "Family"));
        categoryProjectButton.setOnClickListener(v -> selectCategory(categoryProjectButton, "Project"));
        categoryCoupleButton.setOnClickListener(v -> selectCategory(categoryCoupleButton, "Couple"));
        categoryEventButton.setOnClickListener(v -> selectCategory(categoryEventButton, "Event"));
        categoryOtherButton.setOnClickListener(v -> selectCategory(categoryOtherButton, "Other"));

        cancelButton.setOnClickListener(v -> finish());

        continueButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageToFirebase(selectedImageUri);
            } else {
                validateAndProceed();
            }
        });
    }

    private void setupCurrencySpinner() {
        String[] currencyOptions = {"INR", "USD", "EUR", "JPY"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCurrency = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Selected Currency: " + selectedCurrency);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void selectCategory(Button button, String category) {
        // Reset all buttons to default state
        resetCategoryButtons();

        // Set the selected button to the pressed state
        button.setSelected(true);
        button.setBackgroundResource(R.drawable.btn_white_bg);
        button.setTextColor(getColor(R.color.white));

        selectedCategory = category;
        Log.d(TAG, "Selected Category: " + selectedCategory);
    }

    private void resetCategoryButtons() {
        categoryTripButton.setSelected(false);
        categoryFamilyButton.setSelected(false);
        categoryProjectButton.setSelected(false);
        categoryCoupleButton.setSelected(false);
        categoryEventButton.setSelected(false);
        categoryOtherButton.setSelected(false);

        categoryTripButton.setBackgroundResource(R.drawable.btn_white_bg);
        categoryFamilyButton.setBackgroundResource(R.drawable.btn_white_bg);
        categoryProjectButton.setBackgroundResource(R.drawable.btn_white_bg);
        categoryCoupleButton.setBackgroundResource(R.drawable.btn_white_bg);
        categoryEventButton.setBackgroundResource(R.drawable.btn_white_bg);
        categoryOtherButton.setBackgroundResource(R.drawable.btn_white_bg);

        categoryTripButton.setTextColor(getColor(R.color.background));
        categoryFamilyButton.setTextColor(getColor(R.color.background));
        categoryProjectButton.setTextColor(getColor(R.color.background));
        categoryCoupleButton.setTextColor(getColor(R.color.background));
        categoryEventButton.setTextColor(getColor(R.color.background));
        categoryOtherButton.setTextColor(getColor(R.color.background));
    }

    private void uploadImageToFirebase(Uri fileUri) {
        if (fileUri != null) {
            // Create a reference to the file in Firebase Storage with a unique name using timestamp
            StorageReference fileRef = storageRef.child(System.currentTimeMillis() + "." + getFileExtension(fileUri));

            // Create metadata for the file (optional)
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/" + getFileExtension(fileUri))  // Specify the file type
                    .build();

            // Upload the file along with the metadata
            fileRef.putFile(fileUri, metadata)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the URL of the uploaded file
                        Task<Uri> downloadUrlTask = fileRef.getDownloadUrl();
                        downloadUrlTask.addOnSuccessListener(uri -> {
                            uploadedImageUrl = uri.toString();  // Save the download URL
                            Log.d(TAG, "Image uploaded successfully: " + uploadedImageUrl);

                            // Proceed with the next step (e.g., saving data to Firestore)
                            validateAndProceed();
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to retrieve download URL", e);
                            Toast.makeText(AddGroupActivity.this, "Failed to get the image URL", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image", e);
                        Toast.makeText(AddGroupActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Helper method to get the file extension from the Uri
    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void validateAndProceed() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }

        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            return;
        }

        if (selectedCurrency.isEmpty()) {
            Toast.makeText(this, "Please select a currency", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uploadedImageUrl == null) {
            Toast.makeText(this, "Please upload an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed with saving the data to Firestore
        saveGroupDataToFirestore(title, description, selectedCurrency, selectedCategory, uploadedImageUrl);
    }

    private void saveGroupDataToFirestore(String title, String description, String currency, String category, String imageUrl) {
        // Create a map to store the group data
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("title", title);
        groupData.put("description", description);
        groupData.put("currency", currency);
        groupData.put("category", category);
        groupData.put("imageUrl", imageUrl);

        // Save the data to Firestore
        db.collection("groups")
                .add(groupData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Group data saved with ID: " + documentReference.getId());
                    Toast.makeText(AddGroupActivity.this, "Group added successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back after successful submission
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving group data", e);
                    Toast.makeText(AddGroupActivity.this, "Failed to add group", Toast.LENGTH_SHORT).show();
                });
    }
}
