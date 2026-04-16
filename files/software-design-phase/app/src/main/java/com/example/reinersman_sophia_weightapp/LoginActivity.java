package com.example.reinersman_sophia_weightapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private AppDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new AppDatabaseHelper(this);

        TextInputLayout tilUsername = findViewById(R.id.tilUsername);
        TextInputLayout tilPassword = findViewById(R.id.tilPassword);
        TextInputEditText etUsername = findViewById(R.id.etUsername);
        TextInputEditText etPassword = findViewById(R.id.etPassword);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnCreate = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> {
            tilUsername.setError(null);
            tilPassword.setError(null);

            String u = (etUsername.getText() == null) ? "" : etUsername.getText().toString().trim();
            String p = (etPassword.getText() == null) ? "" : etPassword.getText().toString().trim();

            boolean hasError = false;

            if (!ValidationUtils.isValidUsername(u)) {
                tilUsername.setError("Username must be at least 3 characters and use only letters, numbers, or _");
                hasError = true;
            }

            if (!ValidationUtils.isValidPassword(p)) {
                tilPassword.setError("Password must be at least 4 characters.");
                hasError = true;
            }

            if (hasError) return;

            long userId = db.validateLogin(u, p);
            if (userId == -1) {
                Toast.makeText(this, "Login failed. Check your username and password.", Toast.LENGTH_SHORT).show();
            } else {
                saveUserId(userId);
                startActivity(new Intent(LoginActivity.this, ProgressActivity.class));
                finish();
            }
        });

        btnCreate.setOnClickListener(v -> {
            tilUsername.setError(null);
            tilPassword.setError(null);

            String u = (etUsername.getText() == null) ? "" : etUsername.getText().toString().trim();
            String p = (etPassword.getText() == null) ? "" : etPassword.getText().toString().trim();

            boolean hasError = false;

            if (!ValidationUtils.isValidUsername(u)) {
                tilUsername.setError("Username must be at least 3 characters and use only letters, numbers, or _");
                hasError = true;
            }

            if (!ValidationUtils.isValidPassword(p)) {
                tilPassword.setError("Password must be at least 4 characters.");
                hasError = true;
            }

            if (hasError) return;

            if (db.usernameExists(u)) {
                tilUsername.setError("That username already exists. Try logging in.");
                return;
            }

            long newUserId = db.createUser(u, p);
            if (newUserId == -1) {
                Toast.makeText(this, "Account creation failed. Try again.", Toast.LENGTH_SHORT).show();
            } else {
                saveUserId(newUserId);
                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, ProgressActivity.class));
                finish();
            }
        });
    }

    private void saveUserId(long userId) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putLong("user_id", userId).apply();
    }
}