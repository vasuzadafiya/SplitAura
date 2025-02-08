package com.example.splitaura;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.fragment.app.Fragment;

import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.fragments.GroupFragment;
import com.example.splitaura.fragments.HomeFragment;
import com.example.splitaura.fragments.PaymentFragment;
import com.example.splitaura.fragments.ProfileFragment;
import com.example.splitaura.fragments.SpotlightFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Homepage extends BaseActivity {
    FrameLayout frameLayout;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);

        frameLayout = findViewById(R.id.frame_layout);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new HomeFragment()).commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_groups) {
                selectedFragment = new GroupFragment();
            } else if (itemId == R.id.nav_payments) {
                selectedFragment = new PaymentFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_spotlight) {
                selectedFragment = new SpotlightFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, selectedFragment).commit();
            }
            return true;
        });

    }
}