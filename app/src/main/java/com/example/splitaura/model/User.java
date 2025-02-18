package com.example.splitaura.model;

import java.util.List;

public class User {
    private String fname;
    private String lname;
    private String profile_picture; // Image URL
    private List<String> searchKeywords;

    // Firestore requires an empty constructor
    public User() { }

    public User(String fname, String lname, String profile_picture, List<String> searchKeywords) {
        this.fname = fname;
        this.lname = lname;
        this.profile_picture = profile_picture;
        this.searchKeywords = searchKeywords;
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getProfile_picture() {
        return profile_picture;
    }

    public List<String> getSearchKeywords() {
        return searchKeywords;
    }

    // Helper to combine first and last names
    public String getFullName() {
        return fname + " " + lname;
    }
}
