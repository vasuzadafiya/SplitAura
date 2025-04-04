package com.example.splitaura;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.splitaura.fragments.PendingActionsFragment;
import com.example.splitaura.fragments.RecentTransactionsFragment;
import com.example.splitaura.viewmodels.PaymentViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class PaymentDetailsActivity extends AppCompatActivity {
    private static final String TAG = "PaymentDetailsActivity";
    public static final String EXTRA_TYPE = "type";
    public static final String TYPE_PENDING = "pending";
    public static final String TYPE_TRANSACTIONS = "transactions";

    private PaymentViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize the ViewModel
        viewModel = new ViewModelProvider(this).get(PaymentViewModel.class);

        // Load data
        Log.d(TAG, "Loading initial data in PaymentDetailsActivity");
        viewModel.loadInitialData();
        viewModel.loadFriends();

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        Fragment fragment;
        if (TYPE_PENDING.equals(type)) {
            toolbar.setTitle("Pending Actions");
            fragment = new PendingActionsFragment();
        } else {
            toolbar.setTitle("Recent Transactions");
            fragment = new RecentTransactionsFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}