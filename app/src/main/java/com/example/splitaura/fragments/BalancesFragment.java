package com.example.splitaura.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.adapters.BalanceAdapter;
import com.example.splitaura.adapters.OweAdapter;
import com.example.splitaura.models.Balance;
import com.example.splitaura.models.OweRelationship;

import java.util.Arrays;
import java.util.List;

public class BalancesFragment extends Fragment {

    private RecyclerView recyclerViewBalances, recyclerViewOwing;
    private BalanceAdapter balanceAdapter;
    private OweAdapter oweAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balances, container, false);

        // Initialize RecyclerViews
        recyclerViewBalances = view.findViewById(R.id.recycler_view_balances);
        recyclerViewOwing = view.findViewById(R.id.recycler_view_owing);

        // Set up LayoutManagers
        recyclerViewBalances.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewOwing.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapters with static data
        balanceAdapter = new BalanceAdapter(getStaticBalances());
        oweAdapter = new OweAdapter(getStaticOweRelationships());

        recyclerViewBalances.setAdapter(balanceAdapter);
        recyclerViewOwing.setAdapter(oweAdapter);

        return view;
    }

    // Static data for balances (positive/negative)
    private List<Balance> getStaticBalances() {
        return Arrays.asList(
                new Balance("Laila Ainsley", 249.90),   // Positive balance (blue)
                new Balance("Charlotte Hanlin", -349.90), // Negative balance (red)
                new Balance("Darron Kulikowski", -323.89), // Negative balance (red)
                new Balance("Joseph Thomas", 4567.90),   // Positive balance (blue)
                new Balance("Kristan Watson", 768.21),   // Positive balance (blue)
                new Balance("Maryland Winkles", -6426.5) // Negative balance (red)
        );
    }

    // Static data for owe relationships
    private List<OweRelationship> getStaticOweRelationships() {
        return Arrays.asList(
                new OweRelationship("Laila Ainsley", "Vasu Zadaiya (You)", 349.90),
                new OweRelationship("Darron Kulikowski", "Vasu Zadaiya (You)", 768.21),
                new OweRelationship("Kristan Watson", "Vasu Zadaiya (You)", 768.21),
                new OweRelationship("Charlotte Hanlin", "Vasu Zadaiya (You)", 500.00),
                new OweRelationship("Joseph Thomas", "Vasu Zadaiya (You)", 1200.50),
                new OweRelationship("Maryland Winkles", "Vasu Zadaiya (You)", 1500.75),
                new OweRelationship("Vasu Zadaiya (You)", "Laila Ainsley", 200.30), // You owe Laila
                new OweRelationship("Vasu Zadaiya (You)", "Darron Kulikowski", 450.80), // You owe Darron
                new OweRelationship("Vasu Zadaiya (You)", "Charlotte Hanlin", 300.25)
        );
    }
}