package com.example.splitaura.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class PersonalExpense {
    private String documentId;
    private String title;
    private double amount;
    private String notes;
    private Timestamp timestamp;
    private String paymentMethod;
    private List<String> categories;

    // No-argument constructor required for Firestore deserialization
    public PersonalExpense() {}

    public PersonalExpense(String documentId, String title, double amount, String notes,
                           Timestamp timestamp, String paymentMethod, List<String> categories) {
        this.documentId = documentId;
        this.title = title;
        this.amount = amount;
        this.notes = notes;
        this.timestamp = timestamp;
        this.paymentMethod = paymentMethod;
        this.categories = categories;
    }

    // Getters and setters
    public String getDocumentId() {
        return documentId;
    }
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    public String getTitle() {
        return title;
    }
    public double getAmount() {
        return amount;
    }
    public String getNotes() {
        return notes;
    }
    public Timestamp getTimestamp() {
        return timestamp;
    }
    public String getPaymentMethod() {
        return paymentMethod;
    }
    public List<String> getCategories() {
        return categories;
    }
}
