package com.example.nkbtradesphere.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nkbtradesphere.R;

import java.util.List;
import java.util.Locale;

public class AllCategoriesAdapter extends RecyclerView.Adapter<AllCategoriesAdapter.VH> {

    interface OnCategoryTap {
        void onTap(@NonNull AllCategoriesActivity.CategoryCardItem item);
    }

    private final Context context;
    private final List<AllCategoriesActivity.CategoryCardItem> items;
    private final OnCategoryTap onCategoryTap;

    public AllCategoriesAdapter(Context context,
                                List<AllCategoriesActivity.CategoryCardItem> items,
                                OnCategoryTap onCategoryTap) {
        this.context = context;
        this.items = items;
        this.onCategoryTap = onCategoryTap;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_all_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AllCategoriesActivity.CategoryCardItem item = items.get(position);
        Glide.with(context).load(item.imageUrl).centerCrop().into(h.ivImage);
        h.tvName.setText(item.emoji + " " + item.title);
        h.tvCount.setText(String.format(Locale.US, "%d %s", item.itemCount, item.itemCount == 1 ? "ITEM" : "ITEMS"));
        h.itemView.setOnClickListener(v -> onCategoryTap.onTap(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvName;
        final TextView tvCount;

        VH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivAllCategoryImage);
            tvName = itemView.findViewById(R.id.tvAllCategoryName);
            tvCount = itemView.findViewById(R.id.tvAllCategoryCount);
        }
    }
}

