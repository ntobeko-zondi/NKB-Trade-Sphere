package com.example.nkbtradesphere.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.ui.main.MainActivity;
import com.example.nkbtradesphere.ui.onboarding.OnboardingInterestScreen;
import com.example.nkbtradesphere.ui.welcome.WelcomeActivity;
import com.example.nkbtradesphere.util.ValidationUtils;

public class LoginActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private EditText emailField;
    private EditText passwordField;
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = findViewById(R.id.progressBar);
        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        btnSignIn = findViewById(R.id.btnSignIn);

        TextView back = findViewById(R.id.textView3);
        back.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
            finish();
        });

        TextView forgotPassword = findViewById(R.id.txtForgot);
        forgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class)));

        btnSignIn.setOnClickListener(v -> attemptSignIn());

        TextView signup = findViewById(R.id.txtSignup);
        signup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, CreateProfileActivity.class)));
    }

    private void attemptSignIn() {
        String email = ValidationUtils.normalizeEmail(emailField.getText().toString());
        String password = passwordField.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.fill_email_password, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        ApiClient.initialize(this);
        ApiClient.authenticateUser(email, password, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                org.json.JSONObject user = response.optJSONObject("user");
                String fullName = user == null ? email : user.optString("full_name", email);
                String userEmail = user == null ? email : user.optString("email", email);
                prefs.edit()
                        .putString(AppPreferences.KEY_USER_NAME, fullName)
                        .putString(AppPreferences.KEY_USER_EMAIL, userEmail)
                        .remove(AppPreferences.KEY_USER_PASSWORD)
                        .putBoolean(AppPreferences.KEY_LOGGED_IN, true)
                        .apply();
                setLoading(false);
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        boolean onboardingComplete = prefs.getBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, false);
        Intent intent = onboardingComplete
                ? new Intent(this, MainActivity.class)
                : new Intent(this, OnboardingInterestScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setVisibility(loading ? View.GONE : View.VISIBLE);
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
    }
}
