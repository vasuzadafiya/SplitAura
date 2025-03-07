package com.example.splitaura.adapters;

public class MemberData {
    private String name;
    private String email;
    private int profileResourceId;

    // Required for Firestore deserialization
    public MemberData() {
    }

    public MemberData(String name, String email, int profileResourceId) {
        this.name = name;
        this.email = email;
        this.profileResourceId = profileResourceId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public int getProfileResourceId() {
        return profileResourceId;
    }

    // Optional setters if updates are needed
    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfileResourceId(int profileResourceId) {
        this.profileResourceId = profileResourceId;
    }
}
