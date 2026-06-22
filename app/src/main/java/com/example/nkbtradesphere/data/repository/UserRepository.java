package com.example.nkbtradesphere.data.repository;

import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.data.model.LocalUser;
import com.example.nkbtradesphere.data.security.PasswordHasher;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * User authentication now handled via API (ApiClient).
 * Local database support removed - use ApiClient.authenticateUser and ApiClient.registerUser instead.
 */
public final class UserRepository {

    public UserRepository(@NonNull Context context) {
        // Constructor kept for backward compatibility but no-op now
    }

    /**
     * Normalizes email for storage and lookup (lowercase trim).
     */
    @NonNull
    public static String normalizeEmail(@NonNull String email) {
        return email.trim().toLowerCase();
    }

    /**
     * Inserts a new user. Returns row id, or -1 if email already exists.
     * @deprecated Use ApiClient.registerUser() instead
     */
    @Deprecated
    public long insertUser(
            @NonNull String email,
            @NonNull String displayName,
            @NonNull String plainPassword) {
        return -1; // Local database no longer supported, use API
    }

    /**
     * Gets a user by email.
     * @deprecated Use ApiClient.getUserDetails() instead
     */
    @Deprecated
    @Nullable
    public LocalUser getUserByEmail(@NonNull String email) {
        return null; // Local database no longer supported, use API
    }

    /**
     * Verifies a password.
     * @deprecated Use ApiClient.authenticateUser() instead
     */
    @Deprecated
    public boolean verifyPassword(@NonNull LocalUser user, @NonNull String plainPassword) {
        return false; // Local database no longer supported, use API
    }

    /**
     * Migrates legacy prefs if needed.
     * @deprecated No longer needed, using API authentication
     */
    @Deprecated
    public void migrateLegacyPrefsIfNeeded(@NonNull SharedPreferences prefs) {
        // No-op: local database no longer supported
    }

    @Nullable
    private static LocalUser localUserFromCursor(@NonNull Cursor c) {
        return null; // Local database no longer supported
    }

    @Nullable
    private static String getStringCol(@NonNull Cursor c, @NonNull String column) {
        return null; // Local database no longer supported
    }
}
