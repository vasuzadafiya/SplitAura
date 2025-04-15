package com.example.splitaura.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splitaura.utils.UserSessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> profilePicUrl = new MutableLiveData<>();
    private final MutableLiveData<Double> saving = new MutableLiveData<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private final UserSessionManager sessionManager;

    public HomeViewModel(Application application) {
        super(application);
        sessionManager = UserSessionManager.getInstance(application);

        // Load cached user data first
        String cachedUsername = sessionManager.getUsername();
        String cachedPicUrl = sessionManager.getProfilePicUrl();

        username.setValue((cachedUsername != null) ? cachedUsername : "Hello User\nManage your finance easily");
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

    public LiveData<Double> getSaving() {
        return saving;
    }

    private void fetchUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    // Retrieve and format user's name
                    String fname = documentSnapshot.getString("fname");
                    String greetingName = (fname != null && !fname.isEmpty()) ? fname.toLowerCase() : "User";
                    username.setValue(greetingName);
                    sessionManager.saveUsername(greetingName);

                    // Retrieve and update profile picture URL if available
                    String picUrl = documentSnapshot.getString("profile_picture");
                    if (picUrl != null && !picUrl.isEmpty()) {
                        profilePicUrl.setValue(picUrl);
                        sessionManager.saveProfilePicUrl(picUrl);
                    }

                    // Retrieve and update saving value independently
                    Double savingValue = documentSnapshot.getDouble("saving");
                    saving.setValue(savingValue != null ? savingValue : 0.0);
                })
                .addOnFailureListener(e -> Log.e("fetchUserData", "Error fetching user data", e));
    }
}
