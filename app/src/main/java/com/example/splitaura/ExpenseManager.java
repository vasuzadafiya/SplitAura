package com.example.splitaura;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.splitaura.models.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseManager {
    private static final String TAG = "ExpenseManager";
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final String groupId;
    private final MutableLiveData<Map<String, UserExpenseData>> userExpenseData = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<List<Expense>> expenses = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> totalSpending = new MutableLiveData<>(0.0);
    private ListenerRegistration expenseListener;
    private ListenerRegistration groupListener;
    private List<String> memberIds = new ArrayList<>();

    public ExpenseManager(String groupId) {
        this.groupId = groupId;
        fetchGroupMembersAndSetupListeners();
    }

    public static class Expense {
        private String title, amount, paidBy, splitBy, notes, date, imageUrl, description;
        private List<String> categories;
        private Map<String, Double> participants;

        public Expense() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
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
        public Map<String, Double> getParticipants() { return participants; }
        public void setParticipants(Map<String, Double> participants) { this.participants = participants; }
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

    public void addExpense(Map<String, Object> expenseData, Runnable onSuccess, Runnable onFailure) {
        if (!validateExpenseData(expenseData)) {
            Log.e(TAG, "Invalid expense data");
            onFailure.run();
            return;
        }
        firestore.collection("groups").document(groupId).collection("expenses")
                .add(expenseData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Expense added: " + docRef.getId());
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add expense: ", e);
                    onFailure.run();
                });
    }

    public LiveData<Map<String, UserExpenseData>> getUserExpenseData() {
        return userExpenseData;
    }

    public LiveData<List<Expense>> getExpenses() {
        return expenses;
    }

    public LiveData<Double> getTotalSpending() {
        return totalSpending;
    }

    private boolean validateExpenseData(Map<String, Object> expenseData) {
        return expenseData.containsKey("paidBy") && expenseData.containsKey("amount") &&
                expenseData.containsKey("participants") && expenseData.get("amount") instanceof String;
    }

    private void fetchGroupMembersAndSetupListeners() {
        groupListener = firestore.collection("groups").document(groupId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching group data: ", e);
                        memberIds = new ArrayList<>();
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds == null) memberIds = new ArrayList<>();
                        Double total = doc.getDouble("totalSpending");
                        totalSpending.setValue(total != null ? total : 0.0);
                    } else {
                        memberIds = new ArrayList<>();
                        totalSpending.setValue(0.0);
                    }
                    setupExpenseListener();
                });
    }

    private void setupExpenseListener() {
        if (expenseListener != null) expenseListener.remove();
        expenseListener = firestore.collection("groups").document(groupId).collection("expenses")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e(TAG, "Error fetching expenses: ", e);
                        userExpenseData.setValue(new HashMap<>());
                        expenses.setValue(new ArrayList<>());
                        totalSpending.setValue(0.0);
                        return;
                    }
                    calculateExpenseData(snapshots);
                });
    }

    private void calculateExpenseData(QuerySnapshot snapshots) {
        Map<String, UserExpenseData> data = new HashMap<>();
        List<Expense> expenseList = new ArrayList<>();
        double totalSpendingValue = 0.0;

        for (String userId : memberIds) {
            data.put(userId, new UserExpenseData(0.0, 0.0, 0.0, new HashMap<>(), new HashMap<>()));
        }

        for (QueryDocumentSnapshot doc : snapshots) {
            Expense expense = doc.toObject(Expense.class);
            expenseList.add(expense);

            String paidBy = expense.getPaidBy();
            String amountStr = expense.getAmount();
            Map<String, Double> participants = expense.getParticipants();

            if (paidBy == null || amountStr == null || participants == null) {
                Log.w(TAG, "Invalid expense data in document: " + doc.getId());
                continue;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid amount format in document: " + doc.getId(), e);
                continue;
            }

            totalSpendingValue += amount;
            data.computeIfAbsent(paidBy, k -> new UserExpenseData(0.0, 0.0, 0.0, new HashMap<>(), new HashMap<>()));
            UserExpenseData payerData = data.get(paidBy);
            payerData.totalSpent += amount;

            int participantCount = participants.size();
            double equalShare = participantCount > 0 ? amount / participantCount : 0.0;

            for (Map.Entry<String, Double> participant : participants.entrySet()) {
                String userId = participant.getKey();
                double share = participant.getValue() != null && participant.getValue() > 0 ? participant.getValue() : equalShare;

                data.computeIfAbsent(userId, k -> new UserExpenseData(0.0, 0.0, 0.0, new HashMap<>(), new HashMap<>()));
                UserExpenseData userData = data.get(userId);
                userData.totalShare += share;

                if (!userId.equals(paidBy) && share > 0) {
                    userData.debts.put(paidBy, userData.debts.getOrDefault(paidBy, 0.0) + share);
                    payerData.credits.put(userId, payerData.credits.getOrDefault(userId, 0.0) + share);
                }
            }
        }

        for (UserExpenseData userData : data.values()) {
            userData.netBalance = userData.totalSpent - userData.totalShare;
        }

        userExpenseData.setValue(data);
        expenses.setValue(expenseList);
        updateTotalSpending(totalSpendingValue);
    }

    private void updateTotalSpending(double totalSpendingValue) {
        if (totalSpending.getValue() == null || totalSpending.getValue() != totalSpendingValue) {
            totalSpending.setValue(totalSpendingValue);
            firestore.collection("groups").document(groupId)
                    .update("totalSpending", totalSpendingValue)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update total spending: ", e));
        }
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
        userExpenseData.setValue(currentData);
    }

    public List<Transaction> simplifyDebts() {
        Map<String, UserExpenseData> data = userExpenseData.getValue();
        if (data == null) return new ArrayList<>();

        Map<String, Double> netBalances = new HashMap<>();
        for (Map.Entry<String, UserExpenseData> entry : data.entrySet()) {
            netBalances.put(entry.getKey(), entry.getValue().netBalance);
        }

        List<Transaction> simplifiedTransactions = new ArrayList<>();
        while (!netBalances.isEmpty()) {
            String maxDebtor = null, maxCreditor = null;
            double maxDebt = 0.0, maxCredit = 0.0;

            for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
                double balance = entry.getValue();
                if (balance < 0 && Math.abs(balance) > maxDebt) {
                    maxDebtor = entry.getKey();
                    maxDebt = Math.abs(balance);
                } else if (balance > 0 && balance > maxCredit) {
                    maxCreditor = entry.getKey();
                    maxCredit = balance;
                }
            }

            if (maxDebtor == null || maxCreditor == null) break;

            double amount = Math.min(maxDebt, maxCredit);
            simplifiedTransactions.add(new Transaction(maxDebtor, amount, "", "PENDING", maxCreditor, "Simplified"));
            settleDebt(maxDebtor, maxCreditor, amount);

            netBalances.put(maxDebtor, netBalances.get(maxDebtor) + amount);
            netBalances.put(maxCreditor, netBalances.get(maxCreditor) - amount);
            netBalances.entrySet().removeIf(entry -> Math.abs(entry.getValue()) < 0.01);
        }

        return simplifiedTransactions;
    }

    public void removeListener() {
        if (expenseListener != null) expenseListener.remove();
        if (groupListener != null) groupListener.remove();
    }
}