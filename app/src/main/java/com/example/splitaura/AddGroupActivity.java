package com.example.splitaura;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.services.ImgurApiService;
import com.example.splitaura.services.ImgurResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddGroupActivity extends AppCompatActivity {

    private static final String TAG = "AddGroupActivity";
    private static final String IMGUR_CLIENT_ID = "Client-ID 974699ce54d1dfb";
    private static final int BUFFER_SIZE = 1024;

    // Reuse Retrofit instance
    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.imgur.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

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
    private Button selectedCategoryButton = null;
    private ProgressBar progressBar;
    private Button continueButton;

    private String selectedCurrency = "";
    private String selectedCategory = "";
    private Uri selectedImageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Loading Dialog
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_group);

        // Initialize LoadingDialog
        loadingDialog = new LoadingDialog(this);

        // Initialize views and set up UI
        initializeViews();
        setupListeners();
        setupCurrencySpinner();
        setupImagePickerLauncher();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        uploadImageLayout = findViewById(R.id.uploadImageLayout);
        uploadIcon = findViewById(R.id.uploadIcon);
        titleEditText = findViewById(R.id.title_edit_text);
        descriptionEditText = findViewById(R.id.description_edit_text);
        currencySpinner = findViewById(R.id.spinner_currency);
        categoryTripButton = findViewById(R.id.btn_category_trip);
        categoryFamilyButton = findViewById(R.id.btn_category_family);
        categoryProjectButton = findViewById(R.id.btn_category_project);
        categoryCoupleButton = findViewById(R.id.btn_category_couple);
        categoryEventButton = findViewById(R.id.btn_category_event);
        categoryOtherButton = findViewById(R.id.btn_category_other);
        cancelButton = findViewById(R.id.skip_btn);
        continueButton = findViewById(R.id.next_button);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        uploadImageLayout.setOnClickListener(v -> openImagePicker());

        categoryTripButton.setOnClickListener(v -> selectCategory(categoryTripButton, "Trip ✈️"));
        categoryFamilyButton.setOnClickListener(v -> selectCategory(categoryFamilyButton, getString(R.string.family)));
        categoryProjectButton.setOnClickListener(v -> selectCategory(categoryProjectButton, "Project 👜"));
        categoryCoupleButton.setOnClickListener(v -> selectCategory(categoryCoupleButton, "Couple 💑"));
        categoryEventButton.setOnClickListener(v -> selectCategory(categoryEventButton, "Event 🎉"));
        categoryOtherButton.setOnClickListener(v -> selectCategory(categoryOtherButton, "Other 🔀"));

        cancelButton.setOnClickListener(v -> finish());

        continueButton.setOnClickListener(v -> validateAndProceed());
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

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    selectedImageUri = data.getData();
                    uploadIcon.setImageURI(selectedImageUri);
                    Log.d(TAG, "Selected Image URI: " + selectedImageUri);
                }
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void selectCategory(Button button, String category) {
        if (selectedCategoryButton == button) {
            // Deselect if the same button is clicked again
            button.setSelected(false);
            button.setBackgroundResource(R.drawable.btn_bg_transparent);
            button.setTextColor(getColor(R.color.background));
            selectedCategory = "";
            selectedCategoryButton = null;
        } else {
            resetCategoryButtons();
            button.setSelected(true);
            button.setBackgroundResource(R.drawable.btn_bg);
            button.setTextColor(getColor(R.color.white));
            selectedCategory = category;
            selectedCategoryButton = button;
        }
        Log.d(TAG, "Selected Category: " + selectedCategory);
    }

    private void resetCategoryButtons() {
        Button[] buttons = {categoryTripButton, categoryFamilyButton, categoryProjectButton, categoryCoupleButton, categoryEventButton, categoryOtherButton};
        for (Button btn : buttons) {
            btn.setSelected(false);
            btn.setBackgroundResource(R.drawable.btn_bg_transparent);
            btn.setTextColor(getColor(R.color.background));
        }
    }

    private void validateAndProceed() {
        String title = Objects.requireNonNull(titleEditText.getText()).toString().trim();
        String description = Objects.requireNonNull(descriptionEditText.getText()).toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            titleEditText.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            descriptionEditText.requestFocus();
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

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog instead of progress bar
        loadingDialog.show();
        continueButton.setEnabled(false);

        File imageFile = getFileFromUri(selectedImageUri);
        if (imageFile != null) {
            uploadImageToImgur(imageFile, title, description, selectedCurrency, selectedCategory);
        } else {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            loadingDialog.hide();
            continueButton.setEnabled(true);
        }
    }

    private File getFileFromUri(Uri uri) {
        File file = new File(getCacheDir(), "temp_image.jpg");
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    private void uploadImageToImgur(File imageFile, String title, String description, String currency, String category) {
        ImgurApiService apiService = retrofit.create(ImgurApiService.class);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        Call<ImgurResponse> call = apiService.uploadImage(IMGUR_CLIENT_ID, body);
        call.enqueue(new Callback<ImgurResponse>() {
            @Override
            public void onResponse(Call<ImgurResponse> call, Response<ImgurResponse> response) {
                loadingDialog.hide();
                continueButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().data.link;
                    Log.d(TAG, "Uploaded image URL: " + imageUrl);
                    nextNavigation(title, description, currency, category, imageUrl);
                } else {
                    try {
                        String errorResponse = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Upload failed: " + errorResponse);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Error parsing error body");
                    }
                    if (imageFile != null) {
                        Log.d(TAG, "File path: " + imageFile.getAbsolutePath() + ", size: " + imageFile.length());
                    }
                    Toast.makeText(AddGroupActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ImgurResponse> call, Throwable t) {
                loadingDialog.hide();
                continueButton.setEnabled(true);
                Toast.makeText(AddGroupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Upload error: " + t.getMessage());
            }
        });
    }

    private void nextNavigation(String title, String description, String currency, String category, String imageUrl) {
        Intent intent = new Intent(this, SelectGroupMembers.class);
        intent.putExtra("title", title);
        intent.putExtra("description", description);
        intent.putExtra("currency", currency);
        intent.putExtra("category", category);
        intent.putExtra("imageUrl", imageUrl);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure loading dialog is dismissed to prevent memory leaks
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
    }
}