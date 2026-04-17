package com.example.reinersman_sophia_weightapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }
}