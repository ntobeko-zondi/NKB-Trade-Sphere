package com.example.nkbtradesphere.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import androidx.annotation.Nullable;

import com.example.nkbtradesphere.util.RatingFormat;

public class Product {
    private final int listingId;
    private final String sellerId;
    private final String name;
    private final String price;
    private final String location;
    private final String imageUrl;
    private final String description;
    private final String sellerName;
    private final String sellerRating;
    private final String condition;
    private final String category;
    private final String badge; // "New", "Hot", or null
    /** Average buyer rating for this listing (demo text, e.g. {@code "4.7"}). */
    private final String itemAverageRating;
    private final String listedTime;
    private final String sellerImageUrl;
    /** Detail gallery: at least one URL (same as {@link #imageUrl} when no extras). */
    private final List<String> imageGallery;
    private final int quantity;
    private final String status;

    public Product(String name, String price, String location, String imageUrl,
                   String description, String sellerName, String sellerRating,
                   String condition, String category, String badge) {
        this(name, price, location, imageUrl, description, sellerName, sellerRating,
                condition, category, badge, "", "", "", null, 1, "active");
    }

    public Product(String name, String price, String location, String imageUrl,
                   String description, String sellerName, String sellerRating,
                   String condition, String category, String badge,
                   String listedTime, String sellerImageUrl) {
        this(name, price, location, imageUrl, description, sellerName, sellerRating,
                condition, category, badge, "", listedTime, sellerImageUrl, null, 1, "active");
    }

    public Product(String name, String price, String location, String imageUrl,
                   String description, String sellerName, String sellerRating,
                   String condition, String category, String badge,
                   String itemAverageRating,
                   String listedTime, String sellerImageUrl,
                   @Nullable List<String> galleryImageUrls,
                   int quantity,
                   String status) {
        this(-1, "", name, price, location, imageUrl, description, sellerName, sellerRating,
                condition, category, badge, itemAverageRating, listedTime, sellerImageUrl, galleryImageUrls, quantity, status);
    }

    public Product(int listingId,
                   String name, String price, String location, String imageUrl,
                   String description, String sellerName, String sellerRating,
                   String condition, String category, String badge,
                   String itemAverageRating,
                   String listedTime, String sellerImageUrl,
                   @Nullable List<String> galleryImageUrls,
                   int quantity,
                   String status) {
        this(listingId, "", name, price, location, imageUrl, description, sellerName, sellerRating,
                condition, category, badge, itemAverageRating, listedTime, sellerImageUrl, galleryImageUrls, quantity, status);
    }

    public Product(int listingId,
                   String sellerId,
                   String name, String price, String location, String imageUrl,
                   String description, String sellerName, String sellerRating,
                   String condition, String category, String badge,
                   String itemAverageRating,
                   String listedTime, String sellerImageUrl,
                   @Nullable List<String> galleryImageUrls,
                   int quantity,
                   String status) {
        this.listingId = listingId;
        this.sellerId = sellerId == null ? "" : sellerId;
        this.name = name;
        this.price = price;
        this.location = location;
        this.imageUrl = imageUrl;
        this.description = description;
        this.sellerName = sellerName;
        this.sellerRating = sellerRating;
        this.condition = condition;
        this.category = category;
        this.badge = badge;
        this.itemAverageRating = itemAverageRating == null ? "" : itemAverageRating;
        this.listedTime = listedTime == null ? "" : listedTime;
        this.sellerImageUrl = sellerImageUrl == null ? "" : sellerImageUrl;
        this.imageGallery = buildImageGallery(imageUrl, galleryImageUrls);
        this.quantity = Math.max(0, quantity);
        this.status = status == null ? "active" : status;
    }

    private static List<String> buildImageGallery(String primary,
                                                  @Nullable List<String> galleryImageUrls) {
        if (galleryImageUrls != null && !galleryImageUrls.isEmpty()) {
            List<String> raw = new ArrayList<>();
            for (String u : galleryImageUrls) {
                if (u != null && !u.trim().isEmpty()) {
                    raw.add(u.trim());
                }
            }
            List<String> deduped = dedupeImageUrls(raw);
            if (!deduped.isEmpty()) {
                return Collections.unmodifiableList(deduped);
            }
        }
        String p = primary == null ? "" : primary.trim();
        return Collections.singletonList(p);
    }

    /**
     * Trim, drop blanks, keep first occurrence of each URL (order preserved).
     * Use for detail carousel / thumbnails so the same photo is not shown twice.
     */
    public static List<String> dedupeImageUrls(@Nullable List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String u : urls) {
            if (u == null) {
                continue;
            }
            String t = u.trim();
            if (!t.isEmpty()) {
                seen.add(t);
            }
        }
        return new ArrayList<>(seen);
    }

    public int getListingId()       { return listingId; }
    public String getSellerId()      { return sellerId; }
    public String getName()            { return name; }
    public String getPrice()           { return price; }
    public String getLocation()        { return location; }
    public String getImageUrl()        { return imageUrl; }
    public String getDescription()     { return description; }
    public String getSellerName()      { return sellerName; }
    public String getSellerRating()    { return RatingFormat.valueOnly(sellerRating); }
    public String getCondition()       { return condition; }
    public String getCategory()        { return category; }
    public String getBadge()           { return badge; }
    public String getItemAverageRating() { return RatingFormat.valueOnly(itemAverageRating); }
    public String getListedTime()     { return listedTime; }
    public String getSellerImageUrl()  { return sellerImageUrl; }

    /** URLs for product detail thumbnails / hero (never empty). */
    public List<String> getImageGallery() {
        return imageGallery;
    }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public boolean isOutOfStock() { return quantity <= 0; }
}
