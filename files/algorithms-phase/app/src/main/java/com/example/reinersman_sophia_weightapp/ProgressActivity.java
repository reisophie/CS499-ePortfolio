package com.example.reinersman_sophia_weightapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

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

    private TextView tvCurrentWeight;
    private TextView tvGoalWeight;
    private TextView tvMotivation;

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

        sortEntriesByDateDescending();

        adapter.notifyDataSetChanged();
        updateSummaryCard();
    }

    private void sortEntriesByDateDescending() {
        Collections.sort(entries, new Comparator<WeightEntry>() {
            @Override
            public int compare(WeightEntry a, WeightEntry b) {
                Date dateA = ValidationUtils.parseDate(a.date);
                Date dateB = ValidationUtils.parseDate(b.date);

                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;

                return dateB.compareTo(dateA); // newest date first
            }
        });
    }

    private void updateSummaryCard() {
        currentGoal = db.getGoalWeight(userId);

        if (entries.isEmpty()) {
            tvCurrentWeight.setText("Current: No entries yet");
            if (currentGoal == -1) {
                tvGoalWeight.setText("Goal: Not set");
            } else {
                tvGoalWeight.setText("Goal: " + ValidationUtils.formatWeight(currentGoal) + " lbs");
            }
            tvMotivation.setText("Add your first weigh-in to start tracking progress.");
            return;
        }

        WeightEntry newestEntry = entries.get(0);
        WeightEntry oldestEntry = entries.get(entries.size() - 1);

        double currentWeight = newestEntry.weight;
        double startingWeight = oldestEntry.weight;
        double totalChange = currentWeight - startingWeight;

        tvCurrentWeight.setText("Current: " + ValidationUtils.formatWeight(currentWeight) + " lbs");

        if (currentGoal == -1) {
            tvGoalWeight.setText("Goal: Not set");
        } else {
            tvGoalWeight.setText("Goal: " + ValidationUtils.formatWeight(currentGoal) + " lbs");
        }

        if (entries.size() == 1) {
            if (currentGoal != -1) {
                double remaining = currentWeight - currentGoal;
                if (remaining > 0) {
                    tvMotivation.setText("You are " + ValidationUtils.formatWeight(remaining) + " lbs from your goal.");
                } else {
                    tvMotivation.setText("Goal reached — great job!");
                }
            } else {
                tvMotivation.setText("Keep it up. Add more entries to see your progress.");
            }
            return;
        }

        WeightEntry previousEntry = entries.get(1);
        double recentChange = currentWeight - previousEntry.weight;

        String recentText;
        if (recentChange < 0) {
            recentText = "Down " + ValidationUtils.formatWeight(Math.abs(recentChange)) + " lbs since last entry.";
        } else if (recentChange > 0) {
            recentText = "Up " + ValidationUtils.formatWeight(recentChange) + " lbs since last entry.";
        } else {
            recentText = "No change since last entry.";
        }

        String totalText;
        if (totalChange < 0) {
            totalText = " Down " + ValidationUtils.formatWeight(Math.abs(totalChange)) + " lbs from your first entry.";
        } else if (totalChange > 0) {
            totalText = " Up " + ValidationUtils.formatWeight(totalChange) + " lbs from your first entry.";
        } else {
            totalText = " No overall change from your first entry.";
        }

        if (currentGoal != -1) {
            double remaining = currentWeight - currentGoal;
            String goalText;
            if (remaining > 0) {
                goalText = " You are " + ValidationUtils.formatWeight(remaining) + " lbs from your goal.";
            } else {
                goalText = " Goal reached — great job!";
            }
            tvMotivation.setText(recentText + totalText + goalText);
        } else {
            tvMotivation.setText(recentText + totalText);
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
            updateSummaryCard();
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