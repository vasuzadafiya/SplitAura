package com.example.splitaura.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_PROFILE_PIC = "profilePicUrl";
    private static final String KEY_USERNAME = "username";
    private static UserSessionManager instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private UserSessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized UserSessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserSessionManager(context);
        }
        return instance;
    }

    public void saveProfilePicUrl(String url) {
        editor.putString(KEY_PROFILE_PIC, url);
        editor.apply();
    }

    public String getProfilePicUrl() {
        return sharedPreferences.getString(KEY_PROFILE_PIC, null);
    }

    public void saveUsername(String name) {
        editor.putString(KEY_USERNAME, name);
        editor.apply();
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }
}
