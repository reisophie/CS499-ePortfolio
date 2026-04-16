package com.example.reinersman_sophia_weightapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private AppDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new AppDatabaseHelper(this);

        TextInputEditText etUsername = findViewById(R.id.etUsername);
        TextInputEditText etPassword = findViewById(R.id.etPassword);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnCreate = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(v -> {
            String u = (etUsername.getText() == null) ? "" : etUsername.getText().toString().trim();
            String p = (etPassword.getText() == null) ? "" : etPassword.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            long userId = db.validateLogin(u, p);
            if (userId == -1) {
                Toast.makeText(this, "Login failed. Check your username/password.", Toast.LENGTH_SHORT).show();
            } else {
                saveUserId(userId);
                startActivity(new Intent(LoginActivity.this, ProgressActivity.class));
                finish();
            }
        });

        btnCreate.setOnClickListener(v -> {
            String u = (etUsername.getText() == null) ? "" : etUsername.getText().toString().trim();
            String p = (etPassword.getText() == null) ? "" : etPassword.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (db.usernameExists(u)) {
                Toast.makeText(this, "That username already exists. Try logging in.", Toast.LENGTH_SHORT).show();
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