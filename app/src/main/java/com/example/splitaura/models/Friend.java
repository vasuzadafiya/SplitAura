package com.example.splitaura.models;

import java.util.Objects;

public class Friend {
    private String uid;
    private String email;
    private String fname;
    private String lname;
    private String profile_picture;

    public Friend() {
        // Firestore requires a public no-argument constructor.
    }

    // Getters and setters
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getFname() {
        return fname;
    }
    public void setFname(String fname) {
        this.fname = fname;
    }
    public String getLname() {
        return lname;
    }
    public void setLname(String lname) {
        this.lname = lname;
    }
    public String getProfile_picture() {
        return profile_picture;
    }
    public void setProfile_picture(String profile_picture) {
        this.profile_picture = profile_picture;
    }

    // Helper method to combine first and last name
    public String getFullName() {
        return fname + " " + lname;
    }

    // Optional: override equals and hashCode using email as unique identifier
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return Objects.equals(email, friend.email);
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }

}
