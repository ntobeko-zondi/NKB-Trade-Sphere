package com.example.nkbtradesphere.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.ui.home.CategoryUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class InterestsAdapter extends RecyclerView.Adapter<InterestsAdapter.InterestViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private final List<CategoryUtils.CategoryInfo> categories;
    private final Set<String> selectedCategoryIds = new LinkedHashSet<>();
    private final OnSelectionChangedListener selectionChangedListener;

    public InterestsAdapter(@NonNull List<CategoryUtils.CategoryInfo> categories,
                            @NonNull OnSelectionChangedListener listener) {
        this.categories = categories;
        this.selectionChangedListener = listener;
    }

    @NonNull
    @Override
    public InterestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interest_category, parent, false);
        return new InterestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InterestViewHolder holder, int position) {
        CategoryUtils.CategoryInfo info = categories.get(position);
        boolean isSelected = selectedCategoryIds.contains(info.id);
        holder.bind(info, isSelected);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @NonNull
    public List<String> getSelectedCategoryIds() {
        return new ArrayList<>(selectedCategoryIds);
    }

    class InterestViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEmoji;
        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final View root;

        InterestViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            tvEmoji = itemView.findViewById(R.id.tvInterestEmoji);
            tvTitle = itemView.findViewById(R.id.tvInterestTitle);
            tvSubtitle = itemView.findViewById(R.id.tvInterestSubtitle);
        }

        void bind(@NonNull CategoryUtils.CategoryInfo info, boolean isSelected) {
            tvEmoji.setText(info.emoji);
            tvTitle.setText(info.title);
            tvSubtitle.setText(isSelected ? "Selected" : "Tap to select");
            root.setActivated(isSelected);

            root.setOnClickListener(v -> {
                if (selectedCategoryIds.contains(info.id)) {
                    selectedCategoryIds.remove(info.id);
                } else {
                    selectedCategoryIds.add(info.id);
                }
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position);
                }
                selectionChangedListener.onSelectionChanged(selectedCategoryIds.size());
            });
        }
    }
}
