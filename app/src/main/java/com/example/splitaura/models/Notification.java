package com.example.splitaura.models;

public class Notification {
    private String actionType; // "PAYMENT_REQUEST", "PAYMENT_SENT", "SETTLEMENT"
    private String senderId;   // Who initiated the action
    private String receiverId; // Who needs to confirm/receive
    private String actionId;   // Unique ID for this notification
    private String transactionId; // Links to the Transaction
    private String actionStatus; // "PENDING", "CONFIRMED", "REJECTED"

    // Default constructor for Firestore
    public Notification() {}

    public Notification(String actionType, String senderId, String receiverId, String actionId,
                        String transactionId, String actionStatus) {
        this.actionType = actionType;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.actionId = actionId;
        this.transactionId = transactionId;
        this.actionStatus = actionStatus;
    }

    // Getters and Setters
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }
}