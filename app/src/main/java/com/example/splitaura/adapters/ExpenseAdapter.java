package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {
    private List<ExpenseManager.Expense> expenseList;

    public ExpenseAdapter(List<ExpenseManager.Expense> expenseList) {
        this.expenseList = expenseList != null ? new ArrayList<>(expenseList) : new ArrayList<>();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        ExpenseManager.Expense expense = expenseList.get(position);

        holder.expenseName.setText(expense.getTitle());
        String formattedText = String.format("₹ %s", expense.getAmount());
        holder.expenseAmount.setText(formattedText);
        holder.expensePaidBy.setText("Paid by " + expense.getPaidBy());
        holder.expenseDate.setText(expense.getDate());

        String category = (expense.getCategories() == null || expense.getCategories().isEmpty())
                ? "Other"
                : expense.getCategories().get(0);

        setCategoryIcon(holder.categoryIcon, category);
    }

    private void setCategoryIcon(ImageView categoryIcon, String category) {
        int[] backgroundColors = {
                R.drawable.icon_background_blue, R.drawable.icon_background_green,
                R.drawable.icon_background_orange, R.drawable.icon_background_purple,
                R.drawable.icon_background_red, R.drawable.icon_background_teal,
                R.drawable.icon_background_yellow, R.drawable.icon_background_pink,
                R.drawable.icon_background_cyan
        };

        int randomIndex = new Random().nextInt(backgroundColors.length);
        int backgroundColor = backgroundColors[randomIndex];

        switch (category) {
            case "General":
                categoryIcon.setImageResource(R.drawable.ic_general);
                break;
            case "Food":
                categoryIcon.setImageResource(R.drawable.ic_food);
                break;
            case "Games":
                categoryIcon.setImageResource(R.drawable.ic_games);
                break;
            case "Sports":
                categoryIcon.setImageResource(R.drawable.ic_sports);
                break;
            case "Music":
                categoryIcon.setImageResource(R.drawable.ic_music);
                break;
            case "Movies":
                categoryIcon.setImageResource(R.drawable.ic_movies);
                break;
            case "Liquor":
                categoryIcon.setImageResource(R.drawable.ic_liquor);
                break;
            case "Grocery":
                categoryIcon.setImageResource(R.drawable.ic_grocery);
                break;
            case "Shop":
                categoryIcon.setImageResource(R.drawable.ic_shop);
                break;
            default:
                categoryIcon.setImageResource(R.drawable.ic_category);
                break;
        }
        categoryIcon.setBackgroundResource(backgroundColor);
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView expenseName, expensePaidBy, expenseAmount, expenseDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            expenseName = itemView.findViewById(R.id.expense_name);
            expensePaidBy = itemView.findViewById(R.id.expense_paid_by);
            expenseAmount = itemView.findViewById(R.id.expense_amount);
            expenseDate = itemView.findViewById(R.id.expense_date);
        }
    }

    public void updateExpenses(List<ExpenseManager.Expense> newExpenses) {
        this.expenseList.clear();
        this.expenseList.addAll(newExpenses != null ? newExpenses : new ArrayList<>());
        notifyDataSetChanged();
    }
}