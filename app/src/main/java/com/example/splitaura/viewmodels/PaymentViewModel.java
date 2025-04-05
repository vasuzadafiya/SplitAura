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
    private final List<Notification> senderNotifications = new ArrayList<>();
    private final List<Notification> receiverNotifications = new ArrayList<>();
    private ListenerRegistration senderListener;
    private ListenerRegistration receiverListener;
    private boolean isListeningToNotifications = false;

    public PaymentViewModel() {
        Log.d(TAG, "ViewModel initialized");
        loadExpenseManagers(); // Load expense managers on initialization
    }

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
        calculateBalance(); // Recalculate balance when friend changes
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

        Log.d(TAG, "Listening to notifications for user: " + currentUserId);
        isListeningToNotifications = true;

        // Remove existing listeners if they exist
        if (senderListener != null) {
            senderListener.remove();
        }
        if (receiverListener != null) {
            receiverListener.remove();
        }

        // Listen for notifications where the user is the sender
        senderListener = db.collection("notifications")
                .whereIn("actionStatus", List.of("PENDING", "CONFIRMED"))
                .whereEqualTo("senderId", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to sender notifications", e);
                        errorMessage.setValue("Notification error: " + e.getMessage());
                        isLoading.setValue(false);
                        return;
                    }
                    handleNotificationSnapshot(snapshots, "sender");
                });

        // Listen for notifications where the user is the receiver
        receiverListener = db.collection("notifications")
                .whereIn("actionStatus", List.of("PENDING", "CONFIRMED"))
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to receiver notifications", e);
                        errorMessage.setValue("Notification error: " + e.getMessage());
                        isLoading.setValue(false);
                        return;
                    }
                    handleNotificationSnapshot(snapshots, "receiver");
                });
    }

    private void handleNotificationSnapshot(QuerySnapshot snapshots, String type) {
        List<Notification> notifications = new ArrayList<>();

        if (snapshots != null && !snapshots.isEmpty()) {
            for (QueryDocumentSnapshot doc : snapshots) {
                Notification notification = doc.toObject(Notification.class);
                notifications.add(notification);
                Log.d(TAG, "Notification added (" + type + "): " + notification.getActionId());
            }
        } else {
            Log.d(TAG, "No notifications found for " + type);
        }

        // Filter out duplicate notifications based on sender, receiver, and action type
        List<Notification> filteredNotifications = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();
        for (Notification notification : notifications) {
            String key = notification.getSenderId() + "_" + notification.getReceiverId() + "_" +
                    notification.getActionType();
            if (uniqueKeys.add(key)) {
                filteredNotifications.add(notification);
            } else {
                Log.d(TAG, "Filtered out duplicate notification: " + notification.getActionId());
            }
        }

        // Update the appropriate list based on type
        if (type.equals("sender")) {
            senderNotifications.clear();
            senderNotifications.addAll(filteredNotifications);
        } else if (type.equals("receiver")) {
            receiverNotifications.clear();
            receiverNotifications.addAll(filteredNotifications);
        }

        // Merge sender and receiver notifications and update LiveData
        updatePendingNotifications();
        isLoading.setValue(false);
        Log.d(TAG, "Notifications updated (" + type + "), count: " + filteredNotifications.size());
    }

    private void updatePendingNotifications() {
        List<Notification> allNotifications = new ArrayList<>();
        allNotifications.addAll(senderNotifications);
        allNotifications.addAll(receiverNotifications);
        pendingNotifications.setValue(allNotifications);
        Log.d(TAG, "Merged notifications, total count: " + allNotifications.size());
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
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Transaction> txList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Transaction tx = doc.toObject(Transaction.class);
                        txList.add(tx);
                        Log.d(TAG, "Transaction loaded: " + tx.getPaymentId());
                    }
                    transactions.setValue(txList);
                    isLoading.setValue(false);
                    Log.d(TAG, "Transactions loaded, count: " + txList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions", e);
                    errorMessage.setValue("Error loading transactions: " + e.getMessage());
                    isLoading.setValue(false);
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
                    loadTransactions(); // Refresh transactions after logging
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

    private void calculateBalance() {
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

                        // Observe changes in user expense data to recalculate balance
                        manager.getUserExpenseData().observeForever(userExpenseData -> {
                            calculateBalance();
                            Log.d(TAG, "UserExpenseData updated for group: " + groupId);
                        });
                    }
                    calculateBalance(); // Initial balance calculation after loading
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading groups for ExpenseManager", e);
                    errorMessage.setValue("Error loading expense data: " + e.getMessage());
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        for (ExpenseManager manager : expenseManagers) {
            manager.removeListener();
        }
        if (senderListener != null) {
            senderListener.remove();
        }
        if (receiverListener != null) {
            receiverListener.remove();
        }
        senderNotifications.clear();
        receiverNotifications.clear();
        isListeningToNotifications = false;
        Log.d(TAG, "ViewModel cleared, listeners removed, notification lists cleared");
    }
}