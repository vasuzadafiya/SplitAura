package com.example.splitaura;

import com.example.splitaura.adapters.MemberData;

import java.util.List;

public class GroupData {
    private final String title;
    private final String description;
    private final String currency;
    private final String category;
    private final List<MemberData> members;

    public GroupData(String title, String description, String currency, String category, List<MemberData> members) {
        this.title = title;
        this.description = description;
        this.currency = currency;
        this.category = category;
        this.members = members;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCurrency() { return currency; }
    public String getCategory() { return category; }
    public List<MemberData> getMembers() { return members; }
}