package com.example.nkbtradesphere.ui.main;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.ui.home.HomeFragment;
import com.example.nkbtradesphere.ui.messages.MessagesFragment;
import com.example.nkbtradesphere.ui.onboarding.OnboardingInterestScreen;
import com.example.nkbtradesphere.ui.profile.ProfileFragment;
import com.example.nkbtradesphere.ui.search.SearchFragment;
import com.example.nkbtradesphere.ui.sell.SellFragment;
import com.example.nkbtradesphere.ui.welcome.WelcomeActivity;
import com.example.nkbtradesphere.util.MessageNotificationHelper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 1201;
    private static final String STATE_SELECTED_TAB = "main_selected_nav_tab";
    private static final String TAG_HOME = "tab_home";
    private static final String TAG_SELL = "tab_sell";
    private static final String TAG_SEARCH = "tab_search";
    private static final String TAG_MESSAGES = "tab_messages";
    private static final String TAG_PROFILE = "tab_profile";

    private int selectedTabId = R.id.nav_home;

    private View navHome;
    private View navSell;
    private View navSearch;
    private View navMessages;
    private View navProfile;

    private ViewTreeObserver.OnGlobalLayoutListener floatingDockLayoutListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);

        if (!hasValidSavedSession(prefs)) {
            prefs.edit()
                    .putBoolean(AppPreferences.KEY_LOGGED_IN, false)
                    .remove(AppPreferences.KEY_USER_EMAIL)
                    .remove(AppPreferences.KEY_USER_NAME)
                    .apply();
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (!prefs.getBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, false)) {
            Intent intent = new Intent(this, OnboardingInterestScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        maybeRequestNotificationPermission();
        refreshUnreadMessageNotification();

        AppCompatDelegate.setDefaultNightMode(
                prefs.getBoolean(AppPreferences.KEY_DARK_THEME, true)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        configureTransparentSystemNavBar();
        findViewById(R.id.floating_nav_host).bringToFront();
        bindFloatingNav();
        applySystemBarInsets();

        if (savedInstanceState == null) {
            selectedTabId = R.id.nav_home;
        } else {
            selectedTabId = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home);
        }
        showTab(selectedTabId);
        updateNavSelection(selectedTabId);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTabId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUnreadMessageNotification();
    }


    private boolean hasValidSavedSession(SharedPreferences prefs) {
        if (!prefs.getBoolean(AppPreferences.KEY_LOGGED_IN, false)) {
            return false;
        }

        String email = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
        // Avoid blocking cold start with DB I/O here.
        return !TextUtils.isEmpty(email);
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQ_POST_NOTIFICATIONS);
    }

    private void refreshUnreadMessageNotification() {
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        String userId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
        if (TextUtils.isEmpty(userId)) {
            MessageNotificationHelper.cancelUnreadMessagesNotification(this);
            return;
        }
        ApiClient.initialize(this);
        ApiClient.getUnreadMessageCount(userId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                int unreadCount = response.optInt("count", 0);
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    if (unreadCount > 0) {
                        MessageNotificationHelper.showUnreadMessagesNotification(
                                MainActivity.this,
                                unreadCount,
                                "",
                                "New message",
                                0
                        );
                    } else {
                        MessageNotificationHelper.cancelUnreadMessagesNotification(MainActivity.this);
                    }
                });
            }

            @Override
            public void onError(String error) {
                MessageNotificationHelper.cancelUnreadMessagesNotification(MainActivity.this);
            }
        });
    }

    @Override
    protected void onDestroy() {
        View dock = findViewById(R.id.floating_nav_host);
        if (dock != null && floatingDockLayoutListener != null) {
            dock.getViewTreeObserver().removeOnGlobalLayoutListener(floatingDockLayoutListener);
            floatingDockLayoutListener = null;
        }
        super.onDestroy();
    }

    private void bindFloatingNav() {
        navHome = findViewById(R.id.nav_home);
        navSell = findViewById(R.id.nav_sell);
        navSearch = findViewById(R.id.nav_search);
        navMessages = findViewById(R.id.nav_messages);
        navProfile = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> navigateTo(R.id.nav_home));
        navSell.setOnClickListener(v -> navigateTo(R.id.nav_sell));
        navSearch.setOnClickListener(v -> navigateTo(R.id.nav_search));
        navMessages.setOnClickListener(v -> navigateTo(R.id.nav_messages));
        navProfile.setOnClickListener(v -> navigateTo(R.id.nav_profile));
    }

    private void navigateTo(int tabId) {
        if (tabId == selectedTabId) {
            refreshFloatingDockAndContentPadding();
            return;
        }
        selectedTabId = tabId;
        showTab(tabId);
        updateNavSelection(tabId);
    }

    private void showTab(int tabId) {
        String selectedTag = tagForTab(tabId);
        if (selectedTag == null) return;

        Fragment selected = getSupportFragmentManager().findFragmentByTag(selectedTag);
        if (selected == null) {
            selected = createFragmentForTab(tabId);
        }
        if (selected == null) return;

        FragmentTransaction tx = getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true);

        hideIfExists(tx, TAG_HOME);
        hideIfExists(tx, TAG_SELL);
        hideIfExists(tx, TAG_SEARCH);
        hideIfExists(tx, TAG_MESSAGES);
        hideIfExists(tx, TAG_PROFILE);

        if (selected.isAdded()) {
            tx.show(selected);
        } else {
            tx.add(R.id.fragment_container, selected, selectedTag);
        }
        tx.commit();

        View dockHost = findViewById(R.id.floating_nav_host);
        if (dockHost != null) {
            dockHost.post(this::refreshFloatingDockAndContentPadding);
        }
    }

    private void hideIfExists(@NonNull FragmentTransaction tx, @NonNull String tag) {
        Fragment existing = getSupportFragmentManager().findFragmentByTag(tag);
        if (existing != null && existing.isAdded()) {
            tx.hide(existing);
        }
    }

    private Fragment createFragmentForTab(int tabId) {
        if (tabId == R.id.nav_home) {
            return new HomeFragment();
        } else if (tabId == R.id.nav_search) {
            return new SearchFragment();
        } else if (tabId == R.id.nav_sell) {
            return new SellFragment();
        } else if (tabId == R.id.nav_messages) {
            return new MessagesFragment();
        } else if (tabId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return null;
    }

    private String tagForTab(int tabId) {
        if (tabId == R.id.nav_home) return TAG_HOME;
        if (tabId == R.id.nav_sell) return TAG_SELL;
        if (tabId == R.id.nav_search) return TAG_SEARCH;
        if (tabId == R.id.nav_messages) return TAG_MESSAGES;
        if (tabId == R.id.nav_profile) return TAG_PROFILE;
        return null;
    }

    private void updateNavSelection(int selectedTabId) {
        navHome.setSelected(selectedTabId == R.id.nav_home);
        navSell.setSelected(selectedTabId == R.id.nav_sell);
        navSearch.setSelected(selectedTabId == R.id.nav_search);
        navMessages.setSelected(selectedTabId == R.id.nav_messages);
        navProfile.setSelected(selectedTabId == R.id.nav_profile);
    }

    /**
     * Removes the default opaque system navigation “shoebox” so the screen background continues
     * behind the gesture area and only the custom chips read as floating.
     */
    private void configureTransparentSystemNavBar() {
        Window window = getWindow();
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(window, window.getDecorView());
        c.setAppearanceLightNavigationBars(!night);
    }

    /**
     * Draw behind system bars; float the nav above content; pad the fragment bottom so lists
     * can scroll under the dock while the last items stay reachable.
     */
    private void applySystemBarInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View mainRoot = findViewById(R.id.main_root);
        View dockHost = findViewById(R.id.floating_nav_host);

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            refreshFloatingDockAndContentPadding();
            return insets;
        });

        if (floatingDockLayoutListener == null) {
            floatingDockLayoutListener = this::refreshFloatingDockAndContentPadding;
            dockHost.getViewTreeObserver().addOnGlobalLayoutListener(floatingDockLayoutListener);
        }

        ViewCompat.requestApplyInsets(mainRoot);
        dockHost.post(this::refreshFloatingDockAndContentPadding);
    }

    private void refreshFloatingDockAndContentPadding() {
        View mainRoot = findViewById(R.id.main_root);
        View fragmentContainer = findViewById(R.id.fragment_container);
        View dockHost = findViewById(R.id.floating_nav_host);
        if (mainRoot == null || fragmentContainer == null || dockHost == null) {
            return;
        }

        int topPad = 0;
        int navBarBottom = 0;
        WindowInsetsCompat wi = ViewCompat.getRootWindowInsets(mainRoot);
        if (wi != null) {
            Insets topInset = wi.getInsets(WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets navBars = wi.getInsets(WindowInsetsCompat.Type.navigationBars());
            topPad = topInset.top;
            navBarBottom = navBars.bottom;
        }

        dockHost.setPaddingRelative(
                dockHost.getPaddingStart(),
                dockHost.getPaddingTop(),
                dockHost.getPaddingEnd(),
                navBarBottom);

        int dockHeight = dockHost.getHeight();
        if (dockHeight <= 0) {
            dockHeight = getResources().getDimensionPixelSize(R.dimen.floating_nav_clearance_fallback);
        }
        int guard = getResources().getDimensionPixelSize(R.dimen.floating_nav_content_guard);
        // Do not pad fragment_container bottom: that leaves an empty strip (looks like a “nav box”).
        // Pad scroll / inset targets instead so content draws full-bleed under the floating dock.
        fragmentContainer.setPadding(0, topPad, 0, 0);
        applyFloatingNavBottomInsetToContent(navBarBottom + dockHeight + guard);
    }

    private void applyFloatingNavBottomInsetToContent(int bottomInsetPx) {
        View raw = findViewById(R.id.fragment_container);
        if (!(raw instanceof ViewGroup)) {
            return;
        }
        ViewGroup container = (ViewGroup) raw;
        View fallbackTarget = null;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            View insetTarget = child.findViewById(R.id.nav_scroll_bottom_inset);
            if (insetTarget == null) {
                continue;
            }
            if (fallbackTarget == null) {
                fallbackTarget = insetTarget;
            }
            if (child.getVisibility() == View.VISIBLE) {
                insetTarget.setPaddingRelative(
                        insetTarget.getPaddingStart(),
                        insetTarget.getPaddingTop(),
                        insetTarget.getPaddingEnd(),
                        bottomInsetPx);
                return;
            }
        }
        if (fallbackTarget != null) {
            fallbackTarget.setPaddingRelative(
                    fallbackTarget.getPaddingStart(),
                    fallbackTarget.getPaddingTop(),
                    fallbackTarget.getPaddingEnd(),
                    bottomInsetPx);
        }
    }


    /** Used when leaving Search (no fragment back stack — avoids finishing the activity). */
    public void selectHomeTab() {
        navigateTo(R.id.nav_home);
    }

    /** Allows other screens/fragments to jump directly to Messages tab. */
    public void selectMessagesTab() {
        navigateTo(R.id.nav_messages);
    }
}
