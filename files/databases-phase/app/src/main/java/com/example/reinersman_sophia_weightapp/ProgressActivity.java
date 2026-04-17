package com.example.reinersman_sophia_weightapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.TextView;
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

    private WeightRepository weightRepository;
    private GoalRepository goalRepository;
    private SessionManager sessionManager;
    private long userId;

    private final ArrayList<WeightEntry> entries = new ArrayList<>();
    private WeightAdapter adapter;

    private double currentGoal = -1;

    private TextInputLayout tilDate;
    private TextInputLayout tilWeight;
    private TextInputEditText etDate;
    private TextInputEditText etWeight;
    private TextView tvCurrentWeight;
    private TextView tvGoalWeight;
    private TextView tvMotivation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        AppDatabaseHelper dbHelper = new AppDatabaseHelper(this);
        weightRepository = new WeightRepository(dbHelper);
        goalRepository = new GoalRepository(dbHelper);
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tilDate = findViewById(R.id.tilDate);
        tilWeight = findViewById(R.id.tilWeight);
        etDate = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight);
        tvGoalWeight = findViewById(R.id.tvGoalWeight);
        tvMotivation = findViewById(R.id.tvMotivation);

        MaterialButton btnAddWeight = findViewById(R.id.btnAddWeight);
        MaterialButton btnSetGoal = findViewById(R.id.btnSetGoal);
        MaterialButton btnNotifications = findViewById(R.id.btnNotifications);
        MaterialButton btnBackToLogin = findViewById(R.id.btnBackToLogin);

        RecyclerView rvWeights = findViewById(R.id.rvWeights);
        rvWeights.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WeightAdapter(entries,
                entry -> {
                    boolean ok = weightRepository.deleteWeight(userId, entry.id);
                    if (ok) {
                        reloadWeights();
                        Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show();
                    }
                },
                this::showEditDialog);

        rvWeights.setAdapter(adapter);

        currentGoal = goalRepository.getGoalWeight(userId);
        reloadWeights();

        btnAddWeight.setOnClickListener(v -> addWeightEntry());
        btnSetGoal.setOnClickListener(v -> showGoalDialog());
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(ProgressActivity.this, NotificationsActivity.class)));

        btnBackToLogin.setOnClickListener(v -> {
            sessionManager.clearUser();
            startActivity(new Intent(ProgressActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void addWeightEntry() {
        tilDate.setError(null);
        tilWeight.setError(null);

        String date = ValidationUtils.safeText(etDate);
        String weightStr = ValidationUtils.safeText(etWeight);

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

        if (hasError) {
            return;
        }

        long newId = weightRepository.addWeight(userId, date, weight);
        if (newId == -1) {
            tilDate.setError("There is already an entry for that date.");
            Toast.makeText(this, "Save failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        etDate.setText(ValidationUtils.todayAsDisplayDate());
        etWeight.setText("");

        reloadWeights();
        checkGoalAndSendSmsIfNeeded(weight);
    }

    private void reloadWeights() {
        entries.clear();
        entries.addAll(weightRepository.getAllWeights(userId));
        adapter.notifyDataSetChanged();
        updateSummaryCard();
    }

    private void updateSummaryCard() {
        currentGoal = goalRepository.getGoalWeight(userId);
        ProgressSummary summary = weightRepository.getProgressSummary(userId);

        if (summary.latestWeight == null) {
            tvCurrentWeight.setText("Current: No entries yet");
            tvGoalWeight.setText(currentGoal == -1 ? "Goal: Not set" : "Goal: " + ValidationUtils.formatWeight(currentGoal));
            tvMotivation.setText("Start with your first weigh-in.");
            return;
        }

        tvCurrentWeight.setText("Current: " + ValidationUtils.formatWeight(summary.latestWeight));
        tvGoalWeight.setText(currentGoal == -1 ? "Goal: Not set" : "Goal: " + ValidationUtils.formatWeight(currentGoal));

        Double change = summary.getChangeFromStart();
        if (change == null) {
            tvMotivation.setText("Entries logged: " + summary.entryCount);
        } else if (change < 0) {
            tvMotivation.setText("Down " + ValidationUtils.formatWeight(Math.abs(change)) + " from your first entry. Entries logged: " + summary.entryCount);
        } else if (change > 0) {
            tvMotivation.setText("Up " + ValidationUtils.formatWeight(change) + " from your first entry. Entries logged: " + summary.entryCount);
        } else {
            tvMotivation.setText("No change from your first entry yet. Entries logged: " + summary.entryCount);
        }
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
            String newDate = ValidationUtils.safeText(inputDate);
            String newWeightStr = ValidationUtils.safeText(inputWeight);

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

            boolean ok = weightRepository.updateWeight(userId, entry.id, newDate, newWeight);
            if (ok) {
                reloadWeights();
                Toast.makeText(this, "Updated.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Update failed. Make sure that date is not already used.", Toast.LENGTH_SHORT).show();
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
            String goalStr = ValidationUtils.safeText(input);

            Double goal = ValidationUtils.parseWeight(goalStr);
            if (goal == null) {
                Toast.makeText(this, "Goal must be a number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isRealisticWeight(goal)) {
                Toast.makeText(this, "Enter a realistic goal between 50 and 700 lbs.", Toast.LENGTH_SHORT).show();
                return;
            }

            goalRepository.setGoalWeight(userId, goal);
            currentGoal = goal;
            updateSummaryCard();
            Toast.makeText(this, "Goal saved.", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void checkGoalAndSendSmsIfNeeded(double newWeight) {
        currentGoal = goalRepository.getGoalWeight(userId);
        if (currentGoal == -1 || newWeight > currentGoal) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Goal reached!", Toast.LENGTH_LONG).show();
            return;
        }

        String phone = sessionManager.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "Goal reached!", Toast.LENGTH_LONG).show();
            return;
        }

        String msg = "You hit your goal! New weigh-in: "
                + ValidationUtils.formatWeight(newWeight)
                + ". Goal: "
                + ValidationUtils.formatWeight(currentGoal)
                + ".";

        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phone, null, msg, null, null);
            Toast.makeText(this, "Goal reached! SMS sent.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Goal reached, but SMS failed to send.", Toast.LENGTH_LONG).show();
        }
    }
}