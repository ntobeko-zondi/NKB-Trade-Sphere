package com.example.nkbtradesphere.ui.product;

import com.example.nkbtradesphere.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.Collections;
import java.util.List;

/**
 * Full-bleed pages for {@link androidx.viewpager2.widget.ViewPager2} on the product detail screen.
 */
public class ProductDetailCarouselAdapter extends RecyclerView.Adapter<ProductDetailCarouselAdapter.PageHolder> {

    private final List<String> urls;

    public ProductDetailCarouselAdapter(@NonNull List<String> urls) {
        if (urls.isEmpty()) {
            this.urls = Collections.singletonList("");
        } else {
            this.urls = urls;
        }
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_detail_carousel_page, parent, false);
        return new PageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.bind(urls.get(position));
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        private final ImageView image;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivCarouselPage);
            itemView.setLongClickable(false);
            itemView.setHapticFeedbackEnabled(false);
            itemView.setOnLongClickListener(v -> true);
            image.setLongClickable(false);
            image.setHapticFeedbackEnabled(false);
            image.setOnLongClickListener(v -> true);
        }

        void bind(String url) {
            if (url == null || url.isEmpty()) {
                image.setImageDrawable(null);
                return;
            }
            Glide.with(image)
                    .load(url)
                    .centerCrop()
                    .into(image);
        }
    }
}
