package com.example.nkbtradesphere.ui.profile;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.MyListingsActivity;
import com.example.nkbtradesphere.SavedItemsActivity;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.database.DatabaseHelper;
import com.example.nkbtradesphere.ui.auth.LoginActivity;
import com.example.nkbtradesphere.util.AvatarUtils;
import com.example.nkbtradesphere.util.ImageStorageUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.core.widget.ImageViewCompat;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvListingsCount;
    private TextView tvSavedCount;
    private TextView tvRating;
    private TextView tvDisplayName;
    private SwitchCompat switchDarkMode;
    private String currentUserId;
    private boolean statsLoading = false;
    private long lastStatsLoadAt = 0L;
    private static final long STATS_REFRESH_INTERVAL_MS = 10_000L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ShapeableImageView imgAvatar = view.findViewById(R.id.imgProfile);
        tvDisplayName = view.findViewById(R.id.txtProfileName);
        tvListingsCount = view.findViewById(R.id.tvListingsCount);
        tvSavedCount = view.findViewById(R.id.tvSavedCount);
        tvRating = view.findViewById(R.id.tvRating);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        View btnEditName = view.findViewById(R.id.btnEditName);
        MaterialButton btnMyListings = view.findViewById(R.id.btnMyListings);
        MaterialButton btnSavedItems = view.findViewById(R.id.btnSavedItems);
        MaterialButton btnSignOut = view.findViewById(R.id.btnLogout);
        MaterialButton btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        SharedPreferences prefs = requireContext().getSharedPreferences(AppPreferences.PREFS_NKB, 0);
        String name = prefs.getString(AppPreferences.KEY_USER_NAME, getString(R.string.profile_default_name));
        String email = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
        currentUserId = email;

        if (!TextUtils.isEmpty(name)) {
            tvDisplayName.setText(name);
        } else if (!TextUtils.isEmpty(email)) {
            int at = email.indexOf('@');
            tvDisplayName.setText(at > 0 ? email.substring(0, at) : email);
        } else {
            tvDisplayName.setText(R.string.profile_default_name);
        }

        ImageViewCompat.setImageTintList(imgAvatar, null);
        imgAvatar.setImageDrawable(AvatarUtils.createInitialsAvatar(requireContext(), tvDisplayName.getText().toString(), 110));

        boolean isDarkMode = prefs.getBoolean(AppPreferences.KEY_DARK_THEME, true);
        switchDarkMode.setChecked(isDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(AppPreferences.KEY_DARK_THEME, checked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            requireActivity().recreate();
        });

        btnMyListings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MyListingsActivity.class)));
        btnSavedItems.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SavedItemsActivity.class)));

        refreshProfileStats();

        btnEditName.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileEditActivity.class)));
        btnSignOut.setOnClickListener(v -> signOut());
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = requireContext().getSharedPreferences(AppPreferences.PREFS_NKB, 0);
        String name = prefs.getString(AppPreferences.KEY_USER_NAME, getString(R.string.profile_default_name));
        if (!TextUtils.isEmpty(name)) {
            tvDisplayName.setText(name);
        }
        refreshProfileStats();
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshProfileStats();
        }
    }

    private void refreshProfileStats() {
        if (TextUtils.isEmpty(currentUserId)) {
            tvListingsCount.setText("0");
            tvSavedCount.setText("0");
            tvRating.setText("N/A");
            return;
        }

        long now = System.currentTimeMillis();
        if (statsLoading || (lastStatsLoadAt > 0 && now - lastStatsLoadAt < STATS_REFRESH_INTERVAL_MS)) {
            return;
        }

        statsLoading = true;
        Context appContext = requireContext().getApplicationContext();
        final String userId = currentUserId;
        new Thread(() -> {
            DatabaseHelper db = DatabaseHelper.getInstance(appContext);
            int listingCount = db.getSellerListingCount(userId);
            int savedCount = db.getSavedItemsCount(userId);
            DatabaseHelper.UserData user = db.getUserByEmail(userId);
            String ratingText = user != null
                    ? String.format(Locale.US, "%.1f", user.rating)
                    : "0.0";

            if (!isAdded()) {
                statsLoading = false;
                return;
            }

            requireActivity().runOnUiThread(() -> {
                statsLoading = false;
                lastStatsLoadAt = System.currentTimeMillis();
                if (!isAdded()) return;
                tvListingsCount.setText(String.valueOf(listingCount));
                tvSavedCount.setText(String.valueOf(savedCount));
                tvRating.setText(ratingText);
            });
        }).start();
    }

    private void signOut() {
        SharedPreferences prefs = requireContext().getSharedPreferences(AppPreferences.PREFS_NKB, 0);
        prefs.edit()
                .putBoolean(AppPreferences.KEY_LOGGED_IN, false)
                .remove(AppPreferences.KEY_USER_EMAIL)
                .remove(AppPreferences.KEY_USER_NAME)
                .apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void confirmDeleteAccount() {
        if (TextUtils.isEmpty(currentUserId)) {
            return;
        }
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad / 2, pad, 0);

        TextView hint = new TextView(requireContext());
        hint.setText("Type DELETE to confirm permanent account deletion.");
        form.addView(hint);

        EditText input = new EditText(requireContext());
        input.setHint("Type DELETE");
        form.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete account?")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!"DELETE".equals(value)) {
                        Toast.makeText(requireContext(), "Deletion cancelled: confirmation text did not match.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    deleteAccount();
                })
                .show();
    }

    private void deleteAccount() {
        if (TextUtils.isEmpty(currentUserId)) {
            return;
        }

        final Context appContext = requireContext().getApplicationContext();
        final String userIdToDelete = currentUserId;
        Toast.makeText(requireContext(), "Deleting account...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            DatabaseHelper db = DatabaseHelper.getInstance(appContext);

            // Remove locally stored image files for this seller before removing the server rows.
            for (DatabaseHelper.ListingData listing : db.getListingsBySeller(userIdToDelete)) {
                if (listing.imageGallery == null) continue;
                for (String imagePath : listing.imageGallery) {
                    if (!TextUtils.isEmpty(imagePath)
                            && !imagePath.startsWith("http://")
                            && !imagePath.startsWith("https://")
                            && !imagePath.startsWith("data:image")) {
                        ImageStorageUtils.deleteImage(imagePath);
                    }
                }
            }

            boolean deleted = db.deleteUserAccount(userIdToDelete);

            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (!deleted) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete failed")
                            .setMessage("Could not delete account. Please check your connection and try again.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                SharedPreferences prefs = requireContext().getSharedPreferences(AppPreferences.PREFS_NKB, 0);
                prefs.edit()
                        .putBoolean(AppPreferences.KEY_LOGGED_IN, false)
                        .remove(AppPreferences.KEY_USER_EMAIL)
                        .remove(AppPreferences.KEY_USER_NAME)
                        .remove(AppPreferences.KEY_USER_PASSWORD)
                        .remove(AppPreferences.KEY_SELECTED_CATEGORIES)
                        .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, false)
                        .apply();

                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }).start();
    }
}
