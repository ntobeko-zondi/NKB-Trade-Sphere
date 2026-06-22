package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.data.model.Category;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.data.repository.NetworkRepository;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.util.AvatarUtils;
import com.example.nkbtradesphere.util.WishlistAnimator;
import com.example.nkbtradesphere.NotificationCenterActivity;
import com.example.nkbtradesphere.ui.product.ProductDetailActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {


    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable tapHintHideRunnable;
    private Runnable badgeRefreshRunnable;
    private int lastUnreadCount = -1;

    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private TextView tvThemeToggle;
    private TextView tvRecCount;
    private TextView tvNotifBadge;

    private SwipeCardStackView swipeStack;
    private View layoutStackEmpty;
    private TextView tvStackTapHint;

    private static final long PRODUCT_REFRESH_INTERVAL_MS = 60_000L;

    private final List<Product> productCatalog = new ArrayList<>();
    private final List<Product> fullCatalog = new ArrayList<>();
    private HomeSearchOverlay homeSearchOverlay;
    private boolean productsLoading = false;
    private long lastProductLoadAt = 0L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvCategories = view.findViewById(R.id.rvCategories);
        tvThemeToggle = view.findViewById(R.id.tvThemeToggle);
        tvRecCount = view.findViewById(R.id.tvRecCount);
        tvNotifBadge = view.findViewById(R.id.tvNotifBadge);

        swipeStack = view.findViewById(R.id.swipeStack);
        layoutStackEmpty = view.findViewById(R.id.layoutStackEmpty);
        tvStackTapHint = view.findViewById(R.id.tvStackTapHint);
        Button btnStackStartOver = view.findViewById(R.id.btnStackStartOver);
        homeSearchOverlay = view.findViewById(R.id.homeSearchOverlay);

        ImageView ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
        TextView tvUsername = view.findViewById(R.id.tvUsername);

        SharedPreferences authPrefs = requireContext().getSharedPreferences(
                AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        String savedName = authPrefs.getString(AppPreferences.KEY_USER_NAME, "");
        String userId = authPrefs.getString(AppPreferences.KEY_USER_EMAIL, "");
        if (!savedName.isEmpty()) {
            tvUsername.setText(savedName);
        }
        String avatarNameSeed = savedName.isEmpty() ? tvUsername.getText().toString() : savedName;
        ivUserAvatar.setImageDrawable(AvatarUtils.createInitialsAvatar(requireContext(), avatarNameSeed, 44));

        syncThemeToggleLabel();
        tvThemeToggle.setOnClickListener(v -> toggleTheme());

        setupCategories();
        setupSwipeStack();

        btnStackStartOver.setOnClickListener(v -> restartRecommendationDeck());

        ImageView ivNotification = view.findViewById(R.id.ivNotification);
        ImageView ivWishlist = view.findViewById(R.id.ivWishlist);
        TextView tvSeeAllCategories = view.findViewById(R.id.tvSeeAllCategories);
        TextView tvSeeAllRecommended = view.findViewById(R.id.tvSeeAllRecommended);

        ivNotification.setOnClickListener(v -> {
            v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                    .start();
            startActivity(new Intent(requireContext(), NotificationCenterActivity.class));
        });

        ivWishlist.setOnClickListener(v -> {
            WishlistAnimator.playSavedShortcutPulse(ivWishlist);
            startActivity(new Intent(requireContext(), com.example.nkbtradesphere.SavedItemsActivity.class));
        });

        tvSeeAllCategories.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AllCategoriesActivity.class)));
        tvSeeAllRecommended.setOnClickListener(v ->
                applyCategoryFilter(null));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHomeSearchOverlay();
    }

    @Override
    public void onDestroyView() {
        if (tapHintHideRunnable != null) {
            mainHandler.removeCallbacks(tapHintHideRunnable);
            tapHintHideRunnable = null;
        }
        if (badgeRefreshRunnable != null) {
            mainHandler.removeCallbacks(badgeRefreshRunnable);
            badgeRefreshRunnable = null;
        }
        super.onDestroyView();
    }

    private void scheduleTapHintFade() {
        if (tvStackTapHint == null) return;
        if (tapHintHideRunnable != null) {
            mainHandler.removeCallbacks(tapHintHideRunnable);
        }
        tvStackTapHint.setAlpha(1f);
        tvStackTapHint.setVisibility(View.VISIBLE);
        tapHintHideRunnable = () -> {
            if (tvStackTapHint != null && tvStackTapHint.getVisibility() == View.VISIBLE) {
                tvStackTapHint.animate().alpha(0f).setDuration(700).start();
            }
        };
        mainHandler.postDelayed(tapHintHideRunnable, 3000);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncThemeToggleLabel();
        if (!isHidden()) {
            startBadgeAutoRefresh();
            refreshNotificationBadge();
            loadProductsIfNeeded(false);
        }
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            if (badgeRefreshRunnable != null) {
                mainHandler.removeCallbacks(badgeRefreshRunnable);
            }
        } else {
            syncThemeToggleLabel();
            startBadgeAutoRefresh();
            refreshNotificationBadge();
            loadProductsIfNeeded(false);
        }
    }

    @Override
    public void onPause() {
        if (badgeRefreshRunnable != null) {
            mainHandler.removeCallbacks(badgeRefreshRunnable);
        }
        super.onPause();
    }

    private void syncThemeToggleLabel() {
        if (tvThemeToggle == null || getContext() == null) return;
        SharedPreferences p = requireContext().getSharedPreferences(
                AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        boolean dark = p.getBoolean(AppPreferences.KEY_DARK_THEME, true);
        tvThemeToggle.setText(dark ? "☀️" : "🌙");
    }

    private void refreshNotificationBadge() {
        if (tvNotifBadge == null || getContext() == null) return;
        Context appContext = requireContext().getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(
                AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        String userId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
        ApiClient.initialize(appContext);
        ApiClient.getUnreadMessageCount(userId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                int unread = response.optInt("count", 0);
                mainHandler.post(() -> {
                    if (!isAdded() || tvNotifBadge == null) return;
                    if (unread > 0) {
                        tvNotifBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                        tvNotifBadge.setVisibility(View.VISIBLE);
                        if (unread != lastUnreadCount) {
                            tvNotifBadge.setScaleX(0.85f);
                            tvNotifBadge.setScaleY(0.85f);
                            tvNotifBadge.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
                        }
                    } else {
                        tvNotifBadge.setVisibility(View.GONE);
                    }
                    lastUnreadCount = unread;
                });
            }

            @Override
            public void onError(String error) { }
        });
    }

    private void startBadgeAutoRefresh() {
        if (badgeRefreshRunnable != null) {
            mainHandler.removeCallbacks(badgeRefreshRunnable);
        }
        badgeRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                refreshNotificationBadge();
                mainHandler.postDelayed(this, 15000);
            }
        };
        mainHandler.postDelayed(badgeRefreshRunnable, 1200);
    }

    private void toggleTheme() {
        Context ctx = requireContext();
        SharedPreferences p = ctx.getSharedPreferences(AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        boolean wasDark = p.getBoolean(AppPreferences.KEY_DARK_THEME, true);
        boolean nextDark = !wasDark;
        p.edit().putBoolean(AppPreferences.KEY_DARK_THEME, nextDark).apply();
        AppCompatDelegate.setDefaultNightMode(
                nextDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        syncThemeToggleLabel();
    }

    private void setupCategories() {
        List<Category> categories = CategoryUtils.homeCategories();

        categoryAdapter = new CategoryAdapter(requireContext(), categories,
                category -> startActivity(CategoryListingsActivity.newIntent(
                        requireContext(),
                        CategoryUtils.displayTitle(category.getName()),
                        CategoryUtils.emojiForName(category.getName()))));
        rvCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupSwipeStack() {
        swipeStack.setCallbacks(new SwipeCardStackView.Callbacks() {
            @Override
            public void onCardTapped(Product product) {
                openDetailSheet(product);
            }

            @Override
            public void onStackEmptyChanged(boolean empty) {
                if (layoutStackEmpty != null) {
                    layoutStackEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                }
                if (swipeStack != null) {
                    swipeStack.setVisibility(empty ? View.GONE : View.VISIBLE);
                }
                if (tvStackTapHint != null) {
                    if (empty) {
                        tvStackTapHint.setVisibility(View.GONE);
                    } else {
                        tvStackTapHint.setVisibility(View.VISIBLE);
                        scheduleTapHintFade();
                    }
                }
            }
        });

        if (!productCatalog.isEmpty()) {
            swipeStack.setProducts(productCatalog);
        } else if (tvRecCount != null) {
            tvRecCount.setText(getString(R.string.items_count_fmt, 0));
        }

        loadProductsIfNeeded(false);
    }

    private void restartRecommendationDeck() {
        if (!isAdded()) return;

        // If personal-interest filtering leaves no cards, Start over should still give the user
        // something useful instead of returning to the same empty screen.
        if (productCatalog.isEmpty() && !fullCatalog.isEmpty()) {
            productCatalog.clear();
            productCatalog.addAll(fullCatalog);
            if (tvRecCount != null) {
                tvRecCount.setText(getString(R.string.items_count_fmt, productCatalog.size()));
            }
            if (swipeStack != null) {
                swipeStack.setProducts(productCatalog);
            }
            if (homeSearchOverlay != null) {
                homeSearchOverlay.bindCatalog(productCatalog);
            }
            Toast.makeText(requireContext(), "Showing all listings again", Toast.LENGTH_SHORT).show();
            scheduleTapHintFade();
            return;
        }

        if (!fullCatalog.isEmpty()) {
            applyRecommendationFilterFromPreferences();
            if (swipeStack != null) {
                swipeStack.resetDeckFromSource();
            }
            Toast.makeText(requireContext(), "Recommendation cards restarted", Toast.LENGTH_SHORT).show();
            scheduleTapHintFade();
        } else {
            Toast.makeText(requireContext(), "Loading listings...", Toast.LENGTH_SHORT).show();
            loadProductsIfNeeded(true);
        }
    }

    private void loadProductsIfNeeded(boolean force) {
        if (!isAdded() || productsLoading) return;
        long now = System.currentTimeMillis();
        boolean hasCachedProducts = !fullCatalog.isEmpty();
        if (!force && hasCachedProducts && now - lastProductLoadAt < PRODUCT_REFRESH_INTERVAL_MS) {
            applyRecommendationFilterFromPreferences();
            return;
        }

        productsLoading = true;
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            try {
                List<Product> products = new NetworkRepository(appContext).getAllProducts();
                mainHandler.post(() -> {
                    productsLoading = false;
                    if (!isAdded()) return;
                    productCatalog.clear();
                    fullCatalog.clear();
                    fullCatalog.addAll(products);
                    lastProductLoadAt = System.currentTimeMillis();
                    applyRecommendationFilterFromPreferences();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> productsLoading = false);
            }
        }).start();
    }

    private void setupHomeSearchOverlay() {
        if (homeSearchOverlay == null) return;

        homeSearchOverlay.bindCatalog(productCatalog);
        homeSearchOverlay.setProductListener(product -> {
            homeSearchOverlay.hideAnimated();
            openDetailSheet(product);
        });
        homeSearchOverlay.configureForOverlayOnHome(getViewLifecycleOwner());

        View cardSearch = requireView().findViewById(R.id.cardSearch);
        ImageButton btnSearchFilter = requireView().findViewById(R.id.btnSearchFilter);

        View.OnClickListener openSearch = v -> {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            homeSearchOverlay.showAnimated();
        };
        cardSearch.setOnClickListener(openSearch);
        btnSearchFilter.setOnClickListener(openSearch);
    }

    private void openDetailSheet(Product product) {
        Intent intent = ProductDetailActivity.newIntent(requireContext(), product);
        startActivity(intent);
    }

    private void applyCategoryFilter(@Nullable String categoryName) {
        productCatalog.clear();
        if (categoryName == null || categoryName.trim().isEmpty()) {
            productCatalog.addAll(fullCatalog);
            Toast.makeText(requireContext(), "Showing all listings", Toast.LENGTH_SHORT).show();
        } else {
            String target = CategoryUtils.displayTitle(categoryName);
            productCatalog.addAll(CategoryUtils.filterProductsByCategory(target, fullCatalog));
            Toast.makeText(requireContext(), "Showing " + target + " listings", Toast.LENGTH_SHORT).show();
        }
        if (tvRecCount != null) {
            tvRecCount.setText(getString(R.string.items_count_fmt, productCatalog.size()));
        }
        if (swipeStack != null) {
            swipeStack.setProducts(productCatalog);
        }
        if (homeSearchOverlay != null) {
            homeSearchOverlay.bindCatalog(productCatalog);
        }
    }

    private void applyRecommendationFilterFromPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences(
                AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        String csv = prefs.getString(AppPreferences.KEY_SELECTED_CATEGORIES, "");
        Set<String> selected = parseCsv(csv);

        if (selected.isEmpty()) {
            productCatalog.clear();
            productCatalog.addAll(fullCatalog);
        } else {
            List<Product> personalized = new ArrayList<>();
            for (Product product : fullCatalog) {
                if (selected.contains(CategoryUtils.normalizeCategory(product.getCategory()))) {
                    personalized.add(product);
                }
            }
            productCatalog.clear();
            productCatalog.addAll(personalized);
        }

        if (tvRecCount != null) {
            tvRecCount.setText(getString(R.string.items_count_fmt, productCatalog.size()));
        }
        if (swipeStack != null) {
            swipeStack.setProducts(productCatalog);
        }
        if (homeSearchOverlay != null) {
            homeSearchOverlay.bindCatalog(productCatalog);
        }
    }

    @NonNull
    private Set<String> parseCsv(@Nullable String csv) {
        Set<String> selected = new LinkedHashSet<>();
        if (TextUtils.isEmpty(csv)) {
            return selected;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            String normalized = CategoryUtils.normalizeCategory(part);
            if (!TextUtils.isEmpty(normalized)) {
                selected.add(normalized);
            }
        }
        return selected;
    }

}
