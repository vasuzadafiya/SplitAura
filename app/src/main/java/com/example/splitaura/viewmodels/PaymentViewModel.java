package com.example.splitaura.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.splitaura.ExpenseManager;
import com.example.splitaura.models.Friend;
import com.example.splitaura.models.Notification;
import com.example.splitaura.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PaymentViewModel extends ViewModel {
    private static final String TAG = "PaymentViewModel";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Friend> selectedFriend = new MutableLiveData<>();
    private final MutableLiveData<Double> balance = new MutableLiveData<>(0.0);
    private final MutableLiveData<List<Notification>> pendingNotifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Friend>> friends = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final List<ExpenseManager> expenseManagers = new ArrayList<>();
    private ListenerRegistration receiverListener;
    private boolean isListeningToNotifications = false;

    public PaymentViewModel() {
        Log.d(TAG, "ViewModel initialized");
        loadExpenseManagers();
    }

    // Getters
    public LiveData<Friend> getSelectedFriend() { return selectedFriend; }
    public LiveData<Double> getBalance() { return balance; }
    public LiveData<List<Notification>> getPendingNotifications() { return pendingNotifications; }
    public LiveData<List<Transaction>> getTransactions() { return transactions; }
    public LiveData<List<Friend>> getFriends() { return friends; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public List<ExpenseManager> getExpenseManagers() { return expenseManagers; }

    public void setSelectedFriend(Friend friend) {
        selectedFriend.setValue(friend);
        Log.d(TAG, "Selected friend set: " + (friend != null ? friend.getFullName() : "null"));
        calculateBalance();
    }

    public String getCurrentUserId() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) Log.w(TAG, "Current user ID is null");
        return uid;
    }

    public void loadInitialData() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            isLoading.setValue(false);
            Log.e(TAG, "Cannot load initial data: User not authenticated");
            return;
        }
        isLoading.setValue(true);
        Log.d(TAG, "Starting to load initial data for user: " + currentUserId);
        listenToNotifications();
        loadTransactions();
        loadFriends();
        calculateBalance();
    }

    public void refreshData() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            Log.e(TAG, "Cannot refresh data: User not authenticated");
            return;
        }
        Log.d(TAG, "Refreshing data for user: " + currentUserId);
        listenToNotifications();
        loadTransactions();
        loadFriends();
        calculateBalance();
        for (ExpenseManager manager : expenseManagers) {
            manager.recalculateExpenses();
        }
    }

    private void listenToNotifications() {
        if (isListeningToNotifications) {
            Log.d(TAG, "Already listening to notifications, skipping");
            return;
        }

        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            isLoading.setValue(false);
            return;
        }

        Log.d(TAG, "Listening to receiver notifications for user: " + currentUserId);
        isListeningToNotifications = true;

        if (receiverListener != null) receiverListener.remove();

        receiverListener = db.collection("notifications")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("actionStatus", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to receiver notifications", e);
                        errorMessage.setValue("Notification error: " + e.getMessage());
                        isLoading.setValue(false);
                        return;
                    }
                    handleNotificationSnapshot(snapshots);
                });
    }

    private void handleNotificationSnapshot(QuerySnapshot snapshots) {
        List<Notification> notifications = new ArrayList<>();

        if (snapshots != null && !snapshots.isEmpty()) {
            for (QueryDocumentSnapshot doc : snapshots) {
                Notification notification = doc.toObject(Notification.class);
                notifications.add(notification);
                Log.d(TAG, "Receiver notification added: " + notification.getActionId());
            }
        } else {
            Log.d(TAG, "No pending receiver notifications found");
        }

        List<Notification> filteredNotifications = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();
        for (Notification notification : notifications) {
            String key = notification.getSenderId() + "_" + notification.getReceiverId() + "_" + notification.getActionType();
            if (uniqueKeys.add(key)) {
                filteredNotifications.add(notification);
            } else {
                Log.d(TAG, "Filtered out duplicate notification: " + notification.getActionId());
            }
        }

        pendingNotifications.setValue(filteredNotifications);
        isLoading.setValue(false);
        Log.d(TAG, "Pending receiver notifications updated, count: " + filteredNotifications.size());
    }

    private void loadTransactions() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            isLoading.setValue(false);
            return;
        }

        Log.d(TAG, "Loading transactions for user: " + currentUserId);
        db.collection("users").document(currentUserId).collection("transactions")
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error loading transactions", e);
                        errorMessage.setValue("Error loading transactions: " + e.getMessage());
                        isLoading.setValue(false);
                        return;
                    }
                    List<Transaction> txList = new ArrayList<>();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Transaction tx = doc.toObject(Transaction.class);
                            txList.add(tx);
                            Log.d(TAG, "Transaction loaded: " + tx.getPaymentId());
                        }
                    }
                    transactions.setValue(txList);
                    isLoading.setValue(false);
                    Log.d(TAG, "Transactions loaded, count: " + txList.size());
                });
    }

    public void logTransaction(Transaction tx) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "Cannot log transaction, user not authenticated");
            return;
        }

        db.collection("users").document(currentUserId).collection("transactions")
                .document(tx.getPaymentId())
                .set(tx)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction logged: " + tx.getPaymentId());
                    loadTransactions();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to log transaction: " + tx.getPaymentId(), e));
    }

    public void settleDebt(String debtorId, String creditorId, double amount) {
        for (ExpenseManager manager : expenseManagers) {
            manager.settleDebt(debtorId, creditorId, amount);
            Log.d(TAG, "Debt settled in ExpenseManager: " + debtorId + " -> " + creditorId + ": " + amount);
        }
        calculateBalance();
    }

    public void calculateBalance() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "Cannot calculate balance, user not authenticated");
            return;
        }

        double totalBalance = 0.0;
        for (ExpenseManager manager : expenseManagers) {
            Map<String, ExpenseManager.UserExpenseData> data = manager.getUserExpenseData().getValue();
            if (data != null && data.containsKey(currentUserId)) {
                totalBalance += data.get(currentUserId).netBalance;
                Log.d(TAG, "Balance contribution from group " + manager.getGroupId() + ": " + data.get(currentUserId).netBalance);
            }
        }
        balance.setValue(totalBalance);
        Log.d(TAG, "Total balance calculated: " + totalBalance);
    }

    public void loadFriends() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            errorMessage.setValue("User not authenticated");
            Log.w(TAG, "Cannot load friends, user not authenticated");
            return;
        }

        Log.d(TAG, "Loading friends for user: " + currentUserId);
        db.collection("users").document(currentUserId).collection("friends")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Friend> friendList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Friend friend = doc.toObject(Friend.class);
                        friendList.add(friend);
                        Log.d(TAG, "Friend loaded: " + friend.getFullName() + " (UID: " + friend.getUid() + ")");
                    }
                    friends.setValue(friendList);
                    Log.d(TAG, "Friends loaded, count: " + friendList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading friends", e);
                    errorMessage.setValue("Error loading friends: " + e.getMessage());
                });
    }

    private void loadExpenseManagers() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "Cannot load expense managers, user not authenticated");
            return;
        }

        Log.d(TAG, "Loading expense managers for user: " + currentUserId);
        db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    expenseManagers.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String groupId = doc.getId();
                        ExpenseManager manager = new ExpenseManager(groupId);
                        expenseManagers.add(manager);
                        Log.d(TAG, "ExpenseManager loaded for group: " + groupId);

                        manager.getUserExpenseData().observeForever(userExpenseData -> {
                            calculateBalance();
                            Log.d(TAG, "UserExpenseData updated for group: " + groupId);
                        });
                    }
                    calculateBalance();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading expense managers", e));
    }
}