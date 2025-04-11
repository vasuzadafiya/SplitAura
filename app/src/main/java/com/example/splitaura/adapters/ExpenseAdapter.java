package com.example.splitaura.adapters;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.splitaura.ExpenseManager;
import com.example.splitaura.R;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private static final String GENERAL = "General";

    private final List<ExpenseManager.Expense> expenseList;
    private final Context context;
    private final OnExpenseActionListener actionListener;
    private final Map<String, String> userNamesCache = new HashMap<>();

    public ExpenseAdapter(Context context, OnExpenseActionListener listener) {
        this.context = context;
        this.expenseList = new ArrayList<>();
        this.actionListener = listener;
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
        holder.expenseAmount.setText(context.getString(R.string.amount_format, expense.getAmount()));
        holder.expensePaidBy.setText(context.getString(R.string.paid_by_format, expense.getPaidBy()));
        holder.expenseDate.setText(context.getString(R.string.date_format, expense.getDate()));

        setCategoryIcon(holder.categoryIcon, expense.getCategories() != null && !expense.getCategories().isEmpty() ? expense.getCategories().get(0) : "Other");

        holder.itemView.setOnClickListener(v -> showExpenseOptionsDialog(expense));
    }

    private void showExpenseOptionsDialog(ExpenseManager.Expense expense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogTheme);

        // Inflate custom view
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_details_expense, null);
        builder.setView(dialogView);

        // Initialize views
        TextView addedByView = dialogView.findViewById(R.id.dialog_expense_added_by);
        ImageView expenseImage = dialogView.findViewById(R.id.expense_image);
        String imageUrl = expense.getImageUrl();

        if (imageUrl != null) {
            expenseImage.setVisibility(View.VISIBLE);
            int radius = 40;

            // Create RequestOptions with both CenterCrop and RoundedCorners transformations.
            RequestOptions requestOptions = new RequestOptions()
                    .transform(new CenterCrop(), new RoundedCorners(radius));

            Glide.with(context)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .placeholder(R.mipmap.app_icon) // optional placeholder image
                    .into(expenseImage);

        } else {
            expenseImage.setVisibility(View.GONE);
        }

        // Set creator info if available
        if (expense.getAddedBy() != null && !expense.getAddedBy().equals("unknown")) {
            // Check cache first
            if (userNamesCache.containsKey(expense.getAddedBy())) {
                String creatorName = userNamesCache.get(expense.getAddedBy());
                addedByView.setText(context.getString(R.string.added_by_format, creatorName));
                addedByView.setVisibility(View.VISIBLE);
            } else {
                // Fetch name if not in cache
                fetchUserName(expense.getAddedBy(), name -> {
                    addedByView.setText(context.getString(R.string.added_by_format, name));
                    addedByView.setVisibility(View.VISIBLE);
                });
            }
        } else {
            addedByView.setVisibility(View.GONE);
        }
        ImageView icon = dialogView.findViewById(R.id.dialog_expense_icon);
        TextView title = dialogView.findViewById(R.id.dialog_expense_title);
        TextView amount = dialogView.findViewById(R.id.dialog_expense_amount);
        TextView paidBy = dialogView.findViewById(R.id.dialog_expense_paid_by);
        TextView splitBy = dialogView.findViewById(R.id.dialog_expense_split_by);
        TextView date = dialogView.findViewById(R.id.dialog_expense_date);
        TextView notes = dialogView.findViewById(R.id.dialog_expense_notes);
        TextView headNotes = dialogView.findViewById(R.id.head_notes);
        FlexboxLayout categoriesContainer = dialogView.findViewById(R.id.categories_container);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btn_update);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btn_delete);

        // Set data
        setCategoryIcon(icon, expense.getCategories() != null && !expense.getCategories().isEmpty() ?
                expense.getCategories().get(0) : GENERAL);
        title.setText(expense.getTitle());
        amount.setText(context.getString(R.string.amount_format, expense.getAmount()));
        paidBy.setText(expense.getPaidBy());
        splitBy.setText(expense.getSplitBy());
        date.setText(expense.getDate());

        // Handle notes visibility
        if (expense.getNotes() != null && !expense.getNotes().isEmpty()) {
            notes.setText(expense.getNotes());
            notes.setVisibility(View.VISIBLE);
            headNotes.setVisibility(View.VISIBLE);
        } else {
            notes.setVisibility(View.GONE);
            headNotes.setVisibility(View.GONE);
            dialogView.findViewById(R.id.dialog_expense_notes).setVisibility(View.GONE);
        }

        // Populate categories
        categoriesContainer.removeAllViews();
        if (expense.getCategories() != null && !expense.getCategories().isEmpty()) {
            for (String category : expense.getCategories()) {
                TextView chip = (TextView) LayoutInflater.from(context)
                        .inflate(R.layout.category_chip, categoriesContainer, false);
                chip.setText(category);
                categoriesContainer.addView(chip);
            }
        } else {
            categoriesContainer.setVisibility(View.GONE);
            dialogView.findViewById(R.id.categories_container).setVisibility(View.GONE);
        }

        // Create dialog
        AlertDialog dialog = builder.create();

        // Set button click listeners
        btnUpdate.setOnClickListener(v -> {
            dialog.dismiss();
            showUpdateConfirmationDialog(expense);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteConfirmationDialog(expense);
        });

        dialog.show();

        // Add animation
        dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
    }

    private void fetchUserName(String uid, OnNameFetchedListener listener) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("fname");
                        String lastName = documentSnapshot.getString("lname");
                        String fullName = (firstName != null ? firstName : "") +
                                (lastName != null ? " " + lastName : "");
                        fullName = fullName.trim();

                        if (!fullName.isEmpty()) {
                            userNamesCache.put(uid, fullName);
                            listener.onNameFetched(fullName);
                        } else {
                            listener.onNameFetched("User " + uid.substring(0, 6));
                        }
                    } else {
                        listener.onNameFetched("Unknown User");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user name", e);
                    listener.onNameFetched("User " + uid.substring(0, 6));
                });
    }

    private interface OnNameFetchedListener {
        void onNameFetched(String name);
    }

    private void showUpdateConfirmationDialog(ExpenseManager.Expense expense) {
        new MaterialAlertDialogBuilder(context, R.style.LightDialogTheme)
                .setTitle("Confirm Update")
                .setMessage("Are you sure you want to update this expense?")
                .setPositiveButton("Update", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onEditExpense(expense);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmationDialog(ExpenseManager.Expense expense) {
        new MaterialAlertDialogBuilder(context, R.style.LightDialogTheme)
                .setTitle("Delete Expense")
                .setMessage("This action cannot be undone. Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onDeleteExpense(expense);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setCategoryIcon(ImageView categoryIcon, String category) {
        int iconResId;
        int bgColorResId;

        switch (category != null ? category : GENERAL) {
            case GENERAL:
                iconResId = R.drawable.ic_general;
                bgColorResId = R.drawable.icon_background_teal;
                break;
            case "Games":
                iconResId = R.drawable.ic_games;
                bgColorResId = R.drawable.icon_background_blue;
                break;
            case "Sports":
                iconResId = R.drawable.ic_sports;
                bgColorResId = R.drawable.icon_background_orange;
                break;
            case "Music":
                iconResId = R.drawable.ic_music;
                bgColorResId = R.drawable.icon_background_purple;
                break;
            case "Movies":
                iconResId = R.drawable.ic_movies;
                bgColorResId = R.drawable.icon_background_red;
                break;
            case "Food":
                iconResId = R.drawable.ic_food;
                bgColorResId = R.drawable.icon_background_green;
                break;
            case "Liquor":
                iconResId = R.drawable.ic_liquor;
                bgColorResId = R.drawable.icon_background_pink;
                break;
            case "Grocery":
                iconResId = R.drawable.ic_grocery;
                bgColorResId = R.drawable.icon_background_orange;
                break;
            case "Shop":
                iconResId = R.drawable.ic_shop;
                bgColorResId = R.drawable.icon_background_cyan;
                break;
            default:
                iconResId = R.drawable.ic_category;
                bgColorResId = R.drawable.icon_background_teal;
        }

        categoryIcon.setImageResource(iconResId);
        categoryIcon.setBackgroundResource(bgColorResId);
    }

    public void updateExpenses(List<ExpenseManager.Expense> newExpenses) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ExpenseDiffCallback(expenseList, newExpenses));
        expenseList.clear();
        expenseList.addAll(newExpenses);
        result.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public ExpenseManager.Expense getExpenseAt(int position) {
        if (position >= 0 && position < expenseList.size()) {
            return expenseList.get(position);
        }
        return null;
    }

    public interface OnExpenseActionListener {
        void onEditExpense(ExpenseManager.Expense expense);

        void onDeleteExpense(ExpenseManager.Expense expense);
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView expenseName;
        TextView expensePaidBy;
        TextView expenseAmount;
        TextView expenseDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.category_icon);
            expenseName = itemView.findViewById(R.id.expense_name);
            expensePaidBy = itemView.findViewById(R.id.expense_paid_by);
            expenseAmount = itemView.findViewById(R.id.expense_amount);
            expenseDate = itemView.findViewById(R.id.expense_date);
        }
    }

    private static class ExpenseDiffCallback extends DiffUtil.Callback {
        private final List<ExpenseManager.Expense> oldList;
        private final List<ExpenseManager.Expense> newList;

        public ExpenseDiffCallback(List<ExpenseManager.Expense> oldList, List<ExpenseManager.Expense> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getExpenseId().equals(newList.get(newPos).getExpenseId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).equals(newList.get(newPos));
        }
    }
}