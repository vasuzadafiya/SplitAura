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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.LoginActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.TransactionPaymentAdapter;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Transaction;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
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

public class PaymentFragment extends Fragment {
    private static final String TAG = "PaymentFragment";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String RAZORPAY_TEST_KEY = "rzp_test_1DP5mmOlF5G5ag";

    private TextView tvBalanceSummary;
    private Spinner spinnerFriends;
    private EditText etAmount;
    private MaterialButton btnPay, btnPayQr, btnGeneratePdf;
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

    private PaymentResultCallback paymentResultCallback;
    private boolean hasLoadedDummyData = false;
    private Task<?> fetchGroupsTask;
    private Task<?> fetchFriendsTask;
    private ListenerRegistration listenerRegistration;

    public PaymentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Checkout.preload(getContext());
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof PaymentResultCallback) {
            paymentResultCallback = (PaymentResultCallback) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PaymentResultCallback");
        }
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

        tvBalanceSummary = view.findViewById(R.id.tv_balance_summary);
        spinnerFriends = view.findViewById(R.id.spinner_friends);
        etAmount = view.findViewById(R.id.et_amount);
        btnPay = view.findViewById(R.id.btn_pay);
        btnPayQr = view.findViewById(R.id.btn_pay_qr);
        btnGeneratePdf = view.findViewById(R.id.btn_generate_pdf);
        rvTransactions = view.findViewById(R.id.rv_transactions);

        friendsList = new ArrayList<>();
        balancesMap = new HashMap<>();
        transactionList = new ArrayList<>();
        groupIds = new ArrayList<>();
        uniqueFriendIds = new HashSet<>();
        expenseManagers = new HashMap<>();

        // Initialize RecyclerView
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionPaymentAdapter(getContext());
        rvTransactions.setAdapter(transactionAdapter);

        loadAllGroupsAndFriends();
        addDummyTransactions();
        setupTransactionListener();

        btnPay.setOnClickListener(v -> initiatePayment());
        btnPayQr.setOnClickListener(v -> initiateQrPayment());
        btnGeneratePdf.setOnClickListener(v -> generateTransactionPdf());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        if (isAdded()) {
            Toast.makeText(getContext(), "User not authenticated. Please log in.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    private void loadAllGroupsAndFriends() {
        // Cancel any existing fetch task
        if (fetchGroupsTask != null && !fetchGroupsTask.isComplete()) {
            fetchGroupsTask.isCanceled();
            Log.d(TAG, "Canceled previous fetchGroupsTask");
        }

        try {
            fetchGroupsTask = db.collection("groups")
                    .whereArrayContains("memberIds", currentUserId)
                    .get()
                    .addOnSuccessListener(query -> {
                        try {
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
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing groups data: ", e);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error processing groups data", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching groups: ", e);
                        showErrorAndNavigateBack("Error loading groups: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error initiating loadAllGroupsAndFriends: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error fetching groups", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchFriendDetails(String friendId) {
        // Cancel any existing fetch task for this friend
        if (fetchFriendsTask != null && !fetchFriendsTask.isComplete()) {
            fetchFriendsTask.isCanceled();
            Log.d(TAG, "Canceled previous fetchFriendsTask");
        }

        try {
            fetchFriendsTask = db.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        try {
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
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing friend details for " + friendId + ": ", e);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error processing friend details", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching friend details for " + friendId + ": ", e);
                        if (friendsList.size() == uniqueFriendIds.size()) {
                            setupFriendsSpinner();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error initiating fetchFriendDetails for " + friendId + ": ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error fetching friend details", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchAndUpdateBalance() {
        if (selectedFriend == null || getView() == null || getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }

        final double[] totalBalance = {0.0};
        for (String groupId : groupIds) {
            ExpenseManager expenseManager = expenseManagers.get(groupId);
            if (expenseManager != null) {
                try {
                    expenseManager.getUserExpenseData().observe(getViewLifecycleOwner(), userDataMap -> {
                        try {
                            ExpenseManager.UserExpenseData currentUserData = userDataMap.get(currentUserId);
                            ExpenseManager.UserExpenseData friendData = userDataMap.get(selectedFriend.getUid());

                            if (currentUserData != null && friendData != null) {
                                double currentNetBalance = currentUserData.netBalance;
                                double friendNetBalance = friendData.netBalance;
                                totalBalance[0] = currentNetBalance - friendNetBalance;
                                balancesMap.put(selectedFriend.getUid(), totalBalance[0]);
                                updateBalanceSummary(totalBalance[0]);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user expense data: ", e);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error calculating balance", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error observing user expense data: ", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error calculating balance", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void updateBalanceSummary(double balance) {
        if (getView() == null) return;

        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error updating balance summary: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error updating balance", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupFriendsSpinner() {
        // Check if the Fragment is attached to a context
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached to a context in setupFriendsSpinner, skipping");
            return;
        }

        try {
            List<String> friendNames = new ArrayList<>();
            for (Friend friend : friendsList) {
                friendNames.add(friend.getFname() + " " + friend.getLname());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    friendNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerFriends.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up friends spinner: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error setting up friends list", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initiatePayment() {
        if (!validatePaymentInput()) return;

        try {
            double amount = Double.parseDouble(etAmount.getText().toString());
            int amountInPaise = (int) (amount * 100);

            btnPay.setEnabled(false);
            btnPayQr.setEnabled(false);

            Checkout checkout = new Checkout();
            checkout.setKeyID(RAZORPAY_TEST_KEY);

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
            Log.e(TAG, "initiatePayment: Error in starting Razorpay Checkout", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error initiating payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            btnPay.setEnabled(true);
            btnPayQr.setEnabled(true);
        }
    }

    private void initiateQrPayment() {
        if (!validatePaymentInput()) return;

        try {
            double amount = Double.parseDouble(etAmount.getText().toString());
            int amountInPaise = (int) (amount * 100);

            btnPay.setEnabled(false);
            btnPayQr.setEnabled(false);

            String dummyQrUrl = "https://via.placeholder.com/150?text=QR+Code+Test";
            showQrCodeDialog(dummyQrUrl, amount);
        } catch (Exception e) {
            Log.e(TAG, "initiateQrPayment: Error initiating QR payment", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error initiating QR payment", Toast.LENGTH_SHORT).show();
            }
            btnPay.setEnabled(true);
            btnPayQr.setEnabled(true);
        }
    }

    private boolean validatePaymentInput() {
        try {
            if (selectedFriend == null) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Please select a friend", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            String amountStr = etAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amountStr)) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            Double balance = balancesMap.getOrDefault(selectedFriend.getUid(), 0.0);
            if (balance >= 0) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "No payment needed. You are owed money or settled.", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            double amountOwed = Math.abs(balance);
            if (amount <= 0 || amount > amountOwed) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Amount must be between 0 and ₹" + String.format("%.2f", amountOwed), Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating payment input: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error validating payment input", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    private void showQrCodeDialog(String imageUrl, double amount) {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached in showQrCodeDialog, skipping");
            return;
        }

        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);

            if (dialogView == null) {
                dialogView = new LinearLayout(getContext());
                ((LinearLayout) dialogView).setOrientation(LinearLayout.VERTICAL);
                dialogView.setPadding(16, 16, 16, 16);

                TextView tvAmount = new TextView(getContext());
                tvAmount.setId(View.generateViewId());
                tvAmount.setText(String.format("Amount: ₹%.2f", amount));
                tvAmount.setTextSize(16);
                tvAmount.setPadding(0, 0, 0, 16);

                ImageView ivQrCode = new ImageView(getContext());
                ivQrCode.setId(View.generateViewId());
                ivQrCode.setLayoutParams(new LinearLayout.LayoutParams(200, 200));

                ((LinearLayout) dialogView).addView(tvAmount);
                ((LinearLayout) dialogView).addView(ivQrCode);
            }

            builder.setView(dialogView);

            ImageView ivQrCode = dialogView.findViewById(R.id.iv_qr_code);
            TextView tvAmount = dialogView.findViewById(R.id.tv_amount);

            if (tvAmount != null) {
                tvAmount.setText(String.format("Amount: ₹%.2f", amount));
            }

            if (ivQrCode != null) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(ivQrCode);
            } else {
                Log.e(TAG, "showQrCodeDialog: ImageView iv_qr_code not found in dialog layout");
            }

            builder.setTitle("Scan to Pay (Test Mode)")
                    .setPositiveButton("Done", (dialog, which) -> {
                        dialog.dismiss();
                        btnPay.setEnabled(true);
                        btnPayQr.setEnabled(true);
                        if (isAdded()) {
                            Toast.makeText(getContext(), "QR Payment Simulated Successfully", Toast.LENGTH_LONG).show();
                        }
                        if (paymentResultCallback != null) {
                            paymentResultCallback.onPaymentSuccess("test_payment_" + System.currentTimeMillis());
                        }
                    })
                    .setOnCancelListener(dialog -> {
                        btnPay.setEnabled(true);
                        btnPayQr.setEnabled(true);
                    });

            android.app.AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing QR code dialog: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error showing QR code", Toast.LENGTH_SHORT).show();
            }
            btnPay.setEnabled(true);
            btnPayQr.setEnabled(true);
        }
    }

    private void setupTransactionListener() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "setupTransactionListener: User not authenticated");
            if (isAdded()) {
                Toast.makeText(getContext(), "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
                redirectToLogin();
            }
            return;
        }

        try {
            Query query = db.collection("users")
                    .document(currentUserId)
                    .collection("transactions")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50);

            listenerRegistration = query.addSnapshotListener((snapshots, e) -> {
                try {
                    if (e != null) {
                        Log.e(TAG, "setupTransactionListener: Error listening to transactions", e);
                        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                                ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Permission denied to access transactions. Check Firestore rules.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error loading transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
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

                        transactionAdapter.updateTransactions(transactionList);
                    } else {
                        if (!hasLoadedDummyData) {
                            addDummyTransactions();
                        }
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error processing transaction snapshot: ", ex);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error processing transactions", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up transaction listener: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error loading transactions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addDummyTransactions() {
        try {
            transactionList.clear();

            String[] friendNames = {"John Doe", "Tushar Chauhan", "Vasu Zadafiya"};
            String[] statuses = {"SUCCESS", "FAILED", "PENDING"};
            for (int i = 0; i < 10; i++) {
                String friendName = friendNames[i % friendNames.length];
                double amount = (Math.random() * 1000) + 100;
                long timestamp = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000);
                String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(timestamp));
                String status = statuses[i % statuses.length];
                transactionList.add(new Transaction(friendName, amount, date, status));
            }

            transactionAdapter.updateTransactions(transactionList);
            hasLoadedDummyData = true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding dummy transactions: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error loading dummy transactions", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Cancel Firestore tasks
        if (fetchGroupsTask != null && !fetchGroupsTask.isComplete()) {
            fetchGroupsTask.isCanceled();
            Log.d(TAG, "Canceled fetchGroupsTask on detach");
        }
        if (fetchFriendsTask != null && !fetchFriendsTask.isComplete()) {
            fetchFriendsTask.isCanceled();
            Log.d(TAG, "Canceled fetchFriendsTask on detach");
        }
    }

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
    public void onDestroy() {
        super.onDestroy();
        for (ExpenseManager manager : expenseManagers.values()) {
            manager.removeListener();
        }
        friendsList.clear();
        balancesMap.clear();
        transactionList.clear();
        groupIds.clear();
        uniqueFriendIds.clear();
        expenseManagers.clear();
    }

    public void onPaymentSuccess(String paymentId) {
        if (isAdded()) {
            Toast.makeText(getContext(), "Payment Successful! ID: " + paymentId, Toast.LENGTH_LONG).show();
        }
        btnPay.setEnabled(true);
        btnPayQr.setEnabled(true);

        try {
            double paidAmount = Double.parseDouble(etAmount.getText().toString());
            logTransaction(paidAmount, "SUCCESS");
            createSettlementExpense(paidAmount);
        } catch (Exception e) {
            Log.e(TAG, "Error processing payment success: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error processing payment", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onPaymentError(int code, String response) {
        if (isAdded()) {
            Toast.makeText(getContext(), "Payment Failed: Code: " + code, Toast.LENGTH_LONG).show();
        }
        btnPay.setEnabled(true);
        btnPayQr.setEnabled(true);

        try {
            double paidAmount = Double.parseDouble(etAmount.getText().toString());
            logTransaction(paidAmount, "FAILED");
        } catch (Exception e) {
            Log.e(TAG, "Error processing payment error: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error processing payment failure", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createSettlementExpense(double paidAmount) {
        try {
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
                            () -> {},
                            () -> Log.e(TAG, "createSettlementExpense: Failed to add settlement expense"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating settlement expense: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error creating settlement", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void logTransaction(double amount, String status) {
        try {
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
                    .addOnSuccessListener(documentReference -> {})
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "logTransaction: Error logging transaction", e);
                        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                                ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Permission denied to log transaction. Check Firestore rules.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Error logging transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error initiating logTransaction: ", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error logging transaction", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generateTransactionPdf() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached in generateTransactionPdf, skipping");
            return;
        }

        try {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
                return;
            }

            if (transactionList.isEmpty()) {
                Toast.makeText(getContext(), "No transactions available to generate PDF.", Toast.LENGTH_LONG).show();
                return;
            }

            String fileName = "SplitAura_Transactions_" + System.currentTimeMillis() + ".pdf";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("SplitAura Transaction Report")
                    .setBold()
                    .setFontSize(18));

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            long startTimestamp = calendar.getTimeInMillis();
            long endTimestamp = System.currentTimeMillis();
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

            for (Transaction transaction : transactionList) {
                table.addCell(transaction.getFriendName());
                table.addCell(String.format("₹%.2f", transaction.getAmount()));
                table.addCell(transaction.getDate());
                table.addCell(transaction.getStatus());
            }

            document.add(table);
            document.close();

            Toast.makeText(getContext(), "PDF generated: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "generateTransactionPdf: Error generating PDF", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showErrorAndNavigateBack(String message) {
        if (isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateTransactionPdf();
            } else {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
