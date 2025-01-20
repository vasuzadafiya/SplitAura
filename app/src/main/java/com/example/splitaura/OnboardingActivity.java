package com.example.splitaura;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        DotsIndicator dotsIndicator = findViewById(R.id.dotsIndicator);

        List<OnboardingItem> onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_image1, getString(R.string.title1), getString(R.string.desc1)));
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_image2, getString(R.string.title2), getString(R.string.desc2)));
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_image3, getString(R.string.title3), getString(R.string.desc3)));

        OnboardingAdapter adapter = new OnboardingAdapter(onboardingItems);
        viewPager.setAdapter(adapter);

        dotsIndicator.attachTo(viewPager);

        adapter.setOnboardingListener(new OnboardingAdapter.OnboardingListener() {
            @Override
            public void onNextClicked(int position) {
                if (position <= 3) {
                    startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
                }

                if (position < onboardingItems.size() - 1) {
                    // Go to next page when "Next" is clicked
                    viewPager.setCurrentItem(position + 1);
                }
            }

            @Override
            public void onSkipClicker(int position) {
                startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
            }
        });


    }
}
