package com.example.splitaura.models;

import java.util.List;

//this class defines the data structure for each group item. that we will display in homeFragment.
public class GroupItem {
    private String groupName;
    private String date;
    private double amount;
    private int groupImage;
    private List<Integer> memberImages;
    private int moreMembersCount;

    public GroupItem(String groupName, String date, double amount, int groupImage, List<Integer> memberImages, int moreMembersCount) {
        this.groupName = groupName;
        this.date = date;
        this.amount = amount;
        this.groupImage = groupImage;
        this.memberImages = memberImages;
        this.moreMembersCount = moreMembersCount;
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

    public int getGroupImage() {
        return groupImage;
    }

    public List<Integer> getMemberImages() {
        return memberImages;
    }

    public int getMoreMembersCount() {
        return moreMembersCount;
    }
}
