package com.example.splitaura.utils;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class LanguageUtils {

    public static void setLocale(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        // Update configuration to load the correct resources (strings.xml)
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    // Utility to map language name (from strings.xml) to language code
    public static String getLanguageCodeFromName(String name) {
        if (name.contains("हिंदी")) return "hi";
        if (name.contains("ગુજરાતી")) return "gu";
        if (name.contains("मराठी")) return "mr";
        if (name.contains("বাংলা")) return "bn";
        if (name.contains("ਪੰਜਾਬੀ")) return "pa";
        if (name.contains("മലയാളം")) return "ml";
        if (name.contains("தமிழ்")) return "ta";
        if (name.contains("తెలుగు")) return "te";
        if (name.contains("ಕನ್ನಡ")) return "kn";
        if (name.contains("ଓଡ଼ିଆ")) return "or";
        if (name.contains("অসমীয়া")) return "as";
        if (name.contains("राजस्थानी")) return "raj";
        if (name.contains("हरियाणवी")) return "har";
        if (name.contains("भोजपुरी")) return "bho";
        return "en";
    }
}
