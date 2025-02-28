package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.R;
import com.example.splitaura.models.Contact;

import java.util.List;

// Adapter for displaying selected contacts in a horizontal list
public class SelectedContactsAdapter extends RecyclerView.Adapter<SelectedContactsAdapter.SelectedContactViewHolder> {

    private List<Contact> selectedContacts;

    public SelectedContactsAdapter(List<Contact> selectedContacts) {
        this.selectedContacts = selectedContacts;
    }

    @Override
    public SelectedContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_contact, parent, false);
        return new SelectedContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SelectedContactViewHolder holder, int position) {
        Contact contact = selectedContacts.get(position);
        holder.ivProfile.setImageResource(contact.profileImageRes);
    }

    @Override
    public int getItemCount() {
        return selectedContacts.size();
    }

    public static class SelectedContactViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;

        public SelectedContactViewHolder(View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
        }
    }
}
