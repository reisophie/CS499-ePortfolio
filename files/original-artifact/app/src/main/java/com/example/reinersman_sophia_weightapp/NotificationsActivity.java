package com.example.reinersman_sophia_weightapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class NotificationsActivity extends AppCompatActivity {

    private static final int REQ_SMS = 1001;
    private TextView tvPermissionStatus;
    private TextInputEditText etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        MaterialButton btnRequestPermission = findViewById(R.id.btnRequestPermission);
        MaterialButton btnNotNow = findViewById(R.id.btnNotNow);
        MaterialButton btnBack = findViewById(R.id.btnBackToProgress);
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        etPhone = findViewById(R.id.etPhone);

        loadSavedPhone();
        updateStatusText();

        btnRequestPermission.setOnClickListener(v -> {
            savePhone(); // save whatever they typed

            if (hasSmsPermission()) {
                updateStatusText();
                Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.SEND_SMS},
                        REQ_SMS
                );
            }
        });

        btnNotNow.setOnClickListener(v -> {
            savePhone();
            tvPermissionStatus.setText("Status: Denied (App still works, no SMS texts will send)");
        });

        btnBack.setOnClickListener(v -> {
            savePhone();
            finish();
        });
    }

    private void savePhone() {
        String phone = (etPhone.getText() == null) ? "" : etPhone.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putString("sms_phone", phone).apply();
    }

    private void loadSavedPhone() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String phone = prefs.getString("sms_phone", "");
        etPhone.setText(phone);
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updateStatusText() {
        if (hasSmsPermission()) {
            tvPermissionStatus.setText("Status: Granted (Goal reached texts enabled)");
        } else {
            tvPermissionStatus.setText("Status: Not granted (App still works, no SMS texts will send)");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SMS) {
            updateStatusText();
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. App still works without SMS.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}