package com.example.nkbtradesphere.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared formatting for rating values across cards, detail pages and profile screens.
 * Numeric ratings are always displayed rounded to one decimal place.
 */
public final class RatingFormat {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)");

    private RatingFormat() {
    }

    /**
     * Returns only the numeric rating text rounded to one decimal place, for example "4.3".
     * Blank input returns an empty string. Non-numeric labels are returned unchanged.
     */
    @NonNull
    public static String valueOnly(@Nullable String rating) {
        if (rating == null || rating.trim().isEmpty()) {
            return "";
        }
        String r = rating.trim();
        Matcher matcher = NUMBER_PATTERN.matcher(r);
        if (!matcher.find()) {
            return r;
        }
        try {
            double value = Double.parseDouble(matcher.group());
            return String.format(Locale.US, "%.1f", value);
        } catch (NumberFormatException ignored) {
            return r;
        }
    }

    /** Empty string when blank; otherwise returns star-prefixed text such as "★ 4.3". */
    @NonNull
    public static String starLine(@Nullable String rating) {
        String formatted = valueOnly(rating);
        if (formatted.isEmpty()) {
            return "";
        }
        if (formatted.startsWith("★") || formatted.startsWith("\u2605")) {
            return formatted;
        }
        return "★ " + formatted;
    }
}
