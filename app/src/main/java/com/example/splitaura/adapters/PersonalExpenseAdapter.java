package com.example.splitaura.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.PersonalExpense;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class PersonalExpenseAdapter extends RecyclerView.Adapter<PersonalExpenseAdapter.PersonalExpenseViewHolder> {
    private final Context context;
    private final List<PersonalExpense> expenseList;
    private final OnExpenseClickListener listener;

    public PersonalExpenseAdapter(Context context, List<PersonalExpense> expenseList, OnExpenseClickListener listener) {
        this.context = context;
        this.expenseList = expenseList != null ? expenseList : new ArrayList<>();
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull PersonalExpenseViewHolder holder, int position) {
        PersonalExpense expense = expenseList.get(position);
        holder.expenseName.setText(expense.getTitle());
        String formattedAmount = String.format("₹ %.2f", expense.getAmount());
        holder.expenseAmount.setText(formattedAmount);

        // Use notes as the "paid by" description.
        String paidByText = (expense.getNotes() != null && !expense.getNotes().isEmpty()) ? expense.getNotes() : "Unknown";
        holder.expensePaidBy.setText(paidByText);

        // Format and set the expense date using Firestore Timestamp
        Timestamp ts = expense.getTimestamp();
        Date date = ts.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.expenseDate.setText(sdf.format(date));

        // Determine category: use first category from the list, default to "Other" if none exists
        String category = (expense.getCategories() == null || expense.getCategories().isEmpty()) ? "Other" : expense.getCategories().get(0);
        setCategoryIcon(holder.categoryIcon, category);

        // On item click, display the document id as a Toast message
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });
    }

    @NonNull
    @Override
    public PersonalExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new PersonalExpenseViewHolder(view);
    }

    public interface OnExpenseClickListener {
        void onExpenseClick(PersonalExpense expense);
    }

    private void setCategoryIcon(ImageView categoryIcon, String category) {
        int[] backgroundColors = {R.drawable.icon_background_blue, R.drawable.icon_background_green, R.drawable.icon_background_orange, R.drawable.icon_background_purple, R.drawable.icon_background_red, R.drawable.icon_background_teal, R.drawable.icon_background_yellow, R.drawable.icon_background_pink, R.drawable.icon_background_cyan};

        Random random = new Random();
        int randomIndex = random.nextInt(backgroundColors.length);
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

    public static class PersonalExpenseViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView expenseName;
        TextView expenseAmount;
        TextView expensePaidBy;
        TextView expenseDate;

        public PersonalExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            expenseName = itemView.findViewById(R.id.expense_name);
            expenseAmount = itemView.findViewById(R.id.expense_amount);
            expensePaidBy = itemView.findViewById(R.id.expense_paid_by);
            expenseDate = itemView.findViewById(R.id.expense_date);
        }
    }

    public void updateExpenses(List<PersonalExpense> newExpenses) {
        this.expenseList.clear();
        this.expenseList.addAll(newExpenses != null ? newExpenses : new ArrayList<>());
        notifyDataSetChanged();
    }
}
