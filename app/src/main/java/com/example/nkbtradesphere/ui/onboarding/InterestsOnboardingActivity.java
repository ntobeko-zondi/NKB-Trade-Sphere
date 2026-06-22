package com.example.nkbtradesphere.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.ui.home.CategoryUtils;
import com.example.nkbtradesphere.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class InterestsOnboardingActivity extends AppCompatActivity {

    private static final int MIN_SELECTIONS = 3;

    private TextView tvSelectionMeta;
    private Button btnContinue;
    private InterestsAdapter interestsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests_onboarding);

        RecyclerView rvInterests = findViewById(R.id.rvInterests);
        tvSelectionMeta = findViewById(R.id.tvSelectionMeta);
        btnContinue = findViewById(R.id.btnContinueInterests);
        TextView tvSkip = findViewById(R.id.tvSkipInterests);

        List<CategoryUtils.CategoryInfo> categories =
                new ArrayList<>(CategoryUtils.defaultCategoryDeck().values());
        interestsAdapter = new InterestsAdapter(categories, this::updateSelectionState);
        rvInterests.setLayoutManager(new GridLayoutManager(this, 2));
        rvInterests.setAdapter(interestsAdapter);

        updateSelectionState(0);

        btnContinue.setOnClickListener(v -> {
            List<String> selectedIds = interestsAdapter.getSelectedCategoryIds();
            if (selectedIds.size() < MIN_SELECTIONS) {
                updateSelectionState(selectedIds.size());
                return;
            }
            savePreferencesAndNavigate(selectedIds);
        });

        tvSkip.setOnClickListener(v -> savePreferencesAndNavigate(new ArrayList<>()));
    }

    private void updateSelectionState(int selectedCount) {
        tvSelectionMeta.setText(getString(R.string.interests_selection_count, selectedCount, MIN_SELECTIONS));
        btnContinue.setEnabled(selectedCount >= MIN_SELECTIONS);
    }

    private void savePreferencesAndNavigate(@NonNull List<String> selectedCategoryIds) {
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, Context.MODE_PRIVATE);
        String csv = joinAsCsv(selectedCategoryIds);
        prefs.edit()
                .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, true)
                .putString(AppPreferences.KEY_SELECTED_CATEGORIES, csv)
                .apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @NonNull
    private String joinAsCsv(@NonNull List<String> selectedCategoryIds) {
        if (selectedCategoryIds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedCategoryIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(selectedCategoryIds.get(i));
        }
        return sb.toString();
    }
}
