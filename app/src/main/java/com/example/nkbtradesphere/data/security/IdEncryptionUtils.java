package com.example.nkbtradesphere.data.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-GCM encryption for sensitive PII like ID numbers.
 * Uses Android KeyStore for secure key storage (API 23+).
 * Falls back to simple Base64 encoding on older devices for compatibility.
 */
public final class IdEncryptionUtils {

    private static final String KEYSTORE_ALIAS = "nkb_trade_sphere_id_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String ENCRYPTION_MODE = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12; // 96 bits for GCM
    private static final int KEY_SIZE = 256;

    private IdEncryptionUtils() {
    }

    /**
     * Encrypt an ID number.
     * Format: base64(iv || ciphertext || tag)
     */
    @NonNull
    public static String encryptId(@NonNull String plainId, @NonNull Context context) {
        if (plainId.isEmpty()) {
            return plainId;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return encryptIdAndroid23Plus(plainId, context);
        } else {
            // Fallback for older devices - just Base64 encode with prefix to indicate it's not encrypted
            return "UNENCRYPTED_" + Base64.encodeToString(plainId.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        }
    }

    /**
     * Decrypt an ID number.
     */
    @NonNull
    public static String decryptId(@NonNull String encryptedId, @NonNull Context context) {
        if (encryptedId.isEmpty()) {
            return encryptedId;
        }

        // Handle unencrypted fallback format
        if (encryptedId.startsWith("UNENCRYPTED_")) {
            String base64Part = encryptedId.substring("UNENCRYPTED_".length());
            byte[] decoded = Base64.decode(base64Part, Base64.NO_WRAP);
            return new String(decoded, StandardCharsets.UTF_8);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return decryptIdAndroid23Plus(encryptedId, context);
        }

        return encryptedId; // Fallback - return as-is
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static String encryptIdAndroid23Plus(@NonNull String plainId, @NonNull Context context) {
        try {
            SecretKey key = getOrCreateKey(context);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_MODE);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] plainBytes = plainId.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plainBytes);

            // Combine IV and ciphertext (ciphertext already includes authentication tag for GCM)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt ID: " + e.getMessage(), e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static String decryptIdAndroid23Plus(@NonNull String encryptedId, @NonNull Context context) {
        try {
            byte[] combined = Base64.decode(encryptedId, Base64.NO_WRAP);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            SecretKey key = getOrCreateKey(context);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_MODE);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt ID: " + e.getMessage(), e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static SecretKey getOrCreateKey(@NonNull Context context) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        }

        KeyGenerator keyGen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
    }
}
