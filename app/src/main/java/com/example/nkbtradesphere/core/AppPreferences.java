package com.example.nkbtradesphere.core;

/**
 * SharedPreferences file name and keys for session and UI settings.
 */
public final class AppPreferences {
    private AppPreferences() {
    }

    public static final String PREFS_NKB = "nkb_prefs";
    public static final String KEY_DARK_THEME = "dark_theme";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_PASSWORD = "user_password";
    public static final String KEY_LOGGED_IN = "logged_in";
    public static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    public static final String KEY_SELECTED_CATEGORIES = "selected_categories_csv";
}