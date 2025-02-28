package com.example.splitaura.models;

import java.util.Collections;
import java.util.List;

public class Expense {
    private String title;
    private String amount;
    private List<String> category = Collections.emptyList(); // Ensure it's initialized to avoid null issues
    private String paidBy;
    private String splitBy;
    private String notes;
    private String imageUrl;
    private String description; // Added description field

    // Required for Firestore
    public Expense() {}

    public Expense(String title, String amount, List<String> category, String paidBy, String splitBy, String notes, String imageUrl, String description) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.paidBy = paidBy;
        this.splitBy = splitBy;
        this.notes = notes;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public List<String> getCategory() {
        return category;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(String paidBy) {
        this.paidBy = paidBy;
    }

    public String getSplitBy() {
        return splitBy;
    }

    public void setSplitBy(String splitBy) {
        this.splitBy = splitBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
