package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.network.ApiClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nkbtradesphere.util.WishlistAnimator;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> products;
    private final OnProductClickListener listener;
    private final String currentUserId;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
        SharedPreferences prefs = context.getSharedPreferences(AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        this.currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);

        holder.tvPrice.setText(product.isOutOfStock() ? "Out of stock" : product.getPrice());
        holder.tvName.setText(product.getName());
        holder.tvLocation.setText(product.getLocation());

        Glide.with(context)
                .load(product.getImageUrl())
                .centerCrop()
                .into(holder.ivImage);

        // Badge visibility
        if (product.getBadge() != null && !product.getBadge().isEmpty()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(product.getBadge());
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        // Entrance animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(30f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(position * 80L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Tap card → open detail sheet
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                    .start();
            listener.onProductClick(product);
        });

        int listingId = product.getListingId();
        final boolean isOwnListing = !TextUtils.isEmpty(currentUserId)
                && !TextUtils.isEmpty(product.getSellerId())
                && currentUserId.equalsIgnoreCase(product.getSellerId());
        final boolean[] wishlisted = { false };
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        ApiClient.initialize(context.getApplicationContext());

        WishlistAnimator.applyProductCard(holder.ivWishlist, false);
        holder.ivWishlist.setAlpha(isOwnListing ? 0.45f : 1f);

        if (!TextUtils.isEmpty(currentUserId) && listingId > 0 && !isOwnListing) {
            ApiClient.isSaved(currentUserId, listingId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    boolean saved = response.optBoolean("is_saved", false);
                    mainHandler.post(() -> {
                        if (holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                            wishlisted[0] = saved;
                            WishlistAnimator.applyProductCard(holder.ivWishlist, saved);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    // Keep the icon unselected. The user can still retry by tapping it.
                }
            });
        }

        holder.ivWishlist.setOnClickListener(v -> {
            if (TextUtils.isEmpty(currentUserId)) {
                Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listingId <= 0) {
                Toast.makeText(context, "This item cannot be saved", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isOwnListing) {
                Toast.makeText(context, "You cannot add your own products to the wishlist", Toast.LENGTH_SHORT).show();
                return;
            }

            holder.ivWishlist.setEnabled(false);
            ApiClient.ApiCallback callback = new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    mainHandler.post(() -> {
                        holder.ivWishlist.setEnabled(true);
                        wishlisted[0] = !wishlisted[0];
                        WishlistAnimator.applyProductCard(holder.ivWishlist, wishlisted[0]);
                        WishlistAnimator.playToggleCelebration(holder.ivWishlist, wishlisted[0]);
                        Toast.makeText(context,
                                wishlisted[0] ? "Saved to wishlist" : "Removed from wishlist",
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    mainHandler.post(() -> {
                        holder.ivWishlist.setEnabled(true);
                        Toast.makeText(context, "Wishlist failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            };

            if (wishlisted[0]) {
                ApiClient.removeSavedItem(currentUserId, listingId, callback);
            } else {
                ApiClient.saveItem(currentUserId, listingId, callback);
            }
        });
    }

    @Override
    public int getItemCount() { return products.size(); }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, ivWishlist;
        TextView tvPrice, tvName, tvLocation, tvBadge;

        ProductViewHolder(View itemView) {
            super(itemView);
            ivImage    = itemView.findViewById(R.id.ivProductImage);
            ivWishlist = itemView.findViewById(R.id.ivWishlistProduct);
            tvPrice    = itemView.findViewById(R.id.tvPrice);
            tvName     = itemView.findViewById(R.id.tvProductName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvBadge    = itemView.findViewById(R.id.tvBadge);
        }
    }
}
