package com.example.splitaura;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
    private boolean hasShownUPIDialog = false;
    private int previousItemId = R.id.nav_home; // Track previous navigation item for animation direction

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);

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

        findViewById(R.id.personalContainer).setOnClickListener(v -> openPersonalActivity());
        findViewById(R.id.groupContainer).setOnClickListener(v -> openGroupActivity());
        findViewById(R.id.friendsContainer).setOnClickListener(v -> openFriendsActivity());

        shadowOverlay.setOnClickListener(v -> hideCurvedBracket());

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
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                // Determine animation direction based on navigation order
                if (getNavigationIndex(itemId) != getNavigationIndex(previousItemId)) {
                    if (getNavigationIndex(itemId) > getNavigationIndex(previousItemId)) {
                        transaction.setCustomAnimations(
                                R.anim.slide_in_right,  // Enter animation for new fragment
                                R.anim.slide_out_left   // Exit animation for old fragment
                        );
                    } else {
                        transaction.setCustomAnimations(
                                R.anim.slide_in_left,   // Enter animation for new fragment (backwards)
                                R.anim.slide_out_right  // Exit animation for old fragment (backwards)
                        );
                    }
                }

                transaction.replace(R.id.frame_layout, selectedFragment)
                        .commit();
                previousItemId = itemId; // Update previous item
            }
            return true;
        });

        // Handle back stack for consistent animations
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                // Ensure proper animation when returning to the initial fragment
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
                if (currentFragment != null && !(currentFragment instanceof HomeFragment)) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    );
                    transaction.replace(R.id.frame_layout, new HomeFragment()).commit();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    previousItemId = R.id.nav_home;
                }
            }
        });

        handleNavigationIntent(getIntent());
        checkUPIStatus();
    }

    // Helper method to determine navigation order for animation direction
    private int getNavigationIndex(int itemId) {
        if (itemId == R.id.nav_home) return 0;
        if (itemId == R.id.nav_groups) return 1;
        if (itemId == R.id.nav_spotlight) return 2;
        if (itemId == R.id.nav_payments) return 3;
        if (itemId == R.id.nav_profile) return 4;
        return -1;
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
            shadowOverlay.setClickable(true);

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
                shadowOverlay.setClickable(false);
            }).start();

            curvedBracketContainer.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> curvedBracketContainer.setVisibility(View.GONE)).start();
        }
    }

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

    public void navigateToAddFriendFragment() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_groups);
        }
    }

    public void navigateToPaymentFragments() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_payments);
        }
    }

    private void checkUPIStatus() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(Homepage.this, "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Homepage.this, LoginActivity.class));
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String upi = documentSnapshot.getString("upi");
                        Log.d("Homepage", "UPI from Firestore: " + upi);
                        if ((upi == null || upi.isEmpty() || !validateUPI(upi)) && !hasShownUPIDialog) {
                            hasShownUPIDialog = true;
                            String fname = documentSnapshot.getString("fname");
                            String lname = documentSnapshot.getString("lname");
                            String fullName = "";
                            if (fname != null) fullName += fname;
                            if (lname != null) fullName += " " + lname;
                            String profileImage = documentSnapshot.getString("profile_picture");
                            showUPIDialog(fullName, profileImage);
                        }
                    } else {
                        if (!isFinishing()) {
                            Toast.makeText(Homepage.this, "User profile not found in Firestore", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(Homepage.this, "Error checking UPI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showUPIDialog(String fullName, String profileImage) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.get_upi, null);

        TextInputEditText upiEditText = dialogView.findViewById(R.id.upi_edit_text);
        MaterialButton addUPIButton = dialogView.findViewById(R.id.addUPI);
        TextView tvDialogName = dialogView.findViewById(R.id.tvUserName);
        ImageView ivDialogProfile = dialogView.findViewById(R.id.iv_upi_profile);
        TextView tvDialogAlertText = dialogView.findViewById(R.id.tvAlertText);

        tvDialogName.setText(fullName);
        String guidanceText = "Please enter your valid UPI ID carefully. An incorrect UPI ID may result in financial issues.\n\n" +
                "How to find your UPI ID:\n" +
                "• Google Pay: Open the app, tap your profile picture (top-left), and find your UPI ID under your name (e.g., yourname@okaxis).\n" +
                "• PhonePe: Open the app, go to 'My Profile' (top-right), and check your UPI ID under 'UPI Settings' (e.g., yourname@ybl).\n" +
                "• Paytm: Open the app, tap the profile icon (top-left), go to 'UPI & Payment Settings,' and copy your UPI ID (e.g., yourname@paytm).\n\n" +
                "Example UPI ID: exampleuser@okhdfcbank";
        tvDialogAlertText.setText(guidanceText);

        Glide.with(this)
                .load(profileImage)
                .placeholder(R.drawable.obsmallmen2)
                .circleCrop()
                .into(ivDialogProfile);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        addUPIButton.setOnClickListener(v -> {
            String upiInput = upiEditText.getText().toString().trim();
            if (validateUPI(upiInput)) {
                new AlertDialog.Builder(Homepage.this)
                        .setTitle("Confirm UPI ID")
                        .setMessage("Please confirm that your UPI: " + upiInput + " is correct. Providing an incorrect UPI ID may result in financial issues.")
                        .setPositiveButton("Confirm", (dialogInterface, i) -> saveUPIToFirestore(upiInput, dialog))
                        .setNegativeButton("Edit", null)
                        .show();
            } else {
                upiEditText.setError("Enter a valid UPI ID!");
                upiEditText.requestFocus();
            }
        });
    }

    private boolean validateUPI(String upiId) {
        String regex = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9]{2,64}$";
        boolean isValid = Pattern.matches(regex, upiId);
        Log.d("Homepage", "Validating UPI: " + upiId + ", Is Valid: " + isValid);
        return isValid;
    }

    private void saveUPIToFirestore(String upiInput, AlertDialog dialog) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("upi", upiInput)
                .addOnSuccessListener(aVoid -> {
                    if (!isFinishing()) {
                        Toast.makeText(Homepage.this, "UPI added successfully!", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(Homepage.this, "Failed to add UPI. Try again!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("navigateToPayment", false)) {
            navigateToPaymentFragments();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }
}