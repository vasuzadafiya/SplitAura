package com.example.splitaura.models;

import java.util.List;

public class User {
    private String fname;
    private String lname;
    private String profile_picture; // Image URL
    private List<String> searchKeywords;

    // Firestore requires an empty constructor
    public User() {
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public void setProfile_picture(String profile_picture) {
        this.profile_picture = profile_picture;
    }

    public void setSearchKeywords(List<String> searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

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
