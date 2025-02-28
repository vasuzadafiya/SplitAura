package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.OweRelationship;

import java.util.List;

public class OweAdapter extends RecyclerView.Adapter<OweAdapter.OweViewHolder> {

    private List<OweRelationship> oweRelationships;
    private String currentUserName = "Vasu Zadaiya (You)"; // Hardcoded for simplicity; make dynamic if needed

    public OweAdapter(List<OweRelationship> oweRelationships) {
        this.oweRelationships = oweRelationships;
    }

    @NonNull
    @Override
    public OweViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.owe_item, parent, false);
        return new OweViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OweViewHolder holder, int position) {
        OweRelationship owe = oweRelationships.get(position);
        holder.debtorName.setText(owe.getDebtor());
        holder.creditorText.setText(owe.getCreditor());
        holder.oweAmount.setText(String.format("₹%.2f", owe.getAmount()));

        // Set up three-dot menu click listener
        holder.menuDots.setOnClickListener(v -> showPopupMenu(v, owe, position));
    }

    @Override
    public int getItemCount() {
        return oweRelationships != null ? oweRelationships.size() : 0;
    }

    public static class OweViewHolder extends RecyclerView.ViewHolder {
        TextView debtorName, oweText, creditorText, oweAmount, menuDots;

        public OweViewHolder(@NonNull View itemView) {
            super(itemView);
            debtorName = itemView.findViewById(R.id.debtor_name);
            oweText = itemView.findViewById(R.id.owe_text);
            creditorText = itemView.findViewById(R.id.creditor_text);
            oweAmount = itemView.findViewById(R.id.owe_amount);
            menuDots = itemView.findViewById(R.id.menu_dots);

            // Handle three-dot menu clicks (already handled in onBindViewHolder via showPopupMenu)
        }
    }

    public void updateOweRelationships(List<OweRelationship> newOweRelationships) {
        oweRelationships = newOweRelationships;
        notifyDataSetChanged();
    }

    private void showPopupMenu(View view, OweRelationship owe, int position) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        String debtor = owe.getDebtor();
        String creditor = owe.getCreditor();

        // Determine if the current user owes or is owed
        if (debtor.equals(currentUserName)) {
            // Current user owes money (debtor)
            popup.getMenu().add("Pay Now");
        } else if (creditor.equals(currentUserName)) {
            // Someone owes the current user (creditor)
            popup.getMenu().add("Request Payment");
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String action = item.getTitle().toString();
                switch (action) {
                    case "Pay Now":
                        Toast.makeText(view.getContext(), "Pay Now clicked for " + debtor + " owing ₹" + owe.getAmount(), Toast.LENGTH_SHORT).show();
                        // Add logic to handle payment (e.g., navigate to payment screen, update balance)
                        break;
                    case "Request Payment":
                        Toast.makeText(view.getContext(), "Request Payment clicked for " + creditor + " owing ₹" + owe.getAmount(), Toast.LENGTH_SHORT).show();
                        // Add logic to handle payment request (e.g., send notification, show dialog)
                        break;
                }
                return true;
            }
        });

        popup.show();
    }
}