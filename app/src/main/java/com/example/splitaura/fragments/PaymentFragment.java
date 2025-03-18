package com.example.splitaura.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.LoginActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.TransactionPaymentAdapter;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Transaction;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PaymentFragment extends Fragment implements PaymentResultListener {
    private static final String TAG = "PaymentFragment";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String RAZORPAY_TEST_KEY = "rzp_test_1DP5mmOlF5G5ag"; // Replace with your test key

    private TextView tvBalanceSummary, tvTestModeNotice;
    private Spinner spinnerFriends;
    private EditText etAmount;
    private MaterialButton btnPay, btnPayQr, btnGeneratePdf;
    private ProgressBar progressBar;
    private RecyclerView rvTransactions;
    private TransactionPaymentAdapter transactionAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private List<Friend> friendsList;
    private Map<String, Double> balancesMap;
    private List<Transaction> transactionList;
    private Friend selectedFriend;
    private List<String> groupIds;
    private Set<String> uniqueFriendIds;
    private Map<String, ExpenseManager> expenseManagers;

    public PaymentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Checkout.preload(getContext()); // Preload Razorpay SDK
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            redirectToLogin();
            return view;
        }

//        tvTestModeNotice = view.findViewById(R.id.tv_test_mode_notice);
        tvBalanceSummary = view.findViewById(R.id.tv_balance_summary);
        spinnerFriends = view.findViewById(R.id.spinner_friends);
        etAmount = view.findViewById(R.id.et_amount);
        btnPay = view.findViewById(R.id.btn_pay);
        btnPayQr = view.findViewById(R.id.btn_pay_qr);
        btnGeneratePdf = view.findViewById(R.id.btn_generate_pdf);
        progressBar = view.findViewById(R.id.progress_bar);
        rvTransactions = view.findViewById(R.id.rv_transactions);

        friendsList = new ArrayList<>();
        balancesMap = new HashMap<>();
        transactionList = new ArrayList<>();
        groupIds = new ArrayList<>();
        uniqueFriendIds = new HashSet<>();
        expenseManagers = new HashMap<>();

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionPaymentAdapter(getContext());
        rvTransactions.setAdapter(transactionAdapter);

        loadAllGroupsAndFriends();
        setupTransactionListener();

        btnPay.setOnClickListener(v -> initiatePayment());
        btnPayQr.setOnClickListener(v -> initiateQrPayment());
        btnGeneratePdf.setOnClickListener(v -> generateTransactionPdf());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set up observer for selected friend balance
        spinnerFriends.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFriend = friendsList.get(position);
                fetchAndUpdateBalance();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFriend = null;
                updateBalanceSummary(0.0);
            }
        });
    }

    private void redirectToLogin() {
        if (getContext() != null) {
            Toast.makeText(getContext(), "User not authenticated. Please log in.", Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void loadAllGroupsAndFriends() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    groupIds.clear();
                    uniqueFriendIds.clear();

                    for (QueryDocumentSnapshot groupDoc : query) {
                        String groupId = groupDoc.getId();
                        groupIds.add(groupId);
                        expenseManagers.put(groupId, new ExpenseManager(groupId));

                        List<String> memberIds = (List<String>) groupDoc.get("memberIds");
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                if (!memberId.equals(currentUserId) && !uniqueFriendIds.contains(memberId)) {
                                    uniqueFriendIds.add(memberId);
                                    fetchFriendDetails(memberId);
                                }
                            }
                        }
                    }

                    if (uniqueFriendIds.isEmpty()) {
                        showErrorAndNavigateBack("No friends or groups found");
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching groups: ", e);
                    showErrorAndNavigateBack("Error loading groups: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void fetchFriendDetails(String friendId) {
        db.collection("users").document(friendId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        Friend friend = new Friend();
                        friend.setUid(userDoc.getId());
                        friend.setEmail(userDoc.getString("email"));
                        friend.setFname(userDoc.getString("fname"));
                        friend.setLname(userDoc.getString("lname"));
                        friend.setProfile_picture(userDoc.getString("profile_picture"));
                        friendsList.add(friend);
                    }
                    if (friendsList.size() == uniqueFriendIds.size()) {
                        setupFriendsSpinner();
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching friend details for " + friendId + ": ", e);
                    if (friendsList.size() == uniqueFriendIds.size()) {
                        setupFriendsSpinner();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void fetchAndUpdateBalance() {
        if (selectedFriend == null || getView() == null || getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }

        final double[] totalBalance = {0.0};
        for (String groupId : groupIds) {
            ExpenseManager expenseManager = expenseManagers.get(groupId);
            if (expenseManager != null) {
                expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), userDataMap -> {
                    ExpenseManager.UserExpenseData currentUserData = userDataMap.get(currentUserId);
                    ExpenseManager.UserExpenseData friendData = userDataMap.get(selectedFriend.getUid());

                    if (currentUserData != null && friendData != null) {
                        double currentNetBalance = currentUserData.netBalance;
                        double friendNetBalance = friendData.netBalance;
                        totalBalance[0] = currentNetBalance - friendNetBalance; // Update total balance
                        balancesMap.put(selectedFriend.getUid(), totalBalance[0]);
                        updateBalanceSummary(totalBalance[0]);
                    }
                });
            }
        }
    }

    private void updateBalanceSummary(double balance) {
        if (getView() == null) return;

        String balanceText;
        if (balance > 0) {
            balanceText = String.format("₹%.2f (You are owed)", balance);
            tvBalanceSummary.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            etAmount.setEnabled(false);
            etAmount.setText("");
            btnPay.setEnabled(false);
            btnPayQr.setEnabled(false);
        } else if (balance < 0) {
            double amountOwed = Math.abs(balance);
            balanceText = String.format("₹%.2f (You owe)", amountOwed);
            tvBalanceSummary.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            etAmount.setEnabled(true);
            etAmount.setText(String.format("%.2f", amountOwed));
            btnPay.setEnabled(true);
            btnPayQr.setEnabled(true);
        } else {
            balanceText = "₹0.00 (Settled)";
            tvBalanceSummary.setTextColor(getResources().getColor(android.R.color.black));
            etAmount.setEnabled(false);
            etAmount.setText("");
            btnPay.setEnabled(false);
            btnPayQr.setEnabled(true);
        }
        tvBalanceSummary.setText(balanceText);
    }

    private void setupFriendsSpinner() {
        List<String> friendNames = new ArrayList<>();
        for (Friend friend : friendsList) {
            friendNames.add(friend.getFname() + " " + friend.getLname());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                friendNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFriends.setAdapter(adapter);
    }

    private void initiatePayment() {
        if (!validatePaymentInput()) return;

        double amount = Double.parseDouble(etAmount.getText().toString());
        int amountInPaise = (int) (amount * 100);

        progressBar.setVisibility(View.VISIBLE);
        btnPay.setEnabled(false);
        btnPayQr.setEnabled(false);

        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_TEST_KEY);

        try {
            JSONObject options = new JSONObject();
            options.put("name", "SplitAura Payment (Test Mode)");
            options.put("description", "Settling balance with " + selectedFriend.getFname());
            options.put("currency", "INR");
            options.put("amount", amountInPaise);
            options.put("prefill.email", auth.getCurrentUser().getEmail());
            options.put("prefill.contact", "9999999999");
            options.put("theme.color", "#009688");

            checkout.open(requireActivity(), options);
        } catch (Exception e) {
            Log.e(TAG, "Error in starting Razorpay Checkout", e);
            Toast.makeText(getContext(), "Error initiating payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnPay.setEnabled(true);
            btnPayQr.setEnabled(true);
        }
    }

    private void initiateQrPayment() {
        if (!validatePaymentInput()) return;

        double amount = Double.parseDouble(etAmount.getText().toString());
        int amountInPaise = (int) (amount * 100);

        progressBar.setVisibility(View.VISIBLE);
        btnPay.setEnabled(false);
        btnPayQr.setEnabled(false);

        try {
            JSONObject qrRequest = new JSONObject();
            qrRequest.put("type", "upi_qr");
            qrRequest.put("name", "SplitAura_" + selectedFriend.getFname());
            qrRequest.put("usage", "single_use");
            qrRequest.put("fixed_amount", true);
            qrRequest.put("payment_amount", amountInPaise);
            qrRequest.put("description", "Payment to " + selectedFriend.getFname());
            qrRequest.put("close_by", (System.currentTimeMillis() / 1000) + 3600);

            JSONObject qrResponse = new JSONObject();
            qrResponse.put("id", "qr_test_id");
            qrResponse.put("image_url", "https://rzp.io/i/test_qr");

            showQrCodeDialog(qrResponse.getString("image_url"), amount);
        } catch (Exception e) {
            Log.e(TAG, "Error creating QR code", e);
            Toast.makeText(getContext(), "Error creating QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnPay.setEnabled(true);
            btnPayQr.setEnabled(true);
        }
    }

    private boolean validatePaymentInput() {
        if (selectedFriend == null) {
            Toast.makeText(getContext(), "Please select a friend", Toast.LENGTH_SHORT).show();
            return false;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            return false;
        }

        Double balance = balancesMap.getOrDefault(selectedFriend.getUid(), 0.0);
        if (balance >= 0) {
            Toast.makeText(getContext(), "No payment needed. You are owed money or settled.", Toast.LENGTH_SHORT).show();
            return false;
        }

        double amountOwed = Math.abs(balance);
        if (amount <= 0 || amount > amountOwed) {
            Toast.makeText(getContext(), "Amount must be between 0 and ₹" + String.format("%.2f", amountOwed), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showQrCodeDialog(String imageUrl, double amount) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);
        builder.setView(dialogView);

        ImageView ivQrCode = dialogView.findViewById(R.id.iv_qr_code);
        TextView tvAmount = dialogView.findViewById(R.id.tv_amount);

        tvAmount.setText(String.format("Amount: ₹%.2f", amount));

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(ivQrCode);

        builder.setTitle("Scan to Pay (Test Mode)")
                .setPositiveButton("Done", (dialog, which) -> {
                    dialog.dismiss();
                    progressBar.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    btnPayQr.setEnabled(true);
                    Toast.makeText(getContext(), "Use 'success@razorpay' to simulate payment", Toast.LENGTH_LONG).show();
                })
                .setOnCancelListener(dialog -> {
                    progressBar.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    btnPayQr.setEnabled(true);
                });

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupTransactionListener() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated. Cannot set up transaction listener.");
            showToastSafely("User not authenticated. Please log in again.", Toast.LENGTH_LONG);
            redirectToLogin();
            return;
        }

        Query query = db.collection("users")
                .document(currentUserId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);

        // Store the listener to remove it later
        listenerRegistration = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Error listening to transactions: ", e);
                if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                        ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    showToastSafely("Permission denied to access transactions. Check Firestore rules.", Toast.LENGTH_LONG);
                } else {
                    showToastSafely("Error loading transactions: " + e.getMessage(), Toast.LENGTH_SHORT);
                }
                return;
            }

            if (snapshots != null) {
                transactionList.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    String friendId = doc.getString("friendId");
                    Double amount = doc.getDouble("amount");
                    Long timestamp = doc.getLong("timestamp");
                    String status = doc.getString("status");

                    if (friendId == null || amount == null || timestamp == null || status == null) {
                        continue;
                    }

                    String friendName = "Unknown";
                    for (Friend friend : friendsList) {
                        if (friend.getUid().equals(friendId)) {
                            friendName = friend.getFname() + " " + friend.getLname();
                            break;
                        }
                    }

                    String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            .format(new Date(timestamp));
                    transactionList.add(new Transaction(friendName, amount, date, status));
                }

                if (transactionList.isEmpty()) {
                    showToastSafely("No transactions found yet.", Toast.LENGTH_SHORT);
                    transactionAdapter.updateTransactions(transactionList);
                    return;
                }

                transactionAdapter.updateTransactions(transactionList);
            } else {
                showToastSafely("No transactions found yet.", Toast.LENGTH_SHORT);
                transactionList.clear();
                transactionAdapter.updateTransactions(transactionList);
            }
        });
    }

    // Add this helper method to safely show Toast
    private void showToastSafely(String message, int duration) {
        if (getContext() != null && !isDetached() && !isRemoving()) {
            Toast.makeText(getContext(), message, duration).show();
        } else {
            Log.w(TAG, "Unable to show Toast: Context is null or fragment is detached. Message: " + message);
        }
    }

    // Add a field to store the listener
    private ListenerRegistration listenerRegistration;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        for (ExpenseManager manager : expenseManagers.values()) {
            manager.removeListener();
        }
    }

    @Override
    public void onPaymentSuccess(String paymentId) {
        progressBar.setVisibility(View.GONE);
        btnPay.setEnabled(true);
        btnPayQr.setEnabled(true);

        Toast.makeText(getContext(), "Test Payment Successful! ID: " + paymentId, Toast.LENGTH_LONG).show();
        double paidAmount = Double.parseDouble(etAmount.getText().toString());
        logTransaction(paidAmount, "SUCCESS");
        createSettlementExpense(paidAmount);
    }

    @Override
    public void onPaymentError(int code, String response) {
        progressBar.setVisibility(View.GONE);
        btnPay.setEnabled(true);
        btnPayQr.setEnabled(true);

        Toast.makeText(getContext(), "Test Payment Failed: " + response, Toast.LENGTH_LONG).show();
        double paidAmount = Double.parseDouble(etAmount.getText().toString());
        logTransaction(paidAmount, "FAILED");
    }

    private void createSettlementExpense(double paidAmount) {
        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("title", "Settlement with " + selectedFriend.getFname());
        expenseData.put("amount", String.valueOf(paidAmount));
        expenseData.put("paidBy", currentUserId);
        expenseData.put("splitBy", "equal");
        expenseData.put("notes", "Test payment settlement via Razorpay");
        expenseData.put("date", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
        Map<String, Double> participants = new HashMap<>();
        participants.put(selectedFriend.getUid(), paidAmount);
        participants.put(currentUserId, 0.0);
        expenseData.put("participants", participants);

        for (String groupId : groupIds) {
            ExpenseManager expenseManager = expenseManagers.get(groupId);
            if (expenseManager != null) {
                expenseManager.addExpense(expenseData,
                        () -> Log.d(TAG, "Settlement expense added successfully"),
                        () -> Log.e(TAG, "Failed to add settlement expense"));
            }
        }
    }

    private void logTransaction(double amount, String status) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("payerId", currentUserId);
        transaction.put("friendId", selectedFriend.getUid());
        transaction.put("amount", amount);
        transaction.put("timestamp", System.currentTimeMillis());
        transaction.put("status", status);

        db.collection("users")
                .document(currentUserId)
                .collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction logged successfully with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error logging transaction: ", e);
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                            ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Toast.makeText(getContext(), "Permission denied to log transaction. Check Firestore rules.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error logging transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generateTransactionPdf() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        long startTimestamp = calendar.getTimeInMillis();
        long endTimestamp = System.currentTimeMillis();

        progressBar.setVisibility(View.VISIBLE);
        btnGeneratePdf.setEnabled(false);

        db.collection("users")
                .document(currentUserId)
                .collection("transactions")
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)
                .get()
                .addOnSuccessListener(query -> {
                    List<Transaction> pdfTransactions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        String friendId = doc.getString("friendId");
                        Double amount = doc.getDouble("amount");
                        Long timestamp = doc.getLong("timestamp");
                        String status = doc.getString("status");

                        if (friendId == null || amount == null || timestamp == null || status == null) {
                            continue;
                        }

                        String friendName = "Unknown";
                        for (Friend friend : friendsList) {
                            if (friend.getUid().equals(friendId)) {
                                friendName = friend.getFname() + " " + friend.getLname();
                                break;
                            }
                        }

                        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                .format(new Date(timestamp));
                        pdfTransactions.add(new Transaction(friendName, amount, date, status));
                    }

                    if (pdfTransactions.isEmpty()) {
                        Toast.makeText(getContext(), "No transactions found for the selected period.", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        btnGeneratePdf.setEnabled(true);
                        return;
                    }

                    try {
                        String fileName = "SplitAura_Transactions_" + System.currentTimeMillis() + ".pdf";
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);

                        document.add(new Paragraph("SplitAura Transaction Report")
                                .setBold()
                                .setFontSize(18));

                        String dateRange = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(new Date(startTimestamp)) + " to " +
                                new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        .format(new Date(endTimestamp));
                        document.add(new Paragraph("Period: " + dateRange)
                                .setFontSize(12));

                        Table table = new Table(new float[]{2, 2, 2, 1});
                        table.addHeaderCell("Friend");
                        table.addHeaderCell("Amount");
                        table.addHeaderCell("Date");
                        table.addHeaderCell("Status");

                        for (Transaction transaction : pdfTransactions) {
                            table.addCell(transaction.getFriendName());
                            table.addCell(String.format("₹%.2f", transaction.getAmount()));
                            table.addCell(transaction.getDate());
                            table.addCell(transaction.getStatus());
                        }

                        document.add(table);
                        document.close();

                        Toast.makeText(getContext(), "PDF generated: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error generating PDF: ", e);
                        Toast.makeText(getContext(), "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        progressBar.setVisibility(View.GONE);
                        btnGeneratePdf.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching transactions for PDF: ", e);
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                            ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Toast.makeText(getContext(), "Permission denied to access transactions. Check Firestore rules.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error fetching transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                    btnGeneratePdf.setEnabled(true);
                });
    }

    private void showErrorAndNavigateBack(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateTransactionPdf();
            } else {
                Toast.makeText(getContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (ExpenseManager manager : expenseManagers.values()) {
            manager.removeListener();
        }
    }
}