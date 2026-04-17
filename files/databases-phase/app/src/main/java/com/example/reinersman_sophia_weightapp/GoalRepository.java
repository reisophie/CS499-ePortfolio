package com.example.reinersman_sophia_weightapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class GoalRepository {

    private final AppDatabaseHelper dbHelper;

    public GoalRepository(AppDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public void setGoalWeight(long userId, double goalWeight) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppContract.Goals.USER_ID, userId);
        values.put(AppContract.Goals.GOAL_WEIGHT, goalWeight);

        int updated = db.update(
                AppContract.Goals.TABLE,
                values,
                AppContract.Goals.USER_ID + "=?",
                new String[]{String.valueOf(userId)}
        );

        if (updated == 0) {
            db.insert(AppContract.Goals.TABLE, null, values);
        }
    }

    public double getGoalWeight(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                AppContract.Goals.TABLE,
                new String[]{AppContract.Goals.GOAL_WEIGHT},
                AppContract.Goals.USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
            return -1;
        } finally {
            cursor.close();
        }
    }
}