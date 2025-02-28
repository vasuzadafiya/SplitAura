package com.example.splitaura.adapters;

public class MemberData {
    private String name;
    private String email;
    private int profileResourceId;

    public MemberData(String name, String email, int profileResourceId) {
        this.name = name;
        this.email = email;
        this.profileResourceId = profileResourceId;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getProfileResourceId() { return profileResourceId; }
}
