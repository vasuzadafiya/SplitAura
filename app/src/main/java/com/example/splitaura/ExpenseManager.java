package com.example.splitaura;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseManager {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final String groupId;
    private final MutableLiveData<Map<String, UserExpenseData>> userExpenseData = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> expenses = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration expenseListener;
    private List<String> memberIds = new ArrayList<>();

    public ExpenseManager(String groupId) {
        this.groupId = groupId;
        fetchGroupMembersAndSetupListener(); // Combined fetch and setup
    }

    public static class Expense {
        private String title, amount, paidBy, splitBy, notes, date, imageUrl, description;
        private List<String> categories;
        private Map<String, Double> participants;

        public Expense() {} // Required for Firestore

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

        UserExpenseData(double totalSpent, double totalShare, double netBalance,
                        Map<String, Double> debts, Map<String, Double> credits) {
            this.totalSpent = totalSpent;
            this.totalShare = totalShare;
            this.netBalance = netBalance;
            this.debts = debts;
            this.credits = credits;
        }
    }

    public void addExpense(Map<String, Object> expenseData, Runnable onSuccess, Runnable onFailure) {
        firestore.collection("groups").document(groupId).collection("expenses")
                .add(expenseData)
                .addOnSuccessListener(docRef -> {
                    calculateAndUpdateTotalSpending(); // Update total spending after adding expense
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onFailure.run());
    }

    public LiveData<Map<String, UserExpenseData>> getUserExpenseData() {
        return userExpenseData;
    }

    public LiveData<List<Expense>> getExpenses() {
        return expenses;
    }

    private void fetchGroupMembersAndSetupListener() {
        firestore.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    List<String> ids = (List<String>) doc.get("memberIds");
                    if (ids != null) {
                        memberIds = ids;
                    }
                    setupExpenseListener(); // Setup listener after members are fetched
                })
                .addOnFailureListener(e -> {
                    memberIds = new ArrayList<>(); // Fallback to empty list
                    setupExpenseListener(); // Proceed with empty list
                });
    }

    private void setupExpenseListener() {
        expenseListener = firestore.collection("groups").document(groupId).collection("expenses")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        userExpenseData.setValue(new HashMap<>());
                        expenses.setValue(new ArrayList<>());
                        return;
                    }
                    calculateExpenseData(snapshots);
                });
    }

    private void calculateExpenseData(QuerySnapshot snapshots) {
        Map<String, UserExpenseData> data = new HashMap<>();
        List<Expense> expenseList = new ArrayList<>();
        double totalSpending = 0.0; // Track total spending for the group

        // Initialize data for all known members
        for (String userId : memberIds) {
            data.put(userId, new UserExpenseData(0.0, 0.0, 0.0, new HashMap<>(), new HashMap<>()));
        }

        for (QueryDocumentSnapshot doc : snapshots) {
            Expense expense = doc.toObject(Expense.class);
            expenseList.add(expense);

            String paidBy = expense.getPaidBy();
            String amountStr = expense.getAmount();
            Map<String, Double> participants = expense.getParticipants();

            if (paidBy == null || amountStr == null || participants == null) continue;

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                continue; // Skip invalid amounts
            }

            // Add to total spending
            totalSpending += amount;

            // Ensure payer is in the data map
            if (!data.containsKey(paidBy)) {
                data.put(paidBy, new UserExpenseData(0.0, 0.0, 0.0, new HashMap<>(), new HashMap<>()));
            }
            UserExpenseData payerData = data.get(paidBy);
            payerData.totalSpent += amount;

            for (Map.Entry<String, Double> participant : participants.entrySet()) {
                String userId = participant.getKey();
                double share = participant.getValue();

                // Ensure participant is in the data map
                if (!data.containsKey(userId)) {
                    data.put(userId, new UserExpenseData(0.0, 0.0, 0.0, new HashMap<>(), new HashMap<>()));
                }
                UserExpenseData userData = data.get(userId);
                userData.totalShare += share;

                if (!userId.equals(paidBy)) {
                    userData.debts.put(paidBy, userData.debts.getOrDefault(paidBy, 0.0) + share);
                    payerData.credits.put(userId, payerData.credits.getOrDefault(userId, 0.0) + share);
                }
            }
        }

        // Calculate net balances for all users in data
        for (String userId : data.keySet()) {
            UserExpenseData userData = data.get(userId);
            double totalCredits = userData.credits.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalDebts = userData.debts.values().stream().mapToDouble(Double::doubleValue).sum();
            userData.netBalance = userData.totalSpent - userData.totalShare + totalCredits - totalDebts;
        }

        userExpenseData.setValue(data);
        expenses.setValue(expenseList);

        // Update the total spending in the group document
        updateTotalSpending(totalSpending);
    }

    private void calculateAndUpdateTotalSpending() {
        firestore.collection("groups").document(groupId).collection("expenses")
                .get()
                .addOnSuccessListener(snapshots -> {
                    double totalSpending = 0.0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String amountStr = doc.getString("amount");
                        if (amountStr != null) {
                            try {
                                totalSpending += Double.parseDouble(amountStr);
                            } catch (NumberFormatException e) {
                                // Skip invalid amounts
                            }
                        }
                    }
                    updateTotalSpending(totalSpending);
                });
    }

    private void updateTotalSpending(double totalSpending) {
        firestore.collection("groups").document(groupId)
                .update("amount", totalSpending)
                .addOnSuccessListener(aVoid -> Log.d("ExpenseManager", "Total spending updated successfully"))
                .addOnFailureListener(e -> Log.e("ExpenseManager", "Failed to update total spending", e));
    }

    public void removeListener() {
        if (expenseListener != null) {
            expenseListener.remove();
        }
    }
}