package com.example.splitaura;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.fragments.AddFriendFragment;
import com.example.splitaura.fragments.FriendRequestFragment;
import com.example.splitaura.fragments.FriendsFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class ManageFriends extends BaseActivity {

    private MaterialButton btnFriends, btnAddFriends, btnRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_friends);

        setButton();
    }

    private void setButton() {
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroup);
        btnFriends = findViewById(R.id.btn_friends);
        btnAddFriends = findViewById(R.id.btn_add_friends);
        btnRequests = findViewById(R.id.btn_requests);

        // Set default selection (Friends tab selected)
        btnFriends.setChecked(true);
        updateButtonStyle(btnFriends);
        replaceFragment(new FriendsFragment());

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                MaterialButton selectedButton = findViewById(checkedId);
                updateButtonStyle(selectedButton);

                // Fragment switching logic
                if (checkedId == R.id.btn_friends) {
                    replaceFragment(new FriendsFragment());
                } else if (checkedId == R.id.btn_add_friends) {
                    replaceFragment(new AddFriendFragment());
                } else if (checkedId == R.id.btn_requests) {
                    replaceFragment(new FriendRequestFragment());
                }
            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private void updateButtonStyle(MaterialButton selectedButton) {
        // Reset all buttons to default state
        resetButtonStyle(btnFriends);
        resetButtonStyle(btnAddFriends);
        resetButtonStyle(btnRequests);

        // Set selected button style
        selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.f_pass_color));
        selectedButton.setTextColor(Color.WHITE);

        // Update height dynamically
        ViewGroup.LayoutParams params = selectedButton.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()); // Convert DP to PX
        selectedButton.setLayoutParams(params);
    }

    private void resetButtonStyle(MaterialButton button) {
        button.setBackgroundColor(Color.WHITE);
        button.setTextColor(Color.GRAY);

        // Reset height if needed
        ViewGroup.LayoutParams params = button.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Reset to default height
        button.setLayoutParams(params);
    }

}
