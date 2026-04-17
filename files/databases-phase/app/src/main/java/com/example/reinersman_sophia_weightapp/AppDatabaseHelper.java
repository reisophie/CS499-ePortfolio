package com.example.reinersman_sophia_weightapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    public AppDatabaseHelper(@Nullable Context context) {
        super(context, AppContract.DB_NAME, null, AppContract.DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + AppContract.Users.TABLE + " ("
                + AppContract.Users.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + AppContract.Users.USERNAME + " TEXT NOT NULL UNIQUE, "
                + AppContract.Users.PASSWORD_HASH + " TEXT NOT NULL"
                + ");";

        String createWeights = "CREATE TABLE " + AppContract.Weights.TABLE + " ("
                + AppContract.Weights.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + AppContract.Weights.USER_ID + " INTEGER NOT NULL, "
                + AppContract.Weights.DATE + " TEXT NOT NULL, "
                + AppContract.Weights.WEIGHT + " REAL NOT NULL, "
                + "UNIQUE(" + AppContract.Weights.USER_ID + ", " + AppContract.Weights.DATE + "), "
                + "FOREIGN KEY(" + AppContract.Weights.USER_ID + ") REFERENCES "
                + AppContract.Users.TABLE + "(" + AppContract.Users.ID + ") ON DELETE CASCADE"
                + ");";

        String createGoals = "CREATE TABLE " + AppContract.Goals.TABLE + " ("
                + AppContract.Goals.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + AppContract.Goals.USER_ID + " INTEGER NOT NULL UNIQUE, "
                + AppContract.Goals.GOAL_WEIGHT + " REAL NOT NULL, "
                + "FOREIGN KEY(" + AppContract.Goals.USER_ID + ") REFERENCES "
                + AppContract.Users.TABLE + "(" + AppContract.Users.ID + ") ON DELETE CASCADE"
                + ");";

        db.execSQL(createUsers);
        db.execSQL(createWeights);
        db.execSQL(createGoals);
        db.execSQL("CREATE INDEX idx_weights_user_date ON " + AppContract.Weights.TABLE
                + "(" + AppContract.Weights.USER_ID + ", " + AppContract.Weights.DATE + " DESC);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + AppContract.Goals.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + AppContract.Weights.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + AppContract.Users.TABLE);
        onCreate(db);
    }
}