package com.example.splitaura;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.splitaura.models.OweRelationship;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExpenseManager {
    private static final String TAG = "ExpenseManager";
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final String groupId;
    private final MutableLiveData<Map<String, UserExpenseData>> userExpenseData = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<List<Expense>> expenses = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> groupAmount = new MutableLiveData<>(0.0);
    // New LiveData for the simplified owe relationships.
    private final MutableLiveData<List<OweRelationship>> minimalOweTransactions = new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration expenseListener;
    private ListenerRegistration groupListener;
    private List<String> memberIds = new ArrayList<>();
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ExpenseManager(String groupId) {
        this.groupId = groupId;
        fetchGroupMembersAndSetupListeners();
    }

    public static class Expense {
        private String expenseId;
        private String title;
        private String amount;
        private String paidBy;
        private String splitBy;
        private String notes;
        private String date;
        private String imageUrl;
        private String description;
        private String addedBy;
        private List<String> categories;
        private Map<String, Double> participants;

        public Expense() {}

        // Getters and Setters
        public String getExpenseId() { return expenseId; }
        public void setExpenseId(String expenseId) { this.expenseId = expenseId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getPaidBy() { return paidBy; }
        public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
        public String getSplitBy() { return splitBy; }
        public void setSplitBy(String splitBy) { this.splitBy = splitBy; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAddedBy() { return addedBy != null ? addedBy : "unknown"; }
        public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public Map<String, Double> getParticipants() { return participants; }
        public void setParticipants(Map<String, Double> participants) { this.participants = participants; }

        public static Expense fromMap(Map<String, Object> map) {
            Expense expense = new Expense();
            try {
                expense.setTitle((String) map.get("title"));
                expense.setAmount((String) map.get("amount"));
                expense.setPaidBy((String) map.get("paidBy"));
                expense.setSplitBy((String) map.get("splitBy"));
                expense.setNotes((String) map.get("notes"));
                expense.setDate((String) map.get("date"));
                expense.setImageUrl((String) map.get("imageUrl"));
                expense.setDescription((String) map.get("description"));

                if (map.containsKey("addedBy")) {
                    expense.setAddedBy((String) map.get("addedBy"));
                }

                if (map.containsKey("categories")) {
                    expense.setCategories((List<String>) map.get("categories"));
                }

                if (map.containsKey("participants")) {
                    expense.setParticipants((Map<String, Double>) map.get("participants"));
                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Error casting expense fields", e);
            }
            return expense;
        }
    }

    public static class UserExpenseData {
        public double totalSpent;
        public double totalShare;
        public double netBalance;
        public Map<String, Double> debts;
        public Map<String, Double> credits;

        public UserExpenseData(double totalSpent, double totalShare, double netBalance,
                               Map<String, Double> debts, Map<String, Double> credits) {
            this.totalSpent = totalSpent;
            this.totalShare = totalShare;
            this.netBalance = netBalance;
            this.debts = debts != null ? new HashMap<>(debts) : new HashMap<>();
            this.credits = credits != null ? new HashMap<>(credits) : new HashMap<>();
        }
    }

    public LiveData<Map<String, UserExpenseData>> getUserExpenseData() {
        return userExpenseData;
    }

    public LiveData<List<Expense>> getExpenses() {
        return expenses;
    }

    public LiveData<Double> getGroupAmount() {
        return groupAmount;
    }

    // New LiveData accessor for minimal owe transactions.
    public LiveData<List<OweRelationship>> getMinimalOweTransactions() {
        return minimalOweTransactions;
    }

    public void addExpense(Map<String, Object> expenseData, Runnable onSuccess, Runnable onFailure) {
        if (!isCurrentUserGroupMember()) {
            Log.w(TAG, "User not authorized to add expense");
            onFailure.run();
            return;
        }

        if (!validateExpenseData(expenseData)) {
            Log.e(TAG, "Invalid expense data");
            onFailure.run();
            return;
        }

        if (!expenseData.containsKey("addedBy")) {
            expenseData.put("addedBy", getCurrentUserUid());
        }

        firestore.collection("groups").document(groupId).collection("expenses")
                .add(expenseData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Expense added: " + docRef.getId());
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add expense", e);
                    onFailure.run();
                });
    }

    public void deleteExpense(String expenseId, Runnable onSuccess, Runnable onFailure) {
        if (!isCurrentUserGroupMember()) {
            Log.w(TAG, "User not authorized to delete expense");
            onFailure.run();
            return;
        }

        firestore.collection("groups").document(groupId).collection("expenses")
                .document(expenseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Expense deleted: " + expenseId);
                    recalculateExpenses();
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete expense: " + expenseId, e);
                    onFailure.run();
                });
    }

    public void deleteGroup(Runnable onSuccess, Runnable onFailure) {
        firestore.collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String creatorId = document.getString("createdBy");
                        if (creatorId != null && creatorId.equals(getCurrentUserUid())) {
                            executeGroupDeletion(onSuccess, onFailure);
                        } else {
                            Log.w(TAG, "User not authorized to delete group");
                            onFailure.run();
                        }
                    } else {
                        Log.w(TAG, "Group already deleted");
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error verifying group creator", e);
                    onFailure.run();
                });
    }

    private void executeGroupDeletion(Runnable onSuccess, Runnable onFailure) {
        firestore.collection("groups").document(groupId).collection("expenses")
                .get()
                .addOnSuccessListener(expensesSnapshot -> {
                    WriteBatch batch = firestore.batch();

                    for (QueryDocumentSnapshot expenseDoc : expensesSnapshot) {
                        batch.delete(expenseDoc.getReference());
                    }

                    DocumentReference groupRef = firestore.collection("groups").document(groupId);
                    batch.delete(groupRef);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Group and all expenses deleted successfully");
                                cleanupAfterDeletion();
                                onSuccess.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete group", e);
                                onFailure.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get expenses for deletion", e);
                    onFailure.run();
                });
    }

    private void cleanupAfterDeletion() {
        handler.post(() -> {
            userExpenseData.setValue(new HashMap<>());
            expenses.setValue(new ArrayList<>());
            groupAmount.setValue(0.0);
            minimalOweTransactions.setValue(new ArrayList<>());
        });
        removeListener();
    }

    public void settleDebt(String debtorId, String creditorId, double amount) {
        Map<String, UserExpenseData> currentData = userExpenseData.getValue();
        if (currentData == null || !currentData.containsKey(debtorId) || !currentData.containsKey(creditorId)) {
            Log.w(TAG, "Invalid user data for settlement");
            return;
        }

        UserExpenseData debtorData = currentData.get(debtorId);
        UserExpenseData creditorData = currentData.get(creditorId);
        double currentDebt = debtorData.debts.getOrDefault(creditorId, 0.0);

        if (amount <= 0 || currentDebt < amount) {
            Log.w(TAG, "Invalid settlement amount: " + amount + " vs debt: " + currentDebt);
            return;
        }

        debtorData.debts.put(creditorId, currentDebt - amount);
        creditorData.credits.put(debtorId, creditorData.credits.getOrDefault(debtorId, 0.0) - amount);

        // Update net balances
        debtorData.netBalance = debtorData.totalSpent - debtorData.totalShare;
        creditorData.netBalance = creditorData.totalSpent - creditorData.totalShare;

        userExpenseData.setValue(currentData);
    }

    public void removeListener() {
        if (expenseListener != null) {
            expenseListener.remove();
            expenseListener = null;
        }
        if (groupListener != null) {
            groupListener.remove();
            groupListener = null;
        }
    }

    private void fetchGroupMembersAndSetupListeners() {
        groupListener = firestore.collection("groups").document(groupId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching group data", e);
                        memberIds = new ArrayList<>();
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds == null) memberIds = new ArrayList<>();
                        setupExpenseListener();
                    } else {
                        memberIds = new ArrayList<>();
                        handler.post(() -> {
                            userExpenseData.setValue(new HashMap<>());
                            expenses.setValue(new ArrayList<>());
                            groupAmount.setValue(0.0);
                            minimalOweTransactions.setValue(new ArrayList<>());
                        });
                    }
                });
    }

    private void setupExpenseListener() {
        if (expenseListener != null) expenseListener.remove();

        expenseListener = firestore.collection("groups").document(groupId).collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching expenses", e);
                        handler.post(() -> {
                            userExpenseData.setValue(new HashMap<>());
                            expenses.setValue(new ArrayList<>());
                            groupAmount.setValue(0.0);
                            minimalOweTransactions.setValue(new ArrayList<>());
                        });
                        return;
                    }

                    if (snapshots != null) {
                        calculateExpenseData(snapshots);
                    }
                });
    }

    private void calculateExpenseData(QuerySnapshot snapshots) {
        executor.execute(() -> {
            Map<String, UserExpenseData> data = new HashMap<>();
            List<Expense> expenseList = new ArrayList<>();
            double groupAmountValue = 0.0;

            // Initialize for all members
            for (String userId : memberIds) {
                data.put(userId, new UserExpenseData(0.0, 0.0, 0.0, new HashMap<>(), new HashMap<>()));
            }

            // Process each expense
            for (QueryDocumentSnapshot doc : snapshots) {
                try {
                    Expense expense = Expense.fromMap(doc.getData());
                    expense.setExpenseId(doc.getId());
                    expenseList.add(expense);

                    String paidBy = expense.getPaidBy();
                    double amount = parseAmount(expense.getAmount());
                    Map<String, Double> participants = expense.getParticipants();

                    if (paidBy == null || participants == null) continue;

                    groupAmountValue += amount;
                    UserExpenseData payerData = data.get(paidBy);
                    if (payerData != null) {
                        payerData.totalSpent += amount;
                    }

                    // Calculate total shares.
                    double totalShares = participants.values().stream()
                            .mapToDouble(share -> share != null ? share : 0.0)
                            .sum();

                    if (totalShares <= 0) {
                        // Equal split if no shares specified.
                        double equalShare = amount / participants.size();
                        participants.replaceAll((k, v) -> equalShare);
                        totalShares = amount;
                    }

                    // Distribute shares.
                    for (Map.Entry<String, Double> entry : participants.entrySet()) {
                        String userId = entry.getKey();
                        double share = entry.getValue() != null ?
                                (entry.getValue() / totalShares) * amount : 0.0;

                        UserExpenseData userData = data.get(userId);
                        if (userData != null) {
                            userData.totalShare += share;

                            if (!userId.equals(paidBy)) {
                                userData.debts.merge(paidBy, share, Double::sum);
                                if (payerData != null) {
                                    payerData.credits.merge(userId, share, Double::sum);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing expense", e);
                }
            }

            // Calculate net balances.
            data.forEach((userId, userData) -> {
                userData.netBalance = userData.totalSpent - userData.totalShare;
            });

            // Prepare a map of net balances.
            Map<String, Double> netBalances = new HashMap<>();
            for (Map.Entry<String, UserExpenseData> entry : data.entrySet()) {
                netBalances.put(entry.getKey(), entry.getValue().netBalance);
            }

            // Compute minimal owe transactions.
            List<OweRelationship> minimalTransactions = DebtSettlementCalculator.calculateMinimalOwe(netBalances);

            double finalGroupAmountValue = groupAmountValue;
            handler.post(() -> {
                userExpenseData.setValue(data);
                expenses.setValue(expenseList);
                groupAmount.setValue(finalGroupAmountValue);
                minimalOweTransactions.setValue(minimalTransactions);
                updateGroupAmountInFirestore(finalGroupAmountValue);
            });
        });
    }

    private void recalculateExpenses() {
        firestore.collection("groups").document(groupId).collection("expenses")
                .get()
                .addOnSuccessListener(this::calculateExpenseData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to recalculate expenses", e);
                    handler.post(() -> {
                        userExpenseData.setValue(new HashMap<>());
                        expenses.setValue(new ArrayList<>());
                        groupAmount.setValue(0.0);
                        minimalOweTransactions.setValue(new ArrayList<>());
                    });
                });
    }

    private void updateGroupAmountInFirestore(double groupAmountValue) {
        if (!isCurrentUserGroupMember()) {
            Log.w(TAG, "User not authorized to update group amount");
            return;
        }

        firestore.collection("groups").document(groupId)
                .update("amount", groupAmountValue)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update group amount", e));
    }

    private boolean isCurrentUserGroupMember() {
        String currentUser = getCurrentUserUid();
        return memberIds.contains(currentUser);
    }

    private String getFriendName(String userId) {
        final String[] name = {"Unknown"};
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String fname = document.getString("fname");
                        String lname = document.getString("lname");
                        name[0] = (fname != null ? fname : "") + " " + (lname != null ? lname : "");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch friend name", e));
        return name[0]; // Synchronous fallback; ideally, use an asynchronous callback.
    }

    private boolean validateExpenseData(Map<String, Object> expenseData) {
        return expenseData.containsKey("paidBy") &&
                expenseData.containsKey("amount") &&
                expenseData.containsKey("participants") &&
                expenseData.get("amount") instanceof String;
    }

    private double parseAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid amount format: " + amountStr);
            return 0.0;
        }
    }

    private String getCurrentUserUid() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : "unknown_user";
    }
}