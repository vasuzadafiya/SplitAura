package com.example.splitaura.models;

public class Language {
    private final int nameResId;         // For UI label like "हिंदी\nHindi"
    private final int iconResId;         // Icon shown in UI
    private final int backgroundResId;   // Card background
    private final String langCode;       // "hi", "gu", etc.

    public Language(int nameResId, int iconResId, int backgroundResId, String langCode) {
        this.nameResId = nameResId;
        this.iconResId = iconResId;
        this.backgroundResId = backgroundResId;
        this.langCode = langCode;
    }

    public int getNameResId() {
        return nameResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getBackgroundResId() {
        return backgroundResId;
    }

    public String getLangCode() {
        return langCode;
    }
}
