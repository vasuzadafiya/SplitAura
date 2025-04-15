package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.Language;

import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    private final List<Language> languages;
    private final OnLanguageClickListener listener;
    private int selectedPosition = -1;

    public LanguageAdapter(List<Language> languages, OnLanguageClickListener listener) {
        this.languages = languages;
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        Language language = languages.get(position);
        holder.languageName.setText(holder.itemView.getContext().getString(language.getNameResId()));
        holder.languageIcon.setImageResource(language.getIconResId());
        holder.layout.setBackgroundResource(language.getBackgroundResId());
        holder.selectedBorder.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);

        holder.layout.setOnClickListener(v -> {
            int prevSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(prevSelected);
            notifyItemChanged(selectedPosition);
            listener.onLanguageClick(language);
        });
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    public interface OnLanguageClickListener {
        void onLanguageClick(Language language);
    }

    static class LanguageViewHolder extends RecyclerView.ViewHolder {
        TextView languageName;
        ImageView languageIcon;
        View layout, selectedBorder;

        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            languageName = itemView.findViewById(R.id.languageName);
            languageIcon = itemView.findViewById(R.id.languageIcon);
            layout = itemView.findViewById(R.id.langCard);
            selectedBorder = itemView.findViewById(R.id.selectedBorder);
        }
    }
}
