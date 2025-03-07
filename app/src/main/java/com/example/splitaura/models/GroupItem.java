package com.example.splitaura.models;

import java.util.List;

public class GroupItem {
    private String groupId;  // New field for Firestore document ID
    private String groupName;
    private String date;
    private double amount;
    private String groupImageUrl;
    private List<String> memberImagesUrls;
    private int moreMembersCount;

    public GroupItem(String groupId, String groupName, String date, double amount, String groupImageUrl, List<String> memberImagesUrls, int moreMembersCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.date = date;
        this.amount = amount;
        this.groupImageUrl = groupImageUrl;
        this.memberImagesUrls = memberImagesUrls;
        this.moreMembersCount = moreMembersCount;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public String getGroupImageUrl() {
        return groupImageUrl;
    }

    public List<String> getMemberImagesUrls() {
        return memberImagesUrls;
    }

    public int getMoreMembersCount() {
        return moreMembersCount;
    }
}
