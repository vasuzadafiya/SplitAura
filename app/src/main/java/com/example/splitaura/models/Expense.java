package com.example.splitaura.models;

import java.util.ArrayList;
import java.util.List;

public class Expense {
    private String title;
    private String amount;
    private List<String> categories = new ArrayList<>(); // Ensure this is List<String>
    private String paidBy;
    private String splitBy;
    private String notes;
    private String date;
    private String imageUrl;
    private String description;

    // Required for Firestore
    public Expense() {}

    public Expense(String title, String amount, List<String> categories, String paidBy, String splitBy, String date, String notes, String imageUrl, String description) {
        this.title = title;
        this.amount = amount;
        this.categories = categories;
        this.paidBy = paidBy;
        this.splitBy = splitBy;
        this.date = date;
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

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    @Override
    public String toString() {
        return "Expense{" +
                "title='" + title + '\'' +
                ", amount='" + amount + '\'' +
                ", categories=" + categories +
                ", paidBy='" + paidBy + '\'' +
                ", splitBy='" + splitBy + '\'' +
                ", notes='" + notes + '\'' +
                ", date='" + date + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}