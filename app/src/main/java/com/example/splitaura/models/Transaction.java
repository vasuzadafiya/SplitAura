package com.example.splitaura.models;

public class Transaction {
    private String friendName;
    private double amount;
    private String date;
    private String status; // "SUCCESS" or "FAILED"

    public Transaction(String friendName, double amount, String date, String status) {
        this.friendName = friendName;
        this.amount = amount;
        this.date = date;
        this.status = status;
    }

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
}