package com.example.splitaura;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import com.airbnb.lottie.LottieAnimationView;

public class LoadingDialog {
    private final Dialog dialog;

    //    This Class will use for Create custom animation like custom progressbar or Loading bar
    public LoadingDialog(Context context) {
        dialog = new Dialog(context, R.style.CustomLoadingDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.loadingbar_layout);


        LottieAnimationView animationView = dialog.findViewById(R.id.animation_view);
        animationView.playAnimation();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(-1, -1);
            dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            );
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }


    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void hide() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

}