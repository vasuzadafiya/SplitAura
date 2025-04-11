package com.example.splitaura.admin;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class BannerAdHelper {

    public static void loadBanner(Context context, AdView adView) {
        if (adView == null) {
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("AdHelper", "Banner Ad Loaded");
                Toast.makeText(context, "Banner Ad Loaded", Toast.LENGTH_LONG).show();
                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError adError) {
                Log.e("AdHelper", "Banner Ad Failed: " + adError.getMessage());
                Toast.makeText(context, "Banner Ad Failed", Toast.LENGTH_LONG).show();
                adView.setVisibility(View.GONE);
            }
        });
    }
}
