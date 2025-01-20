package com.example.splitaura;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class Utill {
    //    this class will contain utility methods like showing toast, hide keyboard...
    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showToast(Activity activity, String s) {
        Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
    }

    public static boolean isValidAmount(String amount) {
        // Regular expression to match valid numbers excluding zero
        String regex = "^(?!0$)(0\\.\\d*[1-9]|[1-9]\\d*(\\.\\d+)?|0[1-9]\\d*)$";

        if (amount == null || amount.isEmpty()) {
            return false;
        }

        // Check if the input matches the regular expression
        return amount.matches(regex);
    }

}
