package com.example.nkbtradesphere.ui.welcome;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.ui.auth.CreateProfileActivity;
import com.example.nkbtradesphere.ui.auth.LoginActivity;
import com.example.nkbtradesphere.ui.main.MainActivity;
import com.example.nkbtradesphere.ui.onboarding.OnboardingInterestScreen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);

        // Do not call the online API on the welcome screen before the UI is shown.
        // The login and register screens validate the account with the PHP API.
        if (prefs.getBoolean(AppPreferences.KEY_LOGGED_IN, false)) {
            String email = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");
            if (!TextUtils.isEmpty(email)) {
                boolean onboardingComplete = prefs.getBoolean(AppPreferences.KEY_ONBOARDING_COMPLETED, false);
                Intent intent = onboardingComplete
                        ? new Intent(this, MainActivity.class)
                        : new Intent(this, OnboardingInterestScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            prefs.edit()
                    .putBoolean(AppPreferences.KEY_LOGGED_IN, false)
                    .remove(AppPreferences.KEY_USER_EMAIL)
                    .remove(AppPreferences.KEY_USER_NAME)
                    .apply();
        }

        setContentView(R.layout.activity_welcome);

        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, CreateProfileActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
