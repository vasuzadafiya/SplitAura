package com.example.splitaura.admin;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.splitaura.dialogues.NoInternetDialog;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set the network change listener
        NetworkChangeReceiver.setNetworkChangeListener(isConnected -> {
            if (isConnected) {
                NoInternetDialog.dismiss();
            } else {
                NoInternetDialog.show(BaseActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check network when activity is resumed
        if (!NetworkUtil.isNetworkAvailable(this)) {
            NoInternetDialog.show(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NoInternetDialog.dismiss(); // Prevent memory leaks
    }
}
