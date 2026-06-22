package com.example.nkbtradesphere.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared formatting for listing posted dates.
 * Converts PostgreSQL/API timestamps such as "2026-05-17 13:42:10.123456"
 * into a user-friendly card label such as "17 May 2026".
 */
public final class PostedDateFormat {

    private static final Pattern LONG_FRACTION = Pattern.compile("(\\.\\d{3})\\d+");
    private static final Pattern ISO_OFFSET = Pattern.compile("([+-]\\d{2}:?\\d{2})$");

    private PostedDateFormat() {
    }

    @NonNull
    public static String displayDate(@Nullable String rawDate) {
        Date parsed = parse(rawDate);
        if (parsed == null) {
            return TextUtils.isEmpty(rawDate) ? "—" : rawDate.trim();
        }
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsed);
    }

    @Nullable
    private static Date parse(@Nullable String rawDate) {
        if (TextUtils.isEmpty(rawDate)) {
            return null;
        }

        String value = rawDate.trim();
        if (value.isEmpty()) {
            return null;
        }

        if (value.matches("^\\d{10,13}$")) {
            try {
                long millis = Long.parseLong(value);
                if (value.length() == 10) {
                    millis *= 1000L;
                }
                return new Date(millis);
            } catch (NumberFormatException ignored) {
                // Continue to formatted parsing.
            }
        }

        String normalized = normalize(value);
        String[] patterns = new String[] {
                "yyyy-MM-dd HH:mm:ss.SSSX",
                "yyyy-MM-dd HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(false);
                if (pattern.endsWith("X")) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                return sdf.parse(normalized);
            } catch (ParseException ignored) {
                // Try the next pattern.
            }
        }
        return null;
    }

    @NonNull
    private static String normalize(@NonNull String value) {
        String out = value.trim();
        out = out.replace('T', ' ');
        if (out.endsWith("Z")) {
            out = out.substring(0, out.length() - 1) + "+0000";
        }

        Matcher offset = ISO_OFFSET.matcher(out);
        if (offset.find()) {
            String zone = offset.group(1).replace(":", "");
            out = out.substring(0, offset.start(1)) + zone;
        }

        Matcher fraction = LONG_FRACTION.matcher(out);
        out = fraction.replaceFirst("$1");
        return out;
    }
}
