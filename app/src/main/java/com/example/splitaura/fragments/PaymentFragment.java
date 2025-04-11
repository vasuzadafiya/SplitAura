package com.example.splitaura.fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
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
import com.example.splitaura.admin.AdManager;
import com.example.splitaura.databinding.FragmentPaymentBinding;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Notification;
import com.example.splitaura.models.Transaction;
import com.example.splitaura.viewmodels.PaymentViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
    ContactsAdapter contactsAdapter;
    private CountDownTimer confirmationTimer;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPaymentBinding.inflate(inflater, container, false);
        Log.d(TAG, "View created");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
        setupAmountInput();
        observeViewModel();
        viewModel.loadInitialData();
        viewModel.loadFriends();
        Log.d(TAG, "View setup complete, initial data loading started");
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
            showConfirmationDialog(message, () -> initiateSendPayment(amount, selectedFriend));
        });

        binding.btnRequest.setOnClickListener(v -> {
            if (!validateInput()) return;
            Friend selectedFriend = viewModel.getSelectedFriend().getValue();
            double amount = Double.parseDouble(binding.etAmount.getText().toString());
            String message = String.format(Locale.getDefault(), "Are you sure you want to request ₹%.2f from %s?", amount, selectedFriend.getFullName());
            showConfirmationDialog(message, () -> initiateRequestPayment(amount, selectedFriend));
        });

        binding.btnQrPayment.setOnClickListener(v -> initiateQrPayment());

        binding.btnExportPdf.setOnClickListener(v -> {
            try {
                AdManager.showInterstitial(requireActivity(), null);
            } catch (Exception e) {
                AdManager.loadInterstitial(requireContext());
            }
            String msg = "Are you sure you want to export the PDF?";
            showConfirmationDialog(msg, this::generatePdf);
        });

        binding.btnPendingActions.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PaymentDetailsActivity.class);
            intent.putExtra(PaymentDetailsActivity.EXTRA_TYPE, PaymentDetailsActivity.TYPE_PENDING);
            startActivity(intent);
        });

        binding.btnRecentTransactions.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PaymentDetailsActivity.class);
            intent.putExtra(PaymentDetailsActivity.EXTRA_TYPE, PaymentDetailsActivity.TYPE_TRANSACTIONS);
            startActivity(intent);
        });
        Log.d(TAG, "Button listeners set up");
    }

    private void setupAmountInput() {
        binding.etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

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
            if (pending.isEmpty()) {
                binding.tvPendingCount.setText("No pending actions");
                binding.tvPendingCount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green));
                binding.btnPendingActions.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.green));
            } else {
                binding.tvPendingCount.setText(getResources().getQuantityString(R.plurals.pending_actions_count, pending.size(), pending.size()));
                binding.tvPendingCount.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red));
                binding.btnPendingActions.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
            }
            Log.d(TAG, "Pending notifications updated, count: " + pending.size());
        });

        viewModel.getFriends().observe(getViewLifecycleOwner(), friends -> {
            if (friends != null && contactsAdapter != null) {
                contactsAdapter.updateFriendsList(friends);
                Log.d(TAG, "Friends list updated, count: " + friends.size());
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (!TextUtils.isEmpty(error)) {
                showToast("Error: " + error);
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

        for (ExpenseManager manager : viewModel.getExpenseManagers()) {
            manager.getUserExpenseData().observe(getViewLifecycleOwner(), data -> {
                viewModel.calculateBalance();
                updateDebtInfo(viewModel.getSelectedFriend().getValue());
            });
        }
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
            Log.d(TAG, "No friend selected, resetting debt info");
            return;
        }

        executor.execute(() -> {
            try {
                double netAmount = calculateNetAmountOwed(friend);
                requireActivity().runOnUiThread(() -> {
                    binding.tvBalance.setText(String.format(Locale.getDefault(), "₹%.2f", Math.abs(netAmount)));
                    binding.tvBalanceStatus.setText(netAmount > 0 ? "You are owed" : netAmount < 0 ? "You owe" : "Settled up");
                    binding.tvBalance.setTextColor(ContextCompat.getColor(requireContext(), netAmount > 0 ? R.color.green : netAmount < 0 ? R.color.red : R.color.black));

                    boolean sendEnabled = netAmount < 0;
                    boolean requestEnabled = netAmount > 0;

                    binding.btnSend.setEnabled(sendEnabled);
                    binding.btnRequest.setEnabled(requestEnabled);

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
                requireActivity().runOnUiThread(() -> showToast("Error calculating debt: " + e.getMessage()));
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
                Log.w(TAG, "UserExpenseData is null for group: " + manager.getGroupId());
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
            }
        }

        Log.d(TAG, "Net amount owed with " + friend.getFullName() + ": " + netAmountOwed);
        return netAmountOwed;
    }

    private void initiateSendPayment(double amount, Friend selectedFriend) {
        loadingDialog.show();
        String currentUserId = viewModel.getCurrentUserId();

        executor.execute(() -> {
            try {
                String transactionId = "PAY_" + System.currentTimeMillis();
                String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

                Transaction senderTx = new Transaction(
                        "Payment to " + selectedFriend.getFullName(), amount, date, "PENDING", transactionId, "CASH", selectedFriend.getUid());
                Notification notification = new Notification(
                        "PAYMENT_SENT", currentUserId, selectedFriend.getUid(), transactionId, transactionId, "PENDING");

                WriteBatch batch = db.batch();
                batch.set(db.collection("users").document(currentUserId).collection("transactions").document(transactionId), senderTx);
                batch.set(db.collection("notifications").document(transactionId), notification);

                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Send payment initiated: " + transactionId);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Payment sent, awaiting confirmation");
                                loadingDialog.hide();
                                viewModel.refreshData();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to initiate send payment: " + transactionId, e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error sending payment: " + e.getMessage());
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Send payment failed", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error sending payment: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private void initiateRequestPayment(double amount, Friend selectedFriend) {
        loadingDialog.show();
        String currentUserId = viewModel.getCurrentUserId();

        executor.execute(() -> {
            try {
                String transactionId = "REQ_" + System.currentTimeMillis();
                String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

                Transaction senderTx = new Transaction(
                        "Request from " + selectedFriend.getFullName(), amount, date, "PENDING", transactionId, "CASH", selectedFriend.getUid());
                Notification notification = new Notification(
                        "PAYMENT_REQUEST", currentUserId, selectedFriend.getUid(), transactionId, transactionId, "PENDING");

                WriteBatch batch = db.batch();
                batch.set(db.collection("users").document(currentUserId).collection("transactions").document(transactionId), senderTx);
                batch.set(db.collection("notifications").document(transactionId), notification);

                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Request payment initiated: " + transactionId);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Payment request sent, awaiting response");
                                loadingDialog.hide();
                                viewModel.refreshData();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to initiate request payment: " + transactionId, e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error requesting payment: " + e.getMessage());
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Request payment failed", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error requesting payment: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private void initiateQrPayment() {
        try {
            AdManager.showInterstitial(requireActivity(), null);
        } catch (Exception e) {
            AdManager.loadInterstitial(requireContext());
        }
        if (!validateInput()) return;
        loadingDialog.show();
        String currentUserId = viewModel.getCurrentUserId();
        Friend selectedFriend = viewModel.getSelectedFriend().getValue();
        double amount = Double.parseDouble(binding.etAmount.getText().toString());

        executor.execute(() -> {
            try {
                String transactionId = "QR_" + System.currentTimeMillis();
                String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

                db.collection("users").document(selectedFriend.getUid()).get()
                        .addOnSuccessListener(friendDoc -> {
                            try {
                                if (!friendDoc.exists())
                                    throw new IllegalStateException("Friend's data not found");
                                String upiId = friendDoc.getString("upi");
                                if (TextUtils.isEmpty(upiId))
                                    throw new IllegalStateException("Friend's UPI ID not available");

                                String upiString = String.format(Locale.US, "upi://pay?pa=%s&am=%.2f&cu=INR&tn=SplitAuraPayment_%s", upiId, amount, transactionId);
                                Bitmap qrBitmap = generateQrCode(upiString);

                                Transaction senderTx = new Transaction(
                                        "QR Payment to " + selectedFriend.getFullName(), amount, date, "PENDING", transactionId, "QR", selectedFriend.getUid());
                                Notification notification = new Notification(
                                        "QR", currentUserId, selectedFriend.getUid(), transactionId, transactionId, "PENDING");

                                WriteBatch batch = db.batch();
                                batch.set(db.collection("users").document(currentUserId).collection("transactions").document(transactionId), senderTx);
                                batch.set(db.collection("notifications").document(transactionId), notification);

                                batch.commit()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "QR payment transaction logged: " + transactionId);
                                            requireActivity().runOnUiThread(() -> {
                                                showQrDialog(qrBitmap, transactionId, amount);
                                                loadingDialog.hide();
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to log QR payment transaction: " + transactionId, e);
                                            requireActivity().runOnUiThread(() -> {
                                                showToast("Error initiating QR payment: " + e.getMessage());
                                                loadingDialog.hide();
                                            });
                                        });
                            } catch (Exception e) {
                                Log.e(TAG, "QR payment error", e);
                                requireActivity().runOnUiThread(() -> {
                                    showToast("Error generating QR code: " + e.getMessage());
                                    loadingDialog.hide();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch friend data", e);
                            requireActivity().runOnUiThread(() -> {
                                showToast("Error fetching friend's UPI ID: " + e.getMessage());
                                loadingDialog.hide();
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "QR payment failed", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error initiating QR payment: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private void showQrDialog(Bitmap qrBitmap, String transactionId, double amount) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_code, null);
        ImageView ivQrCode = dialogView.findViewById(R.id.iv_qr_code);
        ivQrCode.setImageBitmap(qrBitmap);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setTitle(String.format(Locale.getDefault(), "Scan to Pay ₹%.2f", amount))
                .setView(dialogView)
                .setMessage("Share this QR code with your friend to confirm payment.")
                .setPositiveButton("Done", (d, w) -> {
                    showToast("QR payment sent, awaiting receiver confirmation");
                    viewModel.refreshData();
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    updateTransactionStatus(viewModel.getCurrentUserId(), transactionId, "FAILED");
                    db.collection("notifications").document(transactionId).delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification deleted for cancelled QR: " + transactionId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to delete notification: " + transactionId, e));
                    showToast("QR payment cancelled");
                    viewModel.refreshData();
                })
                .create();

        dialog.show();
        Log.d(TAG, "QR dialog shown for transaction: " + transactionId);
    }

    private Bitmap generateQrCode(String data) throws Exception {
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
    }

    private void generatePdf() {
        loadingDialog.show();
        executor.execute(() -> {
            try {
                String date = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(new Date());
                String fileName = "SplitAura_Report_" + date + ".pdf";

                OutputStream outputStream;
                Uri pdfUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    ContentResolver resolver = requireContext().getContentResolver();
                    pdfUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (pdfUri == null)
                        throw new IOException("Failed to create new MediaStore record.");
                    outputStream = resolver.openOutputStream(pdfUri);
                } else {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    File pdfFile = new File(dir, fileName);
                    outputStream = new FileOutputStream(pdfFile);
                    pdfUri = FileProvider.getUriForFile(requireContext(), "com.example.splitaura.fileprovider", pdfFile);
                }

                PdfWriter writer = new PdfWriter(outputStream);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                document.setMargins(20, 20, 20, 20);

                Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.applogo);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                logoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                Image logoImage = new Image(ImageDataFactory.create(stream.toByteArray()));
                logoImage.setWidth(120);
                logoImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(logoImage);

                document.add(new Paragraph("SplitAura Transaction Report").setFontSize(22).setBold().setTextAlignment(TextAlignment.CENTER));

                List<Transaction> transactions = viewModel.getTransactions().getValue() != null ? new ArrayList<>(viewModel.getTransactions().getValue()) : new ArrayList<>();
                String period = transactions.isEmpty() ? "No transactions" : "Period: " + transactions.get(transactions.size() - 1).getRecordedAt() + " to " + transactions.get(0).getRecordedAt();
                document.add(new Paragraph(period).setFontSize(14).setItalic().setTextAlignment(TextAlignment.CENTER));

                if (!transactions.isEmpty()) {
                    Table table = new Table(new float[]{2.5f, 2f, 2.5f, 2f, 2.5f, 2f, 2f});
                    table.setWidth(UnitValue.createPercentValue(100));

                    String[] headers = {"Friend", "Amount", "Date", "Status", "Transaction ID", "UPI", "Method"};
                    for (String header : headers) {
                        table.addHeaderCell(new Cell().add(new Paragraph(header).setFontSize(14).setBold().setTextAlignment(TextAlignment.CENTER)));
                    }

                    for (Transaction tx : transactions) {
                        Friend friend = findFriendByUid(tx.getRecipientId());
                        String friendName = friend != null ? friend.getFullName() : "Unknown";
                        String upi = friend != null && friend.getUpi() != null ? friend.getUpi() : "N/A";
                        String status = tx.getPaymentStatus();

                        DeviceRgb statusColor = status.equals("SUCCESS") ? new DeviceRgb(0, 128, 0) : status.equals("FAILED") ? new DeviceRgb(220, 20, 60) : new DeviceRgb(255, 165, 0);

                        table.addCell(new Cell().add(new Paragraph(friendName)).setVerticalAlignment(VerticalAlignment.MIDDLE).setTextAlignment(TextAlignment.CENTER));
                        table.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "₹%.2f", tx.getAmount()))).setTextAlignment(TextAlignment.CENTER).setBold());
                        table.addCell(new Cell().add(new Paragraph(tx.getRecordedAt())).setVerticalAlignment(VerticalAlignment.MIDDLE));
                        table.addCell(new Cell().add(new Paragraph(status)).setTextAlignment(TextAlignment.CENTER).setFontColor(statusColor));
                        table.addCell(new Cell().add(new Paragraph(tx.getPaymentId())).setFontSize(10).setVerticalAlignment(VerticalAlignment.MIDDLE));
                        table.addCell(new Cell().add(new Paragraph(upi)).setVerticalAlignment(VerticalAlignment.MIDDLE));
                        table.addCell(new Cell().add(new Paragraph(tx.getMethod())).setVerticalAlignment(VerticalAlignment.MIDDLE));
                    }
                    document.add(table);
                } else {
                    document.add(new Paragraph("No transactions found").setItalic());
                }

                document.close();
                Uri finalPdfUri = pdfUri;
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.hide();
                    showToast("PDF exported successfully");
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(finalPdfUri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Open PDF with"));
                    Log.d(TAG, "PDF generated and intent started: " + finalPdfUri);
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate PDF", e);
                requireActivity().runOnUiThread(() -> {
                    showToast("Error generating PDF: " + e.getMessage());
                    loadingDialog.hide();
                });
            }
        });
    }

    private void showFriendSelectionDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_category, null);
        RecyclerView rvFriends = dialogView.findViewById(R.id.recyclerViewCategories);
        TextView tvTitle = dialogView.findViewById(R.id.category_dialog_title);
        tvTitle.setText("Select Friend");
        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<Friend> tempSelected = new ArrayList<>();
        contactsAdapter = new ContactsAdapter(viewModel.getFriends().getValue() != null ? viewModel.getFriends().getValue() : new ArrayList<>(), tempSelected, friend -> {
            tempSelected.clear();
            tempSelected.add(friend);
            contactsAdapter.notifyDataSetChanged();
        });
        rvFriends.setAdapter(contactsAdapter);

        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.FullScreenDialog)
                .setView(dialogView)
                .create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            if (!tempSelected.isEmpty()) {
                viewModel.setSelectedFriend(tempSelected.get(0));
                dialog.dismiss();
            } else {
                showToast("Please select a friend");
            }
        });

        dialog.show();
    }

    private boolean validateInput() {
        String amountStr = binding.etAmount.getText().toString().trim();
        Friend selectedFriend = viewModel.getSelectedFriend().getValue();

        if (TextUtils.isEmpty(amountStr)) {
            binding.amountInputLayout.setError("Enter an amount");
            Log.w(TAG, "Validation failed: Amount is empty");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.amountInputLayout.setError("Amount must be greater than 0");
                Log.w(TAG, "Validation failed: Amount <= 0");
                return false;
            }
        } catch (NumberFormatException e) {
            binding.amountInputLayout.setError("Invalid amount format");
            Log.w(TAG, "Validation failed: Invalid amount format");
            return false;
        }

        if (selectedFriend == null) {
            showToast("Please select a friend");
            Log.w(TAG, "Validation failed: No friend selected");
            return false;
        }

        return true;
    }

    private void showConfirmationDialog(String message, Runnable onConfirm) {
        if (confirmationTimer != null) {
            confirmationTimer.cancel();
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setTitle("Confirm Action")
                .setMessage(message)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setEnabled(true);

        confirmationTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                positiveButton.setText(String.format(Locale.getDefault(), "Confirm (%d)", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                positiveButton.setText("Confirm");
                positiveButton.setOnClickListener(v -> {
                    onConfirm.run();
                    dialog.dismiss();
                });
            }
        }.start();

        Log.d(TAG, "Confirmation dialog shown: " + message);
    }

    private void updateTransactionStatus(String userId, String transactionId, String status) {
        db.collection("users").document(userId).collection("transactions").document(transactionId)
                .update("paymentStatus", status)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction status updated: " + transactionId + " to " + status))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update transaction status: " + transactionId, e));
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

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Toast shown: " + message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (confirmationTimer != null) {
            confirmationTimer.cancel();
        }
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.hide();
        }
        binding = null;
        Log.d(TAG, "View destroyed");
    }
}