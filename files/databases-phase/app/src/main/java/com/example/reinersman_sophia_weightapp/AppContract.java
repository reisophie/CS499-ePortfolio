package com.example.reinersman_sophia_weightapp;

public final class AppContract {

    private AppContract() {
    }

    public static final String DB_NAME = "weight_app.db";
    public static final int DB_VERSION = 2;

    public static final class Prefs {
        public static final String PREFS_NAME = "app_prefs";
        public static final String KEY_USER_ID = "user_id";
        public static final String KEY_SMS_PHONE = "sms_phone";

        private Prefs() {
        }
    }

    public static final class Users {
        public static final String TABLE = "users";
        public static final String ID = "_id";
        public static final String USERNAME = "username";
        public static final String PASSWORD_HASH = "password_hash";

        private Users() {
        }
    }

    public static final class Weights {
        public static final String TABLE = "weights";
        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String DATE = "date";
        public static final String WEIGHT = "weight";

        private Weights() {
        }
    }

    public static final class Goals {
        public static final String TABLE = "goals";
        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String GOAL_WEIGHT = "goal_weight";

        private Goals() {
        }
    }
}