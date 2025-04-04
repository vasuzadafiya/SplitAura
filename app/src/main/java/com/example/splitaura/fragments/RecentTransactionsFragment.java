package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.splitaura.adapters.TransactionPaymentAdapter;
import com.example.splitaura.databinding.FragmentRecentTransactionsBinding;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.viewmodels.PaymentViewModel;

public class RecentTransactionsFragment extends Fragment {
    private static final String TAG = "RecentTransactionsFragment";
    private FragmentRecentTransactionsBinding binding;
    private PaymentViewModel viewModel;
    private TransactionPaymentAdapter transactionAdapter;
    private LoadingDialog loadingDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PaymentViewModel.class);
        loadingDialog = new LoadingDialog(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRecentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionPaymentAdapter(requireContext());
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTransactions.setAdapter(transactionAdapter);
    }

    private void observeViewModel() {
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                loadingDialog.show();
                binding.tvEmptyTransactions.setVisibility(View.GONE);
                binding.rvTransactions.setVisibility(View.GONE);
            } else {
                loadingDialog.hide();
            }
        });

        // Observe transactions
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                binding.tvEmptyTransactions.setVisibility(View.VISIBLE);
                binding.rvTransactions.setVisibility(View.GONE);
            } else {
                binding.tvEmptyTransactions.setVisibility(View.GONE);
                binding.rvTransactions.setVisibility(View.VISIBLE);
                transactionAdapter.updateTransactions(transactions);
            }
            Log.d(TAG, "Transactions updated, count: " + (transactions != null ? transactions.size() : 0));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
        binding = null;
    }
}