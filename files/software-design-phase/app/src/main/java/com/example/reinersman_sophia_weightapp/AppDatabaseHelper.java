package com.example.reinersman_sophia_weightapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * AppDatabaseHelper
 * - Holds 3 tables:
 *   1) users (login/password)
 *   2) weights (daily weigh-ins)
 *   3) goals (one goal per user)
 */
public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "weight_app.db";
    private static final int DB_VERSION = 1;

    // ===== Table: users =====
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "_id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // ===== Table: weights =====
    public static final String TABLE_WEIGHTS = "weights";
    public static final String COL_WEIGHT_ID = "_id";
    public static final String COL_WEIGHT_USER_ID = "user_id";
    public static final String COL_DATE = "date";
    public static final String COL_WEIGHT = "weight";

    // ===== Table: goals =====
    public static final String TABLE_GOALS = "goals";
    public static final String COL_GOAL_ID = "_id";
    public static final String COL_GOAL_USER_ID = "user_id";
    public static final String COL_GOAL_WEIGHT = "goal_weight";

    public AppDatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Users table (username unique so you can't create duplicates)
        String createUsers =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USERNAME + " TEXT NOT NULL UNIQUE, " +
                        COL_PASSWORD + " TEXT NOT NULL" +
                        ");";

        // Weights table (each row belongs to a user)
        String createWeights =
                "CREATE TABLE " + TABLE_WEIGHTS + " (" +
                        COL_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_WEIGHT_USER_ID + " INTEGER NOT NULL, " +
                        COL_DATE + " TEXT NOT NULL, " +
                        COL_WEIGHT + " REAL NOT NULL, " +
                        "FOREIGN KEY(" + COL_WEIGHT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                        ");";

        // Goals table (one row per user)
        String createGoals =
                "CREATE TABLE " + TABLE_GOALS + " (" +
                        COL_GOAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_GOAL_USER_ID + " INTEGER NOT NULL UNIQUE, " +
                        COL_GOAL_WEIGHT + " REAL NOT NULL, " +
                        "FOREIGN KEY(" + COL_GOAL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                        ");";

        db.execSQL(createUsers);
        db.execSQL(createWeights);
        db.execSQL(createGoals);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple approach for class projects
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // =========================
    // Users (Create + Read)
    // =========================

    public boolean usernameExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);

        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public long createUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(COL_USERNAME, username);
        cv.put(COL_PASSWORD, password);

        // returns -1 if insert fails
        return db.insert(TABLE_USERS, null, cv);
    }

    /**
     * Returns userId if valid, otherwise returns -1.
     */
    public long validateLogin(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);

        long userId = -1;
        if (c.moveToFirst()) {
            userId = c.getLong(0);
        }
        c.close();
        return userId;
    }

    // =========================
    // Goals (Create/Update + Read)
    // =========================

    public void setGoalWeight(long userId, double goalWeight) {
        SQLiteDatabase db = getWritableDatabase();

        // Because goal_user_id is UNIQUE, we can update or insert.
        ContentValues cv = new ContentValues();
        cv.put(COL_GOAL_USER_ID, userId);
        cv.put(COL_GOAL_WEIGHT, goalWeight);

        int updated = db.update(TABLE_GOALS, cv, COL_GOAL_USER_ID + "=?",
                new String[]{String.valueOf(userId)});

        if (updated == 0) {
            db.insert(TABLE_GOALS, null, cv);
        }
    }

    /**
     * Returns goal weight if exists, otherwise returns -1.
     */
    public double getGoalWeight(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_GOALS,
                new String[]{COL_GOAL_WEIGHT},
                COL_GOAL_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        double goal = -1;
        if (c.moveToFirst()) {
            goal = c.getDouble(0);
        }
        c.close();
        return goal;
    }

    // =========================
    // Weights CRUD
    // =========================

    public long addWeight(long userId, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_WEIGHT_USER_ID, userId);
        cv.put(COL_DATE, date);
        cv.put(COL_WEIGHT, weight);
        return db.insert(TABLE_WEIGHTS, null, cv);
    }

    public ArrayList<WeightEntry> getAllWeights(long userId) {
        ArrayList<WeightEntry> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.query(TABLE_WEIGHTS,
                new String[]{COL_WEIGHT_ID, COL_DATE, COL_WEIGHT},
                COL_WEIGHT_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_WEIGHT_ID + " DESC"); // newest first

        while (c.moveToNext()) {
            long id = c.getLong(0);
            String date = c.getString(1);
            double weight = c.getDouble(2);
            list.add(new WeightEntry(id, date, weight));
        }
        c.close();
        return list;
    }

    public boolean deleteWeight(long userId, long weightId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_WEIGHTS,
                COL_WEIGHT_USER_ID + "=? AND " + COL_WEIGHT_ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(weightId)});
        return rows > 0;
    }

    public boolean updateWeight(long userId, long weightId, String newDate, double newWeight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DATE, newDate);
        cv.put(COL_WEIGHT, newWeight);

        int rows = db.update(TABLE_WEIGHTS, cv,
                COL_WEIGHT_USER_ID + "=? AND " + COL_WEIGHT_ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(weightId)});
        return rows > 0;
    }
}