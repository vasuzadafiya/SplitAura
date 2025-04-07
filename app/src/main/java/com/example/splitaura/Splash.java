package com.example.splitaura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        EdgeToEdge.enable(this);

        ImageButton button = findViewById(R.id.next_button);
        button.setOnClickListener(v -> {
            startActivity(new Intent(Splash.this, OnboardingScreen.class));
            finish(); // Close splash screen after moving to OnboardingScreen
        });
    }
}
