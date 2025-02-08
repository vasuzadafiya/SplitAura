package com.example.splitaura.admin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkUtil {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
}
