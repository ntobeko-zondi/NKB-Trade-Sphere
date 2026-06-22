package com.example.nkbtradesphere.ui.product;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.network.ApiClient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

public class CheckoutActivity extends AppCompatActivity {
    public static final String EXTRA_REMAINING_QUANTITY = "remaining_quantity";

    private int listingId;
    private String sellerId;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        String itemName = getIntent().getStringExtra(ProductDetailActivity.EXTRA_NAME);
        String itemPrice = getIntent().getStringExtra(ProductDetailActivity.EXTRA_PRICE);
        String itemImage = CheckoutImageStore.get(
                getIntent().getStringExtra(ProductDetailActivity.EXTRA_CHECKOUT_IMAGE_TOKEN));
        if (TextUtils.isEmpty(itemImage)) {
            itemImage = getIntent().getStringExtra(ProductDetailActivity.EXTRA_IMAGE);
        }
        listingId = getIntent().getIntExtra(ProductDetailActivity.EXTRA_LISTING_ID, -1);
        sellerId = getIntent().getStringExtra(ProductDetailActivity.EXTRA_SELLER_ID);

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
        ApiClient.initialize(this);

        TextView tvItemName = findViewById(R.id.tvCheckoutItemName);
        TextView tvItemPrice = findViewById(R.id.tvCheckoutItemPrice);
        ImageView ivItem = findViewById(R.id.ivCheckoutItem);
        MaterialButton btnPay = findViewById(R.id.btnCompletePurchase);
        ImageButton btnBack = findViewById(R.id.btnCheckoutBack);

        tvItemName.setText(TextUtils.isEmpty(itemName) ? "Selected Item" : itemName);
        tvItemPrice.setText(TextUtils.isEmpty(itemPrice) ? "R0" : itemPrice);
        loadCheckoutImage(ivItem, itemImage);
        if (TextUtils.isEmpty(itemImage) && listingId > 0) {
            fetchCheckoutImageFromApi(ivItem);
        }

        btnBack.setOnClickListener(v -> onBackPressed());
        btnPay.setOnClickListener(v -> completePurchase());
    }


    private void loadCheckoutImage(ImageView imageView, String imageValue) {
        String normalized = normalizeImageForGlide(firstImageFromPayload(imageValue));
        if (TextUtils.isEmpty(normalized)) {
            imageView.setImageResource(R.drawable.baseline_error_24);
            return;
        }
        Glide.with(this)
                .load(normalized)
                .placeholder(R.drawable.baseline_error_24)
                .error(R.drawable.baseline_error_24)
                .centerCrop()
                .into(imageView);
    }

    private void fetchCheckoutImageFromApi(ImageView imageView) {
        ApiClient.getListing(listingId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                org.json.JSONObject listing = response.optJSONObject("listing");
                if (listing == null) {
                    return;
                }
                String serverImage = listing.optString("image_url", "");
                runOnUiThread(() -> loadCheckoutImage(imageView, serverImage));
            }

            @Override
            public void onError(String error) {
                // Keep the placeholder. This should not block checkout.
            }
        });
    }

    private static String firstImageFromPayload(String rawImage) {
        if (TextUtils.isEmpty(rawImage)) {
            return "";
        }
        String value = rawImage.trim();
        if (value.contains("|")) {
            String[] parts = value.split("\\|");
            for (String part : parts) {
                if (!TextUtils.isEmpty(part)) {
                    return part.trim();
                }
            }
            return "";
        }
        return value;
    }

    private static String normalizeImageForGlide(String rawImage) {
        if (TextUtils.isEmpty(rawImage)) {
            return "";
        }
        String value = rawImage.trim();
        if (value.startsWith("http") || value.startsWith("content:")
                || value.startsWith("file:") || value.startsWith("data:image")) {
            return value;
        }
        // Listings uploaded through the hosted API are stored as raw base64 strings.
        if (value.length() > 120) {
            return "data:image/jpeg;base64," + value;
        }
        return value;
    }

    private void notifyPurchaseResult(int remainingQuantity) {
        Intent data = new Intent();
        data.putExtra(EXTRA_REMAINING_QUANTITY, remainingQuantity);
        setResult(Activity.RESULT_OK, data);
    }

    private void completePurchase() {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
            return;
        }
        if (listingId <= 0) {
            Toast.makeText(this, "This listing is unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.getListing(listingId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                org.json.JSONObject listing = response.optJSONObject("listing");
                if (listing == null) {
                    Toast.makeText(CheckoutActivity.this, "This listing is unavailable", Toast.LENGTH_SHORT).show();
                    return;
                }

                sellerId = listing.optString("seller_id", sellerId);
                if (!TextUtils.isEmpty(sellerId) && currentUserId.equalsIgnoreCase(sellerId)) {
                    Toast.makeText(CheckoutActivity.this, "You cannot buy your own listing", Toast.LENGTH_SHORT).show();
                    return;
                }

                int currentQuantity = listing.optInt("quantity", 0);
                if (currentQuantity <= 0) {
                    notifyPurchaseResult(0);
                    Toast.makeText(CheckoutActivity.this, "This listing is out of stock", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                ApiClient.purchaseListing(listingId, currentUserId, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject response) {
                        int remainingQuantity = response.optInt("quantity", 0);
                        boolean purchaseCompleted = response.optBoolean("purchase_completed", true);
                        if (!purchaseCompleted) {
                            notifyPurchaseResult(remainingQuantity);
                            Toast.makeText(CheckoutActivity.this, "This listing is out of stock", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        notifyPurchaseResult(remainingQuantity);
                        if (remainingQuantity <= 0) {
                            Toast.makeText(CheckoutActivity.this, "Purchase completed. This item is now out of stock.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CheckoutActivity.this, "Purchase completed. Remaining quantity: " + remainingQuantity, Toast.LENGTH_SHORT).show();
                        }
                        showRatingDialog();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CheckoutActivity.this, "Purchase failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CheckoutActivity.this, "Purchase failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRatingDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final float[] selectedRating = {5f};

        LinearLayout starsLayout = new LinearLayout(this);
        starsLayout.setOrientation(LinearLayout.HORIZONTAL);
        starsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView[] stars = new TextView[5];
        for (int i = 0; i < 5; i++) {
            final int starValue = i + 1;
            TextView star = new TextView(this);
            star.setText("★");
            star.setTextSize(36);
            star.setPadding(6, 0, 6, 0);
            star.setContentDescription(starValue + " star rating");
            star.setOnClickListener(v -> {
                selectedRating[0] = starValue;
                updateStarViews(stars, starValue);
            });
            stars[i] = star;
            starsLayout.addView(star);
        }
        updateStarViews(stars, 5);

        EditText comment = new EditText(this);
        comment.setHint("Optional comment");

        layout.addView(starsLayout);
        layout.addView(comment);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Rate your purchase")
                .setMessage("Tap 1 to 5 stars. Your rating updates this listing average and the seller profile average.")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Submit", (dialog, which) -> {
                    float value = Math.max(1f, Math.min(5f, selectedRating[0]));
                    submitRating(value, comment.getText().toString().trim());
                }).show();
    }

    private void updateStarViews(TextView[] stars, int selectedRating) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setText(i < selectedRating ? "★" : "☆");
        }
    }

    private void submitRating(float value, String comment) {
        ApiClient.rateListing(currentUserId, listingId, value, comment,
                new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject response) {
                        if (!TextUtils.isEmpty(sellerId)) {
                            ApiClient.addRating(currentUserId, sellerId, value, "Auto from purchase", new ApiClient.ApiCallback() {
                                @Override
                                public void onSuccess(org.json.JSONObject response) { }
                                @Override
                                public void onError(String error) { }
                            });
                        }
                        Toast.makeText(CheckoutActivity.this, "Purchase completed and rating saved", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CheckoutActivity.this, "Purchase completed, but rating was not saved: " + error, Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }
}
