package com.example.splitaura;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private List<OnboardingItem> onboardingItems;
    private OnboardingListener listener;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.onboarding_item_layout, parent, false);
        return new OnboardingViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        OnboardingItem item = onboardingItems.get(position);

        holder.image.setImageResource(item.getImage());
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());

        // Handle "Next" button
        holder.nextButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNextClicked(position);
            }
        });

        holder.skipButton.setOnClickListener(v -> listener.onSkipClicker(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    public void setOnboardingListener(OnboardingListener listener) {
        this.listener = listener;
    }

    public interface OnboardingListener {
        void onNextClicked(int position);

        void onSkipClicker(int position);
    }

    public static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView description;
        Button nextButton;
        Button skipButton;

        public OnboardingViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_boarding);
            title = itemView.findViewById(R.id.title_boarding);
            description = itemView.findViewById(R.id.description_bording);
            nextButton = itemView.findViewById(R.id.next_button);
            skipButton = itemView.findViewById(R.id.skip_btn);
        }
    }
}
