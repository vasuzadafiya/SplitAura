package com.example.splitaura.models;

public class DebtSummary {
    private String friendName;
    private String friendId;
    private double amount;

    public DebtSummary(String friendName, String friendId, double amount) {
        this.friendName = friendName;
        this.friendId = friendId;
        this.amount = amount;
    }

    public String getFriendName() {
        return friendName;
    }

    public String getFriendId() {
        return friendId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}