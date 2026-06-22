package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Product;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class HomeSearchResultsAdapter extends RecyclerView.Adapter<HomeSearchResultsAdapter.VH> {

    public interface OnResultClickListener {
        void onResultClick(Product product);
    }

    private final Context context;
    private final List<Product> items = new ArrayList<>();
    private final OnResultClickListener listener;

    public HomeSearchResultsAdapter(Context context, OnResultClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Product> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_home_search_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = items.get(position);
        h.tvName.setText(p.getName());
        h.tvLoc.setText(p.getLocation());
        if (p.isOutOfStock()) {
            h.tvPrice.setText("Out of stock");
        } else {
            String price = p.getPrice();
            if (price != null && !price.startsWith("R")) {
                price = "R" + price;
            }
            h.tvPrice.setText(price);
        }
        h.tvCond.setText(p.getCondition());
        Glide.with(context).load(p.getImageUrl()).centerCrop().into(h.iv);
        h.itemView.setOnClickListener(v -> listener.onResultClick(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView iv;
        final TextView tvName;
        final TextView tvLoc;
        final TextView tvPrice;
        final TextView tvCond;

        VH(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.ivSearchGridImage);
            tvName = itemView.findViewById(R.id.tvSearchGridName);
            tvLoc = itemView.findViewById(R.id.tvSearchGridLoc);
            tvPrice = itemView.findViewById(R.id.tvSearchGridPrice);
            tvCond = itemView.findViewById(R.id.tvSearchGridCond);
        }
    }
}
