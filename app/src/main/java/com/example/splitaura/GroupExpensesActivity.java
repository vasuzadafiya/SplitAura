package com.example.splitaura;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.fragments.BalancesFragment;
import com.example.splitaura.fragments.ExpensesFragment;
import com.example.splitaura.fragments.GroupInfoFragment;
import com.example.splitaura.fragments.TotalsFragment;
import com.google.android.material.button.MaterialButton;


public class GroupExpensesActivity extends BaseActivity {

    private MaterialButton btnExpenses;
    private MaterialButton btnBalances;
    private MaterialButton btnTotals;
    private MaterialButton btnGroups;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ViewPager2 viewPager;
        ImageView backButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_expenses);

        // Set status bar color to transparent
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        backButton = findViewById(R.id.back_navigation);
        btnExpenses = findViewById(R.id.btn_expenses);
        btnBalances = findViewById(R.id.btn_balances);
        btnTotals = findViewById(R.id.btn_totals);
        btnGroups = findViewById(R.id.btn_groups);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);

        // Setup ViewPager2 adapter
        GroupExpensesPagerAdapter adapter = new GroupExpensesPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Setup button listeners to navigate between ViewPager2 pages
        btnExpenses.setOnClickListener(v -> viewPager.setCurrentItem(0));
        btnBalances.setOnClickListener(v -> viewPager.setCurrentItem(1));
        btnTotals.setOnClickListener(v -> viewPager.setCurrentItem(2));
        btnGroups.setOnClickListener(v -> viewPager.setCurrentItem(3));

        // Set default selection (Expenses tab selected)
        btnExpenses.setChecked(true);
        updateButtonStyles(btnExpenses);

        // Page change listener for ViewPager2 to update button styles
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                resetButtons();
                MaterialButton selectedButton = getButtonForPosition(position);
                updateButtonStyles(selectedButton);
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    private MaterialButton getButtonForPosition(int position) {
        switch (position) {
            case 1:
                return btnBalances;
            case 2:
                return btnTotals;
            case 3:
                return btnGroups;
            default:
                return btnExpenses;
        }
    }

    private void resetButtons() {
        btnExpenses.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        btnExpenses.setTextColor(getResources().getColorStateList(R.color.black));
        btnBalances.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        btnBalances.setTextColor(getResources().getColorStateList(R.color.black));
        btnTotals.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        btnTotals.setTextColor(getResources().getColorStateList(R.color.black));
        btnGroups.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        btnGroups.setTextColor(getResources().getColorStateList(R.color.black));
    }

    private void updateButtonStyles(MaterialButton selectedButton) {
        MaterialButton[] buttons = new MaterialButton[]{btnExpenses, btnBalances, btnTotals, btnGroups};
        for (MaterialButton button : buttons) {
            if (button == selectedButton) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.f_pass_color));
                button.setTextColor(Color.WHITE);
                ViewGroup.LayoutParams params = button.getLayoutParams();
                params.height = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
                button.setLayoutParams(params);
            } else {
                button.setBackgroundColor(Color.WHITE);
                button.setTextColor(Color.GRAY);
                ViewGroup.LayoutParams params = button.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                button.setLayoutParams(params);
            }
        }
    }

    // ViewPager2 adapter class for the fragments
    private static class GroupExpensesPagerAdapter extends FragmentStateAdapter {

        public GroupExpensesPagerAdapter(AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new BalancesFragment();
                case 2:
                    return new TotalsFragment();
                case 3:
                    return new GroupInfoFragment();
                default:
                    return new ExpensesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4; // Total number of fragments
        }

    }

}
