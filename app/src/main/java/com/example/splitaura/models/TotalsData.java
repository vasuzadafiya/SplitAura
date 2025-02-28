package com.example.splitaura.models;

public class TotalsData {
    private double groupSpending;
    private double youPaid;
    private double yourShare;

    public TotalsData(double groupSpending, double youPaid, double yourShare) {
        this.groupSpending = groupSpending;
        this.youPaid = youPaid;
        this.yourShare = yourShare;
    }

    public double getGroupSpending() {
        return groupSpending;
    }

    public double getYouPaid() {
        return youPaid;
    }

    public double getYourShare() {
        return yourShare;
    }
}
