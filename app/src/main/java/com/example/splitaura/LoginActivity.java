package com.example.splitaura;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;

public class LoginActivity extends AppCompatActivity {

    private TextView tvlogin, tvsignin;
    private ConstraintLayout cs_login, cs_signin;
    private Button login_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        tvlogin = findViewById(R.id.log_in);
        tvsignin = findViewById(R.id.sign_up);
        cs_login = findViewById(R.id.login_constarint);
        cs_signin = findViewById(R.id.signin_constarint);
        login_button = findViewById(R.id.login_button);

        tvlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cs_signin.setVisibility(View.GONE);
                cs_login.setVisibility(View.VISIBLE);
            }
        });

        tvsignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cs_login.setVisibility(View.GONE);
                cs_signin.setVisibility(View.VISIBLE);
            }
        });

//        login_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                View dialogView = LayoutInflater.from(LoginActivity.this).inflate(R.layout.loginsuccess, null);
//
//                // Create a dialog
//                Dialog dialog = new Dialog(LoginActivity.this);
//                dialog.setContentView(dialogView);
//
//                // Optional: Make the dialog background transparent
//                if (dialog.getWindow() != null) {
//                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                }
////
////                // Find and initialize views in the dialog layout
////                LottieAnimationView animationView = dialogView.findViewById(R.id.done_animation);
////                TextView titleText = dialogView.findViewById(R.id.textView2);
////
//
//                // Optional: Set the dialog to cancel on outside touch
//                dialog.setCancelable(true);
//
//                // Show the dialog
//                dialog.show();
//
//            }
//        });
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Use the Activity context
                View dialogView = LayoutInflater.from(LoginActivity.this).inflate(R.layout.loginsuccess, null);

                // Create a dialog
                Dialog dialog = new Dialog(LoginActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                dialog.setContentView(dialogView);

                // Optional: Make the dialog background transparent or set it to a specific background color
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                // Find and initialize views in the dialog layout
                LottieAnimationView animationView = dialogView.findViewById(R.id.done_animation);
                TextView titleText = dialogView.findViewById(R.id.textView2);

                // Optional: Set the dialog to cancel on outside touch
                dialog.setCancelable(true);

                // Show the dialog
                dialog.show();
            }
        });

    }
}