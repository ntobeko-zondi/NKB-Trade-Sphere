package com.example.nkbtradesphere;

import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.database.DatabaseHelper;
import com.example.nkbtradesphere.ui.product.ProductDetailActivity;
import com.example.nkbtradesphere.util.WishlistAnimator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavedItemsActivity extends AppCompatActivity {

    private RecyclerView rvSavedItems;
    private LinearLayout emptyState;
    private TextView tvSavedSubtitle;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;
    private String currentUserId;
    private final List<DatabaseHelper.ListingData> savedListings = new ArrayList<>();
    private SavedItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_items);

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");

        rvSavedItems = findViewById(R.id.rvSavedItems);
        emptyState = findViewById(R.id.empty_state);
        tvSavedSubtitle = findViewById(R.id.tvSavedSubtitle);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        ImageButton btnBack = findViewById(R.id.btnBack);

        rvSavedItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedItemsAdapter();
        rvSavedItems.setAdapter(adapter);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        updateSubtitleForCount(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedItems();
    }

    private void updateSubtitleForCount(int count) {
        if (TextUtils.isEmpty(currentUserId)) {
            tvSavedSubtitle.setText(R.string.saved_subtitle_empty);
            return;
        }
        if (count <= 0) {
            tvSavedSubtitle.setText(R.string.saved_subtitle_empty);
        } else {
            tvSavedSubtitle.setText(getString(R.string.saved_subtitle_items, count));
        }
    }

    private void showSignedOutEmpty() {
        tvEmptyTitle.setText(R.string.saved_empty_title);
        tvEmptyMessage.setText(R.string.saved_empty_sign_in);
    }

    private void showDefaultEmpty() {
        tvEmptyTitle.setText(R.string.saved_empty_title);
        tvEmptyMessage.setText(R.string.saved_empty_hint);
    }

    private void loadSavedItems() {
        if (TextUtils.isEmpty(currentUserId)) {
            showSignedOutEmpty();
            savedListings.clear();
            adapter.notifyDataSetChanged();
            emptyState.setVisibility(View.VISIBLE);
            rvSavedItems.setVisibility(View.GONE);
            updateSubtitleForCount(0);
            return;
        }

        new Thread(() -> {
            try {
                DatabaseHelper db = DatabaseHelper.getInstance(SavedItemsActivity.this);
                List<DatabaseHelper.ListingData> items = db.getSavedItems(currentUserId);
                runOnUiThread(() -> {
                    savedListings.clear();
                    savedListings.addAll(items);
                    adapter.notifyDataSetChanged();
                    updateSubtitleForCount(savedListings.size());
                    if (savedListings.isEmpty()) {
                        showDefaultEmpty();
                        emptyState.setVisibility(View.VISIBLE);
                        rvSavedItems.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        rvSavedItems.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e("SavedItemsActivity", "Error loading items: " + e.getMessage());
            }
        }).start();
    }

    private static Product listingToProduct(DatabaseHelper db, DatabaseHelper.ListingData listing) {
        String sellerDisplay = listing.sellerId != null ? listing.sellerId : "";
        String sellerProfileRating = "0.0";
        if (!TextUtils.isEmpty(listing.sellerId)) {
            DatabaseHelper.UserData u = db.getUserByEmail(listing.sellerId);
            if (u != null) {
                if (!TextUtils.isEmpty(u.fullName)) {
                    sellerDisplay = u.fullName;
                }
                sellerProfileRating = String.format(Locale.US, "%.1f", u.rating);
            }
        }
        String rawPrice = listing.price == null ? "" : listing.price.trim();
        String priceDisplay = listing.quantity <= 0
                ? "Out of stock"
                : (rawPrice.startsWith("R") ? rawPrice : "R" + rawPrice);
        String itemAvg = String.format(Locale.US, "%.1f", listing.rating);
        String badge = listing.quantity <= 0 ? "OUT OF STOCK" : "";
        return new Product(
                listing.listingId,
                listing.sellerId == null ? "" : listing.sellerId,
                listing.title == null ? "" : listing.title,
                priceDisplay,
                "",
                listing.imageUrl == null ? "" : listing.imageUrl,
                listing.description == null ? "" : listing.description,
                sellerDisplay,
                sellerProfileRating,
                listing.condition == null ? "" : listing.condition,
                listing.category == null ? "" : listing.category,
                badge,
                itemAvg,
                "",
                "",
                listing.imageGallery,
                listing.quantity,
                listing.status == null ? "active" : listing.status
        );
    }

    private class SavedItemsAdapter extends RecyclerView.Adapter<SavedItemsAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saved_listing, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DatabaseHelper.ListingData listing = savedListings.get(position);
            holder.tvTitle.setText(listing.title);
            String price = listing.quantity <= 0
                    ? "Out of stock"
                    : (listing.price != null && listing.price.startsWith("R")
                    ? listing.price
                    : "R" + (listing.price == null ? "0" : listing.price));
            holder.tvPrice.setText(price);
            holder.tvCategory.setText(listing.category != null ? listing.category : "—");
            holder.tvRating.setText("★ " + String.format(Locale.US, "%.1f", listing.rating));

            int cornerPx = (int) (12f * holder.itemView.getResources().getDisplayMetrics().density);
            RequestOptions opts = RequestOptions.bitmapTransform(new RoundedCorners(cornerPx)).centerCrop();
            String img = listing.imageUrl;
            if (TextUtils.isEmpty(img) && listing.imageGallery != null && !listing.imageGallery.isEmpty()) {
                img = listing.imageGallery.get(0);
            }
            Glide.with(SavedItemsActivity.this).clear(holder.ivThumb);
            if (!TextUtils.isEmpty(img)) {
                Glide.with(SavedItemsActivity.this)
                        .load(img)
                        .apply(opts)
                        .into(holder.ivThumb);
            } else {
                holder.ivThumb.setImageResource(R.drawable.ic_home);
            }

            holder.itemView.setOnClickListener(v -> {
                DatabaseHelper db = DatabaseHelper.getInstance(SavedItemsActivity.this);
                Product p = listingToProduct(db, listing);
                startActivity(ProductDetailActivity.newIntent(SavedItemsActivity.this, p));
            });

            holder.btnRemove.setOnClickListener(v -> {
                WishlistAnimator.playToggleCelebration(v, false);
                DatabaseHelper db = DatabaseHelper.getInstance(SavedItemsActivity.this);
                if (!db.removeSavedItem(currentUserId, listing.listingId)) {
                    Toast.makeText(SavedItemsActivity.this, "Could not update wishlist", Toast.LENGTH_SHORT).show();
                    return;
                }
                int idx = -1;
                for (int i = 0; i < savedListings.size(); i++) {
                    if (savedListings.get(i).listingId == listing.listingId) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0) {
                    savedListings.remove(idx);
                    notifyItemRemoved(idx);
                    updateSubtitleForCount(savedListings.size());
                    if (savedListings.isEmpty()) {
                        showDefaultEmpty();
                        emptyState.setVisibility(View.VISIBLE);
                        rvSavedItems.setVisibility(View.GONE);
                    }
                }
                Toast.makeText(SavedItemsActivity.this, R.string.saved_removed_toast, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return savedListings.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView ivThumb;
            final TextView tvTitle;
            final TextView tvPrice;
            final TextView tvCategory;
            final TextView tvRating;
            final ImageButton btnRemove;

            VH(View itemView) {
                super(itemView);
                ivThumb = itemView.findViewById(R.id.ivSavedThumb);
                tvTitle = itemView.findViewById(R.id.tvSavedTitle);
                tvPrice = itemView.findViewById(R.id.tvSavedPrice);
                tvCategory = itemView.findViewById(R.id.tvSavedCategory);
                tvRating = itemView.findViewById(R.id.tvSavedRating);
                btnRemove = itemView.findViewById(R.id.btnRemoveSaved);
            }
        }
    }
}
