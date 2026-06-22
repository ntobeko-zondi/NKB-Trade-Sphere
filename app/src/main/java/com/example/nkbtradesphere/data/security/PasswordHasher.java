package com.example.nkbtradesphere.data.security;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Local-only salted SHA-256 for demo accounts until external auth replaces verification.
 */
public final class PasswordHasher {

    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    @NonNull
    public static String generateSaltHex() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    @NonNull
    public static String hashPassword(@NonNull String saltHex, @NonNull String plainPassword) {
        byte[] salt = hexToBytes(saltHex);
        return bytesToHex(sha256(concat(salt, plainPassword.getBytes(StandardCharsets.UTF_8))));
    }

    public static boolean verify(
            @NonNull String saltHex,
            @NonNull String storedHashHex,
            @NonNull String plainPassword) {
        String computed = hashPassword(saltHex, plainPassword);
        byte[] a = hexToBytes(storedHashHex);
        byte[] b = hexToBytes(computed);
        return MessageDigest.isEqual(a, b);
    }

    @NonNull
    private static byte[] sha256(@NonNull byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @NonNull
    private static byte[] concat(@NonNull byte[] a, @NonNull byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    @NonNull
    private static String bytesToHex(@NonNull byte[] raw) {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[raw.length * 2];
        for (int i = 0; i < raw.length; i++) {
            int v = raw[i] & 0xff;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0f];
        }
        return new String(out);
    }

    @NonNull
    private static byte[] hexToBytes(@NonNull String hex) {
        int n = hex.length();
        if ((n & 1) != 0) {
            return new byte[0];
        }
        byte[] out = new byte[n / 2];
        for (int i = 0; i < n; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                return new byte[0];
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
