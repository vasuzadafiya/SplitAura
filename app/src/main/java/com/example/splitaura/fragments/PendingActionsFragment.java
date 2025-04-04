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

import com.example.splitaura.adapters.PendingActionAdapter;
import com.example.splitaura.databinding.FragmentPendingActionsBinding;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Notification;
import com.example.splitaura.models.Transaction;
import com.example.splitaura.viewmodels.PaymentViewModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                loadingDialog.show();
                binding.tvEmptyPending.setVisibility(View.GONE);
                binding.rvPendingActions.setVisibility(View.GONE);
            } else {
                loadingDialog.hide();
            }
        });

        // Observe pending notifications
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
        Log.d(TAG, "onAction called - Notification: " + notification.getActionId() + ", ActionType: " + notification.getActionType() +
                ", SenderId: " + notification.getSenderId() + ", ReceiverId: " + notification.getReceiverId() +
                ", TransactionId: " + notification.getTransactionId() + ", Accept: " + accept);
        loadingDialog.show();

        executor.execute(() -> {
            try {
                String newStatus = accept ? "CONFIRMED" : "REJECTED";
                String transactionStatus = accept ? "SUCCESS" : "FAILED";
                Log.d(TAG, "New status: " + newStatus + ", Transaction status: " + transactionStatus);

                // Update the transaction status (only for the sender)
                if ("PAYMENT_SENT".equals(notification.getActionType()) || "SETTLEMENT".equals(notification.getActionType())) {
                    Log.d(TAG, "Updating transaction status for sender: " + notification.getSenderId() + ", TransactionId: " + notification.getTransactionId());
                    updateTransactionStatus(notification.getSenderId(), notification.getTransactionId(), transactionStatus);

                    // If PAYMENT_SENT is accepted, log a transaction for the receiver
                    if (accept && "PAYMENT_SENT".equals(notification.getActionType())) {
                        Log.d(TAG, "PAYMENT_SENT accepted, fetching sender transaction: " + notification.getTransactionId());
                        db.collection("users").document(notification.getSenderId())
                                .collection("transactions").document(notification.getTransactionId())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        Log.w(TAG, "Sender transaction does not exist: " + notification.getTransactionId());
                                        return;
                                    }
                                    Transaction senderTransaction = documentSnapshot.toObject(Transaction.class);
                                    if (senderTransaction != null) {
                                        Log.d(TAG, "Sender transaction fetched: Amount: " + senderTransaction.getAmount() +
                                                ", Method: " + senderTransaction.getMethod());
                                        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                                        Friend senderFriend = findFriendByUid(notification.getSenderId());
                                        String senderName = senderFriend != null ? senderFriend.getFullName() : "Unknown User";
                                        Log.d(TAG, "Sender name: " + senderName);
                                        Transaction receiverTransaction = new Transaction(
                                                "Payment Accepted from " + senderName,
                                                senderTransaction.getAmount(), date, "SUCCESS",
                                                notification.getTransactionId() + "_ACCEPTED", senderTransaction.getMethod(),
                                                notification.getSenderId()
                                        );
                                        Log.d(TAG, "Logging receiver transaction: " + receiverTransaction.getPaymentId());
                                        db.collection("users").document(notification.getReceiverId())
                                                .collection("transactions")
                                                .document(receiverTransaction.getPaymentId())
                                                .set(receiverTransaction)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Receiver transaction logged: " + receiverTransaction.getPaymentId()))
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to log receiver transaction", e));
                                    } else {
                                        Log.w(TAG, "Sender transaction is null after conversion");
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch sender transaction: " + notification.getTransactionId(), e));
                    }
                } else if (accept && "PAYMENT_REQUEST".equals(notification.getActionType())) {
                    Log.d(TAG, "PAYMENT_REQUEST accepted, fetching sender transaction: " + notification.getTransactionId());
                    Friend senderFriend = findFriendByUid(notification.getSenderId());
                    String senderName = senderFriend != null ? senderFriend.getFullName() : "Unknown User";
                    Log.d(TAG, "Sender name: " + senderName);
                    db.collection("users").document(notification.getSenderId())
                            .collection("transactions").document(notification.getTransactionId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (!documentSnapshot.exists()) {
                                    Log.w(TAG, "Sender transaction does not exist: " + notification.getTransactionId());
                                    return;
                                }
                                Transaction senderTransaction = documentSnapshot.toObject(Transaction.class);
                                if (senderTransaction != null) {
                                    Log.d(TAG, "Sender transaction fetched: Amount: " + senderTransaction.getAmount());
                                    String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                                    Transaction receiverTransaction = new Transaction(
                                            "Payment to " + senderName,
                                            senderTransaction.getAmount(), date, "SUCCESS",
                                            notification.getTransactionId() + "_TX", "CASH",
                                            notification.getSenderId()
                                    );
                                    Log.d(TAG, "Logging receiver transaction: " + receiverTransaction.getPaymentId());
                                    db.collection("users").document(notification.getReceiverId())
                                            .collection("transactions")
                                            .document(receiverTransaction.getPaymentId())
                                            .set(receiverTransaction)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Receiver transaction logged: " + receiverTransaction.getPaymentId()))
                                            .addOnFailureListener(e -> Log.e(TAG, "Failed to log receiver transaction", e));
                                } else {
                                    Log.w(TAG, "Sender transaction is null after conversion");
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch sender transaction: " + notification.getTransactionId(), e));
                }

                // Delete the notification
                Log.d(TAG, "Deleting notification: " + notification.getActionId());
                db.collection("notifications").document(notification.getActionId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Notification deleted: " + notification.getActionId());
                            viewModel.refreshData();
                            requireActivity().runOnUiThread(() -> {
                                loadingDialog.hide();
                                showToast(accept ? "Action confirmed" : "Action rejected");
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete notification: " + notification.getActionId(), e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error updating action");
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error handling pending action", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }
    private void updateTransactionStatus(String userId, String transactionId, String status) {
        db.collection("users").document(userId).collection("transactions").document(transactionId)
                .update("paymentStatus", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction " + transactionId + " updated to " + status + " for user " + userId);
                    viewModel.refreshData();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update transaction status: " + transactionId + " for user " + userId, e));
    }

    private Friend findFriendByUid(String uid) {
        List<Friend> friendsList = viewModel.getFriends().getValue();
        if (friendsList != null) {
            for (Friend friend : friendsList) {
                if (friend.getUid().equals(uid)) {
                    return friend;
                }
            }
        }
        return null;
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