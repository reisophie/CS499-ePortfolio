package com.example.reinersman_sophia_weightapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class WeightRepository {

    private final AppDatabaseHelper dbHelper;

    public WeightRepository(AppDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long addWeight(long userId, String date, double weight) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppContract.Weights.USER_ID, userId);
        values.put(AppContract.Weights.DATE, ValidationUtils.normalizeDate(date));
        values.put(AppContract.Weights.WEIGHT, weight);
        return db.insert(AppContract.Weights.TABLE, null, values);
    }

    public ArrayList<WeightEntry> getAllWeights(long userId) {
        ArrayList<WeightEntry> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                AppContract.Weights.TABLE,
                new String[]{AppContract.Weights.ID, AppContract.Weights.DATE, AppContract.Weights.WEIGHT},
                AppContract.Weights.USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                AppContract.Weights.DATE + " DESC"
        );

        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String date = cursor.getString(1);
                double weight = cursor.getDouble(2);
                list.add(new WeightEntry(id, date, weight));
            }
        } finally {
            cursor.close();
        }

        return list;
    }

    public boolean deleteWeight(long userId, long weightId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(
                AppContract.Weights.TABLE,
                AppContract.Weights.USER_ID + "=? AND " + AppContract.Weights.ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(weightId)}
        );
        return rows > 0;
    }

    public boolean updateWeight(long userId, long weightId, String newDate, double newWeight) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppContract.Weights.DATE, ValidationUtils.normalizeDate(newDate));
        values.put(AppContract.Weights.WEIGHT, newWeight);

        int rows = db.update(
                AppContract.Weights.TABLE,
                values,
                AppContract.Weights.USER_ID + "=? AND " + AppContract.Weights.ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(weightId)}
        );
        return rows > 0;
    }

    public ProgressSummary getProgressSummary(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor latestCursor = db.query(
                AppContract.Weights.TABLE,
                new String[]{AppContract.Weights.WEIGHT},
                AppContract.Weights.USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                AppContract.Weights.DATE + " DESC",
                "1"
        );

        Cursor firstCursor = db.query(
                AppContract.Weights.TABLE,
                new String[]{AppContract.Weights.WEIGHT},
                AppContract.Weights.USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                AppContract.Weights.DATE + " ASC",
                "1"
        );

        Cursor countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + AppContract.Weights.TABLE + " WHERE " + AppContract.Weights.USER_ID + "=?",
                new String[]{String.valueOf(userId)}
        );

        try {
            Double latestWeight = latestCursor.moveToFirst() ? latestCursor.getDouble(0) : null;
            Double startingWeight = firstCursor.moveToFirst() ? firstCursor.getDouble(0) : null;
            int count = countCursor.moveToFirst() ? countCursor.getInt(0) : 0;
            return new ProgressSummary(count, latestWeight, startingWeight);
        } finally {
            latestCursor.close();
            firstCursor.close();
            countCursor.close();
        }
    }
}