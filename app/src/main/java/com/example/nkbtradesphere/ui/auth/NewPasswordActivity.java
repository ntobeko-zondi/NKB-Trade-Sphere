package com.example.nkbtradesphere.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.util.ValidationUtils;

public class NewPasswordActivity extends AppCompatActivity {

    private EditText edtNewPassword;
    private EditText edtConfirmPassword;
    private String resetEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        resetEmail = getIntent().getStringExtra(ResetPasswordActivity.EXTRA_RESET_EMAIL);
        if (TextUtils.isEmpty(resetEmail)) {
            Toast.makeText(this, "Reset session expired. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView back = findViewById(R.id.txtBackNewPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        Button btnSavePassword = findViewById(R.id.btnSavePassword);

        back.setOnClickListener(v -> finish());
        btnSavePassword.setOnClickListener(v -> saveNewPassword());
    }

    private void saveNewPassword() {
        String newPassword = edtNewPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please enter and confirm your new password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            edtNewPassword.setError("Password must be at least 8 characters and include letters and numbers");
            edtNewPassword.requestFocus();
            return;
        }

        edtNewPassword.setError(null);
        edtConfirmPassword.setError(null);

        if (!newPassword.equals(confirmPassword)) {
            edtConfirmPassword.setError("Passwords do not match");
            Toast.makeText(this, R.string.passwords_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.initialize(this);
        ApiClient.updateUserPassword(resetEmail, newPassword, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                Toast.makeText(NewPasswordActivity.this, "Password updated successfully. Please log in.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(NewPasswordActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(NewPasswordActivity.this, "Could not update password: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
