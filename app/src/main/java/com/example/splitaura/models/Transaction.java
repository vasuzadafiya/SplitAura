package com.example.splitaura.models;

public class Transaction {
    private String friendName;
    private double amount;
    private String date;
    private String status; // "SUCCESS", "FAILED", or "PENDING"
    private String transactionId; // Added field for transaction ID
    private String upiApp;       // Added field for UPI app

    // Constructor for dummy data (without transactionId and upiApp)
    public Transaction(String friendName, double amount, String date, String status) {
        this.friendName = friendName;
        this.amount = amount;
        this.date = date;
        this.status = status;
        this.transactionId = "N/A"; // Default value
        this.upiApp = "Unknown";    // Default value
    }

    // Constructor for Firestore data (with transactionId and upiApp)
    public Transaction(String friendName, double amount, String date, String status, String transactionId, String upiApp) {
        this.friendName = friendName;
        this.amount = amount;
        this.date = date;
        this.status = status;
        this.transactionId = transactionId != null ? transactionId : "N/A";
        this.upiApp = upiApp != null ? upiApp : "Unknown";
    }

    // Getters
    public String getFriendName() {
        return friendName;
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getUpiApp() {
        return upiApp;
    }

    // Setters
    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId != null ? transactionId : "N/A";
    }

    public void setUpiApp(String upiApp) {
        this.upiApp = upiApp != null ? upiApp : "Unknown";
    }
}