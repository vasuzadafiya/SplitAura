package com.example.splitaura;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private TextView tvlogin,tvsignin;
    private ConstraintLayout cs_login,cs_signin;
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
    }
}