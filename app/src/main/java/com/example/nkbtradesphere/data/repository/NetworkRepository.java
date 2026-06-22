package com.example.nkbtradesphere.data.repository;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.network.ApiClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/**
 * Network Repository - Replaces local SQLite ProductRepository
 * Fetches all products from remote API instead of local database
 * Uses synchronous blocking call (suitable for background threads)
 */
public final class NetworkRepository {
    private static final String TAG = "NetworkRepository";
    
    public NetworkRepository(@NonNull Context context) {
        // Initialize API client if needed
        ApiClient.initialize(context);
    }

    /**
     * Get all products from remote API
     * WARNING: This is a synchronous blocking call - call from background thread!
     */
    @NonNull
    public List<Product> getAllProducts() {
        final List<Product> products = new ArrayList<>();
        final Object lock = new Object();
        final boolean[] done = {false};
        final Exception[] error = {null};

        ApiClient.getAllListings(new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray listings = response.getJSONArray("listings");
                    for (int i = 0; i < listings.length(); i++) {
                        JSONObject listing = listings.getJSONObject(i);
                        Product p = parseProduct(listing);
                        if (p != null) {
                            products.add(p);
                        }
                    }
                } catch (JSONException e) {
                    error[0] = e;
                }
                synchronized (lock) {
                    done[0] = true;
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(String err) {
                error[0] = new Exception(err);
                synchronized (lock) {
                    done[0] = true;
                    lock.notifyAll();
                }
            }
        });

        // Wait for response (with timeout)
        synchronized (lock) {
            try {
                if (!done[0]) {
                    lock.wait(10000); // 10 second timeout
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (error[0] != null) {
            android.util.Log.e(TAG, "Error fetching listings", error[0]);
        }

        return products;
    }

    /**
     * Parse single product from API JSON response
     */
    @NonNull
    private Product parseProduct(@NonNull JSONObject json) throws JSONException {
        int listingId = json.optInt("listing_id", -1);
        String sellerId = json.optString("seller_id", "");
        String title = json.optString("title", "");
        String price = withRandPrefix(json.optString("price", ""));
        String imageUrl = normalizeImageData(json.optString("image_url", ""));
        List<String> gallery = extractGallery(json.optString("image_url", ""));
        String description = json.optString("description", "");
        String condition = json.optString("condition", "");
        String category = json.optString("category", "");
        String listedTime = json.optString("created_at", "");
        String sellerName = json.optString("seller_name",
                json.optString("full_name", sellerId));
        String listingAverage = json.optString("average_rating", "");
        int quantity = json.optInt("quantity", 1);
        String status = json.optString("status", "active");
        
        // Return as Product object matching existing model
        return new Product(
                listingId,
                sellerId,
                title,
                price,
                "",
                imageUrl,
                description,
                sellerName,
                "",        // Seller rating
                condition,
                category,
                "",        // Badge
                listingAverage,
                listedTime,
                "",        // Seller image
                gallery,
                quantity,
                status
        );
    }

    /**
     * Normalize image data (convert base64 to URL if needed)
     */
    @NonNull
    private static String normalizeImageData(@NonNull String rawImage) {
        if (TextUtils.isEmpty(rawImage)) {
            return "";
        }
        if (!rawImage.contains("|") && !rawImage.startsWith("data:image")
                && rawImage.length() > 120 && !rawImage.startsWith("http")) {
            return "data:image/jpeg;base64," + rawImage;
        }
        String first = rawImage;
        if (rawImage.contains("|")) {
            first = rawImage.split("\\|")[0];
        }
        if (TextUtils.isEmpty(first)) {
            return "";
        }
        if (first.startsWith("http") || first.startsWith("data:image")) {
            return first;
        }
        return "data:image/jpeg;base64," + first;
    }

    @NonNull
    private static String withRandPrefix(@NonNull String rawPrice) {
        String p = rawPrice.trim();
        if (p.isEmpty()) return "R0";
        if (p.toUpperCase(Locale.US).startsWith("R")) return p;
        return "R" + p;
    }

    @NonNull
    private static List<String> extractGallery(@NonNull String rawImage) {
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(rawImage)) {
            return out;
        }
        if (rawImage.contains("|")) {
            String[] parts = rawImage.split("\\|");
            for (String part : parts) {
                String normalized = normalizeImageData(part);
                if (!TextUtils.isEmpty(normalized)) {
                    out.add(normalized);
                }
            }
            return out;
        }
        String normalized = normalizeImageData(rawImage);
        if (!TextUtils.isEmpty(normalized)) {
            out.add(normalized);
        }
        return out;
    }
}
