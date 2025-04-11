package com.example.splitaura.admin;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdManager {
    private static final String AD_UNIT_ID = "ca-app-pub-1993940483015486/6615121600";
    private static InterstitialAd interstitialAd;
    private static boolean isLoading = false;

    public static void loadInterstitial(Context context) {
        if (interstitialAd != null || isLoading) return;

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                isLoading = false;
                Log.d("AdManager", "Interstitial Ad Loaded");
                Toast.makeText(context, "Ad Loaded", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                interstitialAd = null;
                isLoading = false;
                Log.e("AdManager", "Interstitial Failed: " + adError.getMessage());
                Toast.makeText(context, "Interstitial Failed: " + adError.getMessage(), Toast.LENGTH_LONG).show();

            }
        });
    }

    public static void showInterstitial(Activity activity, Runnable onAdDismissed) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    interstitialAd = null;
                    loadInterstitial(activity); // Reload for next time
                    onAdDismissed.run(); // Resume app flow
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    interstitialAd = null;
                    onAdDismissed.run();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    interstitialAd = null; // Prevent showing again
                }
            });
            interstitialAd.show(activity);
        } else {
            onAdDismissed.run(); // If ad not ready, resume anyway
        }
    }

}
