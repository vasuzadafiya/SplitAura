package com.example.splitaura;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.viewpager2.widget.ViewPager2;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.List;

public class OnboardingScreen extends BaseActivity {

    private ViewPager2 viewPager;
    private List<OnboardingItem> onboardingItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.onboarding_screen);


        initOnboardingItems();
        setupViewPager();

        // Buttons
        findViewById(R.id.skipButton).setOnClickListener(v -> navigateToLogin());
        findViewById(R.id.nextButton).setOnClickListener(v -> handleNextButtonClick());
    }

    private void initOnboardingItems() {
        onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_image1, getString(R.string.title1), getString(R.string.desc1)));
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_image2, getString(R.string.title2), getString(R.string.desc2)));
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_image3, getString(R.string.title3), getString(R.string.desc3)));
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        OnboardingAdapter adapter = new OnboardingAdapter(onboardingItems);
        viewPager.setAdapter(adapter);

        DotsIndicator dotsIndicator = findViewById(R.id.dotsIndicator);
        dotsIndicator.attachTo(viewPager);
    }

    private void handleNextButtonClick() {
        if (viewPager.getCurrentItem() < onboardingItems.size() - 1) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
        } else {
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finishAffinity();
    }
}
