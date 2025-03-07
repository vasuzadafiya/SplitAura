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

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;

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
        String formattedText = String.format("Expense: %s", String.valueOf(expense.getAmount()));
        holder.expenseAmount.setText(formattedText);
        holder.expensePaidBy.setText("Paid by " + expense.getPaidBy());
        holder.expenseDate.setText("Today");

        // Assuming the category list contains one category for now
        String category = expense.getCategory().isEmpty() ? "Other" : expense.getCategory().get(0);

        // Set category icon based on the category type
        switch (category) {
            case "General":
                holder.categoryIcon.setImageResource(R.drawable.ic_general);
                holder.categoryIcon.setBackgroundResource(R.drawable.icon_background_gray);
                break;
            case "Food":
                holder.categoryIcon.setImageResource(R.drawable.ic_food);
                holder.categoryIcon.setBackgroundResource(R.drawable.icon_background_gray);
                break;
            case "Games":
                holder.categoryIcon.setImageResource(R.drawable.ic_games);
                holder.categoryIcon.setBackgroundResource(R.drawable.icon_background_gray);
                break;
            case "Sports":
                holder.categoryIcon.setImageResource(R.drawable.ic_sports);
                holder.categoryIcon.setBackgroundResource(R.drawable.icon_background_gray);
                break;

            // Add more cases for other categories
            default:
                holder.categoryIcon.setImageResource(R.drawable.ic_category);
                holder.categoryIcon.setBackgroundResource(R.drawable.icon_background_gray);
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
