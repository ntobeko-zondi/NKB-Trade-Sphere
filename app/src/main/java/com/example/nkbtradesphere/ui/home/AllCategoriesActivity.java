package com.example.nkbtradesphere.ui.home;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AllCategoriesActivity extends AppCompatActivity {

    private final List<CategoryCardItem> categoryItems = new ArrayList<>();
    private AllCategoriesAdapter adapter;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_categories);

        RecyclerView rvAllCategories = findViewById(R.id.rvAllCategories);
        ImageButton btnBack = findViewById(R.id.btnBackAllCategories);
        progressBar = findViewById(R.id.progressAllCategories);

        adapter = new AllCategoriesAdapter(this, categoryItems, item ->
                startActivity(CategoryListingsActivity.newIntent(this, item.title, item.emoji)));
        rvAllCategories.setLayoutManager(new GridLayoutManager(this, 2));
        rvAllCategories.setAdapter(adapter);

        btnBack.setOnClickListener(v -> onBackPressed());
        loadAllCategories();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllCategories();
    }

    private void loadAllCategories() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            DatabaseHelper db = DatabaseHelper.getInstance(getApplicationContext());
            List<DatabaseHelper.ListingData> listings = db.getAllListings();
            LinkedHashMap<String, CategoryCardItem> map = buildDefaultCategoryDeck();

            for (DatabaseHelper.ListingData listing : listings) {
                String normalized = CategoryUtils.normalizeCategory(listing.category);
                CategoryCardItem item = map.get(normalized);
                if (item != null) {
                    item.itemCount++;
                } else {
                    map.get(CategoryUtils.CAT_OTHER).itemCount++;
                }
            }

            runOnUiThread(() -> {
                categoryItems.clear();
                categoryItems.addAll(map.values());
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            });
        }).start();
    }

    @NonNull
    private LinkedHashMap<String, CategoryCardItem> buildDefaultCategoryDeck() {
        LinkedHashMap<String, CategoryCardItem> map = new LinkedHashMap<>();
        for (CategoryUtils.CategoryInfo info : CategoryUtils.defaultCategoryDeck().values()) {
            map.put(info.id, new CategoryCardItem(info.id, info.title, info.emoji, info.imageUrl));
        }
        return map;
    }

    static class CategoryCardItem {
        final String id;
        final String title;
        final String emoji;
        final String imageUrl;
        int itemCount;

        CategoryCardItem(String id, String title, String emoji, String imageUrl) {
            this.id = id;
            this.title = title;
            this.emoji = emoji;
            this.imageUrl = imageUrl;
            this.itemCount = 0;
        }
    }
}
