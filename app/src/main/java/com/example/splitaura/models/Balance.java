package com.example.splitaura.models;

public class Balance {
    private String name;
    private double amount;

    // Default constructor required for Firestore
    public Balance() {
    }

    public Balance(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }
}
