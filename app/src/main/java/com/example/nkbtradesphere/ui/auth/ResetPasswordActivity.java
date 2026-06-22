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

public class ResetPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_RESET_EMAIL = "extra_reset_email";

    private EditText edtResetEmail;
    private EditText edtResetID;
    private EditText edtResetPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        TextView back = findViewById(R.id.textView5);

        edtResetEmail = findViewById(R.id.edtResetEmail);
        edtResetID = findViewById(R.id.edtResetID);
        edtResetPhone = findViewById(R.id.edtResetPhone);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);

        back.setOnClickListener(v -> finish());

        btnResetPassword.setOnClickListener(v -> verifyDetailsAndOpenNewPassword());
    }

    private void clearErrors() {
        edtResetEmail.setError(null);
        edtResetID.setError(null);
        edtResetPhone.setError(null);
    }

    private void verifyDetailsAndOpenNewPassword() {
        String email = edtResetEmail.getText().toString().trim();
        String idNumber = edtResetID.getText().toString().trim();
        String phone = edtResetPhone.getText().toString().trim();

        clearErrors();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(idNumber) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            edtResetEmail.setError("Enter a valid email address");
            edtResetEmail.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidSouthAfricanId(idNumber)) {
            edtResetID.setError("Enter a valid 13-digit South African ID number");
            edtResetID.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            edtResetPhone.setError("Enter a valid SA phone number, e.g. 0821234567 or +27821234567");
            edtResetPhone.requestFocus();
            return;
        }

        String normalizedEmail = ValidationUtils.normalizeEmail(email);
        String normalizedIdNumber = ValidationUtils.normalizeIdNumber(idNumber);
        String normalizedPhone = ValidationUtils.normalizePhone(phone);

        ApiClient.initialize(this);
        ApiClient.verifyResetDetails(normalizedEmail, normalizedIdNumber, normalizedPhone, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                Intent intent = new Intent(ResetPasswordActivity.this, NewPasswordActivity.class);
                intent.putExtra(EXTRA_RESET_EMAIL, normalizedEmail);
                startActivity(intent);
            }

            @Override
            public void onError(String error) {
                if (error.toLowerCase().contains("email")) {
                    edtResetEmail.setError(error);
                    edtResetEmail.requestFocus();
                } else if (error.toLowerCase().contains("id")) {
                    edtResetID.setError(error);
                    edtResetID.requestFocus();
                } else if (error.toLowerCase().contains("phone")) {
                    edtResetPhone.setError(error);
                    edtResetPhone.requestFocus();
                }
                Toast.makeText(ResetPasswordActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
