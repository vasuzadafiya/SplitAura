package com.example.splitaura.dialogues;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;

import com.airbnb.lottie.LottieAnimationView;
import com.example.splitaura.admin.NetworkUtil;
import com.example.splitaura.R;

public class NoInternetDialog {
    private static Dialog dialog;

    public static void show(Context context) {
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        dialog = new Dialog(context, R.style.CustomLoadingDialog);
        dialog.setContentView(R.layout.no_internet_dialog);
        dialog.setCancelable(false); // Prevent dismissal

        Button retryButton = dialog.findViewById(R.id.btn_retry);
        LottieAnimationView animationView = dialog.findViewById(R.id.lottieAnimationView);

        retryButton.setOnClickListener(v -> {
            if (NetworkUtil.isNetworkAvailable(context)) {
                dialog.dismiss();
            } else {
                animationView.playAnimation(); // Continue showing
            }
        });
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        dialog.show();
    }

    public static void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
