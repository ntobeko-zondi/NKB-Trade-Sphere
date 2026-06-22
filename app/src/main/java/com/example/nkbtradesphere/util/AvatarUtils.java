package com.example.nkbtradesphere.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public final class AvatarUtils {

    private AvatarUtils() {
    }

    public static Drawable createInitialsAvatar(Context context, String name, int sizeDp) {
        String initials = extractInitials(name);
        int sizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sizeDp,
                context.getResources().getDisplayMetrics());

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(resolveColor(name));
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sizePx * 0.40f);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = sizePx / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(initials, sizePx / 2f, textY, textPaint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static String extractInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
            }
            if (result.length() == 2) {
                break;
            }
        }
        if (result.length() == 0) {
            return "?";
        }
        return result.toString();
    }

    private static int resolveColor(String seed) {
        int hash = seed == null ? 0 : Math.abs(seed.hashCode());
        int hue = hash % 360;
        // Avoid pink/magenta range.
        if (hue >= 290 && hue <= 345) {
            hue = (hue + 80) % 360;
        }
        float saturation = 0.58f + ((hash / 360) % 20) / 100f; // 0.58 .. 0.77
        float value = 0.70f + ((hash / 3600) % 18) / 100f;     // 0.70 .. 0.87
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }
}
