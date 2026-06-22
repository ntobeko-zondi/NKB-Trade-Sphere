package com.example.nkbtradesphere.util;

import com.example.nkbtradesphere.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

/**
 * Rich wishlist / save feedback: heart swap, overshoot pop, wobble, haptics, and depth pulse.
 */
public final class WishlistAnimator {

    private WishlistAnimator() {}

    public static void applyProductDetailHeader(ImageView view, boolean saved) {
        if (saved) {
            view.setImageResource(R.drawable.ic_wishlist_heart_filled);
            ImageViewCompat.setImageTintList(view,
                    ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.orange)));
        } else {
            view.setImageResource(R.drawable.ic_wishlist_heart_outline);
            ImageViewCompat.setImageTintList(view,
                    ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.white)));
        }
    }

    public static void applyProductCard(ImageView view, boolean saved) {
        Context ctx = view.getContext();
        int orange = ContextCompat.getColor(ctx, R.color.orange);
        int muted = ContextCompat.getColor(ctx, R.color.text_muted);
        if (saved) {
            view.setImageResource(R.drawable.ic_wishlist_heart_filled);
            ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(orange));
        } else {
            view.setImageResource(R.drawable.ic_wishlist_heart_outline);
            ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(muted));
        }
    }

    public static void applyBottomSheet(ImageView view, boolean saved) {
        Context ctx = view.getContext();
        int orange = ContextCompat.getColor(ctx, R.color.orange);
        int muted = ContextCompat.getColor(ctx, R.color.text_muted);
        if (saved) {
            view.setImageResource(R.drawable.ic_wishlist_heart_filled);
            ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(orange));
        } else {
            view.setImageResource(R.drawable.ic_wishlist_heart_outline);
            ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(muted));
        }
    }

    /**
     * Call after {@link #applyProductDetailHeader} / {@link #applyProductCard} / {@link #applyBottomSheet}
     * so the new drawable is visible during the motion.
     */
    public static void playToggleCelebration(View view, boolean nowSaved) {
        view.animate().cancel();
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if (nowSaved) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }

            view.setPivotX(view.getWidth() * 0.5f);
            view.setPivotY(view.getHeight() * 0.55f);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.35f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.35f, 1f);
            scaleX.setInterpolator(new OvershootInterpolator(2.35f));
            scaleY.setInterpolator(new OvershootInterpolator(2.35f));
            scaleX.setDuration(520);
            scaleY.setDuration(520);

            ObjectAnimator rot = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, -18f, 14f, -9f, 6f, 0f);
            rot.setDuration(560);

            float z0 = view.getTranslationZ();
            ObjectAnimator lift = ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, z0, z0 + 14f, z0);

            AnimatorSet burst = new AnimatorSet();
            burst.playTogether(scaleX, scaleY, rot, lift);
            burst.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            });
            burst.start();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
            view.setPivotX(view.getWidth() * 0.5f);
            view.setPivotY(view.getHeight() * 0.5f);

            ObjectAnimator sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.82f, 1.05f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.82f, 1.05f, 1f);
            sx.setDuration(340);
            sy.setDuration(340);

            ObjectAnimator fade = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.55f, 1f);
            fade.setDuration(280);

            AnimatorSet soft = new AnimatorSet();
            soft.playTogether(sx, sy, fade);
            soft.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            });
            soft.start();
        }
    }

    /** Home toolbar “saved items” shortcut — quick elastic nudge before navigation. */
    public static void playSavedShortcutPulse(View view) {
        view.animate().cancel();
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
        view.setPivotX(view.getWidth() * 0.5f);
        view.setPivotY(view.getHeight() * 0.5f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.18f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.18f, 1f);
        sx.setInterpolator(new OvershootInterpolator(1.4f));
        sy.setInterpolator(new OvershootInterpolator(1.4f));
        sx.setDuration(380);
        sy.setDuration(380);
        AnimatorSet s = new AnimatorSet();
        s.playTogether(sx, sy);
        s.start();
    }
}
