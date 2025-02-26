package com.example.splitaura.models;

public class Category {
    private String name;
    private int iconResId;
    private int backgroundColorResId;

    public Category(String name, int iconResId, int backgroundColorResId) {
        this.name = name;
        this.iconResId = iconResId;
        this.backgroundColorResId = backgroundColorResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getBackgroundColorResId() {
        return backgroundColorResId;
    }
}

