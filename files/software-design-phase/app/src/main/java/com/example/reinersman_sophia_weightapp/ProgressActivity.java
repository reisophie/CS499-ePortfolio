package com.example.reinersman_sophia_weightapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class ProgressActivity extends AppCompatActivity {

    private AppDatabaseHelper db;
    private long userId;

    private final ArrayList<WeightEntry> entries = new ArrayList<>();
    private WeightAdapter adapter;

    private double currentGoal = -1;

    private TextInputLayout tilDate;
    private TextInputLayout tilWeight;
    private TextInputEditText etDate;
    private TextInputEditText etWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        db = new AppDatabaseHelper(this);
        userId = getSavedUserId();

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tilDate = findViewById(R.id.tilDate);
        tilWeight = findViewById(R.id.tilWeight);
        etDate = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);

        MaterialButton btnAddWeight = findViewById(R.id.btnAddWeight);
        MaterialButton btnSetGoal = findViewById(R.id.btnSetGoal);
        MaterialButton btnNotifications = findViewById(R.id.btnNotifications);
        MaterialButton btnBackToLogin = findViewById(R.id.btnBackToLogin);

        RecyclerView rvWeights = findViewById(R.id.rvWeights);
        rvWeights.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WeightAdapter(entries,
                entry -> {
                    boolean ok = db.deleteWeight(userId, entry.id);
                    if (ok) {
                        reloadWeights();
                        Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show();
                    }
                },
                this::showEditDialog);

        rvWeights.setAdapter(adapter);

        currentGoal = db.getGoalWeight(userId);
        reloadWeights();

        btnAddWeight.setOnClickListener(v -> addWeightEntry());

        btnSetGoal.setOnClickListener(v -> showGoalDialog());

        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(ProgressActivity.this, NotificationsActivity.class)));

        btnBackToLogin.setOnClickListener(v -> {
            clearSavedUser();
            startActivity(new Intent(ProgressActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void addWeightEntry() {
        tilDate.setError(null);
        tilWeight.setError(null);

        String date = (etDate.getText() == null) ? "" : etDate.getText().toString().trim();
        String weightStr = (etWeight.getText() == null) ? "" : etWeight.getText().toString().trim();

        boolean hasError = false;

        if (!ValidationUtils.isValidDate(date)) {
            tilDate.setError("Enter a valid date in MM/DD/YYYY format.");
            hasError = true;
        }

        Double weight = ValidationUtils.parseWeight(weightStr);
        if (weight == null) {
            tilWeight.setError("Weight must be a number.");
            hasError = true;
        } else if (!ValidationUtils.isRealisticWeight(weight)) {
            tilWeight.setError("Enter a realistic weight between 50 and 700 lbs.");
            hasError = true;
        }

        if (hasError) return;

        long newId = db.addWeight(userId, date, weight);
        if (newId == -1) {
            Toast.makeText(this, "Save failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        etDate.setText("");
        etWeight.setText("");

        reloadWeights();
        checkGoalAndSendSmsIfNeeded(weight);
    }

    private void reloadWeights() {
        entries.clear();
        entries.addAll(db.getAllWeights(userId));
        adapter.notifyDataSetChanged();
    }

    private void showEditDialog(WeightEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Entry");

        final TextInputEditText inputDate = new TextInputEditText(this);
        inputDate.setHint("Date (MM/DD/YYYY)");
        inputDate.setText(entry.date);

        final TextInputEditText inputWeight = new TextInputEditText(this);
        inputWeight.setHint("Weight");
        inputWeight.setText(String.valueOf(entry.weight));
        inputWeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(inputDate);
        layout.addView(inputWeight);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDate = (inputDate.getText() == null) ? "" : inputDate.getText().toString().trim();
            String newWeightStr = (inputWeight.getText() == null) ? "" : inputWeight.getText().toString().trim();

            if (!ValidationUtils.isValidDate(newDate)) {
                Toast.makeText(this, "Enter a valid date in MM/DD/YYYY format.", Toast.LENGTH_SHORT).show();
                return;
            }

            Double newWeight = ValidationUtils.parseWeight(newWeightStr);
            if (newWeight == null) {
                Toast.makeText(this, "Weight must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isRealisticWeight(newWeight)) {
                Toast.makeText(this, "Enter a realistic weight between 50 and 700 lbs.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok = db.updateWeight(userId, entry.id, newDate, newWeight);
            if (ok) {
                reloadWeights();
                Toast.makeText(this, "Updated.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Goal Weight");

        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Goal (lbs)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        if (currentGoal != -1) {
            input.setText(String.valueOf(currentGoal));
        }

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String goalStr = (input.getText() == null) ? "" : input.getText().toString().trim();

            Double goal = ValidationUtils.parseWeight(goalStr);
            if (goal == null) {
                Toast.makeText(this, "Goal must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isRealisticWeight(goal)) {
                Toast.makeText(this, "Enter a realistic goal between 50 and 700 lbs.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.setGoalWeight(userId, goal);
            currentGoal = goal;
            Toast.makeText(this, "Goal saved.", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void checkGoalAndSendSmsIfNeeded(double newWeight) {
        currentGoal = db.getGoalWeight(userId);
        if (currentGoal == -1) return;

        if (newWeight <= currentGoal) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Goal reached!", Toast.LENGTH_LONG).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String phone = prefs.getString("sms_phone", "");

            if (phone == null || phone.trim().isEmpty()) {
                Toast.makeText(this, "Goal reached!", Toast.LENGTH_LONG).show();
                return;
            }

            String msg = "You hit your goal! New weigh-in: " + newWeight + " lbs. Goal: " + currentGoal + " lbs.";

            try {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phone, null, msg, null, null);
                Toast.makeText(this, "Goal reached! SMS sent.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Goal reached, but SMS failed to send.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private long getSavedUserId() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getLong("user_id", -1);
    }

    private void clearSavedUser() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().remove("user_id").apply();
    }
}