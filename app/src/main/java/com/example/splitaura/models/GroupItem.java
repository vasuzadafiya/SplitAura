package com.example.splitaura.models;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class GroupItem {
    private String groupId = "";  // Firestore document ID
    private String groupName = "";
    private String date = "";
    private double amount;
    private String groupImageUrl = "";
    private List<String> memberImagesUrls = new ArrayList<>();
    private int moreMembersCount = 0;
    private Map<String, Double> participants = new HashMap<>();

    // Constructor
    public GroupItem(String groupId, String groupName, String date, double amount, String groupImageUrl, List<String> memberImagesUrls, int moreMembersCount, Map<String, Double> participants) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.date = date;
        this.amount = amount;
        this.groupImageUrl = groupImageUrl;
        this.memberImagesUrls = memberImagesUrls;
        this.moreMembersCount = moreMembersCount;
        this.participants = participants;
    }

    // Getters
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

    public Map<String, Double> getParticipants() {
        return participants;
    }

    // Setters (if needed)
    public void setParticipants(Map<String, Double> participants) {
        this.participants = participants;
    }

    // Optional: Builder Pattern for better object creation
    public static class Builder {
        private String groupId = "";
        private String groupName = "";
        private String date = "";
        private double amount;
        private String ImageUrl = "";
        private List<String> memberImagesUrls = new ArrayList<>();
        private int moreMembersCount = 0;
        private Map<String, Double> participants = new HashMap<>();

        public Builder setGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder setGroupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder setDate(String date) {
            this.date = date;
            return this;
        }

        public Builder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.ImageUrl = imageUrl;
            return this;
        }

        public Builder setMemberImagesUrls(List<String> memberImagesUrls) {
            this.memberImagesUrls = memberImagesUrls;
            return this;
        }

        public Builder setMoreMembersCount(int moreMembersCount) {
            this.moreMembersCount = moreMembersCount;
            return this;
        }

        public Builder setParticipants(Map<String, Double> participants) {
            this.participants = participants;
            return this;
        }

        public GroupItem build() {
            return new GroupItem(groupId, groupName, date, amount, ImageUrl, memberImagesUrls, moreMembersCount, participants);
        }
    }
}