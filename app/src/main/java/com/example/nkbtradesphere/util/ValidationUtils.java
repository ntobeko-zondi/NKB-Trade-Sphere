package com.example.nkbtradesphere.util;

import android.text.TextUtils;
import android.util.Patterns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shared validation helpers for signup and password reset.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class
    }

    public static boolean isValidFullName(String fullName) {
        if (TextUtils.isEmpty(fullName)) {
            return false;
        }
        String trimmed = fullName.trim();
        return trimmed.length() >= 3 && trimmed.matches("^[A-Za-z][A-Za-z .'-]*[A-Za-z]$");
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email) {
        String normalized = normalizeEmail(email);
        return !TextUtils.isEmpty(normalized) && Patterns.EMAIL_ADDRESS.matcher(normalized).matches();
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        String cleaned = phone.trim().replaceAll("[\\s()-]", "");

        if (cleaned.startsWith("+27")) {
            return "0" + cleaned.substring(3);
        }

        if (cleaned.startsWith("27") && cleaned.length() == 11) {
            return "0" + cleaned.substring(2);
        }

        return cleaned;
    }

    /**
     * Accepts common South African mobile/landline formats:
     * 0821234567, +27821234567, or 27821234567.
     */
    public static boolean isValidPhone(String phone) {
        String normalized = normalizePhone(phone);
        return normalized.matches("^0[1-8][0-9]{8}$");
    }

    public static String normalizeIdNumber(String idNumber) {
        return idNumber == null ? "" : idNumber.replaceAll("\\D", "");
    }

    /**
     * Basic South African ID validation:
     * - 13 digits
     * - first 6 digits form a real date
     * - citizenship digit is 0 or 1
     * - final digit passes the Luhn checksum
     */
    public static boolean isValidSouthAfricanId(String idNumber) {
        String normalized = normalizeIdNumber(idNumber);

        if (!normalized.matches("^[0-9]{13}$")) {
            return false;
        }

        if (!hasValidDate(normalized.substring(0, 6))) {
            return false;
        }

        char citizenshipDigit = normalized.charAt(10);
        if (citizenshipDigit != '0' && citizenshipDigit != '1') {
            return false;
        }

        return passesLuhnCheck(normalized);
    }

    private static boolean hasValidDate(String yyMMdd) {
        String yy = yyMMdd.substring(0, 2);
        String mmdd = yyMMdd.substring(2);

        int currentYearTwoDigits = Integer.parseInt(new SimpleDateFormat("yy", Locale.ROOT).format(new Date()));
        int year = Integer.parseInt(yy);
        String century = year <= currentYearTwoDigits ? "20" : "19";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ROOT);
        dateFormat.setLenient(false);

        try {
            dateFormat.parse(century + yy + mmdd);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private static boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean doubleDigit = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasLetter && hasDigit;
    }
}
