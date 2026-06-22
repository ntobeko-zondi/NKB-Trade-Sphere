package com.example.nkbtradesphere.data.repository;

import com.example.nkbtradesphere.data.model.Product;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Use {@link NetworkRepository} instead, which uses ApiClient for remote PostgreSQL data.
 * This class is kept for backward compatibility but now returns empty lists.
 */
@Deprecated
public final class ProductRepository {

    public ProductRepository(@NonNull Context context) {
        // Constructor kept for backward compatibility
    }

    @NonNull
    @Deprecated
    public List<Product> getAllProducts() {
        return new ArrayList<>(); // Use NetworkRepository instead
    }
}

