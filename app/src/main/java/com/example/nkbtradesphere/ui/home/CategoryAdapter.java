package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Category;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final Context context;
    private final List<Category> categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(@NonNull Category category);
    }

    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category cat = categories.get(position);
        holder.tvName.setText(cat.getName().toUpperCase(Locale.US));

        Glide.with(context)
                .load(cat.getImageUrl())
                .centerCrop()
                .into(holder.ivImage);

        // Staggered entrance animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(20f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(position * 60L)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        // Tap: bounce + toast
        holder.itemView.setOnClickListener(v -> {
            v.animate()
                    .scaleX(1.18f).scaleY(1.18f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                    .start();
            if (listener != null) {
                listener.onCategoryClick(cat);
            } else {
                Toast.makeText(context, "Browsing " + cat.getName() + " →", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() { return categories.size(); }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName;

        CategoryViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivCategoryImage);
            tvName  = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}
