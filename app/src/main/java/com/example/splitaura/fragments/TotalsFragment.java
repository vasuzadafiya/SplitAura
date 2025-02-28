package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.splitaura.R;
import com.example.splitaura.models.TotalsData;

public class TotalsFragment extends Fragment {

    private TextView tvGroupSpending, tvYouPaid, tvYourShare;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_totals, container, false);

        // Initialize TextViews
        tvGroupSpending = view.findViewById(R.id.tv_group_spending);
        tvYouPaid = view.findViewById(R.id.tv_you_paid);
        tvYourShare = view.findViewById(R.id.tv_your_share);

        // Set static totals data programmatically
        setStaticTotals();

        return view;
    }

    private void setStaticTotals() {
        // Hardcoded static data (from the image)
        TotalsData totals = new TotalsData(678.20, 234.56, 23.45);

        // Set text with ₹ symbol and formatted to 2 decimal places
        tvGroupSpending.setText(String.format("₹%.2f", totals.getGroupSpending()));
        tvYouPaid.setText(String.format("₹%.2f", totals.getYouPaid()));
        tvYourShare.setText(String.format("₹%.2f", totals.getYourShare()));
    }
}