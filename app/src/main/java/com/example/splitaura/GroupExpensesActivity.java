package com.example.splitaura;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.splitaura.admin.BannerAdHelper;
import com.example.splitaura.admin.BaseActivity;
import com.example.splitaura.fragments.BalancesFragment;
import com.example.splitaura.fragments.ExpensesFragment;
import com.example.splitaura.fragments.GroupInfoFragment;
import com.example.splitaura.fragments.TotalsFragment;
import com.example.splitaura.utils.Utill;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class GroupExpensesActivity extends BaseActivity {

    public static final String GROUP_ID = "groupId";
    private String groupId;

    private ImageView groupIcon;
    private TextView groupName;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private MaterialButton btnExpenses;
    private MaterialButton btnBalances;
    private MaterialButton btnTotals;
    private ImageView backButton;
    private ViewPager2 viewPager;
    View fragmentContainer;
    private MaterialButton btnGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_expenses);
        initViews();
        setTransparentStatusBar();

        AdView bannerAd = findViewById(R.id.bannerAdView);
        BannerAdHelper.loadBanner(this, bannerAd);

        // Retrieve and validate group ID.
        groupId = getIntent().getStringExtra(GROUP_ID);
        if (groupId != null) {
            loadGroupDetails();
        } else {
            Utill.showToast(this, "Group ID not found.");
            finish();
            return;
        }

        setupViewPager();
        setupButtonListeners();
        btnExpenses.setChecked(true);
        updateButtonStyles(btnExpenses);

        // Add back stack listener to restore ViewPager2 visibility
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                viewPager.setVisibility(View.VISIBLE);
                fragmentContainer.setVisibility(View.GONE);
            }
        });
    }

    private void initViews() {
        // Initialize all views from layout.
        groupIcon = findViewById(R.id.group_icon);
        groupName = findViewById(R.id.group_name);
        backButton = findViewById(R.id.back_navigation);
        btnExpenses = findViewById(R.id.btn_expenses);
        btnBalances = findViewById(R.id.btn_balances);
        btnTotals = findViewById(R.id.btn_totals);
        btnGroups = findViewById(R.id.btn_groups);
        viewPager = findViewById(R.id.viewPager);
        fragmentContainer = findViewById(R.id.fragment_container);
        viewPager.setUserInputEnabled(false);
    }

    private void setTransparentStatusBar() {
        // Set status bar to transparent if supported.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void setupViewPager() {
        // Set up the adapter for ViewPager2.
        GroupExpensesPagerAdapter adapter = new GroupExpensesPagerAdapter(this, viewPager);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                resetButtons();
                updateButtonStyles(getButtonForPosition(position));
            }
        });
    }

    private void setupButtonListeners() {
        // Assign click listeners to navigate between pages.
        btnExpenses.setOnClickListener(v -> viewPager.setCurrentItem(0));
        btnBalances.setOnClickListener(v -> viewPager.setCurrentItem(1));
        btnTotals.setOnClickListener(v -> viewPager.setCurrentItem(2));
        btnGroups.setOnClickListener(v -> viewPager.setCurrentItem(3));

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
        // Reset all buttons to default style.
        MaterialButton[] buttons = {btnExpenses, btnBalances, btnTotals, btnGroups};
        for (MaterialButton button : buttons) {
            button.setBackgroundTintList(getResources().getColorStateList(R.color.white));
            button.setTextColor(getResources().getColorStateList(R.color.black));
            ViewGroup.LayoutParams params = button.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            button.setLayoutParams(params);
        }
    }

    private void updateButtonStyles(MaterialButton selectedButton) {
        // Update style for selected and unselected buttons.
        final int selectedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        MaterialButton[] buttons = {btnExpenses, btnBalances, btnTotals, btnGroups};
        for (MaterialButton button : buttons) {
            if (button == selectedButton) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.f_pass_color));
                button.setTextColor(Color.WHITE);
                ViewGroup.LayoutParams params = button.getLayoutParams();
                params.height = selectedHeight;
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

    private void loadGroupDetails() {
        // Fetch group details from Firestore.
        db.collection("groups").document(groupId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("title");
                String groupIconUrl = documentSnapshot.getString("imageUrl");
                if (groupIconUrl != null) {
                    int radius = 20; // adjust as needed

                    // Create RequestOptions with both CenterCrop and RoundedCorners transformations.
                    RequestOptions requestOptions = new RequestOptions()
                            .transform(new CenterCrop(), new RoundedCorners(radius));

                    Glide.with(this)
                            .load(groupIconUrl)
                            .apply(requestOptions)
                            .placeholder(R.drawable.applogo) // optional placeholder image
                            .into(groupIcon);
                }
                groupName.setText(name != null ? name : "Group");
            }
        }).addOnFailureListener(e -> Utill.showToast(this, e.getMessage()));
    }

    // ViewPager2 adapter class for fragments.
    private static class GroupExpensesPagerAdapter extends FragmentStateAdapter {
        private final ViewPager2 viewPager;

        GroupExpensesPagerAdapter(AppCompatActivity activity, ViewPager2 viewPager) {
            super(activity);
            this.viewPager = viewPager;
        }

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
            return 4;
        }
    }
}