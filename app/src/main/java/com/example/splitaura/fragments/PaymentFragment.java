package com.example.splitaura.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.ExpenseManager;
import com.example.splitaura.PaymentDetailsActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.ContactsAdapter;
import com.example.splitaura.adapters.PendingActionAdapter;
import com.example.splitaura.adapters.TransactionPaymentAdapter;
import com.example.splitaura.databinding.FragmentPaymentBinding;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Notification;
import com.example.splitaura.models.Transaction;
import com.example.splitaura.viewmodels.PaymentViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PaymentFragment extends Fragment {
    private static final String TAG = "PaymentFragment";
    private FragmentPaymentBinding binding;
    private PaymentViewModel viewModel;
    private LoadingDialog loadingDialog;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private PendingActionAdapter pendingAdapter;
    private TransactionPaymentAdapter transactionAdapter;
    private ContactsAdapter contactsAdapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PaymentViewModel.class);
        loadingDialog = new LoadingDialog(requireActivity());
        Log.d(TAG, "Fragment created, ViewModel initialized");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPaymentBinding.inflate(inflater, container, false);
        Log.d(TAG, "View created");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerViews();
        setupListeners();
        setupAmountInput();
        observeViewModel();
        viewModel.loadInitialData();
        viewModel.loadFriends();
        Log.d(TAG, "View setup complete, initial data loading started");
    }

    private void setupRecyclerViews() {
        pendingAdapter = new PendingActionAdapter(requireContext(), this::handlePendingAction);
        transactionAdapter = new TransactionPaymentAdapter(requireContext());
        Log.d(TAG, "RecyclerView adapters initialized");
    }

    private void showConfirmationDialog(String message, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setTitle("Confirm Action")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> onConfirm.run())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshData();
            binding.swipeRefreshLayout.setRefreshing(false);
            Log.d(TAG, "Swipe refresh triggered");
        });

        binding.btnSelectFriend.setOnClickListener(v -> showFriendSelectionDialog());

        binding.btnSend.setOnClickListener(v -> {
            if (!validateInput()) return;
            Friend selectedFriend = viewModel.getSelectedFriend().getValue();
            double amount = Double.parseDouble(binding.etAmount.getText().toString());
            String message = String.format(Locale.getDefault(), "Are you sure you want to send ₹%.2f to %s?", amount, selectedFriend.getFullName());
            showConfirmationDialog(message, () -> initiatePayment(false));
        });

        binding.btnRequest.setOnClickListener(v -> {
            if (!validateInput()) return;
            Friend selectedFriend = viewModel.getSelectedFriend().getValue();
            double amount = Double.parseDouble(binding.etAmount.getText().toString());
            String message = String.format(Locale.getDefault(), "Are you sure you want to request ₹%.2f from %s?", amount, selectedFriend.getFullName());
            showConfirmationDialog(message, () -> initiatePayment(true));
        });

        binding.btnSettle.setOnClickListener(v -> {
            if (!validateInput()) return;
            Friend selectedFriend = viewModel.getSelectedFriend().getValue();
            double amount = Double.parseDouble(binding.etAmount.getText().toString());
            String message = String.format(Locale.getDefault(), "Are you sure you want to settle ₹%.2f with %s?", amount, selectedFriend.getFullName());
            showConfirmationDialog(message, this::settleDebt);
        });

        binding.btnExportPdf.setOnClickListener(v -> {
            String msg = String.format(Locale.getDefault(), "Are you sure you want to export the PDF?");
            showConfirmationDialog(msg, this::generatePdf);
        });
        binding.btnQrPayment.setOnClickListener(v -> showQrPaymentDialog());
        binding.btnPendingActions.setOnClickListener(v -> showPendingActionsDialog());
        binding.btnRecentTransactions.setOnClickListener(v -> showRecentTransactionsDialog());
        Log.d(TAG, "Button listeners set up");
    }

    private void setupAmountInput() {
        binding.etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.amountInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "Amount input changed: " + s.toString());
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSelectedFriend().observe(getViewLifecycleOwner(), friend -> {
            binding.tvSelectedFriend.setText(friend != null ? friend.getFullName() : "Select a friend");
            updateDebtInfo(friend);
            Log.d(TAG, "Selected friend updated: " + (friend != null ? friend.getFullName() : "null"));
        });

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance == null) {
                Log.w(TAG, "Balance is null");
                return;
            }
            binding.tvBalance.setText(String.format(Locale.getDefault(), "₹%.2f", Math.abs(balance)));
            binding.tvBalanceStatus.setText(balance > 0 ? "You are owed" : balance < 0 ? "You owe" : "Settled up");
            binding.tvBalance.setTextColor(ContextCompat.getColor(requireContext(),
                    balance > 0 ? R.color.green : balance < 0 ? R.color.red : R.color.black));
            updateDebtInfo(viewModel.getSelectedFriend().getValue());
            Log.d(TAG, "Balance updated: " + balance);
        });

        viewModel.getPendingNotifications().observe(getViewLifecycleOwner(), pending -> {
            if (pending == null) pending = new ArrayList<>();
            binding.tvPendingCount.setText(pending.isEmpty() ? "No pending actions" :
                    getResources().getQuantityString(R.plurals.pending_actions_count, pending.size(), pending.size()));
            pendingAdapter.updatePendingActions(pending);
            Log.d(TAG, "Pending notifications updated, count: " + pending.size());
        });

        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null) transactions = new ArrayList<>();
            transactionAdapter.updateTransactions(transactions);
            Log.d(TAG, "Transactions updated, count: " + transactions.size());
        });

        viewModel.getFriends().observe(getViewLifecycleOwner(), friends -> {
            if (friends != null && contactsAdapter != null) {
                contactsAdapter.updateFriendsList(friends);
                Log.d(TAG, "Friends list updated, count: " + friends.size());
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (!TextUtils.isEmpty(error)) {
                showToast(error);
                loadingDialog.hide();
                Log.e(TAG, "Error message received: " + error);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                loadingDialog.show();
                Log.d(TAG, "Loading started");
            } else {
                loadingDialog.hide();
                Log.d(TAG, "Loading stopped");
            }
        });
    }

    private void updateDebtInfo(Friend friend) {
        if (friend == null) {
            binding.amountInputLayout.setHint("Enter amount");
            binding.etAmount.setText("");
            binding.tvBalance.setText("₹0.00");
            binding.tvBalanceStatus.setText("Settled up");
            binding.tvBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            binding.btnSend.setEnabled(false);
            binding.btnRequest.setEnabled(false);
            binding.btnSettle.setEnabled(false);
            Log.d(TAG, "No friend selected, resetting debt info");
            return;
        }

        executor.execute(() -> {
            try {
                double netAmount = calculateNetAmountOwed(friend);
                requireActivity().runOnUiThread(() -> {
                    binding.tvBalance.setText(String.format(Locale.getDefault(), "₹%.2f", Math.abs(netAmount)));
                    binding.tvBalanceStatus.setText(netAmount > 0 ? "You are owed" : netAmount < 0 ? "You owe" : "Settled up");
                    binding.tvBalance.setTextColor(ContextCompat.getColor(requireContext(),
                            netAmount > 0 ? R.color.green : netAmount < 0 ? R.color.red : R.color.black));

                    boolean sendEnabled = netAmount < 0;
                    boolean requestEnabled = netAmount > 0;
                    boolean settleEnabled = netAmount != 0;

                    binding.btnSend.setEnabled(sendEnabled);
                    binding.btnRequest.setEnabled(requestEnabled);
                    binding.btnSettle.setEnabled(settleEnabled);

                    if (sendEnabled) {
                        binding.amountInputLayout.setHint(String.format(Locale.getDefault(), "You owe ₹%.2f", -netAmount));
                        binding.etAmount.setText(String.format(Locale.getDefault(), "%.2f", -netAmount));
                    } else if (requestEnabled) {
                        binding.amountInputLayout.setHint(String.format(Locale.getDefault(), "They owe you ₹%.2f", netAmount));
                        binding.etAmount.setText(String.format(Locale.getDefault(), "%.2f", netAmount));
                    } else {
                        binding.amountInputLayout.setHint("Settled up");
                        binding.etAmount.setText("");
                    }
                    Log.d(TAG, "Debt info updated for " + friend.getFullName() + ": " + netAmount);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error calculating debt for " + friend.getFullName(), e);
                requireActivity().runOnUiThread(() -> showToast("Error calculating debt"));
            }
        });
    }

    private double calculateNetAmountOwed(Friend friend) {
        String currentUserId = viewModel.getCurrentUserId();
        double netAmountOwed = 0.0;

        List<ExpenseManager> expenseManagers = viewModel.getExpenseManagers();
        if (expenseManagers == null || expenseManagers.isEmpty()) {
            Log.w(TAG, "No expense managers available for debt calculation");
            return 0.0;
        }

        for (ExpenseManager manager : expenseManagers) {
            Map<String, ExpenseManager.UserExpenseData> userExpenseData = manager.getUserExpenseData().getValue();
            if (userExpenseData == null) {
                Log.w(TAG, "UserExpenseData is null for group: " + manager.toString());
                continue;
            }

            ExpenseManager.UserExpenseData currentUserData = userExpenseData.get(currentUserId);
            ExpenseManager.UserExpenseData friendData = userExpenseData.get(friend.getUid());

            if (currentUserData != null && friendData != null) {
                Double debtToFriend = currentUserData.debts.get(friend.getUid());
                if (debtToFriend != null) {
                    netAmountOwed -= debtToFriend;
                    Log.d(TAG, "Current user owes " + friend.getFullName() + ": " + debtToFriend);
                }

                Double creditFromFriend = currentUserData.credits.get(friend.getUid());
                if (creditFromFriend != null) {
                    netAmountOwed += creditFromFriend;
                    Log.d(TAG, friend.getFullName() + " owes current user: " + creditFromFriend);
                }
            } else {
                Log.w(TAG, "Missing data for user " + currentUserId + " or friend " + friend.getUid());
            }
        }

        Log.d(TAG, "Net amount owed with " + friend.getFullName() + ": " + netAmountOwed);
        return netAmountOwed;
    }

    private void initiatePayment(boolean isRequest) {
        if (!validateInput()) return;
        loadingDialog.show();
        String currentUserId = viewModel.getCurrentUserId();

        executor.execute(() -> {
            try {
                double amount = Double.parseDouble(binding.etAmount.getText().toString());
                Friend selectedFriend = viewModel.getSelectedFriend().getValue();
                if (selectedFriend == null) throw new IllegalStateException("No friend selected");

                String transactionId = (isRequest ? "REQ_" : "PAY_") + System.currentTimeMillis();
                String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

                // Create a Transaction if it's a PAYMENT_SENT
                if (!isRequest) {
                    Transaction transaction = new Transaction(
                            "Payment to " + selectedFriend.getFullName(), amount, date, "PENDING",
                            transactionId, "CASH", selectedFriend.getUid()
                    );
                    db.collection("users").document(currentUserId).collection("transactions")
                            .document(transactionId).set(transaction)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction logged: " + transactionId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to log transaction: " + transactionId, e));
                }

                // Create a Notification
                Notification notification = new Notification(
                        isRequest ? "PAYMENT_REQUEST" : "PAYMENT_SENT",
                        currentUserId, selectedFriend.getUid(), transactionId, transactionId, "PENDING"
                );

                db.collection("notifications").document(transactionId).set(notification)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Notification created: " + transactionId + ", type: " + notification.getActionType());
                            requireActivity().runOnUiThread(() -> {
                                showToast(isRequest ? "Payment request sent" : "Payment initiated");
                                loadingDialog.hide();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create notification: " + transactionId, e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error initiating payment");
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Payment initiation failed", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private void settleDebt() {
        if (!validateInput()) return;
        loadingDialog.show();
        String currentUserId = viewModel.getCurrentUserId();
        Friend selectedFriend = viewModel.getSelectedFriend().getValue();

        executor.execute(() -> {
            try {
                double amount = Double.parseDouble(binding.etAmount.getText().toString());
                String transactionId = "SET_" + System.currentTimeMillis();
                String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

                // Create a Transaction
                Transaction transaction = new Transaction(
                        "Settlement with " + selectedFriend.getFullName(), amount, date, "PENDING",
                        transactionId, "SETTLEMENT", selectedFriend.getUid()
                );
                db.collection("users").document(currentUserId).collection("transactions")
                        .document(transactionId).set(transaction)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction logged: " + transactionId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to log transaction: " + transactionId, e));

                // Create a Notification
                Notification notification = new Notification(
                        "SETTLEMENT", currentUserId, selectedFriend.getUid(), transactionId, transactionId, "PENDING"
                );

                db.collection("notifications").document(transactionId).set(notification)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Settlement notification created: " + transactionId);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Settlement initiated, awaiting confirmation");
                                loadingDialog.hide();
                                viewModel.refreshData();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create settlement notification: " + transactionId, e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error initiating settlement");
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Settlement failed", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private void logTransaction(String userId, Friend friend, double amount, String date, String paymentId, String method) {
        Transaction transaction = new Transaction(
                "Payment to " + friend.getFullName(), amount, date, "PENDING", paymentId, method, friend.getUid()
        );

        db.collection("users").document(userId).collection("transactions").document(paymentId)
                .set(transaction)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction logged: " + paymentId + ", method: " + method);
                    viewModel.refreshData();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to log transaction: " + paymentId, e));
    }

    void handlePendingAction(Notification notification, boolean accept) {
        if (notification == null) {
            Log.w(TAG, "Null notification in handlePendingAction");
            return;
        }
        loadingDialog.show();
        String currentUserId = viewModel.getCurrentUserId();

        executor.execute(() -> {
            try {
                String newStatus = accept ? "CONFIRMED" : "REJECTED";
                String transactionStatus = accept ? "SUCCESS" : "FAILED";

                // Update the transaction status
                if ("PAYMENT_SENT".equals(notification.getActionType()) || "SETTLEMENT".equals(notification.getActionType())) {
                    updateTransactionStatus(notification.getSenderId(), notification.getTransactionId(), transactionStatus);

                    // If PAYMENT_SENT is accepted, log a transaction for the receiver
                    if (accept && "PAYMENT_SENT".equals(notification.getActionType())) {
                        db.collection("users").document(notification.getSenderId())
                                .collection("transactions").document(notification.getTransactionId())
                                .get()
                                .addOnSuccessListener(senderTxDoc -> {
                                    if (senderTxDoc.exists()) {
                                        Transaction senderTransaction = senderTxDoc.toObject(Transaction.class);
                                        if (senderTransaction != null) {
                                            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                                            Friend senderFriend = findFriendByUid(notification.getSenderId());
                                            String senderName = senderFriend != null ? senderFriend.getFullName() : "Unknown User";
                                            Transaction receiverTransaction = new Transaction(
                                                    "Payment Accepted from " + senderName,
                                                    senderTransaction.getAmount(), date, "SUCCESS",
                                                    notification.getTransactionId() + "_ACCEPTED", senderTransaction.getMethod(),
                                                    notification.getSenderId()
                                            );
                                            db.collection("users").document(notification.getReceiverId())
                                                    .collection("transactions")
                                                    .document(receiverTransaction.getPaymentId())
                                                    .set(receiverTransaction)
                                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Receiver transaction logged: " + receiverTransaction.getPaymentId()))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to log receiver transaction", e));
                                        }
                                    } else {
                                        Log.w(TAG, "Sender transaction not found: " + notification.getTransactionId());
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch sender transaction", e));
                    }
                } else if (accept && "PAYMENT_REQUEST".equals(notification.getActionType())) {
                    // Fetch the sender's friend object
                    Friend senderFriend = findFriendByUid(notification.getSenderId());
                    String senderName = senderFriend != null ? senderFriend.getFullName() : "Unknown User";
                    db.collection("users").document(notification.getSenderId())
                            .collection("transactions").document(notification.getTransactionId())
                            .get()
                            .addOnSuccessListener(senderTxDoc -> {
                                if (senderTxDoc.exists()) {
                                    Transaction senderTransaction = senderTxDoc.toObject(Transaction.class);
                                    if (senderTransaction != null) {
                                        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                                        Transaction receiverTransaction = new Transaction(
                                                "Payment to " + senderName,
                                                senderTransaction.getAmount(), date, "SUCCESS",
                                                notification.getTransactionId() + "_TX", "CASH",
                                                notification.getSenderId()
                                        );
                                        db.collection("users").document(notification.getReceiverId())
                                                .collection("transactions")
                                                .document(receiverTransaction.getPaymentId())
                                                .set(receiverTransaction)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Receiver transaction logged: " + receiverTransaction.getPaymentId()))
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to log receiver transaction", e));
                                    }
                                } else {
                                    Log.w(TAG, "Sender transaction not found for request: " + notification.getTransactionId());
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch sender transaction", e));
                }

                // Delete the notification
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
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction " + transactionId + " updated to " + status))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update transaction: " + transactionId, e));
    }

    private void showFriendSelectionDialog() {
        try {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.FullScreenDialog);
            builder.setView(dialogView);

            RecyclerView rvFriends = dialogView.findViewById(R.id.recyclerViewCategories);
            TextView tvTitle = dialogView.findViewById(R.id.category_dialog_title);
            tvTitle.setText("Select Friend");
            rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));

            List<Friend> tempSelected = new ArrayList<>();
            contactsAdapter = new ContactsAdapter(
                    viewModel.getFriends().getValue() != null ? viewModel.getFriends().getValue() : new ArrayList<>(),
                    tempSelected, friend -> {
                tempSelected.clear();
                tempSelected.add(friend);
                contactsAdapter.notifyDataSetChanged();
            });
            rvFriends.setAdapter(contactsAdapter);

            Button cancelButton = dialogView.findViewById(R.id.cancel_button);
            Button okButton = dialogView.findViewById(R.id.ok_button);
            AlertDialog dialog = builder.create();

            cancelButton.setOnClickListener(v -> dialog.dismiss());
            okButton.setOnClickListener(v -> {
                if (!tempSelected.isEmpty()) {
                    viewModel.setSelectedFriend(tempSelected.get(0));
                    dialog.dismiss();
                    Log.d(TAG, "Friend selected: " + tempSelected.get(0).getFullName());
                } else {
                    showToast("Please select a friend");
                }
            });

            dialog.show();
            Log.d(TAG, "Friend selection dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing friend selection dialog", e);
            showToast("Error showing friend selection");
        }
    }

    private void showPendingActionsDialog() {
        Intent intent = new Intent(requireContext(), PaymentDetailsActivity.class);
        intent.putExtra(PaymentDetailsActivity.EXTRA_TYPE, PaymentDetailsActivity.TYPE_PENDING);
        startActivity(intent);
    }

    private void showRecentTransactionsDialog() {
        Intent intent = new Intent(requireContext(), PaymentDetailsActivity.class);
        intent.putExtra(PaymentDetailsActivity.EXTRA_TYPE, PaymentDetailsActivity.TYPE_TRANSACTIONS);
        startActivity(intent);
    }

    private void showQrPaymentDialog() {
        if (!validateInput()) return;
        loadingDialog.show();

        executor.execute(() -> {
            try {
                double amount = Double.parseDouble(binding.etAmount.getText().toString());
                Friend selectedFriend = viewModel.getSelectedFriend().getValue();
                if (selectedFriend == null) {
                    throw new IllegalStateException("No friend selected");
                }
                Log.d(TAG, "Selected friend UID: " + selectedFriend.getUid());

                // Fetch the friend's UPI ID from Firestore asynchronously
                db.collection("users")
                        .document(selectedFriend.getUid())
                        .get()
                        .addOnSuccessListener(friendDoc -> {
                            try {
                                Log.d(TAG, "Firestore query successful for user: " + selectedFriend.getUid());
                                if (friendDoc == null || !friendDoc.exists()) {
                                    throw new IllegalStateException("Friend's data not found in Firestore");
                                }

                                String upiId = friendDoc.getString("upi");
                                if (TextUtils.isEmpty(upiId)) {
                                    throw new IllegalStateException("Friend's UPI ID not available");
                                }
                                Log.d(TAG, "UPI ID fetched: " + upiId);

                                // Generate UPI payment string
                                String upiString = String.format(Locale.US, "upi://pay?pa=%s&am=%.2f&cu=INR&tn=SplitAuraPayment",
                                        upiId, amount);
                                Log.d(TAG, "UPI string generated: " + upiString);

                                // Generate QR code
                                Bitmap qrBitmap = generateQrCode(upiString);
                                if (qrBitmap == null) {
                                    throw new Exception("Failed to generate QR code bitmap");
                                }
                                Log.d(TAG, "QR code generated successfully");

                                // Log the transaction
                                String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                                String paymentId = "QR_" + System.currentTimeMillis();
                                logTransaction(viewModel.getCurrentUserId(), selectedFriend, amount, date, paymentId, "QR");
                                Log.d(TAG, "Transaction logged with paymentId: " + paymentId);

                                // Show the QR code dialog on the UI thread
                                requireActivity().runOnUiThread(() -> {
                                    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_code, null);
                                    ImageView ivQrCode = dialogView.findViewById(R.id.iv_qr_code);
                                    ivQrCode.setImageBitmap(qrBitmap);

                                    new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                                            .setTitle(String.format(Locale.getDefault(), "Scan to Pay ₹%.2f", amount))
                                            .setView(dialogView)
                                            .setPositiveButton("Done", null)
                                            .show();
                                    loadingDialog.hide();
                                    Log.d(TAG, "QR payment dialog shown, transaction logged: " + paymentId);
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "QR payment error after Firestore query", e);
                                requireActivity().runOnUiThread(() -> {
                                    showToast("Error generating QR code: " + e.getMessage());
                                    loadingDialog.hide();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore query failed for user: " + selectedFriend.getUid(), e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error fetching friend's UPI ID: " + e.getMessage());
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "QR payment error before Firestore query", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error generating QR code: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private Bitmap generateQrCode(String data) throws Exception {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "QR code generation failed", e);
            throw e;
        }
    }

    private void generatePdf() {
        loadingDialog.show();
        executor.execute(() -> {
            try {
                // Create the PDF file
                File file = new File(requireContext().getExternalFilesDir(null),
                        "Transactions_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf");
                PdfWriter writer = new PdfWriter(new FileOutputStream(file));
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                // Add App Logo
                try {
                    Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.applogo);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    logoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] logoBytes = stream.toByteArray();
                    Image logoImage = new Image(ImageDataFactory.create(logoBytes));
                    logoImage.setWidth(100);
                    logoImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                    document.add(logoImage);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding logo to PDF", e);
                    document.add(new Paragraph("Error loading logo").setItalic());
                }

                // Title
                document.add(new Paragraph("SplitAura Transaction Report")
                        .setFontSize(18)
                        .setBold()
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setMarginTop(10));

                // Fetch transactions from ViewModel
                List<Transaction> transactions = viewModel.getTransactions().getValue() != null ?
                        new ArrayList<>(viewModel.getTransactions().getValue()) : new ArrayList<>();

                // Calculate period
                String period = "No transactions";
                if (!transactions.isEmpty()) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        SimpleDateFormat parseFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                        transactions.sort((t1, t2) -> {
                            try {
                                Date date1 = parseFormat.parse(t1.getRecordedAt());
                                Date date2 = parseFormat.parse(t2.getRecordedAt());
                                return date2.compareTo(date1);
                            } catch (Exception e) {
                                Log.e(TAG, "Error sorting transactions", e);
                                return 0;
                            }
                        });
                        String startDate = "Unknown";
                        String endDate = "Unknown";
                        try {
                            Date start = parseFormat.parse(transactions.get(transactions.size() - 1).getRecordedAt());
                            if (start != null) {
                                startDate = dateFormat.format(start);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing start date", e);
                        }
                        try {
                            Date end = parseFormat.parse(transactions.get(0).getRecordedAt());
                            if (end != null) {
                                endDate = dateFormat.format(end);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing end date", e);
                        }
                        period = String.format("Period: %s to %s", startDate, endDate);
                    } catch (Exception e) {
                        Log.e(TAG, "Error calculating period", e);
                        period = "Period: Unknown";
                    }
                }
                document.add(new Paragraph(period)
                        .setFontSize(12)
                        .setItalic()
                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                        .setMarginBottom(20));

                // Table for transactions
                if (!transactions.isEmpty()) {
                    Table table = new Table(7).useAllAvailableWidth();
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Friend").setBold()));
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Amount").setBold()));
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Date").setBold()));
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Status").setBold()));
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Transaction ID").setBold()));
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("UPI").setBold()));
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Method").setBold()));

                    List<Task<DocumentSnapshot>> upiTasks = new ArrayList<>();
                    for (Transaction tx : transactions) {
                        Task<DocumentSnapshot> task = db.collection("users")
                                .document(tx.getRecipientId())
                                .get();
                        upiTasks.add(task);
                    }

                    Tasks.whenAllSuccess(upiTasks).addOnSuccessListener(results -> {
                        for (int i = 0; i < transactions.size(); i++) {
                            Transaction tx = transactions.get(i);
                            DocumentSnapshot friendDoc = (DocumentSnapshot) results.get(i);
                            String upi = "N/A";
                            if (friendDoc != null && friendDoc.exists()) {
                                upi = friendDoc.getString("upi") != null ? friendDoc.getString("upi") : "N/A";
                            } else {
                                Log.w(TAG, "Friend document not found for recipient: " + tx.getRecipientId());
                            }

                            // Fetch friend name
                            Friend friend = findFriendByUid(tx.getRecipientId());
                            String friendName = friend != null ? friend.getFullName() : (tx.getDescription() != null ? tx.getDescription().replace("Payment to ", "") : "Unknown");

                            // Add row to table
                            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(friendName)));
                            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.format(Locale.getDefault(), "₹%.2f", tx.getAmount()))));
                            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(tx.getRecordedAt() != null ? tx.getRecordedAt() : "Unknown")));
                            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(tx.getPaymentStatus() != null ? tx.getPaymentStatus() : "Unknown")));
                            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(tx.getPaymentId() != null ? tx.getPaymentId() : "N/A")));
                            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(upi)));
                            table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(tx.getMethod() != null ? tx.getMethod() : "Unknown")));
                        }
                        document.add(table);
                        document.close();

                        // Share the PDF
                        requireActivity().runOnUiThread(() -> {
                            showToast("PDF saved to: " + file.getAbsolutePath());
                            Uri fileUri = FileProvider.getUriForFile(requireContext(),
                                    requireContext().getPackageName() + ".fileprovider", file);
                            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                            viewIntent.setDataAndType(fileUri, "application/pdf");
                            viewIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivity(Intent.createChooser(viewIntent, "Open PDF"));
                            } catch (Exception e) {
                                showToast("No PDF viewer found on device");
                            }
                            loadingDialog.hide();
                            Log.d(TAG, "PDF generated: " + file.getAbsolutePath());
                        });
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching UPI IDs for PDF", e);
                        document.add(new Paragraph("Error fetching UPI IDs").setItalic());
                        document.close();
                        requireActivity().runOnUiThread(() -> {
                            showToast("Error generating PDF: " + e.getMessage());
                            loadingDialog.hide();
                        });
                    });
                } else {
                    document.add(new Paragraph("No transactions found").setItalic());
                    document.close();

                    // Share the PDF
                    requireActivity().runOnUiThread(() -> {
                        showToast("PDF saved to: " + file.getAbsolutePath());
                        Uri fileUri = FileProvider.getUriForFile(requireContext(),
                                requireContext().getPackageName() + ".fileprovider", file);
                        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                        viewIntent.setDataAndType(fileUri, "application/pdf");
                        viewIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            startActivity(Intent.createChooser(viewIntent, "Open PDF"));
                        } catch (Exception e) {
                            showToast("No PDF viewer found on device");
                        }
                        loadingDialog.hide();
                        Log.d(TAG, "PDF generated: " + file.getAbsolutePath());
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "PDF generation error", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error generating PDF: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private Friend findFriendByUid(String uid) {
        List<Friend> friends = viewModel.getFriends().getValue();
        if (friends != null) {
            for (Friend friend : friends) {
                if (friend.getUid().equals(uid)) {
                    return friend;
                }
            }
        }
        return null;
    }

    private boolean validateInput() {
        String amountStr = binding.etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            binding.amountInputLayout.setError("Amount cannot be empty");
            Log.w(TAG, "Validation failed: Amount empty");
            return false;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.amountInputLayout.setError("Amount must be positive");
                Log.w(TAG, "Validation failed: Amount <= 0");
                return false;
            }
            if (amount > 1000000) {
                binding.amountInputLayout.setError("Amount too large");
                Log.w(TAG, "Validation failed: Amount too large");
                return false;
            }
        } catch (NumberFormatException e) {
            binding.amountInputLayout.setError("Invalid amount");
            Log.w(TAG, "Validation failed: Invalid amount format");
            return false;
        }
        if (viewModel.getSelectedFriend().getValue() == null) {
            showToast("Please select a friend");
            Log.w(TAG, "Validation failed: No friend selected");
            return false;
        }
        binding.amountInputLayout.setError(null);
        Log.d(TAG, "Input validated successfully");
        return true;
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
        Log.d(TAG, "Fragment view destroyed");
    }
}