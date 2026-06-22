package com.example.nkbtradesphere.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.database.DatabaseHelper;
import com.example.nkbtradesphere.ui.auth.ResetPasswordActivity;
import com.example.nkbtradesphere.util.AvatarUtils;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileEditActivity extends AppCompatActivity {

    private String currentUserId;
    private TextView tvHeaderName;
    private TextView tvFullName;
    private TextView tvPhone;
    private TextView tvEmail;
    private ShapeableImageView imgProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        currentUserId = getSharedPreferences(AppPreferences.PREFS_NKB, 0)
                .getString(AppPreferences.KEY_USER_EMAIL, "");

        imgProfile = findViewById(R.id.imgProfile);
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvFullName = findViewById(R.id.tvFullName);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEditFullName).setOnClickListener(v -> showEditNameDialog());
        findViewById(R.id.btnEditPhone).setOnClickListener(v -> showEditPhoneDialog());
        findViewById(R.id.btnEditPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        refreshUi();
    }

    private void refreshUi() {
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, 0);
        String prefName = prefs.getString(AppPreferences.KEY_USER_NAME, "");
        DatabaseHelper.UserData user = DatabaseHelper.getInstance(this).getUserByEmail(currentUserId);
        String name = !TextUtils.isEmpty(prefName)
                ? prefName
                : (user != null && !TextUtils.isEmpty(user.fullName) ? user.fullName : getString(R.string.profile_default_name));
        String phone = user != null && !TextUtils.isEmpty(user.phone) ? user.phone : "Add phone number";

        tvHeaderName.setText(name);
        tvFullName.setText(name);
        tvPhone.setText(phone);
        tvEmail.setText(currentUserId);
        imgProfile.setImageDrawable(AvatarUtils.createInitialsAvatar(this, name, 110));
    }

    private void showEditNameDialog() {
        showStyledEditDialog(
                "Edit Full Name",
                "Full name",
                tvFullName.getText().toString(),
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                value -> {
                    if (TextUtils.isEmpty(value)) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    DatabaseHelper db = DatabaseHelper.getInstance(this);
                    DatabaseHelper.UserData user = db.getUserByEmail(currentUserId);
                    String phone = user != null ? user.phone : "";
                    if (db.updateUserProfile(currentUserId, value, phone)) {
                        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, 0);
                        prefs.edit().putString(AppPreferences.KEY_USER_NAME, value).apply();
                        refreshUi();
                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(this, "Could not update", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
        );
    }

    private void showEditPhoneDialog() {
        String currentPhone = tvPhone.getText().toString().equals("Add phone number") ? "" : tvPhone.getText().toString();
        showStyledEditDialog(
                "Edit Phone Number",
                "Phone number",
                currentPhone,
                android.text.InputType.TYPE_CLASS_PHONE,
                phone -> {
                    String name = tvFullName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        name = getString(R.string.profile_default_name);
                    }
                    if (DatabaseHelper.getInstance(this).updateUserProfile(currentUserId, name, phone)) {
                        refreshUi();
                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        Toast.makeText(this, "Could not update", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
        );
    }

    private interface EditValueCallback {
        boolean onSave(String value);
    }

    private void showStyledEditDialog(String title,
                                      String hint,
                                      String initialValue,
                                      int inputType,
                                      EditValueCallback callback) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_edit_field, null, false);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etDialogValue = dialogView.findViewById(R.id.etDialogValue);
        TextView btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        TextView btnDialogSave = dialogView.findViewById(R.id.btnDialogSave);

        tvDialogTitle.setText(title);
        etDialogValue.setHint(hint);
        etDialogValue.setInputType(inputType);
        etDialogValue.setText(initialValue);
        etDialogValue.setSelection(etDialogValue.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        dialog.setCanceledOnTouchOutside(true);

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());
        btnDialogSave.setOnClickListener(v -> {
            String value = etDialogValue.getText().toString().trim();
            if (callback.onSave(value)) {
                dialog.dismiss();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
