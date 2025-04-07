package com.example.splitaura;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;

public class splashScreen extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this); // Android 12+ smooth launch
        setTheme(R.style.AppTheme); // Fall back to main theme
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        ImageView logo = findViewById(R.id.logo);

        Animation splashAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(splashAnim);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this,
                    mAuth.getCurrentUser() != null ? Homepage.class : Splash.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_TIME_OUT);
    }
}
