package com.example.splitaura;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkChangeReceiver extends BroadcastReceiver {

    public interface NetworkChangeListener {
        void onNetworkChange(boolean isConnected);
    }

    private static NetworkChangeListener listener;

    public static void setNetworkChangeListener(NetworkChangeListener networkListener) {
        listener = networkListener;
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = NetworkUtil.isNetworkAvailable(context);
        if (listener != null) {
            listener.onNetworkChange(isConnected);
        }
    }
}