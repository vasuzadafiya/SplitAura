package com.example.splitaura;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Splash extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.splash);

         mAuth = FirebaseAuth.getInstance();
         currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), Homepage.class);
            startActivity(intent);
            finishAffinity();
        }

//        for portrait orientation
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


//        for next activity
        ImageButton button = findViewById(R.id.next_button);

        button.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), OnboardingScreen.class);
            startActivity(intent);
        });


    }
}