package com.example.splitaura.models;

public class Language {
    private final int nameResId;
    private final int iconResId;
    private final int backgroundResId;
    private final String langCode;

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
