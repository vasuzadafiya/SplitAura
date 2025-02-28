package com.example.splitaura.models;

public class OweRelationship {
    private String debtor;
    private String creditor;
    private double amount;

    // Default constructor required for Firestore
    public OweRelationship() {
    }

    public OweRelationship(String debtor, String creditor, double amount) {
        this.debtor = debtor;
        this.creditor = creditor;
        this.amount = amount;
    }

    public String getDebtor() {
        return debtor;
    }

    public String getCreditor() {
        return creditor;
    }

    public double getAmount() {
        return amount;
    }
}
