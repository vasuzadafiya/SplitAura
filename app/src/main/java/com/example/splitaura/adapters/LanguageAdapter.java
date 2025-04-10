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

    private final List<Language> languageList;
    private final OnLanguageClickListener listener;
    private int selectedPosition = -1;
    public LanguageAdapter(List<Language> languageList, OnLanguageClickListener listener) {
        this.languageList = languageList;
        this.listener = listener;
        setHasStableIds(true); // Enables stable IDs for smoother recycling.
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        Language language = languageList.get(position);
        // Set language name from string resource.
        holder.languageName.setText(holder.itemView.getContext().getString(language.getNameResId()));
        // Set icon and background from the model.
        holder.languageIcon.setImageResource(language.getIconResId());
        holder.layout.setBackgroundResource(language.getBackgroundResId());

        // Toggle the selection border visibility.
        holder.selectedBorder.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);

        // Set click listener for the card.
        holder.layout.setOnClickListener(v -> {
            if (selectedPosition != position) {
                int prevSelected = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(prevSelected);
                notifyItemChanged(selectedPosition);
                listener.onLanguageClick(language);
            }
        });
    }

    @Override
    public int getItemCount() {
        return languageList.size();
    }

    @Override
    public long getItemId(int position) {
        // Use the language name resource as a unique stable ID.
        return languageList.get(position).getNameResId();
    }

    public interface OnLanguageClickListener {
        void onLanguageClick(Language language);
    }

    static class LanguageViewHolder extends RecyclerView.ViewHolder {
        TextView languageName;
        ImageView languageIcon;
        View layout;
        View selectedBorder;

        LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            languageName = itemView.findViewById(R.id.languageName);
            languageIcon = itemView.findViewById(R.id.languageIcon);
            layout = itemView.findViewById(R.id.langCard);
            selectedBorder = itemView.findViewById(R.id.selectedBorder);
        }
    }
}
