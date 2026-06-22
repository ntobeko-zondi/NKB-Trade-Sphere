package com.example.nkbtradesphere.ui.product;

import com.example.nkbtradesphere.ConversationDetailActivity;
import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.util.AvatarUtils;
import com.example.nkbtradesphere.util.PostedDateFormat;
import com.example.nkbtradesphere.util.WishlistAnimator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PRICE = "price";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_IMAGE = "imageUrl";
    public static final String EXTRA_DESC = "description";
    public static final String EXTRA_SELLER = "sellerName";
    public static final String EXTRA_RATING = "sellerRating";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_BADGE = "badge";
    public static final String EXTRA_LISTED_TIME = "listedTime";
    public static final String EXTRA_ITEM_AVG_RATING = "itemAvgRating";
    public static final String EXTRA_SELLER_IMAGE = "sellerImageUrl";
    public static final String EXTRA_GALLERY = "galleryImageUrls";
    public static final String EXTRA_LISTING_ID = "listingId";
    public static final String EXTRA_SELLER_ID = "sellerId";
    public static final String EXTRA_QUANTITY = "quantity";
    public static final String EXTRA_PRODUCT_TOKEN = "productToken";
    public static final String EXTRA_CHECKOUT_IMAGE_TOKEN = "checkoutImageToken";

    private static final int THUMB_COUNT = 4;

    private int mListingId = -1;
    private int mDisplayedQuantity;
    private String mItemName;
    private String mPriceRaw;
    private String mPrimaryImage;
    private String mCheckoutImage;
    private String mSellerId;
    private MaterialButton mBtnBuy;
    private TextView mTvMetaLoc;
    private TextView mTvPrice;

    private final ActivityResultLauncher<Intent> checkoutLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                int rem = result.getData().getIntExtra(CheckoutActivity.EXTRA_REMAINING_QUANTITY, Integer.MIN_VALUE);
                if (rem != Integer.MIN_VALUE) {
                    mDisplayedQuantity = rem;
                    applyQuantityState(mDisplayedQuantity);
                }
            });

    public static Intent newIntent(@NonNull Context context, @NonNull Product product) {
        Intent i = new Intent(context, ProductDetailActivity.class);

        // Do not put image/base64 gallery strings in Intent extras. Large images can
        // exceed Android's Binder transaction limit and crash the app when tapping a card.
        i.putExtra(EXTRA_PRODUCT_TOKEN, ProductNavigationStore.put(product));

        // Keep only small fallback fields in case Android recreates the activity later.
        i.putExtra(EXTRA_NAME, product.getName());
        i.putExtra(EXTRA_PRICE, product.getPrice());
        i.putExtra(EXTRA_LOCATION, product.getLocation());
        i.putExtra(EXTRA_DESC, product.getDescription());
        i.putExtra(EXTRA_SELLER, product.getSellerName());
        i.putExtra(EXTRA_RATING, product.getSellerRating());
        i.putExtra(EXTRA_CONDITION, product.getCondition());
        i.putExtra(EXTRA_CATEGORY, product.getCategory());
        i.putExtra(EXTRA_BADGE, product.getBadge());
        i.putExtra(EXTRA_LISTED_TIME, product.getListedTime());
        i.putExtra(EXTRA_ITEM_AVG_RATING, product.getItemAverageRating());
        i.putExtra(EXTRA_SELLER_ID, product.getSellerId());
        i.putExtra(EXTRA_LISTING_ID, product.getListingId());
        i.putExtra(EXTRA_QUANTITY, product.getQuantity());
        return i;
    }

    private final int[] thumbSlotIds = {
            R.id.thumbSlot0, R.id.thumbSlot1, R.id.thumbSlot2, R.id.thumbSlot3
    };
    private final int[] thumbImageIds = {
            R.id.ivThumb0, R.id.ivThumb1, R.id.ivThumb2, R.id.ivThumb3
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Intent intent = getIntent();
        Product selectedProduct = ProductNavigationStore.get(intent.getStringExtra(EXTRA_PRODUCT_TOKEN));

        String name;
        String price;
        String location;
        String primaryImage;
        ArrayList<String> galleryExtra;
        String description;
        String sellerName;
        String rating;
        String condition;
        String category;
        String badge;
        String listedTime;
        String itemAvgRating;
        String sellerImageUrl;

        if (selectedProduct != null) {
            name = selectedProduct.getName();
            price = selectedProduct.getPrice();
            location = selectedProduct.getLocation();
            primaryImage = selectedProduct.getImageUrl();
            galleryExtra = new ArrayList<>(selectedProduct.getImageGallery());
            description = selectedProduct.getDescription();
            sellerName = selectedProduct.getSellerName();
            rating = selectedProduct.getSellerRating();
            condition = selectedProduct.getCondition();
            category = selectedProduct.getCategory();
            badge = selectedProduct.getBadge();
            listedTime = selectedProduct.getListedTime();
            itemAvgRating = selectedProduct.getItemAverageRating();
            sellerImageUrl = selectedProduct.getSellerImageUrl();
            mSellerId = selectedProduct.getSellerId();
            mListingId = selectedProduct.getListingId();
            mDisplayedQuantity = selectedProduct.getQuantity();
        } else {
            name = intent.getStringExtra(EXTRA_NAME);
            price = intent.getStringExtra(EXTRA_PRICE);
            location = intent.getStringExtra(EXTRA_LOCATION);
            primaryImage = intent.getStringExtra(EXTRA_IMAGE);
            galleryExtra = intent.getStringArrayListExtra(EXTRA_GALLERY);
            description = intent.getStringExtra(EXTRA_DESC);
            sellerName = intent.getStringExtra(EXTRA_SELLER);
            rating = intent.getStringExtra(EXTRA_RATING);
            condition = intent.getStringExtra(EXTRA_CONDITION);
            category = intent.getStringExtra(EXTRA_CATEGORY);
            badge = intent.getStringExtra(EXTRA_BADGE);
            listedTime = intent.getStringExtra(EXTRA_LISTED_TIME);
            itemAvgRating = intent.getStringExtra(EXTRA_ITEM_AVG_RATING);
            sellerImageUrl = intent.getStringExtra(EXTRA_SELLER_IMAGE);
            mSellerId = intent.getStringExtra(EXTRA_SELLER_ID);
            mListingId = intent.getIntExtra(EXTRA_LISTING_ID, -1);
            mDisplayedQuantity = intent.getIntExtra(EXTRA_QUANTITY, 0);
        }

        final List<String> imageUrls = resolveDetailImageUrls(primaryImage, galleryExtra);
        if (mSellerId == null) {
            mSellerId = "";
        }
        mItemName = name;
        mPriceRaw = price;
        mPrimaryImage = primaryImage;
        mCheckoutImage = !imageUrls.isEmpty() ? imageUrls.get(0) : primaryImage;

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        String currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");

        ImageButton btnBack = findViewById(R.id.btnDetailBack);
        ImageView ivSeller = findViewById(R.id.ivDetailSellerAvatar);
        TextView tvSellerName = findViewById(R.id.tvDetailSellerName);
        ImageButton btnWishlist = findViewById(R.id.btnDetailWishlist);
        ViewPager2 pagerImages = findViewById(R.id.pagerProductImages);
        TextView tvBadge = findViewById(R.id.tvDetailBadge);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        mTvPrice = findViewById(R.id.tvDetailPrice);
        TextView tvStars = findViewById(R.id.tvDetailStars);
        mTvMetaLoc = findViewById(R.id.tvDetailMetaLocation);
        TextView tvMetaMid = findViewById(R.id.tvDetailMetaCondition);
        TextView tvMetaItemRating = findViewById(R.id.tvDetailMetaItemRating);
        TextView tvMetaTime = findViewById(R.id.tvDetailMetaTime);
        TextView tvDesc = findViewById(R.id.tvDetailDescription);
        mBtnBuy = findViewById(R.id.btnDetailBuy);
        MaterialButton btnMessage = findViewById(R.id.btnDetailMessage);

        ApiClient.initialize(this);
        // Disable wishlist button if the current logged-in user is the seller.
        // Use the seller id passed with the listing instead of doing a blocking API call during screen load.
        boolean isOwnListing = !TextUtils.isEmpty(currentUserId)
                && !TextUtils.isEmpty(mSellerId)
                && currentUserId.equalsIgnoreCase(mSellerId);
        btnWishlist.setEnabled(!isOwnListing);
        if (isOwnListing) {
            btnWishlist.setAlpha(0.5f); // Visual feedback that button is disabled
        }

        tvTitle.setText(name);
        tvDesc.setText(description);
        tvSellerName.setText(sellerName);
        String mid = !TextUtils.isEmpty(category)
                ? category
                : (!TextUtils.isEmpty(condition) ? condition : "General");
        tvMetaMid.setText(mid);
        String timeLabel = getPostedDateLabel(listedTime);
        tvMetaTime.setText(timeLabel);

        double avg = parseAverage(itemAvgRating, rating);
        tvStars.setText(starsFor(avg));
        tvMetaItemRating.setText(String.format(Locale.US, "%.1f", avg));

        String badgeText = !TextUtils.isEmpty(badge) ? badge : condition;
        if (!TextUtils.isEmpty(badgeText)) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(badgeText.toUpperCase());
        } else {
            tvBadge.setVisibility(View.GONE);
        }

        int cornerPx = (int) (10f * getResources().getDisplayMetrics().density);
        RequestOptions thumbOpts = RequestOptions.bitmapTransform(new RoundedCorners(cornerPx));

        final int imageCount = imageUrls.size();
        final boolean showThumbStrip = imageCount > 1;
        final int visibleThumbSlots = showThumbStrip ? Math.min(THUMB_COUNT, imageCount) : 0;

        LinearLayout llThumbs = findViewById(R.id.llThumbs);
        llThumbs.setVisibility(showThumbStrip ? View.VISIBLE : View.GONE);
        pagerImages.setUserInputEnabled(imageCount > 1);
        pagerImages.setLongClickable(false);
        pagerImages.setHapticFeedbackEnabled(false);
        pagerImages.setOnLongClickListener(v -> true);

        for (int t = 0; t < THUMB_COUNT; t++) {
            FrameLayout slot = findViewById(thumbSlotIds[t]);
            ImageView ivThumb = findViewById(thumbImageIds[t]);
            if (showThumbStrip && t < visibleThumbSlots) {
                slot.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imageUrls.get(t))
                        .apply(thumbOpts)
                        .centerCrop()
                        .into(ivThumb);
                final int thumbIndex = t;
                slot.setOnClickListener(v -> {
                    pagerImages.setCurrentItem(thumbIndex, true);
                    selectThumb(thumbIndex);
                });
            } else {
                slot.setVisibility(View.GONE);
                slot.setOnClickListener(null);
                ivThumb.setImageDrawable(null);
            }
        }

        pagerImages.setAdapter(new ProductDetailCarouselAdapter(imageUrls));
        if (imageUrls.size() > 1) {
            pagerImages.setOffscreenPageLimit(Math.min(imageUrls.size() - 1, 3));
        }
        pagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (!showThumbStrip) {
                    return;
                }
                int idx = Math.min(position, visibleThumbSlots - 1);
                selectThumb(idx);
            }
        });

        if (!TextUtils.isEmpty(sellerImageUrl)) {
            Glide.with(this)
                    .load(sellerImageUrl)
                    .circleCrop()
                    .into(ivSeller);
        } else {
            ivSeller.setImageDrawable(
                    AvatarUtils.createInitialsAvatar(this, sellerName, 40));
        }

        if (showThumbStrip) {
            selectThumb(0);
        }

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        final boolean[] wishlisted = {false};
        WishlistAnimator.applyProductDetailHeader(btnWishlist, false);
        if (!TextUtils.isEmpty(currentUserId) && mListingId > 0 && !isOwnListing) {
            ApiClient.isSaved(currentUserId, mListingId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    runOnUiThread(() -> {
                        wishlisted[0] = response.optBoolean("is_saved", false);
                        WishlistAnimator.applyProductDetailHeader(btnWishlist, wishlisted[0]);
                    });
                }

                @Override
                public void onError(String error) {
                    // Leave wishlist unselected. The tap action can retry later.
                }
            });
        }

        btnWishlist.setOnClickListener(v -> {
            if (TextUtils.isEmpty(currentUserId)) {
                Toast.makeText(this, getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
                return;
            }
            if (mListingId <= 0) {
                Toast.makeText(this, "This item cannot be saved", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isOwnListing) {
                Toast.makeText(this, "You cannot add your own products to the wishlist", Toast.LENGTH_SHORT).show();
                return;
            }

            btnWishlist.setEnabled(false);
            ApiClient.ApiCallback callback = new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    runOnUiThread(() -> {
                        btnWishlist.setEnabled(true);
                        wishlisted[0] = !wishlisted[0];
                        WishlistAnimator.applyProductDetailHeader(btnWishlist, wishlisted[0]);
                        WishlistAnimator.playToggleCelebration(btnWishlist, wishlisted[0]);
                        Toast.makeText(ProductDetailActivity.this,
                                wishlisted[0] ? getString(R.string.product_wishlist_on) : getString(R.string.product_wishlist_off),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnWishlist.setEnabled(true);
                        Toast.makeText(ProductDetailActivity.this, "Wishlist failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            };

            if (wishlisted[0]) {
                ApiClient.removeSavedItem(currentUserId, mListingId, callback);
            } else {
                ApiClient.saveItem(currentUserId, mListingId, callback);
            }
        });

        applyQuantityState(mDisplayedQuantity);

        btnMessage.setOnClickListener(v -> {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            if (TextUtils.isEmpty(currentUserId)) {
                Toast.makeText(this, getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!TextUtils.isEmpty(mSellerId) && currentUserId.equalsIgnoreCase(mSellerId)) {
                Toast.makeText(this, "You cannot message yourself", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mListingId <= 0) {
                Toast.makeText(this, "Seller conversation is unavailable for this item", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(mSellerId)) {
                ApiClient.getListing(mListingId, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject response) {
                        org.json.JSONObject listing = response.optJSONObject("listing");
                        if (listing != null) {
                            mSellerId = listing.optString("seller_id", "");
                            final String recoveredSellerName = listing.optString("seller_name", sellerName);
                            runOnUiThread(() -> openSellerConversation(recoveredSellerName));
                        } else {
                            runOnUiThread(() -> Toast.makeText(ProductDetailActivity.this,
                                    "Seller conversation is unavailable for this item", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(ProductDetailActivity.this,
                                "Seller conversation failed: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
                return;
            }
            openSellerConversation(sellerName);
        });
    }

    private void openSellerConversation(@Nullable String sellerName) {
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        String currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
        if (TextUtils.isEmpty(mSellerId)) {
            Toast.makeText(this, "Seller conversation is unavailable for this item", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(currentUserId) && currentUserId.equalsIgnoreCase(mSellerId)) {
            Toast.makeText(this, "You cannot message yourself", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent chat = new Intent(this, ConversationDetailActivity.class);
        chat.putExtra("otherUserName", TextUtils.isEmpty(sellerName) ? "Seller" : sellerName);
        chat.putExtra("otherUserId", mSellerId);
        chat.putExtra("listingId", mListingId);
        startActivity(chat);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mListingId > 0) {
            ApiClient.getListing(mListingId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    org.json.JSONObject listing = response.optJSONObject("listing");
                    if (listing != null) {
                        int quantity = listing.optInt("quantity", mDisplayedQuantity);
                        runOnUiThread(() -> applyQuantityState(quantity));
                    }
                }

                @Override
                public void onError(String error) {
                    // Keep the quantity that was loaded with the screen.
                }
            });
        }
    }

    private void applyQuantityState(int quantity) {
        mDisplayedQuantity = quantity;
        if (mTvMetaLoc != null) {
            mTvMetaLoc.setText("Qty " + Math.max(0, quantity));
        }
        if (mTvPrice != null) {
            mTvPrice.setText(quantity <= 0 ? "Out of stock" : formatRand(mPriceRaw));
        }
        if (mBtnBuy == null) {
            return;
        }
        mBtnBuy.setVisibility(View.VISIBLE);
        if (quantity <= 0) {
            mBtnBuy.setText("Out of stock");
            mBtnBuy.setEnabled(false);
            mBtnBuy.setOnClickListener(null);
        } else {
            mBtnBuy.setText("Buy Now");
            mBtnBuy.setEnabled(true);
            mBtnBuy.setOnClickListener(v -> {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                        .start();
                Intent checkout = new Intent(this, CheckoutActivity.class);
                checkout.putExtra(EXTRA_NAME, mItemName);
                checkout.putExtra(EXTRA_PRICE, formatRand(mPriceRaw));

                // Do not send large base64 images directly through Intent extras.
                // Checkout reads the real image through this small in-memory token.
                String imageForCheckout = !TextUtils.isEmpty(mCheckoutImage) ? mCheckoutImage : mPrimaryImage;
                checkout.putExtra(EXTRA_CHECKOUT_IMAGE_TOKEN, CheckoutImageStore.put(imageForCheckout));
                if (!isLargeInlineImage(imageForCheckout)) {
                    checkout.putExtra(EXTRA_IMAGE, imageForCheckout);
                }

                checkout.putExtra(EXTRA_LISTING_ID, mListingId);
                checkout.putExtra(EXTRA_QUANTITY, mDisplayedQuantity);
                checkout.putExtra(EXTRA_SELLER_ID, mSellerId);
                checkoutLauncher.launch(checkout);
            });
        }
    }

    private void selectThumb(int selectedIndex) {
        for (int t = 0; t < THUMB_COUNT; t++) {
            FrameLayout slot = findViewById(thumbSlotIds[t]);
            if (slot.getVisibility() != View.VISIBLE) {
                continue;
            }
            slot.setBackgroundResource(
                    t == selectedIndex
                            ? R.drawable.product_detail_thumb_selected
                            : R.drawable.product_detail_thumb_normal);
        }
    }

    /**
     * Unique image URLs for the hero pager and thumbnails: gallery from the intent when present,
     * otherwise the listing primary image once.
     */
    private static List<String> resolveDetailImageUrls(
            @Nullable String primaryImage, @Nullable ArrayList<String> galleryExtra) {
        List<String> raw = new ArrayList<>();
        if (galleryExtra != null && !galleryExtra.isEmpty()) {
            raw.addAll(galleryExtra);
        } else if (primaryImage != null && !primaryImage.trim().isEmpty()) {
            raw.add(primaryImage);
        } else {
            raw.add("");
        }
        List<String> deduped = Product.dedupeImageUrls(raw);
        if (!deduped.isEmpty()) {
            return deduped;
        }
        String fallback = primaryImage == null ? "" : primaryImage.trim();
        return Collections.singletonList(fallback);
    }

    private static boolean isLargeInlineImage(@Nullable String image) {
        return image != null && image.startsWith("data:image") && image.length() > 100000;
    }

    private static String formatRand(@Nullable String value) {
        if (TextUtils.isEmpty(value)) return "R0";
        String v = value.trim();
        return v.startsWith("R") ? v : "R" + v;
    }

    private static String getPostedDateLabel(@Nullable String listedTime) {
        return PostedDateFormat.displayDate(listedTime);
    }

    private static double parseAverage(@Nullable String itemAvgRating, @Nullable String sellerRating) {
        String[] candidates = new String[] { itemAvgRating, sellerRating };
        for (String c : candidates) {
            if (TextUtils.isEmpty(c)) continue;
            try {
                return Double.parseDouble(c.replaceAll("[^0-9.]", ""));
            } catch (Exception ignored) {
            }
        }
        return 0.0;
    }

    private static String starsFor(double avg) {
        int filled = (int) Math.round(Math.max(0d, Math.min(5d, avg)));
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(i < filled ? '★' : '☆');
        }
        return sb.toString();
    }
}
