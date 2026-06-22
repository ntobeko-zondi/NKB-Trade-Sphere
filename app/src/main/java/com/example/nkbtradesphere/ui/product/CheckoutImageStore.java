package com.example.nkbtradesphere.ui.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps checkout images in memory while moving from the product detail screen to checkout.
 *
 * Some listing images are stored as large base64 strings. Putting those strings directly
 * into an Intent can exceed Android's Binder limit and can also make checkout receive no
 * image when large extras are intentionally skipped. This store passes only a small token
 * through the Intent while keeping the actual image string in the current app process.
 */
public final class CheckoutImageStore {
    private static final int MAX_ITEMS = 20;

    private static final LinkedHashMap<String, String> IMAGES =
            new LinkedHashMap<String, String>(MAX_ITEMS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_ITEMS;
                }
            };

    private CheckoutImageStore() { }

    @NonNull
    public static synchronized String put(@Nullable String image) {
        String token = UUID.randomUUID().toString();
        IMAGES.put(token, image == null ? "" : image);
        return token;
    }

    @Nullable
    public static synchronized String get(@Nullable String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return IMAGES.get(token);
    }
}
