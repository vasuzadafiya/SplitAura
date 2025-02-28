package com.example.splitaura;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Redirect to the homepage if the user is logged in
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, Homepage.class));
            finish(); // Close splash screen
            return; // Avoid executing the rest of the code
        }

        EdgeToEdge.enable(this);
        // Set up the button only if needed
        ImageButton button = findViewById(R.id.next_button);
        button.setOnClickListener(v -> {
            startActivity(new Intent(Splash.this, OnboardingScreen.class));
            finish(); // Close splash screen after moving to OnboardingScreen
        });
    }
}
