package com.example.reinersman_sophia_weightapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(AppContract.Prefs.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(long userId) {
        prefs.edit().putLong(AppContract.Prefs.KEY_USER_ID, userId).apply();
    }

    public long getUserId() {
        return prefs.getLong(AppContract.Prefs.KEY_USER_ID, -1);
    }

    public void clearUser() {
        prefs.edit().remove(AppContract.Prefs.KEY_USER_ID).apply();
    }

    public void savePhone(String phone) {
        prefs.edit().putString(AppContract.Prefs.KEY_SMS_PHONE, ValidationUtils.cleanPhone(phone)).apply();
    }

    public String getPhone() {
        return prefs.getString(AppContract.Prefs.KEY_SMS_PHONE, "");
    }
}