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
import com.example.nkbtradesphere.data.model.Product;

import java.util.List;

public class CategoryListingGridAdapter extends RecyclerView.Adapter<CategoryListingGridAdapter.VH> {

    interface OnProductTap {
        void onTap(@NonNull Product product);
    }

    private final Context context;
    private final List<Product> products;
    private final OnProductTap onProductTap;

    public CategoryListingGridAdapter(Context context, List<Product> products, OnProductTap onProductTap) {
        this.context = context;
        this.products = products;
        this.onProductTap = onProductTap;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_category_listing_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = products.get(position);
        Glide.with(context).load(p.getImageUrl()).centerCrop().into(h.ivImage);
        h.tvName.setText(p.getName());
        h.tvLocation.setText(TextUtilsCompat.safeLocation(p.getLocation()));
        h.tvPrice.setText(p.isOutOfStock() ? "Out of stock" : p.getPrice());
        h.tvCondition.setText(TextUtilsCompat.safeCondition(p.getCondition()));
        h.itemView.setOnClickListener(v -> onProductTap.onTap(p));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvName;
        final TextView tvLocation;
        final TextView tvPrice;
        final TextView tvCondition;

        VH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivCategoryListingImage);
            tvName = itemView.findViewById(R.id.tvCategoryListingName);
            tvLocation = itemView.findViewById(R.id.tvCategoryListingLocation);
            tvPrice = itemView.findViewById(R.id.tvCategoryListingPrice);
            tvCondition = itemView.findViewById(R.id.tvCategoryListingCondition);
        }
    }

    private static class TextUtilsCompat {
        static String safeLocation(String s) {
            if (s == null || s.trim().isEmpty()) return "• SA";
            return "◉ " + s;
        }

        static String safeCondition(String s) {
            if (s == null || s.trim().isEmpty()) return "";
            return s.toUpperCase();
        }
    }
}

