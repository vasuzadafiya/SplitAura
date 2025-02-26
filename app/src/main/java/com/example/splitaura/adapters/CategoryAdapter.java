package com.example.splitaura.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final Context context;
    private final List<String> categories;
    private final List<String> selectedCategories;

    // Icons and background drawable resources (assuming you have these resources)
    private final int[] categoryIcons = {
            R.drawable.ic_general, R.drawable.ic_games, R.drawable.ic_sports,
            R.drawable.ic_music, R.drawable.ic_movies, R.drawable.ic_food,
            R.drawable.ic_liquor, R.drawable.ic_grocery, R.drawable.ic_shop
    };
    private final int[] categoryBackgrounds = {
            R.color.iconBackground, R.color.iconBackground, R.color.iconBackground,
            R.color.iconBackground, R.color.iconBackground, R.color.iconBackground,
            R.color.iconBackground, R.color.iconBackground,R.color.iconBackground
    };

    // Constructor
    public CategoryAdapter(Context context, List<String> selectedCategories) {
        this.context = context;
        this.selectedCategories = selectedCategories;
        this.categories = List.of("General", "Games", "Sports", "Music", "Movies", "Food", "Liquor", "Grocery", "Shop");
    }

    // ViewHolder class for category items
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView categoryIcon;
        public TextView categoryName;
        public View categoryContainer;

        public ViewHolder(View view) {
            super(view);
            categoryIcon = view.findViewById(R.id.iconCategory);
            categoryName = view.findViewById(R.id.textCategoryName);
            categoryContainer = view.findViewById(R.id.category_container);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String category = categories.get(position);

        holder.categoryName.setText(category);
        holder.categoryIcon.setImageDrawable(ContextCompat.getDrawable(context, categoryIcons[position]));

        // Handle selection background
        if (selectedCategories.contains(category)) {
            holder.categoryContainer.setBackgroundColor(Color.parseColor("#2196F3")); // Blue background for selected
            holder.categoryIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_checkmark)); // Checkmark icon
        } else {
            holder.categoryContainer.setBackground(ContextCompat.getDrawable(context, categoryBackgrounds[position])); // Default background
        }

        // Handle click event
        holder.itemView.setOnClickListener(v -> {
            if (selectedCategories.contains(category)) {
                selectedCategories.remove(category); // Deselect category
            } else {
                selectedCategories.add(category); // Select category
            }
            notifyItemChanged(position); // Update item
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }
}
