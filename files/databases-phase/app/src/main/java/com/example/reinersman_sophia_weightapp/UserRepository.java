package com.example.reinersman_sophia_weightapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserRepository {

    private final AppDatabaseHelper dbHelper;

    public UserRepository(AppDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public boolean usernameExists(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                AppContract.Users.TABLE,
                new String[]{AppContract.Users.ID},
                AppContract.Users.USERNAME + "=?",
                new String[]{username},
                null,
                null,
                null
        );

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    public long createUser(String username, String rawPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppContract.Users.USERNAME, username);
        values.put(AppContract.Users.PASSWORD_HASH, PasswordUtils.hashPassword(rawPassword));
        return db.insert(AppContract.Users.TABLE, null, values);
    }

    public long validateLogin(String username, String rawPassword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                AppContract.Users.TABLE,
                new String[]{AppContract.Users.ID},
                AppContract.Users.USERNAME + "=? AND " + AppContract.Users.PASSWORD_HASH + "=?",
                new String[]{username, PasswordUtils.hashPassword(rawPassword)},
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return -1;
        } finally {
            cursor.close();
        }
    }
}