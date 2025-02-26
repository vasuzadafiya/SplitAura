package com.example.splitaura;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.fragments.AddFriendFragment;
import com.example.splitaura.fragments.FriendRequestFragment;
import com.example.splitaura.fragments.FriendsFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;

public class ManageFriends extends BaseActivity {

    private MaterialButton btnFriends;
    private MaterialButton btnAddFriends;
    private MaterialButton btnRequests;
    private ImageButton back;

    @SuppressLint("ObsoleteSdkInt")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_friends);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            // Set the status bar color to transparent
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        back = findViewById(R.id.back_navigation);

        setButton();
    }

    private void setButton() {
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroup);
        btnFriends = findViewById(R.id.btn_friends);
        btnAddFriends = findViewById(R.id.btn_add_friends);
        btnRequests = findViewById(R.id.btn_requests);

        // Set default selection (Friends tab selected)
        btnFriends.setChecked(true);
        updateButtonStyles(btnFriends);
        replaceFragment(new FriendsFragment());

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return; // Skip if button is being unchecked
            MaterialButton selectedButton = findViewById(checkedId);
            updateButtonStyles(selectedButton);

            // Use if-else chain instead of switch-case to avoid constant expression issues.
            if (checkedId == R.id.btn_friends) {
                replaceFragment(new FriendsFragment());
            } else if (checkedId == R.id.btn_add_friends) {
                replaceFragment(new AddFriendFragment());
            } else if (checkedId == R.id.btn_requests) {
                replaceFragment(new FriendRequestFragment());
            }
        });
        back.setOnClickListener(v -> finish());
    }

    private void replaceFragment(Fragment fragment) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no user is logged in
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }


    private void updateButtonStyles(MaterialButton selectedButton) {
        MaterialButton[] buttons = new MaterialButton[]{btnFriends, btnAddFriends, btnRequests};
        for (MaterialButton button : buttons) {
            if (button == selectedButton) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.f_pass_color));
                button.setTextColor(Color.WHITE);
                ViewGroup.LayoutParams params = button.getLayoutParams();
                params.height = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
                button.setLayoutParams(params);
            } else {
                button.setBackgroundColor(Color.WHITE);
                button.setTextColor(Color.GRAY);
                ViewGroup.LayoutParams params = button.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                button.setLayoutParams(params);
            }
        }
    }

    public void navigateToAddFriendFragment() {
        // Programmatically "click" the Add Friends button
        btnAddFriends.setChecked(true);
    }
}
