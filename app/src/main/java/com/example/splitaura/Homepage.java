package com.example.splitaura;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.razorpay.PaymentResultListener;
import com.example.splitaura.fragments.PaymentResultCallback;

import androidx.fragment.app.Fragment;

import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.fragments.GroupFragment;
import com.example.splitaura.fragments.HomeFragment;
import com.example.splitaura.fragments.PaymentFragment;
import com.example.splitaura.fragments.ProfileFragment;
import com.example.splitaura.utils.Utill;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Homepage extends BaseActivity implements PaymentResultCallback {

    private FrameLayout curvedBracketContainer;
    private View shadowOverlay;
    private PaymentResultCallback paymentResultCallback;
    private boolean isCurvedBarVisible = false;
    // Declare bottomNavigationView as a field so it can be used in other methods.
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

        // Clicking on shadowOverlay should hide the curvedBracket
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

    // Function to animate icons smoothly from the bottom navigation
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
        Utill.showToast(this,"Coming Soon");
        hideCurvedBracket();
    }

    private void openFriendsActivity() {
        startActivity(new Intent(this, ManageFriends.class));
        hideCurvedBracket();
    }

    // When navigateToAddFriendFragment is called, automatically select the nav_groups item.
    public void navigateToAddFriendFragment() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_groups);
        }
    }

    public void setPaymentResultCallback(PaymentResultCallback callback) {
        this.paymentResultCallback = callback;
    }

    @Override
    public void onPaymentSuccess(String paymentId) {
        Log.d("Homepage", "onPaymentSuccess: Received paymentId: " + paymentId);
        Toast.makeText(this, "MainActivity: Payment success, ID: " + paymentId, Toast.LENGTH_LONG).show();
        if (paymentResultCallback != null) {
            paymentResultCallback.onPaymentSuccess(paymentId);
        } else {
            Log.w("Homepage", "onPaymentSuccess: paymentResultCallback is null");
            Toast.makeText(this, "Callback not set", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentError(int code, String response) {
        Log.d("Homepage", "onPaymentError: Code: " + code + ", Response: " + response);
        Toast.makeText(this, "MainActivity: Payment error, Code: " + code, Toast.LENGTH_LONG).show();
        if (paymentResultCallback != null) {
            paymentResultCallback.onPaymentError(code, response);
        } else {
            Log.w("Homepage", "onPaymentError: paymentResultCallback is null");
            Toast.makeText(this, "Callback not set", Toast.LENGTH_SHORT).show();
        }
    }
}
