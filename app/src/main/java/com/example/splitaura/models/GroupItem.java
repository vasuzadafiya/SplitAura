package com.example.splitaura.models;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class GroupItem {
    private String groupId;
    private String groupName;
    private String date;
    private double amount;
    private String groupImageUrl;
    private List<String> memberImagesUrls;
    private int moreMembersCount;
    private Map<String, Double> participants;

    public GroupItem(String groupId, String groupName, String date, double amount,
                     String groupImageUrl, List<String> memberImagesUrls,
                     int moreMembersCount, Map<String, Double> participants) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.date = date;
        this.amount = amount;
        this.groupImageUrl = groupImageUrl;
        this.memberImagesUrls = memberImagesUrls != null ? memberImagesUrls : new ArrayList<>();
        this.moreMembersCount = moreMembersCount;
        this.participants = participants != null ? participants : new HashMap<>();
    }

    // Getters
    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
    public String getGroupImageUrl() { return groupImageUrl; }
    public List<String> getMemberImagesUrls() { return memberImagesUrls; }
    public int getMoreMembersCount() { return moreMembersCount; }
    public Map<String, Double> getParticipants() { return participants; }

    // Setters
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setDate(String date) { this.date = date; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setGroupImageUrl(String groupImageUrl) { this.groupImageUrl = groupImageUrl; }
    public void setMemberImagesUrls(List<String> memberImagesUrls) {
        this.memberImagesUrls = memberImagesUrls != null ? memberImagesUrls : new ArrayList<>();
    }
    public void setMoreMembersCount(int moreMembersCount) { this.moreMembersCount = moreMembersCount; }
    public void setParticipants(Map<String, Double> participants) {
        this.participants = participants != null ? participants : new HashMap<>();
    }
}