package com.example.splitaura;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.adapters.LanguageAdapter;
import com.example.splitaura.models.Language;
import com.example.splitaura.utils.LanguageUtils;

import java.util.Arrays;
import java.util.List;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        EdgeToEdge.enable(this);
        showLanguageDialog(this);

        ImageButton button = findViewById(R.id.next_button);
        button.setOnClickListener(v -> {
            startActivity(new Intent(Splash.this, OnboardingScreen.class));
            finish(); // Close splash screen after moving to OnboardingScreen
        });
    }

    public void showLanguageDialog(Context context) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_language, null);

        RecyclerView recyclerView = dialogView.findViewById(R.id.languageRecyclerView);
        Button btnNext = dialogView.findViewById(R.id.btnNext);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        recyclerView.setHasFixedSize(true);

        List<Language> languages = Arrays.asList(
                new Language(R.string.lang_hindi, R.drawable.ic_tajmahal, R.drawable.bg_lang_card_blue, "hi"),
                new Language(R.string.lang_gujarati, R.drawable.ic_gujarati, R.drawable.bg_lang_card_orange, "gu"),
                new Language(R.string.lang_marathi, R.drawable.ic_marathi, R.drawable.bg_lang_card_orange, "mr"),
                new Language(R.string.lang_bengali, R.drawable.ic_bengali, R.drawable.bg_lang_card_blue, "bn"),
                new Language(R.string.lang_punjabi, R.drawable.ic_punjabi, R.drawable.bg_lang_card_blue, "pa"),
                new Language(R.string.lang_malayalam, R.drawable.ic_malayalam, R.drawable.bg_lang_card_orange, "ml"),
                new Language(R.string.lang_tamil, R.drawable.ic_tamil, R.drawable.bg_lang_card_orange, "ta"),
                new Language(R.string.lang_telugu, R.drawable.ic_telugu, R.drawable.bg_lang_card_blue, "te"),
                new Language(R.string.lang_kannada, R.drawable.ic_kannada, R.drawable.bg_lang_card_blue, "kn"),
                new Language(R.string.lang_odia, R.drawable.ic_odia, R.drawable.bg_lang_card_orange, "or"),
                new Language(R.string.lang_assamese, R.drawable.ic_assamese, R.drawable.bg_lang_card_orange, "as"),
                new Language(R.string.lang_rajasthani, R.drawable.ic_rajasthani, R.drawable.bg_lang_card_blue, "raj"),
                new Language(R.string.lang_haryanvi, R.drawable.ic_haryanvi, R.drawable.bg_lang_card_blue, "har"),
                new Language(R.string.lang_bhojpuri, R.drawable.ic_bhojpuri, R.drawable.bg_lang_card_orange, "bho")
        );

        final Language[] selectedLanguage = {null};

        LanguageAdapter adapter = new LanguageAdapter(languages, language -> {
            selectedLanguage[0] = language;
            btnNext.setVisibility(View.VISIBLE);
        });

        recyclerView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).setCancelable(true).create();

        btnNext.setOnClickListener(v -> {
            if (selectedLanguage[0] != null) {
                String langCode = selectedLanguage[0].getLangCode();

                SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                prefs.edit().putString("selected_lang_code", langCode).apply();

                LanguageUtils.setLocale(context, langCode);

                // Restart the app or activity
                Intent intent = new Intent(context, splashScreen.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }

                dialog.dismiss();
            }
        });


        dialog.show();
    }


}
