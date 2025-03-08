package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.Expense;

import java.util.List;
import java.util.Random;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenseList;

    public ExpenseAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);

        // Bind the data to the views
        holder.expenseName.setText(expense.getTitle());
        String formattedText = String.format("₹ %s", expense.getAmount());
        holder.expenseAmount.setText(formattedText);
        holder.expensePaidBy.setText("Paid by " + expense.getPaidBy());
        holder.expenseDate.setText(expense.getDate());

        // Get the first category from the list (if available)
        String category = (expense.getCategories() == null || expense.getCategories().isEmpty())
                ? "Other"
                : expense.getCategories().get(0);

        setCategoryIcon(holder.categoryIcon, category);
    }
    /**
     * Sets the category icon based on the provided category.
     *
     * @param categoryIcon The ImageView to set the icon for.
     * @param category     The category to determine the icon.
     */
    private void setCategoryIcon(ImageView categoryIcon, String category) {
        // Define an array of clean, polished background colors
        int[] backgroundColors = {
                R.drawable.icon_background_blue,
                R.drawable.icon_background_green,
                R.drawable.icon_background_orange,
                R.drawable.icon_background_purple,
                R.drawable.icon_background_red,
                R.drawable.icon_background_teal,
                R.drawable.icon_background_yellow,
                R.drawable.icon_background_pink,
                R.drawable.icon_background_cyan
        };

        // Randomly select a background color
        int randomIndex = new Random().nextInt(backgroundColors.length);
        int backgroundColor = backgroundColors[randomIndex];

        // Set the category icon and background
        switch (category) {
            case "General":
                categoryIcon.setImageResource(R.drawable.ic_general);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Food":
                categoryIcon.setImageResource(R.drawable.ic_food);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Games":
                categoryIcon.setImageResource(R.drawable.ic_games);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Sports":
                categoryIcon.setImageResource(R.drawable.ic_sports);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Music":
                categoryIcon.setImageResource(R.drawable.ic_music);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Movies":
                categoryIcon.setImageResource(R.drawable.ic_movies);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Liquor":
                categoryIcon.setImageResource(R.drawable.ic_liquor);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Grocery":
                categoryIcon.setImageResource(R.drawable.ic_grocery);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            case "Shop":
                categoryIcon.setImageResource(R.drawable.ic_shop);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
            default:
                categoryIcon.setImageResource(R.drawable.ic_category);
                categoryIcon.setBackgroundResource(backgroundColor);
                break;
        }
    }
    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView expenseName;
        TextView expensePaidBy;
        TextView expenseAmount;
        TextView expenseDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            categoryIcon = itemView.findViewById(R.id.category_icon);
            expenseName = itemView.findViewById(R.id.expense_name);
            expensePaidBy = itemView.findViewById(R.id.expense_paid_by);
            expenseAmount = itemView.findViewById(R.id.expense_amount);
            expenseDate = itemView.findViewById(R.id.expense_date);
        }
    }
}