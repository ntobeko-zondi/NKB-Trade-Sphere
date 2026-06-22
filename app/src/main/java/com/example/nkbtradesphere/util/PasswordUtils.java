package com.example.nkbtradesphere.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with per-user random salt and PBKDF2 key stretching.
 * <p>
 * New passwords use {@code pbkdf2$<algorithm>$<iterations>$<salt_b64>$<hash_b64>}.
 * Legacy rows may still use a single Base64 blob of {@code salt(16) || sha256(salt||password)} — those verify
 * and are upgraded to the PBKDF2 format on successful login.
 */
public class PasswordUtils {

    private static final String LEGACY_SHA256 = "SHA-256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final String PBKDF2_PREFIX = "pbkdf2$";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Preferred PBKDF2 PRF (API 26+). Falls back on older devices. */
    private static String preferredPbkdf2Algorithm() {
        try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return "PBKDF2WithHmacSHA256";
        } catch (NoSuchAlgorithmException e) {
            return "PBKDF2WithHmacSHA1";
        }
    }

    /**
     * Hash a password for storage (random salt + PBKDF2).
     */
    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password");
        }
        String algorithm = preferredPbkdf2Algorithm();
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(algorithm, password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTES * 8);
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);
        return PBKDF2_PREFIX + algorithm + "$" + PBKDF2_ITERATIONS + "$" + saltB64 + "$" + hashB64;
    }

    /**
     * Returns true if the stored value is the older SHA-256 + embedded salt format (not PBKDF2).
     */
    public static boolean isLegacyPasswordHash(String storedValue) {
        return storedValue != null && !storedValue.isEmpty() && !storedValue.startsWith(PBKDF2_PREFIX);
    }

    /**
     * Verify a plaintext password against a stored hash (PBKDF2 or legacy SHA-256 + salt).
     */
    public static boolean verifyPassword(String password, String storedValue) {
        if (password == null || storedValue == null || storedValue.trim().isEmpty()) {
            return false;
        }
        if (storedValue.startsWith(PBKDF2_PREFIX)) {
            return verifyPbkdf2(password, storedValue);
        }
        return verifyLegacySha256Salted(password, storedValue);
    }

    private static boolean verifyPbkdf2(String password, String storedValue) {
        try {
            String body = storedValue.substring(PBKDF2_PREFIX.length());
            String[] parts = body.split("\\$", 4);
            if (parts.length != 4) {
                return false;
            }
            String algorithm = parts[0];
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            if (salt.length == 0 || expected.length == 0) {
                return false;
            }
            byte[] computed = pbkdf2(algorithm, password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(computed, expected);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String algorithm, char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 failed", e);
        }
    }

    /**
     * Legacy: random 16-byte salt concatenated with SHA-256(salt || password bytes).
     */
    private static boolean verifyLegacySha256Salted(String password, String storedValue) {
        try {
            byte[] saltAndHash = Base64.decode(storedValue, Base64.NO_WRAP);
            if (saltAndHash.length <= SALT_BYTES) {
                return false;
            }
            byte[] salt = new byte[SALT_BYTES];
            System.arraycopy(saltAndHash, 0, salt, 0, SALT_BYTES);

            MessageDigest digest = MessageDigest.getInstance(LEGACY_SHA256);
            digest.update(salt);
            byte[] computedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            byte[] storedHash = new byte[saltAndHash.length - SALT_BYTES];
            System.arraycopy(saltAndHash, SALT_BYTES, storedHash, 0, storedHash.length);

            return MessageDigest.isEqual(computedHash, storedHash);
        } catch (IllegalArgumentException | IndexOutOfBoundsException | NoSuchAlgorithmException e) {
            return false;
        }
    }
}
