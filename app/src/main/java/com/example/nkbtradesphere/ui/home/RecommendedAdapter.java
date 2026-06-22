package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.util.RatingFormat;
import com.example.nkbtradesphere.util.PostedDateFormat;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.util.List;
import java.util.Locale;

public class RecommendedAdapter extends RecyclerView.Adapter<RecommendedAdapter.RecViewHolder> {

    private final Context context;
    private final List<Product> products;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public RecommendedAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_recommended, parent, false);
        return new RecViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecViewHolder holder, int position) {
        Product product = products.get(position);

        holder.tvPrice.setText(product.isOutOfStock() ? "Out of stock" : product.getPrice());
        holder.tvName.setText(product.getName());
        holder.tvDesc.setText(product.getDescription());
        holder.tvLocChip.setText(product.getLocation());
        holder.tvSellerName.setText(product.getSellerName());
        holder.tvSellerSub.setText(product.getLocation());
        holder.tvListedTime.setText(PostedDateFormat.displayDate(product.getListedTime()));
        String itemStar = RatingFormat.starLine(product.getItemAverageRating());
        if (TextUtils.isEmpty(itemStar)) {
            holder.tvItemAvgRating.setVisibility(View.GONE);
        } else {
            holder.tvItemAvgRating.setVisibility(View.VISIBLE);
            holder.tvItemAvgRating.setText(itemStar);
        }
        holder.tvChipCond.setText(product.getCondition().toUpperCase(Locale.US));
        holder.tvChipCat.setText(product.getCategory().toUpperCase(Locale.US));

        Glide.with(context)
                .load(product.getImageUrl())
                .centerCrop()
                .into(holder.ivImage);

        if (!TextUtils.isEmpty(product.getSellerImageUrl())) {
            Glide.with(context)
                    .load(product.getSellerImageUrl())
                    .transform(new CircleCrop())
                    .into(holder.ivSellerAvatar);
            holder.ivSellerAvatar.setVisibility(View.VISIBLE);
        } else {
            holder.ivSellerAvatar.setVisibility(View.GONE);
        }

        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(28f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(380)
                .setStartDelay(position * 70L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        holder.itemView.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.94f).scaleY(0.94f).setDuration(90)
                    .withEndAction(() ->
                            v.animate().scaleX(1f).scaleY(1f).setDuration(140).start())
                    .start();
            listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class RecViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivSellerAvatar;
        TextView tvSellerName;
        TextView tvSellerSub;
        TextView tvItemAvgRating;
        TextView tvListedTime;
        TextView tvLocChip;
        TextView tvPrice;
        TextView tvName;
        TextView tvDesc;
        TextView tvChipCond;
        TextView tvChipCat;

        RecViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivRecImage);
            ivSellerAvatar = itemView.findViewById(R.id.ivSellerAvatar);
            tvSellerName = itemView.findViewById(R.id.tvSellerName);
            tvSellerSub = itemView.findViewById(R.id.tvSellerSub);
            tvItemAvgRating = itemView.findViewById(R.id.tvItemAvgRating);
            tvListedTime = itemView.findViewById(R.id.tvListedTime);
            tvLocChip = itemView.findViewById(R.id.tvLocChip);
            tvPrice = itemView.findViewById(R.id.tvRecPrice);
            tvName = itemView.findViewById(R.id.tvRecName);
            tvDesc = itemView.findViewById(R.id.tvRecDesc);
            tvChipCond = itemView.findViewById(R.id.tvChipCond);
            tvChipCat = itemView.findViewById(R.id.tvChipCat);
        }
    }
}
