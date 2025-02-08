package com.example.splitaura.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splitaura.OnboardingItem;
import com.example.splitaura.R;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.onboarding_item_layout, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleText;
        private final TextView descriptionText;

        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_boarding);
            titleText = itemView.findViewById(R.id.title_boarding);
            descriptionText = itemView.findViewById(R.id.description_bording);
        }

        void bind(OnboardingItem item) {
            imageView.setImageResource(item.getImageResId());
            titleText.setText(item.getTitle());
            descriptionText.setText(item.getDescription());
        }
    }
}
