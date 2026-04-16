package com.example.reinersman_sophia_weightapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ValidationUtils {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    static {
        DATE_FORMAT.setLenient(false);
    }

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        username = username.trim();
        return username.length() >= 3 && username.matches("[A-Za-z0-9_]+");
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.trim().length() >= 4;
    }

    public static boolean isValidDate(String date) {
        if (date == null || date.trim().isEmpty()) return false;
        try {
            DATE_FORMAT.parse(date.trim());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static Double parseWeight(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRealisticWeight(double weight) {
        return weight >= 50 && weight <= 700;
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10;
    }

    public static String cleanPhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\D", "");
    }
}