package com.example.splitaura.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splitaura.utils.UserSessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> profilePicUrl = new MutableLiveData<>();
    // New LiveData for saving
    private final MutableLiveData<Double> saving = new MutableLiveData<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private final UserSessionManager sessionManager;

    public HomeViewModel(Application application) {
        super(application);
        sessionManager = UserSessionManager.getInstance(application);

        // Load cached data first
        String cachedUsername = sessionManager.getUsername();
        String cachedPicUrl = sessionManager.getProfilePicUrl();

        if (cachedUsername != null) {
            username.setValue(cachedUsername);
        } else {
            username.setValue("Hello User\nManage your finance easily");
        }

        if (cachedPicUrl != null) {
            profilePicUrl.setValue(cachedPicUrl);
        }

        // Fetch fresh data from Firestore
        fetchUserData();
    }

    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getProfilePicUrl() {
        return profilePicUrl;
    }

    // New getter for saving LiveData
    public LiveData<Double> getSaving() {
        return saving;
    }

    private void fetchUserData() {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String fname = documentSnapshot.getString("fname");
                String formattedName = fname != null
                        ? "Hello " + fname.toLowerCase() + "\nManage your finance easily"
                        : "Hello User\nManage your finance easily";
                username.setValue(formattedName);
                sessionManager.saveUsername(formattedName);

                String picUrl = documentSnapshot.getString("profile_picture");
                if (picUrl != null && !picUrl.isEmpty()) {
                    profilePicUrl.setValue(picUrl);
                    sessionManager.saveProfilePicUrl(picUrl);
                }

                // Fetch the saving field from Firestore
                Double savingValue = documentSnapshot.getDouble("saving");
                if (savingValue != null) {
                    saving.setValue(savingValue);
                } else {
                    saving.setValue(0.0); // Or any default value you'd like
                }
            }
        }).addOnFailureListener(e -> {
            // Handle error (optional: you could log or show a toast)
        });
    }
}
