package com.example.splitaura;

import android.app.Application;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Cloudinary configuration
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dngz383wb");  // Replace with your Cloud name
        config.put("api_key", "867768162938844");  // Replace with your API key
        config.put("api_secret", "dSE2KjACxC_w-6XBOYQ0-lNNcGE");  // Replace with your API secret

        // Initialize Cloudinary
        MediaManager.init(this, config);
    }
}
