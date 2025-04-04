package com.example.splitaura.models;

public class Transaction {
    private String description; // Brief description (e.g., "Payment to John")
    private double amount;      // Amount transacted
    private String recordedAt;  // String date (e.g., "03 Apr 2025, 19:45")
    private String paymentStatus; // "PENDING", "SUCCESS", "FAILED"
    private String paymentId;   // Unique ID for this transaction
    private String method;      // "QR", "UPI", "CASH"
    private String recipientId; // Who received the payment

    // Default constructor for Firestore
    public Transaction() {}

    public Transaction(String description, double amount, String recordedAt, String paymentStatus,
                       String paymentId, String method, String recipientId) {
        this.description = description;
        this.amount = amount;
        this.recordedAt = recordedAt;
        this.paymentStatus = paymentStatus;
        this.paymentId = paymentId;
        this.method = method;
        this.recipientId = recipientId;
    }

    // Getters and Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getRecordedAt() { return recordedAt; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
}