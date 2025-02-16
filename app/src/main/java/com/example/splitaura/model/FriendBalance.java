package com.example.splitaura.model;

public class FriendBalance {
    private String name;
    private double balance;
    private int profileImage; // Drawable resource ID

    public FriendBalance(String name, double balance, int profileImage) {
        this.name = name;
        this.balance = balance;
        this.profileImage = profileImage;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public int getProfileImage() {
        return profileImage;
    }
}
