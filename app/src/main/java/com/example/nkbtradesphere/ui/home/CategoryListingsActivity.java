package com.example.nkbtradesphere.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.data.repository.NetworkRepository;
import com.example.nkbtradesphere.ui.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CategoryListingsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";
    public static final String EXTRA_CATEGORY_EMOJI = "extra_category_emoji";

    private final List<Product> allCategoryProducts = new ArrayList<>();
    private final List<Product> shownProducts = new ArrayList<>();
    private CategoryListingGridAdapter adapter;
    private TextView chipRecent;
    private TextView chipPriceAsc;
    private TextView chipPriceDesc;
    private TextView tvCount;
    private String selectedCategoryName = "Category";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_listings);

        selectedCategoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        String categoryEmoji = getIntent().getStringExtra(EXTRA_CATEGORY_EMOJI);
        if (TextUtils.isEmpty(selectedCategoryName)) {
            selectedCategoryName = "Category";
        }
        selectedCategoryName = CategoryUtils.displayTitle(selectedCategoryName);
        if (TextUtils.isEmpty(categoryEmoji)) {
            categoryEmoji = CategoryUtils.emojiForName(selectedCategoryName);
        }

        ImageButton btnBack = findViewById(R.id.btnBackCategoryListings);
        TextView tvTitle = findViewById(R.id.tvCategoryListingsTitle);
        tvCount = findViewById(R.id.tvCategoryListingsCount);
        chipRecent = findViewById(R.id.chipRecent);
        chipPriceAsc = findViewById(R.id.chipPriceAsc);
        chipPriceDesc = findViewById(R.id.chipPriceDesc);
        RecyclerView rv = findViewById(R.id.rvCategoryListings);

        tvTitle.setText(categoryEmoji + " " + selectedCategoryName);
        tvCount.setText("Loading...");
        btnBack.setOnClickListener(v -> onBackPressed());

        adapter = new CategoryListingGridAdapter(this, shownProducts, product ->
                startActivity(ProductDetailActivity.newIntent(this, product)));
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(adapter);

        setupSortChips();
        loadCategoryListings(selectedCategoryName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            loadCategoryListings(selectedCategoryName);
        }
    }

    private void setupSortChips() {
        chipRecent.setOnClickListener(v -> {
            shownProducts.clear();
            shownProducts.addAll(allCategoryProducts);
            adapter.notifyDataSetChanged();
            updateSortChipStyles(SortType.RECENT);
        });
        chipPriceAsc.setOnClickListener(v -> {
            shownProducts.clear();
            shownProducts.addAll(allCategoryProducts);
            Collections.sort(shownProducts, Comparator.comparingDouble(CategoryListingsActivity::extractPrice));
            adapter.notifyDataSetChanged();
            updateSortChipStyles(SortType.PRICE_ASC);
        });
        chipPriceDesc.setOnClickListener(v -> {
            shownProducts.clear();
            shownProducts.addAll(allCategoryProducts);
            Collections.sort(shownProducts, (a, b) -> Double.compare(extractPrice(b), extractPrice(a)));
            adapter.notifyDataSetChanged();
            updateSortChipStyles(SortType.PRICE_DESC);
        });
        updateSortChipStyles(SortType.RECENT);
    }

    private void loadCategoryListings(@NonNull String categoryName) {
        new Thread(() -> {
            List<Product> allProducts = new NetworkRepository(getApplicationContext()).getAllProducts();
            List<Product> filteredProducts = CategoryUtils.filterProductsByCategory(categoryName, allProducts);

            runOnUiThread(() -> {
                allCategoryProducts.clear();
                allCategoryProducts.addAll(filteredProducts);
                shownProducts.clear();
                shownProducts.addAll(filteredProducts);
                adapter.notifyDataSetChanged();
                updateSortChipStyles(SortType.RECENT);
                tvCount.setText(String.format(Locale.US, "%d %s", filteredProducts.size(),
                        filteredProducts.size() == 1 ? "item" : "items"));
            });
        }).start();
    }

    private static double extractPrice(@NonNull Product p) {
        String raw = p.getPrice() == null ? "" : p.getPrice();
        String numeric = raw.replace("R", "").replace(",", "").trim();
        try {
            return Double.parseDouble(numeric);
        } catch (Exception ignored) {
            return 0.0d;
        }
    }

    private enum SortType { RECENT, PRICE_ASC, PRICE_DESC }

    private void updateSortChipStyles(@NonNull SortType type) {
        int selectedText = getColor(R.color.white);
        int normalText = getColor(R.color.text_primary);

        styleChip(chipRecent, type == SortType.RECENT, selectedText, normalText);
        styleChip(chipPriceAsc, type == SortType.PRICE_ASC, selectedText, normalText);
        styleChip(chipPriceDesc, type == SortType.PRICE_DESC, selectedText, normalText);
    }

    private void styleChip(@NonNull TextView chip, boolean selected, int selectedText, int normalText) {
        chip.setBackgroundResource(selected ? R.drawable.chip_filter_active : R.drawable.chip_filter_inactive);
        chip.setTextColor(selected ? selectedText : normalText);
    }

    public static Intent newIntent(@NonNull Context context, @NonNull String categoryName, @NonNull String emoji) {
        Intent intent = new Intent(context, CategoryListingsActivity.class);
        intent.putExtra(EXTRA_CATEGORY_NAME, categoryName);
        intent.putExtra(EXTRA_CATEGORY_EMOJI, emoji);
        return intent;
    }
}
