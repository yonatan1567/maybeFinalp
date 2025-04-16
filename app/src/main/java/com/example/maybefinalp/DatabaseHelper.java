package com.example.maybefinalp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.SharedPreferences;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "game_database.db";
    private static final int DATABASE_VERSION = 1;
    private Context context;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_BONUSES = "bonuses";

    // User table columns
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_COINS = "coins";
    private static final String COLUMN_LAST_LOGIN = "last_login";
    private static final String COLUMN_CONSECUTIVE_DAYS = "consecutive_days";

    // Bonus table columns
    private static final String COLUMN_BONUS_TYPE = "bonus_type";
    private static final String COLUMN_BONUS_TIME = "bonus_time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_EMAIL + " TEXT PRIMARY KEY,"
                + COLUMN_COINS + " INTEGER,"
                + COLUMN_LAST_LOGIN + " INTEGER,"
                + COLUMN_CONSECUTIVE_DAYS + " INTEGER"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create bonuses table
        String CREATE_BONUSES_TABLE = "CREATE TABLE " + TABLE_BONUSES + "("
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_BONUS_TYPE + " TEXT,"
                + COLUMN_BONUS_TIME + " INTEGER,"
                + "PRIMARY KEY (" + COLUMN_EMAIL + ", " + COLUMN_BONUS_TYPE + ")"
                + ")";
        db.execSQL(CREATE_BONUSES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BONUSES);
        onCreate(db);
    }

    public int getUserCoins(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_COINS},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        int coins = 0;
        if (cursor.moveToFirst()) {
            coins = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return coins;
    }

    public boolean checkAndGiveBonus(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis();
        long fourHours = 4 * 60 * 60 * 1000; // 4 hours in milliseconds

        Cursor cursor = db.query(TABLE_BONUSES,
                new String[]{COLUMN_BONUS_TIME},
                COLUMN_EMAIL + "=? AND " + COLUMN_BONUS_TYPE + "=?",
                new String[]{email, "4hour"},
                null, null, null);

        boolean shouldGiveBonus = false;
        if (cursor.moveToFirst()) {
            long lastBonusTime = cursor.getLong(0);
            if (currentTime - lastBonusTime >= fourHours) {
                shouldGiveBonus = true;
            }
        } else {
            shouldGiveBonus = true;
        }
        cursor.close();

        if (shouldGiveBonus) {
            // Update bonus time
            ContentValues values = new ContentValues();
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_BONUS_TYPE, "4hour");
            values.put(COLUMN_BONUS_TIME, currentTime);
            db.replace(TABLE_BONUSES, null, values);

            // Add coins
            int currentCoins = getUserCoins(email);
            ContentValues userValues = new ContentValues();
            userValues.put(COLUMN_COINS, currentCoins + 100);
            db.update(TABLE_USERS, userValues, COLUMN_EMAIL + "=?", new String[]{email});

            // Update SharedPreferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("PlayerData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("coins_" + email, currentCoins + 100);
            editor.apply();
        }

        db.close();
        return shouldGiveBonus;
    }

    public int checkAndUpdateConsecutiveDays(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_LAST_LOGIN, COLUMN_CONSECUTIVE_DAYS},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        int consecutiveDays = 0;
        if (cursor.moveToFirst()) {
            long lastLogin = cursor.getLong(0);
            consecutiveDays = cursor.getInt(1);

            if (currentTime - lastLogin >= oneDay && currentTime - lastLogin < 2 * oneDay) {
                consecutiveDays++;
            } else if (currentTime - lastLogin >= 2 * oneDay) {
                consecutiveDays = 1;
            }

            // Update last login and consecutive days
            ContentValues values = new ContentValues();
            values.put(COLUMN_LAST_LOGIN, currentTime);
            values.put(COLUMN_CONSECUTIVE_DAYS, consecutiveDays);
            db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        } else {
            // First time login
            ContentValues values = new ContentValues();
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_LAST_LOGIN, currentTime);
            values.put(COLUMN_CONSECUTIVE_DAYS, 1);
            db.insert(TABLE_USERS, null, values);
            consecutiveDays = 1;
        }

        cursor.close();
        db.close();
        return consecutiveDays;
    }

    public boolean checkAndGiveComebackBonus(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis();
        long threeDays = 3 * 24 * 60 * 60 * 1000; // 3 days in milliseconds

        Cursor cursor = db.query(TABLE_BONUSES,
                new String[]{COLUMN_BONUS_TIME},
                COLUMN_EMAIL + "=? AND " + COLUMN_BONUS_TYPE + "=?",
                new String[]{email, "comeback"},
                null, null, null);

        boolean shouldGiveBonus = false;
        if (cursor.moveToFirst()) {
            long lastBonusTime = cursor.getLong(0);
            if (currentTime - lastBonusTime >= threeDays) {
                shouldGiveBonus = true;
            }
        } else {
            shouldGiveBonus = true;
        }
        cursor.close();

        if (shouldGiveBonus) {
            // Update bonus time
            ContentValues values = new ContentValues();
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_BONUS_TYPE, "comeback");
            values.put(COLUMN_BONUS_TIME, currentTime);
            db.replace(TABLE_BONUSES, null, values);

            // Add coins
            int currentCoins = getUserCoins(email);
            ContentValues userValues = new ContentValues();
            userValues.put(COLUMN_COINS, currentCoins + 200);
            db.update(TABLE_USERS, userValues, COLUMN_EMAIL + "=?", new String[]{email});

            // Update SharedPreferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("PlayerData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("coins_" + email, currentCoins + 200);
            editor.apply();
        }

        db.close();
        return shouldGiveBonus;
    }

    public boolean checkAndGiveVIPBonus(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

        Cursor cursor = db.query(TABLE_BONUSES,
                new String[]{COLUMN_BONUS_TIME},
                COLUMN_EMAIL + "=? AND " + COLUMN_BONUS_TYPE + "=?",
                new String[]{email, "vip"},
                null, null, null);

        boolean shouldGiveBonus = false;
        if (cursor.moveToFirst()) {
            long lastBonusTime = cursor.getLong(0);
            if (currentTime - lastBonusTime >= oneDay) {
                shouldGiveBonus = true;
            }
        } else {
            shouldGiveBonus = true;
        }
        cursor.close();

        if (shouldGiveBonus) {
            // Update bonus time
            ContentValues values = new ContentValues();
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_BONUS_TYPE, "vip");
            values.put(COLUMN_BONUS_TIME, currentTime);
            db.replace(TABLE_BONUSES, null, values);

            // Add coins
            int currentCoins = getUserCoins(email);
            ContentValues userValues = new ContentValues();
            userValues.put(COLUMN_COINS, currentCoins + 200);
            db.update(TABLE_USERS, userValues, COLUMN_EMAIL + "=?", new String[]{email});

            // Update SharedPreferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("PlayerData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("coins_" + email, currentCoins + 200);
            editor.apply();
        }

        db.close();
        return shouldGiveBonus;
    }
} 