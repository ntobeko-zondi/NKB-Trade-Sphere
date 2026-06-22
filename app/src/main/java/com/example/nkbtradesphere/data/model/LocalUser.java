package com.example.nkbtradesphere.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Row from {@link com.example.nkbtradesphere.data.db.ProductDbHelper#TABLE_USERS} for auth and session display.
 */
public final class LocalUser {

    private final long id;
    @NonNull
    private final String email;
    @NonNull
    private final String displayName;
    @NonNull
    private final String passwordSaltHex;
    @NonNull
    private final String passwordHashHex;
    @Nullable
    private final String externalAuthId;

    public LocalUser(
            long id,
            @NonNull String email,
            @NonNull String displayName,
            @NonNull String passwordSaltHex,
            @NonNull String passwordHashHex,
            @Nullable String externalAuthId) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordSaltHex = passwordSaltHex;
        this.passwordHashHex = passwordHashHex;
        this.externalAuthId = externalAuthId;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getPasswordSaltHex() {
        return passwordSaltHex;
    }

    @NonNull
    public String getPasswordHashHex() {
        return passwordHashHex;
    }

    @Nullable
    public String getExternalAuthId() {
        return externalAuthId;
    }
}
