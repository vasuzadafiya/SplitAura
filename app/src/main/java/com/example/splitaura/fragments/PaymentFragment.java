package com.example.splitaura.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.splitaura.ExpenseManager;
import com.example.splitaura.LoginActivity;
import com.example.splitaura.R;
import com.example.splitaura.adapters.TransactionPaymentAdapter;
import com.example.splitaura.dialogues.LoadingDialog;
import com.example.splitaura.models.DebtSummary;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Transaction;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class PaymentFragment extends Fragment {
    private static final String TAG = "PaymentFragment";
    private static final String DATE_FORMAT = "dd MMM yyyy, HH:mm";
    private static final String PDF_DATE_FORMAT = "dd MMM yyyy";
    private static final int MAX_TRANSACTIONS = 50;
    private static final int UPI_PAYMENT_REQUEST_CODE = 101;
    private static final String UPI_SCHEME = "upi://pay";
    private static final String CACHE_PREFS = "PaymentCache";
    private static final String FRIENDS_LIST_KEY = "friends_list";
    private static final String GROUPS_LIST_KEY = "groups_list";
    private static final String TRANSACTIONS_LIST_KEY = "transactions_list";
    private static final String CACHE_TIMESTAMP = "cache_timestamp";
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    private TextView tvBalanceSummary, tvBalanceStatus, transHistoryTitle;
    private AutoCompleteTextView spinnerFriends;
    private TextInputEditText etAmount;
    private MaterialButton btnPay, btnRequest, btnPayQr, btnGeneratePdf;
    private RecyclerView rvTransactions;
    private SwipeRefreshLayout swipeRefresh;
    private NestedScrollView nestedScrollView;
    private TransactionPaymentAdapter transactionAdapter;
    private ViewTreeObserver.OnScrollChangedListener scrollListener;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private List<Friend> friendsList;
    private List<Transaction> transactionList;
    private List<DebtSummary> debtSummaries;
    private Friend selectedFriend;
    private List<String> groupIds;
    private Set<String> uniqueFriendIds;
    private Map<String, ExpenseManager> expenseManagers;
    private ListenerRegistration transactionListener;
    private boolean hasRealTransactions = false;
    private LoadingDialog loadingDialog;
    private boolean isDataLoading = false;
    private SharedPreferences cachePrefs;
    private final Gson gson = new Gson();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadingDialog = new LoadingDialog(requireContext());
        cachePrefs = requireContext().getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            redirectToLogin();
        }

        initializeLists();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        if (transactionListener != null) {
            transactionListener.remove();
        }
        for (ExpenseManager manager : expenseManagers.values()) {
            manager.removeListener();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);
        initializeViews(view);
        setupRecyclerView();
        setupScrollListener();
        setupListeners();
        loadCachedData();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spinnerFriends.setOnItemClickListener((parent, v, position, id) -> {
            selectedFriend = friendsList.get(position);
            fetchAndUpdateBalance();
        });
    }

    private void initializeLists() {
        friendsList = new ArrayList<>();
        transactionList = new ArrayList<>();
        debtSummaries = new ArrayList<>();
        groupIds = new ArrayList<>();
        uniqueFriendIds = new HashSet<>();
        expenseManagers = new HashMap<>();
    }

    private void initializeViews(View view) {
        tvBalanceSummary = view.findViewById(R.id.tv_balance_summary);
        tvBalanceStatus = view.findViewById(R.id.tv_balance_status);
        transHistoryTitle = view.findViewById(R.id.trans_hist);
        spinnerFriends = view.findViewById(R.id.spinner_friends);
        etAmount = view.findViewById(R.id.et_amount);
        btnPay = view.findViewById(R.id.btn_pay);
        btnRequest = view.findViewById(R.id.btn_request);
        btnPayQr = view.findViewById(R.id.btn_pay_qr);
        btnGeneratePdf = view.findViewById(R.id.btn_generate_pdf);
        rvTransactions = view.findViewById(R.id.rv_transactions);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        nestedScrollView = view.findViewById(R.id.nested_scroll_view);
    }

    private void setupRecyclerView() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionPaymentAdapter(getContext());
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setHasFixedSize(true);
    }

    private void setupScrollListener() {
        scrollListener = () -> {
            if (!isAdded() || getView() == null || nestedScrollView == null) return;

            int[] titleLocation = new int[2];
            transHistoryTitle.getLocationOnScreen(titleLocation);
            int titleTop = titleLocation[1];
            int scrollY = nestedScrollView.getScrollY();
            int titleOffsetInScrollView = transHistoryTitle.getTop() - scrollY;

            if (titleOffsetInScrollView <= 0) {
                nestedScrollView.scrollTo(0, transHistoryTitle.getTop());
            }
        };
        nestedScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
    }

    private void loadCachedData() {
        long lastCacheTime = cachePrefs.getLong(CACHE_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCacheTime < CACHE_DURATION_MS) {
            String friendsListJson = cachePrefs.getString(FRIENDS_LIST_KEY, null);
            String groupsListJson = cachePrefs.getString(GROUPS_LIST_KEY, null);
            String transactionsListJson = cachePrefs.getString(TRANSACTIONS_LIST_KEY, null);

            if (friendsListJson != null && groupsListJson != null && transactionsListJson != null) {
                Type friendListType = new TypeToken<List<Friend>>() {}.getType();
                friendsList = gson.fromJson(friendsListJson, friendListType);

                Type groupListType = new TypeToken<List<String>>() {}.getType();
                groupIds = gson.fromJson(groupsListJson, groupListType);

                Type transactionListType = new TypeToken<List<Transaction>>() {}.getType();
                transactionList = gson.fromJson(transactionsListJson, transactionListType);

                setupFriendsSpinner();
                transactionAdapter.updateTransactions(transactionList);
                calculateDebtSummaries();
                fetchAndUpdateBalance();
                return;
            }
        }

        showLoadingDialog();
        loadAllGroupsAndFriends();
    }

    private void saveDataToCache() {
        executorService.execute(() -> {
            String friendsListJson = gson.toJson(friendsList);
            String groupsListJson = gson.toJson(groupIds);
            String transactionsListJson = gson.toJson(transactionList);

            cachePrefs.edit()
                    .putString(FRIENDS_LIST_KEY, friendsListJson)
                    .putString(GROUPS_LIST_KEY, groupsListJson)
                    .putString(TRANSACTIONS_LIST_KEY, transactionsListJson)
                    .putLong(CACHE_TIMESTAMP, System.currentTimeMillis())
                    .apply();
        });
    }

    private void showLoadingDialog() {
        if (!isDataLoading && loadingDialog != null && !loadingDialog.isShowing()) {
            isDataLoading = true;
            loadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (isDataLoading && loadingDialog != null && loadingDialog.isShowing()) {
            isDataLoading = false;
            loadingDialog.hide();
        }
    }

    private void loadAllGroupsAndFriends() {
        showLoadingDialog();

        db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) {
                        hideLoadingDialog();
                        return;
                    }

                    if (task.isSuccessful()) {
                        groupIds.clear();
                        uniqueFriendIds.clear();
                        friendsList.clear();

                        for (QueryDocumentSnapshot groupDoc : task.getResult()) {
                            String groupId = groupDoc.getId();
                            groupIds.add(groupId);
                            expenseManagers.put(groupId, new ExpenseManager(groupId));
                            List<String> memberIds = (List<String>) groupDoc.get("memberIds");
                            if (memberIds != null) {
                                for (String memberId : memberIds) {
                                    if (!memberId.equals(currentUserId)) {
                                        uniqueFriendIds.add(memberId);
                                    }
                                }
                            }
                        }

                        if (uniqueFriendIds.isEmpty()) {
                            setupFriendsSpinner();
                            hideLoadingDialog();
                        } else {
                            fetchAllFriendDetails(new ArrayList<>(uniqueFriendIds));
                        }
                    } else {
                        handleError("Error fetching groups: " + task.getException().getMessage(), task.getException());
                        hideLoadingDialog();
                    }
                });
    }

    private void fetchAllFriendDetails(List<String> friendIds) {
        AtomicInteger completedCount = new AtomicInteger(0);

        for (String friendId : friendIds) {
            db.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isAdded()) return;

                        if (documentSnapshot.exists()) {
                            Friend friend = documentSnapshot.toObject(Friend.class);
                            if (friend != null) {
                                friendsList.add(friend);
                            }
                        }

                        if (completedCount.incrementAndGet() == friendIds.size()) {
                            setupFriendsSpinner();
                            calculateDebtSummaries();
                            setupTransactionListener();
                            saveDataToCache();
                            hideLoadingDialog();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (completedCount.incrementAndGet() == friendIds.size()) {
                            setupFriendsSpinner();
                            calculateDebtSummaries();
                            setupTransactionListener();
                            saveDataToCache();
                            hideLoadingDialog();
                        }
                    });
        }
    }

    private void setupTransactionListener() {
        if (auth.getCurrentUser() == null) return;

        Query query = db.collection("users")
                .document(currentUserId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_TRANSACTIONS);

        transactionListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                handleError("Error listening to transactions: " + e.getMessage(), e);
                return;
            }

            if (snapshots != null && !snapshots.isEmpty()) {
                transactionList.clear();
                hasRealTransactions = true;

                for (QueryDocumentSnapshot doc : snapshots) {
                    Transaction transaction = parseTransaction(doc);
                    if (transaction != null) {
                        transactionList.add(transaction);
                    }
                }

                transactionAdapter.updateTransactions(transactionList);
                saveDataToCache();
                Log.d(TAG, "Transactions updated: " + transactionList.size() + " items");
            } else if (!hasRealTransactions) {
                addDummyTransactions();
            }

            swipeRefresh.setRefreshing(false);
        });
    }

    private void addDummyTransactions() {
        String[] statuses = {"SUCCESS", "FAILED", "REQUESTED"};
        String[] methods = {"UPI", "Manual", "Wallet"};

        transactionList.clear();
        for (int i = 0; i < Math.min(20, MAX_TRANSACTIONS); i++) {
            String friendName = friendsList.isEmpty() ? "Friend " + (i + 1) :
                    friendsList.get(i % friendsList.size()).getFullName();
            double amount = (Math.random() * 1000) + 100;
            long timestamp = System.currentTimeMillis() - (i * 24 * 60 * 60 * 1000);
            String date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(timestamp);
            transactionList.add(new Transaction(friendName, amount, date, statuses[i % 3], "DUMMY_" + i, methods[i % 3]));
        }
        transactionAdapter.updateTransactions(transactionList);
        Log.d(TAG, "Dummy transactions added: " + transactionList.size());
    }

    private void setupListeners() {
        btnPay.setOnClickListener(v -> initiatePayment());
        btnRequest.setOnClickListener(v -> requestPayment());
        btnPayQr.setOnClickListener(v -> generateQrCode());
        btnGeneratePdf.setOnClickListener(v -> generateTransactionPdf());
        swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    private void redirectToLogin() {
        showToast("User not authenticated. Please log in.");
        startActivity(new Intent(getActivity(), LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        if (getActivity() != null) getActivity().finish();
    }

    private void fetchAndUpdateBalance() {
        if (selectedFriend == null || getView() == null) {
            updateBalanceSummary(0.0);
            return;
        }

        double totalOwed = 0.0;
        for (String groupId : groupIds) {
            ExpenseManager manager = expenseManagers.get(groupId);
            if (manager != null) {
                Map<String, ExpenseManager.UserExpenseData> data = manager.getUserExpenseData().getValue();
                if (data != null) {
                    ExpenseManager.UserExpenseData currentUserData = data.get(currentUserId);
                    ExpenseManager.UserExpenseData friendData = data.get(selectedFriend.getUid());
                    if (currentUserData != null && friendData != null) {
                        double friendOwesUser = friendData.debts.getOrDefault(currentUserId, 0.0);
                        double userOwesFriend = currentUserData.debts.getOrDefault(selectedFriend.getUid(), 0.0);
                        totalOwed += friendOwesUser - userOwesFriend;
                    }
                }
            }
        }
        updateBalanceSummary(totalOwed);
    }

    private void updateBalanceSummary(double balance) {
        if (getView() == null) return;

        String amountText = String.format("₹%.2f", Math.abs(balance));
        tvBalanceSummary.setText(amountText);
        if (balance > 0) {
            tvBalanceStatus.setText(selectedFriend.getFname() + " owes you");
            tvBalanceStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            etAmount.setText(String.format("%.2f", balance));
            btnPay.setEnabled(false);
            btnRequest.setEnabled(true);
            btnPayQr.setEnabled(true);
        } else if (balance < 0) {
            tvBalanceStatus.setText("You owe " + selectedFriend.getFname());
            tvBalanceStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            etAmount.setText(String.format("%.2f", Math.abs(balance)));
            btnPay.setEnabled(true);
            btnRequest.setEnabled(false);
            btnPayQr.setEnabled(true);
        } else {
            tvBalanceStatus.setText("Settled up");
            tvBalanceStatus.setTextColor(getResources().getColor(android.R.color.black));
            etAmount.setText("");
            btnPay.setEnabled(false);
            btnRequest.setEnabled(false);
            btnPayQr.setEnabled(false);
        }
    }

    private void calculateDebtSummaries() {
        debtSummaries.clear();
        for (Friend friend : friendsList) {
            double totalOwed = 0.0;
            for (String groupId : groupIds) {
                ExpenseManager manager = expenseManagers.get(groupId);
                if (manager != null) {
                    Map<String, ExpenseManager.UserExpenseData> data = manager.getUserExpenseData().getValue();
                    if (data != null) {
                        ExpenseManager.UserExpenseData currentUserData = data.get(currentUserId);
                        ExpenseManager.UserExpenseData friendData = data.get(friend.getUid());
                        if (currentUserData != null && friendData != null) {
                            double friendOwesUser = friendData.debts.getOrDefault(currentUserId, 0.0);
                            double userOwesFriend = currentUserData.debts.getOrDefault(friend.getUid(), 0.0);
                            totalOwed += friendOwesUser - userOwesFriend;
                        }
                    }
                }
            }
            debtSummaries.add(new DebtSummary(friend.getFullName(), friend.getUid(), totalOwed));
        }
    }

    private void initiatePayment() {
        if (!validateInput()) return;

        showLoadingDialog();
        double amount = Double.parseDouble(etAmount.getText().toString());

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) {
                        hideLoadingDialog();
                        return;
                    }

                    String userUpi = doc.getString("upi");
                    if (!validateUPI(userUpi)) {
                        showToast("Please add a valid UPI ID in your profile.");
                        hideLoadingDialog();
                        return;
                    }

                    String friendUpi = selectedFriend.getUpi();
                    if (!validateUPI(friendUpi)) {
                        showToast(selectedFriend.getFname() + " hasn't added a valid UPI ID.");
                        hideLoadingDialog();
                        return;
                    }

                    btnPay.setEnabled(false);
                    String upiUrl = buildUpiLink(amount, friendUpi, selectedFriend.getFullName(),
                            "SPLIT_" + System.currentTimeMillis());
                    Intent upiIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(upiUrl));
                    startActivityForResult(upiIntent, UPI_PAYMENT_REQUEST_CODE);
                    Log.d(TAG, "UPI Payment initiated: " + upiUrl);
                    hideLoadingDialog();
                })
                .addOnFailureListener(e -> {
                    handleError("Error fetching user profile: " + e.getMessage(), e);
                    hideLoadingDialog();
                });
    }

    private void requestPayment() {
        if (!validateInput()) return;

        double amount = Double.parseDouble(etAmount.getText().toString());
        String transactionId = "REQ_" + System.currentTimeMillis();
        logTransaction(amount, "REQUESTED", "Manual", transactionId);
        showToast("Payment request sent to " + selectedFriend.getFname());
        Log.d(TAG, "Payment requested: " + amount + " from " + selectedFriend.getFullName() + ", ID: " + transactionId);
    }

    private void generateQrCode() {
        if (!validateInput()) return;

        showLoadingDialog();
        double amount = Double.parseDouble(etAmount.getText().toString());
        double balance = debtSummaries.stream()
                .filter(ds -> ds.getFriendId().equals(selectedFriend.getUid()))
                .mapToDouble(DebtSummary::getAmount)
                .sum();

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) {
                        hideLoadingDialog();
                        return;
                    }
                    String userUpi = doc.getString("upi");
                    if (!validateUPI(userUpi)) {
                        showToast("Please add a valid UPI ID in your profile.");
                        hideLoadingDialog();
                        return;
                    }

                    String upiUrl = balance > 0 ?
                            buildUpiLink(amount, userUpi, auth.getCurrentUser().getDisplayName(), "SPLIT_" + System.currentTimeMillis()) :
                            buildUpiLink(amount, selectedFriend.getUpi(), selectedFriend.getFullName(), "SPLIT_" + System.currentTimeMillis());
                    hideLoadingDialog();
                    showQrDialog(upiUrl, amount);
                })
                .addOnFailureListener(e -> {
                    handleError("Error fetching user UPI: " + e.getMessage(), e);
                    hideLoadingDialog();
                });
    }

    private void showQrDialog(String upiUrl, double amount) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(upiUrl, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qrBitmap = encoder.createBitmap(bitMatrix);

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Scan QR to Pay");

            LinearLayout dialogLayout = new LinearLayout(requireContext());
            dialogLayout.setOrientation(LinearLayout.VERTICAL);
            dialogLayout.setGravity(Gravity.CENTER);
            int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
            dialogLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            dialogLayout.setBackgroundColor(Color.WHITE);

            ImageView qrImageView = new ImageView(requireContext());
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    (int) (300 * getResources().getDisplayMetrics().density),
                    (int) (300 * getResources().getDisplayMetrics().density));
            imageParams.setMargins(0, 0, 0, paddingPx);
            qrImageView.setLayoutParams(imageParams);
            qrImageView.setImageBitmap(qrBitmap);
            dialogLayout.addView(qrImageView);

            TextView instructionText = new TextView(requireContext());
            instructionText.setText("Scan this QR code with a UPI app to pay ₹" + String.format("%.2f", amount));
            instructionText.setTextSize(14);
            instructionText.setTextColor(Color.BLACK);
            instructionText.setGravity(Gravity.CENTER);
            instructionText.setPadding(0, 0, 0, paddingPx);
            dialogLayout.addView(instructionText);

            LinearLayout buttonLayout = new LinearLayout(requireContext());
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.CENTER);

            Button okButton = new Button(requireContext());
            okButton.setText("OK");
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.setMargins(0, 0, paddingPx, 0);
            okButton.setLayoutParams(buttonParams);
            okButton.setBackgroundResource(R.drawable.btn_bg_transparent);
            okButton.setTextColor(Color.BLACK);

            Button backButton = new Button(requireContext());
            backButton.setText("Back");
            backButton.setLayoutParams(buttonParams);
            backButton.setBackgroundResource(R.drawable.btn_bg_transparent);
            backButton.setTextColor(Color.BLACK);

            buttonLayout.addView(okButton);
            buttonLayout.addView(backButton);

            dialogLayout.addView(buttonLayout);
            builder.setView(dialogLayout);
            builder.setCancelable(false);

            AlertDialog dialog = builder.create();
            okButton.setOnClickListener(v -> {
                logTransaction(amount, "PENDING", "QR Code", "QR_" + System.currentTimeMillis());
                dialog.dismiss();
            });
            backButton.setOnClickListener(v -> dialog.dismiss());
            dialog.show();

            showToast("Scan the QR code to pay.");
            Log.d(TAG, "QR Code displayed: " + upiUrl);
        } catch (WriterException e) {
            handleError("Error generating QR code: " + e.getMessage(), e);
        }
    }

    private void generateTransactionPdf() {
        if (!isAdded() || transactionList.isEmpty()) {
            showToast("No transactions available to generate PDF.");
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Generate PDF")
                .setMessage("Do you want to generate a transaction report PDF?")
                .setPositiveButton("Confirm", (dialog, which) -> createAndOpenPdf())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void createAndOpenPdf() {
        PdfDocument document = new PdfDocument();
        final int pageWidth = 595;
        final int pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        int marginPx = 30;
        int lineHeight = 20;
        int headerSize = 22;
        int textSize = 16;
        int yPosition = marginPx;

        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.applogo);
        if (logo != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 120, 120, true);
            int logoX = (pageWidth - scaledLogo.getWidth()) / 2;
            canvas.drawBitmap(scaledLogo, logoX, yPosition, null);
            yPosition += scaledLogo.getHeight() + 10;
        }

        paint.setTextSize(headerSize);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String headerText = "SplitAura Transaction Report";
        float headerWidth = paint.measureText(headerText);
        canvas.drawText(headerText, (pageWidth - headerWidth) / 2, yPosition, paint);
        yPosition += lineHeight + 10;

        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        long endTimestamp = System.currentTimeMillis();
        long startTimestamp = endTimestamp - 30L * 24 * 60 * 60 * 1000;
        String dateRange = "Period: " + new SimpleDateFormat(PDF_DATE_FORMAT, Locale.getDefault()).format(startTimestamp)
                + " to " + new SimpleDateFormat(PDF_DATE_FORMAT, Locale.getDefault()).format(endTimestamp);
        float dateWidth = paint.measureText(dateRange);
        canvas.drawText(dateRange, (pageWidth - dateWidth) / 2, yPosition, paint);
        yPosition += lineHeight + 10;

        int tableLeft = marginPx;
        int tableRight = pageWidth - marginPx;
        int tableWidth = tableRight - tableLeft;
        int[] columnWidths = {140, 80, 100, 70, 90, 70};
        int totalColumnWidth = 0;
        for (int width : columnWidths) totalColumnWidth += width;
        if (totalColumnWidth > tableWidth) {
            float scale = (float) tableWidth / totalColumnWidth;
            for (int i = 0; i < columnWidths.length; i++) columnWidths[i] = (int) (columnWidths[i] * scale);
        }

        int[] columnX = new int[columnWidths.length];
        columnX[0] = tableLeft;
        for (int i = 1; i < columnWidths.length; i++) columnX[i] = columnX[i - 1] + columnWidths[i - 1];

        paint.setColor(Color.LTGRAY);
        canvas.drawRect(tableLeft, yPosition - lineHeight + 5, tableRight, yPosition + 5, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String[] headers = {"Friend", "Amount", "Date", "Status", "Txn ID", "UPI App"};
        for (int i = 0; i < headers.length; i++) {
            if (i == 0) canvas.drawText(headers[i], columnX[i] + 5, yPosition, paint);
            else {
                float textWidth = paint.measureText(headers[i]);
                canvas.drawText(headers[i], columnX[i] + columnWidths[i] - textWidth - 5, yPosition, paint);
            }
        }
        yPosition += lineHeight;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        boolean alternate = false;
        for (Transaction transaction : transactionList) {
            if (yPosition > pageHeight - marginPx - lineHeight) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                yPosition = marginPx;
                paint.setColor(Color.LTGRAY);
                canvas.drawRect(tableLeft, yPosition - lineHeight + 5, tableRight, yPosition + 5, paint);
                paint.setColor(Color.BLACK);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                for (int i = 0; i < headers.length; i++) {
                    if (i == 0) canvas.drawText(headers[i], columnX[i] + 5, yPosition, paint);
                    else {
                        float textWidth = paint.measureText(headers[i]);
                        canvas.drawText(headers[i], columnX[i] + columnWidths[i] - textWidth - 5, yPosition, paint);
                    }
                }
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                yPosition += lineHeight;
            }

            if (alternate) {
                paint.setColor(Color.parseColor("#F5F5F5"));
                canvas.drawRect(tableLeft, yPosition - lineHeight + 5, tableRight, yPosition + 5, paint);
            }
            paint.setColor(Color.BLACK);

            String friendName = truncateText(transaction.getFriendName(), paint, columnWidths[0] - 10);
            String amount = String.format("₹%.2f", transaction.getAmount());
            String date = truncateText(transaction.getDate(), paint, columnWidths[2] - 10);
            String status = truncateText(transaction.getStatus(), paint, columnWidths[3] - 10);
            String txnId = truncateText(transaction.getTransactionId(), paint, columnWidths[4] - 10);
            String upiApp = truncateText(transaction.getUpiApp(), paint, columnWidths[5] - 10);

            canvas.drawText(friendName, columnX[0] + 5, yPosition, paint);
            float amountWidth = paint.measureText(amount);
            canvas.drawText(amount, columnX[1] + columnWidths[1] - amountWidth - 5, yPosition, paint);
            dateWidth = paint.measureText(date);
            canvas.drawText(date, columnX[2] + columnWidths[2] - dateWidth - 5, yPosition, paint);
            float statusWidth = paint.measureText(status);
            canvas.drawText(status, columnX[3] + columnWidths[3] - statusWidth - 5, yPosition, paint);
            float txnIdWidth = paint.measureText(txnId);
            canvas.drawText(txnId, columnX[4] + columnWidths[4] - txnIdWidth - 5, yPosition, paint);
            float upiAppWidth = paint.measureText(upiApp);
            canvas.drawText(upiApp, columnX[5] + columnWidths[5] - upiAppWidth - 5, yPosition, paint);

            paint.setColor(Color.GRAY);
            canvas.drawLine(tableLeft, yPosition + 2, tableRight, yPosition + 2, paint);
            paint.setColor(Color.BLACK);

            yPosition += lineHeight;
            alternate = !alternate;
        }

        document.finishPage(page);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SplitAura_Transactions_" + System.currentTimeMillis() + ".pdf");

        try {
            File dir = file.getParentFile();
            if (!dir.exists()) dir.mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                document.writeTo(fos);
            }
            document.close();

            if (file.exists() && file.length() > 0) {
                showToast("PDF saved to Downloads: " + file.getName());
                Uri contentUri = FileProvider.getUriForFile(requireContext(), "com.example.splitaura.fileprovider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
                Intent chooser = Intent.createChooser(intent, "Open PDF with");
                if (chooser.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(chooser);
                } else {
                    showToast("No PDF viewer found. File saved to Downloads.");
                }
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(contentUri);
                requireContext().sendBroadcast(mediaScanIntent);
                Log.d(TAG, "PDF generated and opened: " + file.getAbsolutePath());
            } else {
                showToast("Failed to generate PDF.");
            }
        } catch (IOException e) {
            handleError("Error saving PDF: " + e.getMessage(), e);
        }
    }

    private String truncateText(String text, Paint paint, int maxWidth) {
        if (paint.measureText(text) <= maxWidth) return text;
        StringBuilder truncated = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (paint.measureText(truncated.toString() + c) <= maxWidth - paint.measureText("...")) {
                truncated.append(c);
            } else {
                truncated.append("...");
                break;
            }
        }
        return truncated.toString();
    }

    private boolean validateInput() {
        if (selectedFriend == null) {
            showToast("Please select a friend");
            return false;
        }
        String amountStr = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            showToast("Please enter an amount");
            return false;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showToast("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Invalid amount");
            return false;
        }
        return true;
    }

    private boolean validateUPI(String upiId) {
        return upiId != null && Pattern.matches("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$", upiId);
    }

    private String buildUpiLink(double amount, String upiId, String name, String transactionRefId) {
        return String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s&tr=%s",
                Uri.encode(upiId), Uri.encode(name), amount, Uri.encode("Payment via SplitAura"), Uri.encode(transactionRefId));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPI_PAYMENT_REQUEST_CODE && isAdded()) {
            btnPay.setEnabled(true);
            if (data != null && "SUCCESS".equals(data.getStringExtra("Status"))) {
                double amount = Double.parseDouble(etAmount.getText().toString());
                String txnId = data.getStringExtra("txnId") != null ? data.getStringExtra("txnId") : "UPI_" + System.currentTimeMillis();
                logTransaction(amount, "SUCCESS", "UPI", txnId);
                for (ExpenseManager manager : expenseManagers.values()) {
                    manager.settleDebt(currentUserId, selectedFriend.getUid(), amount);
                }
                fetchAndUpdateBalance();
                showToast("Payment successful!");
                Log.d(TAG, "UPI Payment successful: " + txnId);
            } else {
                showToast("Payment failed or cancelled.");
                Log.w(TAG, "UPI Payment failed or cancelled.");
            }
        }
    }

    private Transaction parseTransaction(QueryDocumentSnapshot doc) {
        String friendId = doc.getString("friendId");
        Double amount = doc.getDouble("amount");
        Long timestamp = doc.getLong("timestamp");
        String status = doc.getString("status");
        String transactionId = doc.getString("transactionId");
        String method = doc.getString("upiApp");

        if (friendId == null || amount == null || timestamp == null || status == null) {
            Log.w(TAG, "Invalid transaction data: " + doc.getId());
            return null;
        }

        String friendName = friendsList.stream()
                .filter(f -> f.getUid().equals(friendId))
                .map(Friend::getFullName)
                .findFirst().orElse("Unknown");
        String date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(timestamp);
        return new Transaction(friendName, amount, date, status, transactionId, method);
    }

    private void logTransaction(double amount, String status, String method, String transactionId) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("payerId", currentUserId);
        transaction.put("friendId", selectedFriend != null ? selectedFriend.getUid() : "N/A");
        transaction.put("amount", amount);
        transaction.put("timestamp", System.currentTimeMillis());
        transaction.put("status", status);
        transaction.put("upiApp", method);
        transaction.put("transactionId", transactionId);

        db.collection("users")
                .document(currentUserId)
                .collection("transactions")
                .add(transaction)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Transaction logged: " + doc.getId());
                    fetchAndUpdateBalance();
                })
                .addOnFailureListener(e -> handleError("Error logging transaction: " + e.getMessage(), e));
    }

    private void refreshData() {
        showLoadingDialog();
        loadAllGroupsAndFriends();
        calculateDebtSummaries();
        fetchAndUpdateBalance();
        swipeRefresh.setRefreshing(false);
    }

    private void handleError(String message, Exception e) {
        Log.e(TAG, message, e);
        hideLoadingDialog();
        if (isAdded()) showToast(message);
    }

    private void showToast(String message) {
        if (isAdded()) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setupFriendsSpinner() {
        if (!isAdded()) return;

        List<String> friendNames = new ArrayList<>();
        for (Friend friend : friendsList) {
            String fullName = friend.getFullName().trim();
            if (!fullName.isEmpty()) friendNames.add(fullName);
        }
        if (friendNames.isEmpty()) {
            friendNames.add("No friends available");
            spinnerFriends.setEnabled(false);
        } else {
            spinnerFriends.setEnabled(true);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, friendNames);
        spinnerFriends.setAdapter(adapter);
        if (!friendNames.contains("No friends available") && !friendsList.isEmpty()) {
            spinnerFriends.setText(friendNames.get(0), false);
            selectedFriend = friendsList.get(0);
            fetchAndUpdateBalance();
        }
    }
}