package com.example.nkbtradesphere.ui.product;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nkbtradesphere.data.model.Product;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the selected product in memory while moving between product screens.
 *
 * Some listings contain large base64 image strings. Passing those strings through
 * Intent extras can exceed Android's Binder limit and crash the app when a product
 * card is tapped. This store passes only a small token in the Intent and keeps the
 * full Product object inside the current app process.
 */
public final class ProductNavigationStore {
    private static final int MAX_ITEMS = 12;

    private static final LinkedHashMap<String, Product> PRODUCTS =
            new LinkedHashMap<String, Product>(MAX_ITEMS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Product> eldest) {
                    return size() > MAX_ITEMS;
                }
            };

    private ProductNavigationStore() { }

    @NonNull
    public static synchronized String put(@NonNull Product product) {
        String token = UUID.randomUUID().toString();
        PRODUCTS.put(token, product);
        return token;
    }

    @Nullable
    public static synchronized Product get(@Nullable String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return PRODUCTS.get(token);
    }
}
