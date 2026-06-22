package com.example.nkbtradesphere.ui.onboarding;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.ui.home.CategoryUtils;
import com.example.nkbtradesphere.ui.main.MainActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OnboardingInterestScreen extends AppCompatActivity {

    private static final int MIN_SELECTIONS = 3;

    private final Set<String> selectedCategoryIds = new LinkedHashSet<>();

    private ChipGroup chipGroupInterests;
    private TextView tvSelectionMeta;
    private Button btnContinue;
    private TextView tvSkip;
    private View onboardingCard;
    private boolean continueWasEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_interest_screen);

        chipGroupInterests = findViewById(R.id.chipGroupInterests);
        tvSelectionMeta = findViewById(R.id.tvSelectionMeta);
        btnContinue = findViewById(R.id.btnContinueInterests);
        tvSkip = findViewById(R.id.tvSkipInterests);
        onboardingCard = findViewById(R.id.onboardingCard);

        seedInterestChips();
        updateSelectionState();
        playEntryAnimations();

        btnContinue.setOnClickListener(v -> {
            if (selectedCategoryIds.size() < MIN_SELECTIONS) {
                Toast.makeText(
                        this,
                        getString(R.string.interests_min_required, MIN_SELECTIONS),
                        Toast.LENGTH_SHORT
                ).show();
                updateSelectionState();
                return;
            }
            savePreferencesAndNavigate();
        });
        tvSkip.setOnClickListener(v -> skipOnboarding());
    }

    private void seedInterestChips() {
        chipGroupInterests.removeAllViews();
        int index = 0;
        for (CategoryUtils.CategoryInfo info : CategoryUtils.defaultCategoryDeck().values()) {
            Chip chip = new Chip(this);
            chip.setCheckable(true);
            chip.setText(info.emoji + " " + info.title);
            chip.setChipBackgroundColorResource(R.color.selector_interest_chip_bg);
            chip.setTextColor(getColorStateList(R.color.selector_interest_chip_text));
            chip.setChipStrokeWidth(getResources().getDimension(R.dimen.interest_chip_stroke_width));
            chip.setChipStrokeColorResource(R.color.selector_interest_chip_stroke);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCategoryIds.add(info.id);
                } else {
                    selectedCategoryIds.remove(info.id);
                }
                animateChipToggle(chip, isChecked);
                updateSelectionState();
            });
            chip.setAlpha(0f);
            chip.setTranslationY(18f);
            chipGroupInterests.addView(chip);
            chip.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(110L + (index * 28L))
                    .setDuration(260)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
            index++;
        }
    }

    private void updateSelectionState() {
        int selectedCount = selectedCategoryIds.size();
        tvSelectionMeta.setText(getString(R.string.interests_selection_count, selectedCount, MIN_SELECTIONS));
        boolean enableContinue = selectedCount >= MIN_SELECTIONS;
        btnContinue.setEnabled(enableContinue);
        if (enableContinue && !continueWasEnabled) {
            pulseContinueButton();
        }
        continueWasEnabled = enableContinue;
    }

    private void savePreferencesAndNavigate() {
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, true)
                .putString(AppPreferences.KEY_SELECTED_CATEGORIES, joinAsCsv(new ArrayList<>(selectedCategoryIds)))
                .apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void skipOnboarding() {
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, true)
                .putString(AppPreferences.KEY_SELECTED_CATEGORIES, "")
                .apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void playEntryAnimations() {
        if (onboardingCard != null) {
            onboardingCard.setAlpha(0f);
            onboardingCard.setTranslationY(42f);
            onboardingCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(420)
                    .setInterpolator(new OvershootInterpolator(0.85f))
                    .start();
        }
        if (tvSkip != null) {
            tvSkip.setAlpha(0f);
            tvSkip.animate()
                    .alpha(1f)
                    .setStartDelay(170)
                    .setDuration(260)
                    .start();
        }
        tvSelectionMeta.setAlpha(0f);
        tvSelectionMeta.setTranslationX(-22f);
        tvSelectionMeta.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(230)
                .setDuration(300)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void animateChipToggle(@NonNull Chip chip, boolean checked) {
        chip.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        chip.animate()
                .scaleX(checked ? 1.05f : 0.97f)
                .scaleY(checked ? 1.05f : 0.97f)
                .setDuration(110)
                .withEndAction(() -> chip.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140)
                        .start())
                .start();
    }

    private void pulseContinueButton() {
        btnContinue.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnContinue, View.SCALE_X, 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnContinue, View.SCALE_Y, 1f, 1.05f, 1f);
        scaleX.setDuration(320);
        scaleY.setDuration(320);
        scaleX.start();
        scaleY.start();
    }

    @NonNull
    private String joinAsCsv(@NonNull List<String> selectedIds) {
        if (selectedIds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(selectedIds.get(i));
        }
        return sb.toString();
    }
}
