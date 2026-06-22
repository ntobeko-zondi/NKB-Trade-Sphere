package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.ui.main.MainActivity;
import com.example.nkbtradesphere.util.GridSpacingItemDecoration;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen search: dark filter panel (category, condition, price) and result grid.
 */
public class HomeSearchOverlay extends FrameLayout {

    public interface Listener {
        void onProductSelected(Product product);
    }

    private static final String[] CATEGORY_LABELS = {
            "All", "Electronics", "Vehicles", "Fashion", "Home", "Health", "Baby", "Property", "Others"
    };
    private static final String[] CONDITION_LABELS = {
            "Any", "New", "Like New", "Excellent", "Good"
    };
    private final List<Product> catalog = new ArrayList<>();
    private final List<TextView> categoryChipViews = new ArrayList<>();
    private final List<TextView> conditionChipViews = new ArrayList<>();

    private EditText etSearch;
    private EditText etPriceMin;
    private EditText etPriceMax;
    private TextView tvClear;
    private TextView tvCancel;
    private TextView tvFooterResults;
    private TextView tvEmpty;
    private RecyclerView rv;
    private FlexboxLayout flexCategory;
    private FlexboxLayout flexCondition;
    private HomeSearchResultsAdapter adapter;
    private boolean searchGridSpacingAdded;

    private String selectedCategory = "All";
    private String selectedCondition = "Any";

    @Nullable
    private Listener listener;
    @Nullable
    private OnBackPressedCallback backCallback;
    private boolean overlayOnHome = true;

    public HomeSearchOverlay(Context context) {
        this(context, null);
    }

    public HomeSearchOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        LayoutInflater.from(context).inflate(R.layout.view_home_search_overlay, this, true);
        initViews();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void initViews() {
        etSearch = findViewById(R.id.etHomeSearch);
        etPriceMin = findViewById(R.id.etSearchPriceMin);
        etPriceMax = findViewById(R.id.etSearchPriceMax);
        tvClear = findViewById(R.id.tvHomeSearchClear);
        tvCancel = findViewById(R.id.tvHomeSearchCancel);
        tvFooterResults = findViewById(R.id.tvSearchResultsFooter);
        tvEmpty = findViewById(R.id.tvHomeSearchEmpty);
        rv = findViewById(R.id.rvHomeSearch);
        flexCategory = findViewById(R.id.flexSearchCategory);
        flexCondition = findViewById(R.id.flexSearchCondition);

        adapter = new HomeSearchResultsAdapter(getContext(), p -> {
            if (listener != null) {
                listener.onProductSelected(p);
            }
        });
        applySearchGridLayout();
        rv.setAdapter(adapter);

        tvClear.setOnClickListener(v -> {
            etSearch.setText("");
            updateClearVisibility();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
                updateClearVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        TextWatcher priceWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etPriceMin.addTextChangedListener(priceWatcher);
        etPriceMax.addTextChangedListener(priceWatcher);

        buildChipRow(flexCategory, CATEGORY_LABELS, selectedCategory, label -> {
            selectedCategory = label;
            for (TextView c : categoryChipViews) {
                styleFilterChip(c, label.equals(c.getText().toString()));
            }
            applyFilters();
        }, categoryChipViews);

        buildChipRow(flexCondition, CONDITION_LABELS, selectedCondition, label -> {
            selectedCondition = label;
            for (TextView c : conditionChipViews) {
                styleFilterChip(c, label.equals(c.getText().toString()));
            }
            applyFilters();
        }, conditionChipViews);

        setVisibility(GONE);
    }

    private void buildChipRow(FlexboxLayout flex, String[] labels, String initialSelected,
                              ChipSelectListener onSelect, List<TextView> chipList) {
        flex.removeAllViews();
        chipList.clear();
        for (String label : labels) {
            TextView chip = new TextView(getContext());
            chip.setText(label);
            FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            int m = dp(6);
            lp.setMargins(0, 0, m, m);
            chip.setLayoutParams(lp);
            int ph = dp(14);
            int pv = dp(9);
            chip.setPadding(ph, pv, ph, pv);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            int lineHeightPx = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 16f, getResources().getDisplayMetrics()));
            TextViewCompat.setLineHeight(chip, lineHeightPx);
            chip.setOnClickListener(v -> onSelect.onSelect(label));
            flex.addView(chip);
            chipList.add(chip);
            styleFilterChip(chip, label.equals(initialSelected));
        }
    }

    private void styleFilterChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_search_chip_filled : R.drawable.bg_search_chip_outline);
        chip.setTextColor(selected
                ? ContextCompat.getColor(getContext(), R.color.white)
                : ContextCompat.getColor(getContext(), R.color.search_chip_text));
        chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private interface ChipSelectListener {
        void onSelect(String label);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw && rv != null) {
            applySearchGridLayout();
        }
    }

    private void applySearchGridLayout() {
        if (rv == null) {
            return;
        }
        if (!searchGridSpacingAdded) {
            int spacingPx = Math.round(10f * getResources().getDisplayMetrics().density);
            rv.addItemDecoration(new GridSpacingItemDecoration(spacingPx));
            searchGridSpacingAdded = true;
        }

        int swDp = getResources().getConfiguration().screenWidthDp;
        int span = (swDp >= 720) ? 3 : 2;

        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        if (lm instanceof GridLayoutManager) {
            GridLayoutManager glm = (GridLayoutManager) lm;
            if (glm.getSpanCount() != span) {
                glm.setSpanCount(span);
                RecyclerView.Adapter<?> a = rv.getAdapter();
                if (a != null) {
                    a.notifyDataSetChanged();
                }
            }
        } else {
            rv.setLayoutManager(new GridLayoutManager(getContext(), span));
        }
    }

    public void bindCatalog(@Nullable List<Product> products) {
        catalog.clear();
        if (products != null) {
            catalog.addAll(products);
        }
        applyFilters();
    }

    public void setProductListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void configureForOverlayOnHome(@NonNull LifecycleOwner owner) {
        overlayOnHome = true;
        removeBackCallback();
        if (!(getContext() instanceof FragmentActivity)) {
            return;
        }
        FragmentActivity activity = (FragmentActivity) getContext();
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                hideAnimated();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(owner, backCallback);
        tvCancel.setText(getContext().getString(R.string.search_cancel));
        tvCancel.setOnClickListener(v -> hideAnimated());
    }

    public void configureForPrimaryTab(@NonNull Runnable onLeaveSearch) {
        overlayOnHome = false;
        removeBackCallback();
        tvCancel.setText(getContext().getString(R.string.search_cancel));
        tvCancel.setOnClickListener(v -> onLeaveSearch.run());
    }

    private void removeBackCallback() {
        if (backCallback != null) {
            backCallback.remove();
            backCallback = null;
        }
    }

    public boolean isOverlayVisible() {
        return getVisibility() == VISIBLE;
    }

    public void showAnimated() {
        if (overlayOnHome && backCallback != null) {
            backCallback.setEnabled(true);
        }
        setVisibility(VISIBLE);
        int h = getResources().getDisplayMetrics().heightPixels;
        setTranslationY(h);
        animate()
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        applyFilters();
        etSearch.post(() -> {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    public void hideAnimated() {
        if (!overlayOnHome) {
            return;
        }
        if (getVisibility() != VISIBLE) {
            return;
        }
        if (backCallback != null) {
            backCallback.setEnabled(false);
        }
        InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
        etSearch.clearFocus();

        int h = getResources().getDisplayMetrics().heightPixels;
        animate()
                .translationY(h)
                .setDuration(260)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    setVisibility(GONE);
                    setTranslationY(0f);
                })
                .start();
    }

    public void showImmediate() {
        setVisibility(VISIBLE);
        setTranslationY(0f);
        setAlpha(1f);
        applyFilters();
        etSearch.post(() -> {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void updateClearVisibility() {
        tvClear.setVisibility(etSearch.getText().length() > 0 ? VISIBLE : GONE);
    }

    private void applyFilters() {
        String raw = etSearch.getText().toString();
        String q = raw == null ? "" : raw.trim().toLowerCase(Locale.US);
        List<Product> out = new ArrayList<>();
        for (Product p : catalog) {
            if (!textMatchesQuery(p, q)) {
                continue;
            }
            if (!categoryMatches(p)) {
                continue;
            }
            if (!conditionMatches(p)) {
                continue;
            }
            if (!priceMatches(p)) {
                continue;
            }
            out.add(p);
        }
        adapter.setItems(out);

        tvFooterResults.setText(getContext().getString(R.string.home_search_footer_results, out.size()));
        tvEmpty.setVisibility(out.isEmpty() ? VISIBLE : GONE);
    }

    private static boolean textMatchesQuery(Product p, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return containsLower(p.getName(), q)
                || containsLower(p.getDescription(), q)
                || containsLower(p.getLocation(), q)
                || containsLower(p.getSellerName(), q)
                || containsLower(p.getCategory(), q);
    }

    private boolean categoryMatches(Product p) {
        if ("All".equals(selectedCategory)) {
            return true;
        }
        String c = p.getCategory();
        if (c == null) {
            return false;
        }
        if ("Home".equals(selectedCategory)) {
            return c.equalsIgnoreCase("Furniture")
                    || c.equalsIgnoreCase("Home & Living")
                    || c.equalsIgnoreCase("Home");
        }
        if ("Others".equals(selectedCategory)) {
            String u = c.toLowerCase(Locale.US);
            return !u.equals("electronics")
                    && !u.equals("vehicles")
                    && !u.equals("fashion")
                    && !u.equals("furniture")
                    && !u.equals("health")
                    && !u.equals("baby")
                    && !u.equals("property");
        }
        return c.equalsIgnoreCase(selectedCategory);
    }

    private boolean conditionMatches(Product p) {
        if ("Any".equals(selectedCondition)) {
            return true;
        }
        if ("New".equals(selectedCondition)) {
            String cond = p.getCondition();
            String badge = p.getBadge();
            return (cond != null && cond.equalsIgnoreCase("New"))
                    || (badge != null && badge.equalsIgnoreCase("New"));
        }
        String cond = p.getCondition();
        return cond != null && cond.equalsIgnoreCase(selectedCondition);
    }

    private boolean priceMatches(Product p) {
        int min = parseOptionalInt(etPriceMin.getText().toString());
        int max = parseOptionalInt(etPriceMax.getText().toString());
        int price = parseRandFromPrice(p.getPrice());
        if (min > 0 && price < min) {
            return false;
        }
        if (max > 0 && price > max) {
            return false;
        }
        return true;
    }

    private static int parseOptionalInt(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim().replaceAll("\\s+", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseRandFromPrice(String price) {
        if (price == null) {
            return 0;
        }
        String digits = price.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean containsLower(String field, String q) {
        return field != null && field.toLowerCase(Locale.US).contains(q);
    }
}
