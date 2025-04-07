package com.example.splitaura.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.splitaura.ExpenseManager;
import com.example.splitaura.databinding.FragmentPendingActionsBinding;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.adapters.PendingActionAdapter;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Notification;
import com.example.splitaura.models.Transaction;
import com.example.splitaura.viewmodels.PaymentViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PendingActionsFragment extends Fragment implements PendingActionAdapter.OnActionListener {
    private static final String TAG = "PendingActionsFragment";
    private FragmentPendingActionsBinding binding;
    private PaymentViewModel viewModel;
    private PendingActionAdapter pendingAdapter;
    private LoadingDialog loadingDialog;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PaymentViewModel.class);
        loadingDialog = new LoadingDialog(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPendingActionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        pendingAdapter = new PendingActionAdapter(requireContext(), this);
        binding.rvPendingActions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPendingActions.setAdapter(pendingAdapter);
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                loadingDialog.show();
                binding.tvEmptyPending.setVisibility(View.GONE);
                binding.rvPendingActions.setVisibility(View.GONE);
            } else {
                loadingDialog.hide();
            }
        });

        viewModel.getPendingNotifications().observe(getViewLifecycleOwner(), pending -> {
            if (pending == null || pending.isEmpty()) {
                binding.tvEmptyPending.setVisibility(View.VISIBLE);
                binding.rvPendingActions.setVisibility(View.GONE);
            } else {
                binding.tvEmptyPending.setVisibility(View.GONE);
                binding.rvPendingActions.setVisibility(View.VISIBLE);
                pendingAdapter.updatePendingActions(pending);
            }
            Log.d(TAG, "Pending notifications updated, count: " + (pending != null ? pending.size() : 0));
        });
    }

    @Override
    public void onAction(Notification notification, boolean accept) {
        if (notification == null) {
            Log.w(TAG, "Null notification in onAction");
            return;
        }
        Log.d(TAG, "Processing action: " + notification.getActionId() + ", Type: " + notification.getActionType() + ", Accept: " + accept);
        loadingDialog.show();

        executor.execute(() -> {
            try {
                String currentUserId = viewModel.getCurrentUserId();
                if (currentUserId == null) {
                    requireActivity().runOnUiThread(() -> {
                        showToast("User not authenticated");
                        loadingDialog.hide();
                    });
                    return;
                }

                String senderId = notification.getSenderId();
                String transactionStatus = accept ? "SUCCESS" : "FAILED";

                db.collection("users").document(senderId)
                        .collection("transactions").document(notification.getTransactionId())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                Log.e(TAG, "Sender transaction not found: " + notification.getTransactionId());
                                requireActivity().runOnUiThread(() -> {
                                    showToast("Error: Transaction not found");
                                    loadingDialog.hide();
                                });
                                return;
                            }

                            Transaction senderTx = doc.toObject(Transaction.class);
                            if (senderTx == null) {
                                Log.e(TAG, "Failed to parse sender transaction");
                                requireActivity().runOnUiThread(() -> {
                                    showToast("Error: Invalid transaction data");
                                    loadingDialog.hide();
                                });
                                return;
                            }

                            double amount = senderTx.getAmount();
                            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                            Friend senderFriend = findFriendByUid(senderId);
                            String senderName = senderFriend != null ? senderFriend.getFullName() : "Unknown";

                            WriteBatch batch = db.batch();

                            // Update sender's transaction status
                            batch.update(db.collection("users").document(senderId)
                                            .collection("transactions").document(notification.getTransactionId()),
                                    "paymentStatus", transactionStatus);

                            // Variables for debt settlement
                            String debtorId = null;
                            String creditorId = null;
                            Transaction receiverTx = null;

                            if (accept) {
                                String actionType = notification.getActionType();
                                switch (actionType) {
                                    case "PAYMENT_SENT":
                                    case "QR":
                                        receiverTx = new Transaction(
                                                "Payment from " + senderName, amount, date, "SUCCESS",
                                                notification.getTransactionId() + "_RX", "CASH", senderId);
                                        debtorId = senderId;
                                        creditorId = currentUserId;
                                        break;
                                    case "PAYMENT_REQUEST":
                                        receiverTx = new Transaction(
                                                "Payment to " + senderName, amount, date, "SUCCESS",
                                                notification.getTransactionId() + "_RX", "CASH", senderId);
                                        debtorId = currentUserId;
                                        creditorId = senderId;
                                        break;
                                    case "SETTLEMENT":
                                        receiverTx = new Transaction(
                                                "Settlement with " + senderName, amount, date, "SUCCESS",
                                                notification.getTransactionId() + "_RX", "SETTLEMENT", senderId);
                                        double netOwed = senderFriend != null ? calculateNetAmountOwed(senderFriend) : 0.0;
                                        if (netOwed < 0) {
                                            debtorId = currentUserId;
                                            creditorId = senderId;
                                        } else if (netOwed > 0) {
                                            debtorId = senderId;
                                            creditorId = currentUserId;
                                        } else {
                                            debtorId = senderId;
                                            creditorId = currentUserId;
                                        }
                                        break;
                                    default:
                                        Log.w(TAG, "Unknown action type: " + actionType);
                                        debtorId = senderId;
                                        creditorId = currentUserId;
                                        break;
                                }

                                if (receiverTx != null) {
                                    batch.set(db.collection("users").document(currentUserId)
                                            .collection("transactions").document(receiverTx.getPaymentId()), receiverTx);
                                }
                            }

                            // Delete notification
                            batch.delete(db.collection("notifications").document(notification.getActionId()));

                            String finalDebtorId = debtorId;
                            String finalCreditorId = creditorId;
                            batch.commit()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Action processed: " + notification.getActionId());
                                        requireActivity().runOnUiThread(() -> {
                                            if (accept && finalDebtorId != null && finalCreditorId != null) {
                                                viewModel.settleDebt(finalDebtorId, finalCreditorId, amount);
                                                showToast("Action confirmed and debt settled");
                                            } else if (!accept) {
                                                showToast("Action rejected");
                                            }
                                            loadingDialog.hide();
                                            viewModel.refreshData();
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to process action: " + notification.getActionId(), e);
                                        requireActivity().runOnUiThread(() -> {
                                            showToast("Error processing action: " + e.getMessage());
                                            loadingDialog.hide();
                                        });
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch sender transaction", e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error fetching transaction: " + e.getMessage());
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error handling action", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private Friend findFriendByUid(String uid) {
        List<Friend> friendsList = viewModel.getFriends().getValue();
        if (friendsList != null) {
            for (Friend friend : friendsList) {
                if (friend.getUid().equals(uid)) return friend;
            }
        }
        return null;
    }

    private double calculateNetAmountOwed(Friend friend) {
        if (friend == null) return 0.0;
        String currentUserId = viewModel.getCurrentUserId();
        double netAmountOwed = 0.0;

        List<ExpenseManager> expenseManagers = viewModel.getExpenseManagers();
        if (expenseManagers == null || expenseManagers.isEmpty()) {
            Log.w(TAG, "No expense managers available for net owed calculation");
            return 0.0;
        }

        for (ExpenseManager manager : expenseManagers) {
            Map<String, ExpenseManager.UserExpenseData> userExpenseData = manager.getUserExpenseData().getValue();
            if (userExpenseData == null) continue;

            ExpenseManager.UserExpenseData currentUserData = userExpenseData.get(currentUserId);
            ExpenseManager.UserExpenseData friendData = userExpenseData.get(friend.getUid());

            if (currentUserData != null && friendData != null) {
                Double debtToFriend = currentUserData.debts.get(friend.getUid());
                if (debtToFriend != null) netAmountOwed -= debtToFriend;

                Double creditFromFriend = currentUserData.credits.get(friend.getUid());
                if (creditFromFriend != null) netAmountOwed += creditFromFriend;
            }
        }
        Log.d(TAG, "Net amount owed by current user to " + friend.getFullName() + ": " + netAmountOwed);
        return netAmountOwed;
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
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