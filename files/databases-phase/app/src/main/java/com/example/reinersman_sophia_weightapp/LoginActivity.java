package com.example.reinersman_sophia_weightapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        AppDatabaseHelper dbHelper = new AppDatabaseHelper(this);
        userRepository = new UserRepository(dbHelper);
        sessionManager = new SessionManager(this);

        TextInputLayout tilUsername = findViewById(R.id.tilUsername);
        TextInputLayout tilPassword = findViewById(R.id.tilPassword);
        TextInputEditText etUsername = findViewById(R.id.etUsername);
        TextInputEditText etPassword = findViewById(R.id.etPassword);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnCreate = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> {
            tilUsername.setError(null);
            tilPassword.setError(null);

            String username = ValidationUtils.safeText(etUsername);
            String password = ValidationUtils.safeText(etPassword);

            boolean hasError = false;

            if (!ValidationUtils.isValidUsername(username)) {
                tilUsername.setError("Username must be at least 3 characters and use only letters, numbers, or _");
                hasError = true;
            }

            if (!ValidationUtils.isValidPassword(password)) {
                tilPassword.setError("Password must be at least 4 characters.");
                hasError = true;
            }

            if (hasError) {
                return;
            }

            long userId = userRepository.validateLogin(username, password);
            if (userId == -1) {
                Toast.makeText(this, "Login failed. Check your username and password.", Toast.LENGTH_SHORT).show();
            } else {
                sessionManager.saveUserId(userId);
                startActivity(new Intent(LoginActivity.this, ProgressActivity.class));
                finish();
            }
        });

        btnCreate.setOnClickListener(v -> {
            tilUsername.setError(null);
            tilPassword.setError(null);

            String username = ValidationUtils.safeText(etUsername);
            String password = ValidationUtils.safeText(etPassword);

            boolean hasError = false;

            if (!ValidationUtils.isValidUsername(username)) {
                tilUsername.setError("Username must be at least 3 characters and use only letters, numbers, or _");
                hasError = true;
            }

            if (!ValidationUtils.isValidPassword(password)) {
                tilPassword.setError("Password must be at least 4 characters.");
                hasError = true;
            }

            if (hasError) {
                return;
            }

            if (userRepository.usernameExists(username)) {
                tilUsername.setError("That username already exists. Try logging in.");
                return;
            }

            long newUserId = userRepository.createUser(username, password);
            if (newUserId == -1) {
                Toast.makeText(this, "Account creation failed. Try again.", Toast.LENGTH_SHORT).show();
            } else {
                sessionManager.saveUserId(newUserId);
                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, ProgressActivity.class));
                finish();
            }
        });
    }
}