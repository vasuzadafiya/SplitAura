package com.example.splitaura.models;

public class FriendBalance {
    private String name;
    private double balance;
    private String profilePictureUrl; // Changed from int to String

    public FriendBalance(String name, double balance, String profilePictureUrl) {
        this.name = name;
        this.balance = balance;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}