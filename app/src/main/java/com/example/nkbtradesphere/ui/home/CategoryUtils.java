package com.example.nkbtradesphere.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nkbtradesphere.data.model.Category;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared category logic for Home, All Categories, and Category listing pages. */
public final class CategoryUtils {

    private CategoryUtils() {}

    public static final String CAT_ELECTRONICS = "electronics";
    public static final String CAT_FASHION = "fashion";
    public static final String CAT_HOME = "home & living";
    public static final String CAT_VEHICLES = "vehicles";
    public static final String CAT_BOOKS = "books";
    public static final String CAT_PROPERTY = "property";
    public static final String CAT_SPORTS = "sports";
    public static final String CAT_MUSIC = "music";
    public static final String CAT_BABY_KIDS = "baby & kids";
    public static final String CAT_PETS = "pets";
    public static final String CAT_HEALTH_BEAUTY = "health & beauty";
    public static final String CAT_OTHER = "other";

    @NonNull
    public static List<Category> homeCategories() {
        List<Category> categories = new ArrayList<>();
        for (CategoryInfo info : defaultCategoryDeck().values()) {
            categories.add(new Category(info.title, info.imageUrl));
        }
        return categories;
    }

    @NonNull
    public static LinkedHashMap<String, CategoryInfo> defaultCategoryDeck() {
        LinkedHashMap<String, CategoryInfo> map = new LinkedHashMap<>();
        map.put(CAT_ELECTRONICS, new CategoryInfo(CAT_ELECTRONICS, "Electronics", "📱",
                "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_FASHION, new CategoryInfo(CAT_FASHION, "Fashion", "👗",
                "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_HOME, new CategoryInfo(CAT_HOME, "Home & Living", "🛋️",
                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_VEHICLES, new CategoryInfo(CAT_VEHICLES, "Vehicles", "🚗",
                "https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_BOOKS, new CategoryInfo(CAT_BOOKS, "Books", "📚",
                "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_PROPERTY, new CategoryInfo(CAT_PROPERTY, "Property", "🏠",
                "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_SPORTS, new CategoryInfo(CAT_SPORTS, "Sports", "⚽",
                "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_MUSIC, new CategoryInfo(CAT_MUSIC, "Music", "🎵",
                "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_BABY_KIDS, new CategoryInfo(CAT_BABY_KIDS, "Baby & Kids", "🧸",
                "https://images.unsplash.com/photo-1515488764276-beab7607c1e6?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_PETS, new CategoryInfo(CAT_PETS, "Pets", "🐾",
                "https://images.unsplash.com/photo-1450778869180-41d0601e046e?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_HEALTH_BEAUTY, new CategoryInfo(CAT_HEALTH_BEAUTY, "Health & Beauty", "🧴",
                "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=600&h=420&fit=crop&auto=format"));
        map.put(CAT_OTHER, new CategoryInfo(CAT_OTHER, "Other", "🧩",
                "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600&h=420&fit=crop&auto=format"));
        return map;
    }

    @NonNull
    public static String[] categoryTitlesForSellSpinner() {
        LinkedHashMap<String, CategoryInfo> deck = defaultCategoryDeck();
        String[] titles = new String[deck.size()];
        int i = 0;
        for (CategoryInfo info : deck.values()) {
            titles[i++] = info.title;
        }
        return titles;
    }

    @NonNull
    public static List<Product> filterProductsByCategory(@Nullable String selectedCategory,
                                                         @Nullable List<Product> products) {
        List<Product> filtered = new ArrayList<>();
        if (products == null) return filtered;
        String selected = normalizeCategory(selectedCategory);
        for (Product p : products) {
            if (selected.equals(normalizeCategory(p.getCategory()))) {
                filtered.add(p);
            }
        }
        return filtered;
    }

    @NonNull
    public static List<DatabaseHelper.ListingData> filterListingsByCategory(@Nullable String selectedCategory,
                                                                            @Nullable List<DatabaseHelper.ListingData> listings) {
        List<DatabaseHelper.ListingData> filtered = new ArrayList<>();
        if (listings == null) return filtered;
        String selected = normalizeCategory(selectedCategory);
        for (DatabaseHelper.ListingData listing : listings) {
            if (selected.equals(normalizeCategory(listing.category))) {
                filtered.add(listing);
            }
        }
        return filtered;
    }

    public static boolean matchesCategory(@Nullable String selectedCategory, @Nullable String listingCategory) {
        return normalizeCategory(selectedCategory).equals(normalizeCategory(listingCategory));
    }

    @NonNull
    public static String normalizeCategory(@Nullable String raw) {
        if (raw == null) return CAT_OTHER;
        String c = raw.trim().toLowerCase(Locale.US);
        if (c.isEmpty()) return CAT_OTHER;

        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("electronic", CAT_ELECTRONICS);
        aliases.put("electronics", CAT_ELECTRONICS);
        aliases.put("phone", CAT_ELECTRONICS);
        aliases.put("phones", CAT_ELECTRONICS);
        aliases.put("laptop", CAT_ELECTRONICS);
        aliases.put("laptops", CAT_ELECTRONICS);

        aliases.put("fashion", CAT_FASHION);
        aliases.put("clothing", CAT_FASHION);
        aliases.put("clothes", CAT_FASHION);
        aliases.put("clothing & fashion", CAT_FASHION);

        aliases.put("home", CAT_HOME);
        aliases.put("home & living", CAT_HOME);
        aliases.put("furniture", CAT_HOME);

        aliases.put("vehicle", CAT_VEHICLES);
        aliases.put("vehicles", CAT_VEHICLES);
        aliases.put("car", CAT_VEHICLES);
        aliases.put("cars", CAT_VEHICLES);

        aliases.put("book", CAT_BOOKS);
        aliases.put("books", CAT_BOOKS);
        aliases.put("textbook", CAT_BOOKS);
        aliases.put("textbooks", CAT_BOOKS);

        aliases.put("property", CAT_PROPERTY);
        aliases.put("properties", CAT_PROPERTY);
        aliases.put("house", CAT_PROPERTY);
        aliases.put("housing", CAT_PROPERTY);

        aliases.put("sport", CAT_SPORTS);
        aliases.put("sports", CAT_SPORTS);
        aliases.put("fitness", CAT_SPORTS);

        aliases.put("music", CAT_MUSIC);
        aliases.put("instrument", CAT_MUSIC);
        aliases.put("instruments", CAT_MUSIC);

        aliases.put("baby", CAT_BABY_KIDS);
        aliases.put("kids", CAT_BABY_KIDS);
        aliases.put("baby & kids", CAT_BABY_KIDS);
        aliases.put("toys", CAT_BABY_KIDS);

        aliases.put("pet", CAT_PETS);
        aliases.put("pets", CAT_PETS);

        aliases.put("beauty", CAT_HEALTH_BEAUTY);
        aliases.put("health", CAT_HEALTH_BEAUTY);
        aliases.put("health & beauty", CAT_HEALTH_BEAUTY);

        aliases.put("other", CAT_OTHER);
        return aliases.containsKey(c) ? aliases.get(c) : CAT_OTHER;
    }

    @NonNull
    public static String emojiForName(@Nullable String categoryName) {
        CategoryInfo info = defaultCategoryDeck().get(normalizeCategory(categoryName));
        return info == null ? "📦" : info.emoji;
    }

    @NonNull
    public static String displayTitle(@Nullable String categoryName) {
        CategoryInfo info = defaultCategoryDeck().get(normalizeCategory(categoryName));
        return info == null ? "Other" : info.title;
    }

    public static final class CategoryInfo {
        public final String id;
        public final String title;
        public final String emoji;
        public final String imageUrl;
        public int itemCount;

        public CategoryInfo(String id, String title, String emoji, String imageUrl) {
            this.id = id;
            this.title = title;
            this.emoji = emoji;
            this.imageUrl = imageUrl;
            this.itemCount = 0;
        }
    }
}
