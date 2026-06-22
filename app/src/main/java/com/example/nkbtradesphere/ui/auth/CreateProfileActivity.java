package com.example.nkbtradesphere.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.ui.onboarding.OnboardingInterestScreen;
import com.example.nkbtradesphere.ui.welcome.WelcomeActivity;
import com.example.nkbtradesphere.util.ValidationUtils;

public class CreateProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        EditText nameField = findViewById(R.id.name);
        EditText idField = findViewById(R.id.edtUserId);
        EditText phoneField = findViewById(R.id.edtPhone);
        EditText emailField = findViewById(R.id.email);
        EditText passwordField = findViewById(R.id.password);
        EditText confirmField = findViewById(R.id.password2);
        Button btnSignUp = findViewById(R.id.btnSignUp);

        TextView back = findViewById(R.id.back);
        back.setOnClickListener(v -> {
            startActivity(new Intent(CreateProfileActivity.this, WelcomeActivity.class));
            finish();
        });

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);

        btnSignUp.setOnClickListener(v -> {
            String name = nameField.getText().toString().trim();
            String idNumber = idField.getText().toString().trim();
            String phone = phoneField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString();
            String confirm = confirmField.getText().toString();

            clearErrors(nameField, idField, phoneField, emailField, passwordField, confirmField);

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(idNumber) || TextUtils.isEmpty(phone)
                    || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isValidFullName(name)) {
                nameField.setError("Enter a valid full name");
                nameField.requestFocus();
                return;
            }

            if (!ValidationUtils.isValidSouthAfricanId(idNumber)) {
                idField.setError("Enter a valid 13-digit South African ID number");
                idField.requestFocus();
                return;
            }

            if (!ValidationUtils.isValidPhone(phone)) {
                phoneField.setError("Enter a valid SA phone number, e.g. 0821234567 or +27821234567");
                phoneField.requestFocus();
                return;
            }

            if (!ValidationUtils.isValidEmail(email)) {
                emailField.setError("Enter a valid email address");
                emailField.requestFocus();
                return;
            }

            if (!ValidationUtils.isValidPassword(password)) {
                passwordField.setError("Password must be at least 8 characters and include letters and numbers");
                passwordField.requestFocus();
                return;
            }

            if (!password.equals(confirm)) {
                confirmField.setError("Passwords do not match");
                Toast.makeText(this, R.string.passwords_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }

            String normalizedEmail = ValidationUtils.normalizeEmail(email);
            String normalizedIdNumber = ValidationUtils.normalizeIdNumber(idNumber);
            String normalizedPhone = ValidationUtils.normalizePhone(phone);

            ApiClient.initialize(this);
            ApiClient.registerUser(normalizedEmail, password, normalizedEmail, name, normalizedPhone, normalizedIdNumber,
                    new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(org.json.JSONObject response) {
                            prefs.edit()
                                    .putString(AppPreferences.KEY_USER_NAME, name)
                                    .putString(AppPreferences.KEY_USER_EMAIL, normalizedEmail)
                                    .remove(AppPreferences.KEY_USER_PASSWORD)
                                    .putBoolean(AppPreferences.KEY_LOGGED_IN, true)
                                    .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, false)
                                    .apply();

                            Toast.makeText(CreateProfileActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                            navigateToInterestsOnboarding();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(CreateProfileActivity.this,
                                    "Registration failed: " + error,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void clearErrors(EditText... fields) {
        for (EditText field : fields) {
            field.setError(null);
        }
    }

    private void navigateToInterestsOnboarding() {
        Intent intent = new Intent(this, OnboardingInterestScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
