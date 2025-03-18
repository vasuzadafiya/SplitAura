package com.example.splitaura;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.fragments.GroupFragment;
import com.example.splitaura.fragments.HomeFragment;
import com.example.splitaura.fragments.PaymentFragment;
import com.example.splitaura.fragments.ProfileFragment;
import com.example.splitaura.utils.Utill;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

public class Homepage extends BaseActivity {

    private FrameLayout curvedBracketContainer;
    private View shadowOverlay;
    private boolean isCurvedBarVisible = false;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);

        // Draw behind status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        curvedBracketContainer = findViewById(R.id.curvedBracketContainer);
        shadowOverlay = findViewById(R.id.shadowOverlay);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Initialize Click Listeners for Spotlight Icons
        findViewById(R.id.personalContainer).setOnClickListener(v -> openPersonalActivity());
        findViewById(R.id.groupContainer).setOnClickListener(v -> openGroupActivity());
        findViewById(R.id.friendsContainer).setOnClickListener(v -> openFriendsActivity());

        // Clicking on shadowOverlay should hide the curved bracket
        shadowOverlay.setOnClickListener(v -> hideCurvedBracket());

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, new HomeFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                hideCurvedBracket();
            } else if (itemId == R.id.nav_groups) {
                selectedFragment = new GroupFragment();
                hideCurvedBracket();
            } else if (itemId == R.id.nav_spotlight) {
                toggleCurvedBracket();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
                hideCurvedBracket();
            } else if (itemId == R.id.nav_payments) {
                selectedFragment = new PaymentFragment();
                hideCurvedBracket();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_layout, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Check UPI status from Firestore
        checkUPIStatus();
    }

    // Checks Firestore for the current user's UPI status.
    private void checkUPIStatus() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String upi = documentSnapshot.getString("upi");
                        // If UPI is missing or empty, load the dialog with user details.
                        if (upi == null || upi.isEmpty()) {
                            String fname = documentSnapshot.getString("fname");
                            String lname = documentSnapshot.getString("lname");
                            String fullName = "";
                            if (fname != null) fullName += fname;
                            if (lname != null) fullName += " " + lname;
                            String profileImage = documentSnapshot.getString("profile_picture");
                            showUPIDialog(fullName, profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Homepage.this, "Error checking UPI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Displays the UPI input dialog populated with the user's full name, profile image, and description.
    private void showUPIDialog(String fullName, String profileImage) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.get_upi, null);

        TextInputEditText upiEditText = dialogView.findViewById(R.id.upi_edit_text);
        MaterialButton addUPIButton = dialogView.findViewById(R.id.addUPI);
        TextView tvDialogName = dialogView.findViewById(R.id.tvDialogName);
        ImageView ivDialogProfile = dialogView.findViewById(R.id.ivDialogProfile);
        TextView tvDialogAlertText = dialogView.findViewById(R.id.tvDialogAlertText);

        tvDialogName.setText(fullName);
        // Set the description text with a financial warning.
        tvDialogAlertText.setText("Please enter your valid UPI ID carefully. An incorrect UPI ID may result in financial issues.");
        // Load profile image using Glide (ensure you have the dependency)
        Glide.with(this)
                .load(profileImage)
                .placeholder(R.drawable.obsmallmen2)  // Optional placeholder image
                .circleCrop()
                .into(ivDialogProfile);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        // Prevent the user from dismissing the dialog until a valid UPI is added.
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        addUPIButton.setOnClickListener(v -> {
            String upiInput = upiEditText.getText().toString().trim();
            if (validateUPI(upiInput)) {
                // Show a confirmation dialog before saving the UPI.
                new AlertDialog.Builder(Homepage.this)
                        .setTitle("Confirm UPI ID")
                        .setMessage("Please confirm that your UPI: " + upiInput + " is correct. Providing an incorrect UPI ID may result in financial issues.")
                        .setPositiveButton("Confirm", (dialogInterface, i) -> {
                            saveUPIToFirestore(upiInput, dialog);
                        })
                        .setNegativeButton("Edit", null)
                        .show();
            } else {
                upiEditText.setError("Enter a valid UPI ID!");
            }
        });
    }

    // Validates the UPI using a basic regular expression.
    private boolean validateUPI(String upiId) {
        String regex = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$";
        return Pattern.matches(regex, upiId);
    }

    // Saves the UPI to the current user's Firestore document.
    private void saveUPIToFirestore(String upiInput, AlertDialog dialog) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("upi", upiInput)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Homepage.this, "UPI added successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Homepage.this, "Failed to add UPI. Try again!", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleCurvedBracket() {
        if (isCurvedBarVisible) {
            hideCurvedBracket();
        } else {
            showCurvedBracket();
        }
    }

    private void showCurvedBracket() {
        if (!isCurvedBarVisible) {
            isCurvedBarVisible = true;
            shadowOverlay.setVisibility(View.VISIBLE);
            shadowOverlay.setAlpha(0f);
            shadowOverlay.animate().alpha(0.6f).setDuration(200).start();
            shadowOverlay.setClickable(true); // Block background clicks

            curvedBracketContainer.setVisibility(View.VISIBLE);
            curvedBracketContainer.setAlpha(0f);
            curvedBracketContainer.animate().alpha(1f).setDuration(300).start();

            animateIcons();
        }
    }

    private void hideCurvedBracket() {
        if (isCurvedBarVisible) {
            isCurvedBarVisible = false;
            shadowOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                shadowOverlay.setVisibility(View.GONE);
                shadowOverlay.setClickable(false); // Re-enable background clicks
            }).start();

            curvedBracketContainer.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> curvedBracketContainer.setVisibility(View.GONE)).start();
        }
    }

    // Smoothly animates the spotlight icons.
    private void animateIcons() {
        LinearLayout personalIcon = findViewById(R.id.personalContainer);
        LinearLayout groupIcon = findViewById(R.id.groupContainer);
        LinearLayout friendsIcon = findViewById(R.id.friendsContainer);

        personalIcon.setScaleX(0f);
        personalIcon.setScaleY(0f);
        groupIcon.setScaleX(0f);
        groupIcon.setScaleY(0f);
        friendsIcon.setScaleX(0f);
        friendsIcon.setScaleY(0f);

        ObjectAnimator personalScaleX = ObjectAnimator.ofFloat(personalIcon, View.SCALE_X, 1f);
        ObjectAnimator personalScaleY = ObjectAnimator.ofFloat(personalIcon, View.SCALE_Y, 1f);
        ObjectAnimator groupScaleX = ObjectAnimator.ofFloat(groupIcon, View.SCALE_X, 1f);
        ObjectAnimator groupScaleY = ObjectAnimator.ofFloat(groupIcon, View.SCALE_Y, 1f);
        ObjectAnimator friendsScaleX = ObjectAnimator.ofFloat(friendsIcon, View.SCALE_X, 1f);
        ObjectAnimator friendsScaleY = ObjectAnimator.ofFloat(friendsIcon, View.SCALE_Y, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(personalScaleX, personalScaleY, groupScaleX, groupScaleY, friendsScaleX, friendsScaleY);
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void openPersonalActivity() {
        startActivity(new Intent(this, PersonalExpenseActivity.class));
        hideCurvedBracket();
    }

    private void openGroupActivity() {
        Utill.showToast(this, "Coming Soon");
        hideCurvedBracket();
    }

    private void openFriendsActivity() {
        startActivity(new Intent(this, ManageFriends.class));
        hideCurvedBracket();
    }

    // Automatically navigates to the groups fragment when adding a friend.
    public void navigateToAddFriendFragment() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_groups);
        }
    }
}
