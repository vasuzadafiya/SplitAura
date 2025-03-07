package com.example.splitaura.models;

public class TransactionItem {
    private String title;
    private String amount;
    private String paidBy;
    private String date;
    private int iconResId; // resource id for the icon

    public TransactionItem(String title, String amount, String paidBy, String date, int iconResId) {
        this.title = title;
        this.amount = amount;
        this.paidBy = paidBy;
        this.date = date;
        this.iconResId = iconResId;
    }

    // Getters and setters...
    public String getTitle() {
        return title;
    }

    public String getAmount() {
        return amount;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public String getDate() {
        return date;
    }

    public int getIconResId() {
        return iconResId;
    }
}
