package com.example.splitaura.models;

import com.google.firebase.Timestamp;

public class FriendRequest {
    private String sender_uid;
    private String senderName;
    private String senderProfilePicture;
    private String receiver_uid;
    private String status;
    private Timestamp sentAt;

    // Empty constructor required for Firestore
    public FriendRequest() { }

    public FriendRequest(String sender_uid, String senderName, String senderProfilePicture,
                         String receiver_uid, String status, Timestamp sentAt) {
        this.sender_uid = sender_uid;
        this.senderName = senderName;
        this.senderProfilePicture = senderProfilePicture;
        this.receiver_uid = receiver_uid;
        this.status = status;
        this.sentAt = sentAt;
    }

    // Getters
    public String getSender_uid() {
        return sender_uid;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderProfilePicture() {
        return senderProfilePicture;
    }

    public String getReceiver_uid() {
        return receiver_uid;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }
}
