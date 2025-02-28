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

import com.example.splitaura.services.ImgurApiService;
import com.example.splitaura.services.ImgurResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_group);

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Set up currency spinner
        setupCurrencySpinner();

        // Set up image picker launcher
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

        categoryTripButton.setOnClickListener(v -> selectCategory(categoryTripButton, "Trip"));
        categoryFamilyButton.setOnClickListener(v -> selectCategory(categoryFamilyButton, "Family"));
        categoryProjectButton.setOnClickListener(v -> selectCategory(categoryProjectButton, "Project"));
        categoryCoupleButton.setOnClickListener(v -> selectCategory(categoryCoupleButton, "Couple"));
        categoryEventButton.setOnClickListener(v -> selectCategory(categoryEventButton, "Event"));
        categoryOtherButton.setOnClickListener(v -> selectCategory(categoryOtherButton, "Other"));

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
        categoryTripButton.setSelected(false);
        categoryFamilyButton.setSelected(false);
        categoryProjectButton.setSelected(false);
        categoryCoupleButton.setSelected(false);
        categoryEventButton.setSelected(false);
        categoryOtherButton.setSelected(false);

        categoryTripButton.setBackgroundResource(R.drawable.btn_bg_transparent);
        categoryFamilyButton.setBackgroundResource(R.drawable.btn_bg_transparent);
        categoryProjectButton.setBackgroundResource(R.drawable.btn_bg_transparent);
        categoryCoupleButton.setBackgroundResource(R.drawable.btn_bg_transparent);
        categoryEventButton.setBackgroundResource(R.drawable.btn_bg_transparent);
        categoryOtherButton.setBackgroundResource(R.drawable.btn_bg_transparent);

        categoryTripButton.setTextColor(getColor(R.color.background));
        categoryFamilyButton.setTextColor(getColor(R.color.background));
        categoryProjectButton.setTextColor(getColor(R.color.background));
        categoryCoupleButton.setTextColor(getColor(R.color.background));
        categoryEventButton.setTextColor(getColor(R.color.background));
        categoryOtherButton.setTextColor(getColor(R.color.background));
    }

    private void validateAndProceed() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

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

        // Show progress and disable button during upload
        progressBar.setVisibility(View.VISIBLE);
        continueButton.setEnabled(false);

        // Convert URI to File for upload
        File imageFile = getFileFromUri(selectedImageUri);
        if (imageFile != null) {
            uploadImageToImgur(imageFile, title, description, selectedCurrency, selectedCategory);
        } else {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            continueButton.setEnabled(true);
        }
    }

    // Utility method to convert a content URI to a File
    private File getFileFromUri(Uri uri) {
        File file = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            file = new File(getCacheDir(), "temp_image.jpg");
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    // Method to upload image to Imgur using Retrofit
    private void uploadImageToImgur(File imageFile, String title, String description, String currency, String category) {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.imgur.com/").addConverterFactory(GsonConverterFactory.create()).build();

        ImgurApiService apiService = retrofit.create(ImgurApiService.class);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        // Replace YOUR_IMGUR_CLIENT_ID with your actual client ID from Imgur
        Call<ImgurResponse> call = apiService.uploadImage("Client-ID 974699ce54d1dfb", body);
        call.enqueue(new Callback<ImgurResponse>() {
            @Override
            public void onResponse(Call<ImgurResponse> call, Response<ImgurResponse> response) {
                progressBar.setVisibility(View.GONE);
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
                progressBar.setVisibility(View.GONE);
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
    }

}
