package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.util.RatingFormat;
import com.example.nkbtradesphere.util.PostedDateFormat;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

/**
 * Product deck: current item on top, optional faint peek of the next item only. Tap opens
 * detail; horizontal swipe skips. Vertical drags are left to the parent scroll view.
 */
public class SwipeCardStackView extends FrameLayout {

    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_HORIZONTAL = 1;
    private static final int GESTURE_VERTICAL = 2;

    public interface Callbacks {
        /** Fired after a tap on the top card (opens detail UI). */
        void onCardTapped(Product product);

        void onStackEmptyChanged(boolean empty);
    }

    // Number of cards simultaneously rendered in the stacked deck.
    private static final int MAX_VISIBLE = 3;
    // Horizontal card offset per depth layer (index 0 = top card).
    private static final float[] STACK_X_DP = {0f, 14f, 26f, 36f};
    // Vertical card offset per depth layer for staggered peeking.
    private static final float[] STACK_Y_DP = {0f, 16f, 30f, 42f};
    // Slight scale reduction per depth for layered depth cue.
    private static final float[] STACK_SCALE = {1.00f, 0.96f, 0.92f, 0.90f};
    // Subtle alpha drop as cards go deeper in the stack.
    private static final float[] STACK_ALPHA = {1.00f, 0.84f, 0.70f, 0.62f};
    // Rotation per depth to avoid a flat looking stack.
    private static final float[] STACK_ROT = {0f, -1.6f, -3.2f, -4.5f};
    // Dim overlay alpha for each depth layer.
    private static final float[] DIM_ALPHA = {0f, 0.08f, 0.16f, 0.24f};

    private final List<Product> deck = new ArrayList<>();
    private final List<Product> sourceCopy = new ArrayList<>();

    private int topIndex = 0;
    private boolean locked = false;

    private float startRawX;
    private float startRawY;
    private float dragDx;
    private float dragDy;
    private long touchDownTimeMs;
    private int gestureDir = GESTURE_NONE;
    private int touchSlopSq;

    private Callbacks callbacks;

    private final float density;

    /** When true, the new top card fades in after the previous card was swiped away. */
    private boolean animateEnterAfterAdvance;

    public SwipeCardStackView(Context context) {
        this(context, null);
    }

    public SwipeCardStackView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        setClipChildren(false);
        ViewCompat.setNestedScrollingEnabled(this, true);
        int slop = ViewConfiguration.get(context).getScaledTouchSlop();
        touchSlopSq = slop * slop;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void openProductFromCard(View v) {
        if (locked || callbacks == null) {
            return;
        }
        Object tag = v.getTag(R.id.tag_swipe_product);
        if (tag instanceof Product) {
            callbacks.onCardTapped((Product) tag);
        }
    }

    public void setCallbacks(@Nullable Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    /** Replaces the deck and shows the first cards. */
    public void setProducts(List<Product> products) {
        sourceCopy.clear();
        if (products != null) {
            sourceCopy.addAll(products);
        }
        resetDeckFromSource();
    }

    /** Same list as initial — used by “Start over”. */
    public void resetDeckFromSource() {
        deck.clear();
        deck.addAll(sourceCopy);
        topIndex = 0;
        locked = false;
        renderStack();
    }

    public boolean isStackEmpty() {
        return topIndex >= deck.size();
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void requestParentDisallowIntercept(boolean disallow) {
        for (ViewParent p = getParent(); p != null; p = p.getParent()) {
            p.requestDisallowInterceptTouchEvent(disallow);
            if (p instanceof androidx.core.widget.NestedScrollView) {
                break;
            }
        }
    }

    public void renderStack() {
        removeAllViews();
        if (topIndex >= deck.size()) {
            animateEnterAfterAdvance = false;
            if (callbacks != null) {
                callbacks.onStackEmptyChanged(true);
            }
            return;
        }
        if (callbacks != null) {
            callbacks.onStackEmptyChanged(false);
        }

        int n = Math.min(MAX_VISIBLE, deck.size() - topIndex);
        for (int stackPos = n - 1; stackPos >= 0; stackPos--) {
            Product p = deck.get(topIndex + stackPos);
            CardView card = (CardView) LayoutInflater.from(getContext())
                    .inflate(R.layout.item_stack_card, this, false);
            bindProduct(card, p);
            card.setTag(R.id.tag_swipe_product, p);
            card.setTag(R.id.tag_swipe_stack_pos, stackPos);
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
            lp.topMargin = dp(4);
            card.setLongClickable(false);
            card.setHapticFeedbackEnabled(false);
            card.setOnLongClickListener(view -> true); // Consume long press so it never crashes/opens a context action.
            addView(card, lp);
            applyStackTransform(card, stackPos);
        }

        View top = getChildAt(getChildCount() - 1);
        top.setOnTouchListener(this::onTopCardTouch);

        if (animateEnterAfterAdvance) {
            animateEnterAfterAdvance = false;
            animateTopCardEnter(top);
        }
    }

    private void animateTopCardEnter(View top) {
        top.animate().cancel();
        top.post(() -> {
            if (top.getParent() != this) {
                return;
            }
            applyStackTransform(top, 0);
            top.setAlpha(0.97f);
            top.animate()
                    .alpha(1f)
                    .setDuration(90)
                    .setInterpolator(new LinearInterpolator())
                    .start();
        });
    }

    private void bindProduct(CardView root, Product p) {
        ImageView iv = root.findViewById(R.id.ivRecImage);
        ImageView ivSeller = root.findViewById(R.id.ivSellerAvatar);
        TextView tvSellerName = root.findViewById(R.id.tvSellerName);
        TextView tvSellerSub = root.findViewById(R.id.tvSellerSub);
        TextView tvItemRating = root.findViewById(R.id.tvItemAvgRating);
        TextView tvTime = root.findViewById(R.id.tvListedTime);
        TextView tvLoc = root.findViewById(R.id.tvLocChip);
        TextView tvName = root.findViewById(R.id.tvRecName);
        TextView tvDesc = root.findViewById(R.id.tvRecDesc);
        TextView tvPrice = root.findViewById(R.id.tvRecPrice);
        TextView tvCond = root.findViewById(R.id.tvChipCond);
        TextView tvCat = root.findViewById(R.id.tvChipCat);

        tvPrice.setText(p.isOutOfStock() ? "Out of stock" : formatRand(p.getPrice()));
        tvName.setText(p.getName());
        tvDesc.setText(p.getDescription());
        tvLoc.setText("Qty " + Math.max(0, p.getQuantity()));
        tvSellerName.setText(p.getSellerName());
        String listed = getPostedDateLabel(p.getListedTime());
        tvSellerSub.setText("");
        tvTime.setText("Posted · " + listed);
        String itemStar = RatingFormat.starLine(p.getItemAverageRating());
        if (TextUtils.isEmpty(itemStar)) {
            tvItemRating.setVisibility(View.GONE);
        } else {
            tvItemRating.setVisibility(View.VISIBLE);
            tvItemRating.setText(itemStar + " Rating");
        }
        tvCond.setText(p.getCondition().toUpperCase(Locale.US));
        tvCat.setText(p.getCategory().toUpperCase(Locale.US));

        loadCardImage(iv, p.getImageUrl());

        if (!TextUtils.isEmpty(p.getSellerImageUrl())) {
            Glide.with(ivSeller)
                    .load(p.getSellerImageUrl())
                    .placeholder(R.drawable.circle_avatar_bg)
                    .error(R.drawable.circle_avatar_bg)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .transform(new CircleCrop())
                    .dontAnimate()
                    .into(ivSeller);
            ivSeller.setVisibility(View.VISIBLE);
        } else {
            ivSeller.setVisibility(View.GONE);
        }
    }

    private void loadCardImage(ImageView target, String imageUrl) {
        Glide.with(target).clear(target);
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .override(dp(340), dp(255))
                .placeholder(R.drawable.rounded_card_bg)
                .error(R.drawable.rounded_card_bg)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate();

        Glide.with(target)
                .load(imageUrl)
                .thumbnail(0.15f)
                .apply(options)
                .into(target);
    }

    private void applyStackTransform(View card, int stackPos) {
        stackPos = Math.min(stackPos, STACK_Y_DP.length - 1);
        card.setTranslationX(dp(STACK_X_DP[stackPos]));
        card.setTranslationY(dp(STACK_Y_DP[stackPos]));
        card.setRotation(STACK_ROT[stackPos]);
        card.setScaleX(STACK_SCALE[stackPos]);
        card.setScaleY(STACK_SCALE[stackPos]);
        card.setAlpha(STACK_ALPHA[stackPos]);
        View dim = card.findViewById(R.id.vCardDim);
        if (dim != null) {
            dim.setAlpha(DIM_ALPHA[stackPos]);
        }
        ViewCompat.setElevation(card, dp(24 - stackPos * 5));
    }

    private void liftBackCards(float progress) {
        progress = Math.min(1f, Math.max(0f, progress));
        int n = getChildCount();
        if (n < 2) return;
        for (int i = 0; i < n - 1; i++) {
            View child = getChildAt(i);
            Object tag = child.getTag(R.id.tag_swipe_stack_pos);
            if (!(tag instanceof Integer)) continue;
            int pos = (Integer) tag;
            if (pos <= 0) continue;
            int pp = pos - 1;
            float y = lerp(dp(STACK_Y_DP[pos]), dp(STACK_Y_DP[pp]), progress);
            float x = lerp(dp(STACK_X_DP[pos]), dp(STACK_X_DP[pp]), progress);
            float rot = lerp(STACK_ROT[pos], STACK_ROT[pp], progress);
            float scale = lerp(STACK_SCALE[pos], STACK_SCALE[pp], progress);
            float dim = lerp(DIM_ALPHA[pos], DIM_ALPHA[pp], progress);
            float alpha = lerp(STACK_ALPHA[pos], STACK_ALPHA[pp], progress);
            child.setTranslationX(x);
            child.setTranslationY(y);
            child.setRotation(rot);
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(alpha);
            View vDim = child.findViewById(R.id.vCardDim);
            if (vDim != null) {
                vDim.setAlpha(dim);
            }
        }
    }

    private void restoreBackCards() {
        int n = getChildCount();
        for (int i = 0; i < n - 1; i++) {
            View child = getChildAt(i);
            Object tag = child.getTag(R.id.tag_swipe_stack_pos);
            if (!(tag instanceof Integer)) continue;
            applyStackTransform(child, (Integer) tag);
        }
    }

    private boolean onTopCardTouch(View v, MotionEvent e) {
        if (locked) {
            return true;
        }
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                requestParentDisallowIntercept(false);
                gestureDir = GESTURE_NONE;
                startRawX = e.getRawX();
                startRawY = e.getRawY();
                touchDownTimeMs = e.getEventTime();
                dragDx = 0f;
                dragDy = 0f;
                v.bringToFront();
                return true;

            case MotionEvent.ACTION_MOVE:
                dragDx = e.getRawX() - startRawX;
                dragDy = e.getRawY() - startRawY;
                float distSq = dragDx * dragDx + dragDy * dragDy;
                if (gestureDir == GESTURE_NONE && distSq > touchSlopSq) {
                    if (Math.abs(dragDx) > Math.abs(dragDy)) {
                        gestureDir = GESTURE_HORIZONTAL;
                        requestParentDisallowIntercept(true);
                    } else {
                        gestureDir = GESTURE_VERTICAL;
                        requestParentDisallowIntercept(false);
                        restoreBackCards();
                        applyStackTransform(v, 0);
                        v.setTranslationX(0f);
                        v.setTranslationY(dp(STACK_Y_DP[0]));
                        v.setRotation(0f);
                        return false;
                    }
                }
                if (gestureDir == GESTURE_VERTICAL) {
                    requestParentDisallowIntercept(false);
                    return false;
                }
                if (gestureDir == GESTURE_HORIZONTAL) {
                    v.setTranslationX(dragDx);
                    v.setTranslationY(dp(STACK_Y_DP[0]) + dragDy * 0.2f);
                    v.setRotation(dragDx * 0.065f);
                    float p = Math.min(1f, Math.abs(dragDx) / (240f * density));
                    liftBackCards(p);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                requestParentDisallowIntercept(false);
                int dir = gestureDir;
                gestureDir = GESTURE_NONE;

                if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    restoreBackCards();
                    snapBack(v);
                    return true;
                }

                if (dir == GESTURE_VERTICAL) {
                    return false;
                }

                if (dir == GESTURE_HORIZONTAL) {
                    float thr = dp(78);
                    if (Math.abs(dragDx) > thr) {
                        swipeHorizontal(v, dragDx > 0);
                    } else {
                        snapBack(v);
                    }
                } else {
                    long pressDuration = Math.max(0L, e.getEventTime() - touchDownTimeMs);
                    if (pressDuration < ViewConfiguration.getLongPressTimeout()) {
                        // A normal tap opens details. Long-press is deliberately consumed/ignored
                        // because some devices raise unstable long-click handling on image cards.
                        v.performClick();
                        openProductFromCard(v);
                    }
                }
                return true;

            default:
                return false;
        }
    }

    private void snapBack(View top) {
        int n = getChildCount();
        for (int i = 0; i < n - 1; i++) {
            View child = getChildAt(i);
            Object tag = child.getTag(R.id.tag_swipe_stack_pos);
            if (!(tag instanceof Integer)) continue;
            int pos = (Integer) tag;
            child.animate()
                    .translationX(dp(STACK_X_DP[pos]))
                    .translationY(dp(STACK_Y_DP[pos]))
                    .rotation(STACK_ROT[pos])
                    .scaleX(STACK_SCALE[pos])
                    .scaleY(STACK_SCALE[pos])
                    .alpha(STACK_ALPHA[pos])
                    .setDuration(380)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
            View dim = child.findViewById(R.id.vCardDim);
            if (dim != null) {
                dim.animate().alpha(DIM_ALPHA[pos]).setDuration(380).start();
            }
        }
        top.animate()
                .translationX(0f)
                .translationY(dp(STACK_Y_DP[0]))
                .rotation(0f)
                .scaleX(STACK_SCALE[0])
                .scaleY(STACK_SCALE[0])
                .alpha(1f)
                .setDuration(380)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    private void swipeHorizontal(View top, boolean toRight) {
        locked = true;
        float w = getResources().getDisplayMetrics().widthPixels;
        float target = toRight ? w * 1.1f : -w * 1.1f;
        top.animate()
                .translationX(target)
                .rotation(toRight ? 18f : -18f)
                .alpha(0f)
                .setDuration(340)
                .withEndAction(this::advanceDeck)
                .start();
    }

    private void advanceDeck() {
        topIndex++;
        locked = false;
        animateEnterAfterAdvance = true;
        renderStack();
    }

    private static String formatRand(@Nullable String value) {
        if (TextUtils.isEmpty(value)) return "R0";
        String v = value.trim();
        return v.startsWith("R") ? v : "R" + v;
    }

    private static String getPostedDateLabel(@Nullable String listedTime) {
        return PostedDateFormat.displayDate(listedTime);
    }
}
