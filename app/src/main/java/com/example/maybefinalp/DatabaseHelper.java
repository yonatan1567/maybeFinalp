package com.example.maybefinalp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.SharedPreferences;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "game_database.db";
    private static final int DATABASE_VERSION = 2; // Increased version for new tables
    private Context context;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_BONUSES = "bonuses";
    private static final String TABLE_GAME_HISTORY = "game_history";
    private static final String TABLE_ACHIEVEMENTS = "achievements";
    private static final String TABLE_DAILY_STATS = "daily_stats";

    // User table columns
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_COINS = "coins";
    private static final String COLUMN_LAST_LOGIN = "last_login";
    private static final String COLUMN_CONSECUTIVE_DAYS = "consecutive_days";
    private static final String COLUMN_TOTAL_WINS = "total_wins";
    private static final String COLUMN_TOTAL_LOSSES = "total_losses";
    private static final String COLUMN_HIGHEST_COINS = "highest_coins";
    private static final String COLUMN_TOTAL_PLAYTIME = "total_playtime";
    private static final String COLUMN_BONUS_TYPE = "bonus_type";
    private static final String COLUMN_BONUS_TIME = "bonus_time";

    // Game history table columns
    private static final String COLUMN_GAME_ID = "game_id";
    private static final String COLUMN_GAME_TYPE = "game_type";
    private static final String COLUMN_GAME_DATE = "game_date";
    private static final String COLUMN_GAME_RESULT = "game_result";
    private static final String COLUMN_COINS_WON = "coins_won";
    private static final String COLUMN_COINS_LOST = "coins_lost";
    private static final String COLUMN_GAME_DURATION = "game_duration";

    // Achievements table columns
    private static final String COLUMN_ACHIEVEMENT_ID = "achievement_id";
    private static final String COLUMN_ACHIEVEMENT_NAME = "achievement_name";
    private static final String COLUMN_ACHIEVEMENT_DATE = "achievement_date";
    private static final String COLUMN_ACHIEVEMENT_DESCRIPTION = "achievement_description";

    // Daily stats table columns
    private static final String COLUMN_STAT_DATE = "stat_date";
    private static final String COLUMN_GAMES_PLAYED = "games_played";
    private static final String COLUMN_COINS_EARNED = "coins_earned";
    private static final String COLUMN_COINS_SPENT = "coins_spent";
    private static final String COLUMN_WIN_RATE = "win_rate";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table with new columns
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_EMAIL + " TEXT PRIMARY KEY,"
                + COLUMN_USERNAME + " TEXT,"
                + COLUMN_COINS + " INTEGER DEFAULT 1000,"
                + COLUMN_LAST_LOGIN + " INTEGER,"
                + COLUMN_CONSECUTIVE_DAYS + " INTEGER DEFAULT 0,"
                + COLUMN_TOTAL_WINS + " INTEGER DEFAULT 0,"
                + COLUMN_TOTAL_LOSSES + " INTEGER DEFAULT 0,"
                + COLUMN_HIGHEST_COINS + " INTEGER DEFAULT 1000,"
                + COLUMN_TOTAL_PLAYTIME + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create game history table
        String CREATE_GAME_HISTORY_TABLE = "CREATE TABLE " + TABLE_GAME_HISTORY + "("
                + COLUMN_GAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_GAME_TYPE + " TEXT,"
                + COLUMN_GAME_DATE + " INTEGER,"
                + COLUMN_GAME_RESULT + " TEXT,"
                + COLUMN_COINS_WON + " INTEGER,"
                + COLUMN_COINS_LOST + " INTEGER,"
                + COLUMN_GAME_DURATION + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_EMAIL + ")"
                + ")";
        db.execSQL(CREATE_GAME_HISTORY_TABLE);

        // Create achievements table
        String CREATE_ACHIEVEMENTS_TABLE = "CREATE TABLE " + TABLE_ACHIEVEMENTS + "("
                + COLUMN_ACHIEVEMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_ACHIEVEMENT_NAME + " TEXT,"
                + COLUMN_ACHIEVEMENT_DATE + " INTEGER,"
                + COLUMN_ACHIEVEMENT_DESCRIPTION + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_EMAIL + ")"
                + ")";
        db.execSQL(CREATE_ACHIEVEMENTS_TABLE);

        // Create daily stats table
        String CREATE_DAILY_STATS_TABLE = "CREATE TABLE " + TABLE_DAILY_STATS + "("
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_STAT_DATE + " INTEGER,"
                + COLUMN_GAMES_PLAYED + " INTEGER DEFAULT 0,"
                + COLUMN_COINS_EARNED + " INTEGER DEFAULT 0,"
                + COLUMN_COINS_SPENT + " INTEGER DEFAULT 0,"
                + COLUMN_WIN_RATE + " REAL DEFAULT 0,"
                + "PRIMARY KEY(" + COLUMN_EMAIL + ", " + COLUMN_STAT_DATE + "),"
                + "FOREIGN KEY(" + COLUMN_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_EMAIL + ")"
                + ")";
        db.execSQL(CREATE_DAILY_STATS_TABLE);

        // Create bonuses table (existing)
        String CREATE_BONUSES_TABLE = "CREATE TABLE " + TABLE_BONUSES + "("
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_BONUS_TYPE + " TEXT,"
                + COLUMN_BONUS_TIME + " INTEGER,"
                + "PRIMARY KEY (" + COLUMN_EMAIL + ", " + COLUMN_BONUS_TYPE + "),"
                + "FOREIGN KEY(" + COLUMN_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_EMAIL + ")"
                + ")";
        db.execSQL(CREATE_BONUSES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrade
        if (oldVersion < 2) {
            // Add new columns to users table
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_TOTAL_WINS + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_TOTAL_LOSSES + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_HIGHEST_COINS + " INTEGER DEFAULT 1000");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_TOTAL_PLAYTIME + " INTEGER DEFAULT 0");
            
            // Create new tables
            onCreate(db);
        }
    }

    // New methods for game history
    public void addGameHistory(String email, String gameType, String result, int coinsWon, int coinsLost, long duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_GAME_TYPE, gameType);
        values.put(COLUMN_GAME_DATE, System.currentTimeMillis());
        values.put(COLUMN_GAME_RESULT, result);
        values.put(COLUMN_COINS_WON, coinsWon);
        values.put(COLUMN_COINS_LOST, coinsLost);
        values.put(COLUMN_GAME_DURATION, duration);
        
        db.insert(TABLE_GAME_HISTORY, null, values);
        
        // Update user stats
        updateUserStats(email, result.equals("WIN"), coinsWon, coinsLost, duration);
        
        // Update daily stats
        updateDailyStats(email, result.equals("WIN"), coinsWon, coinsLost);
        
        db.close();
    }

    private void updateUserStats(String email, boolean isWin, int coinsWon, int coinsLost, long duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Get current stats
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_TOTAL_WINS, COLUMN_TOTAL_LOSSES, COLUMN_COINS, COLUMN_HIGHEST_COINS, COLUMN_TOTAL_PLAYTIME},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        if (cursor.moveToFirst()) {
            int totalWins = cursor.getInt(0);
            int totalLosses = cursor.getInt(1);
            int currentCoins = cursor.getInt(2);
            int highestCoins = cursor.getInt(3);
            long totalPlaytime = cursor.getLong(4);

            // Update stats
            ContentValues values = new ContentValues();
            if (isWin) {
                values.put(COLUMN_TOTAL_WINS, totalWins + 1);
            } else {
                values.put(COLUMN_TOTAL_LOSSES, totalLosses + 1);
            }
            
            int newCoins = currentCoins + coinsWon - coinsLost;
            values.put(COLUMN_COINS, newCoins);
            if (newCoins > highestCoins) {
                values.put(COLUMN_HIGHEST_COINS, newCoins);
            }
            values.put(COLUMN_TOTAL_PLAYTIME, totalPlaytime + duration);

            db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
            
            // Update SharedPreferences to keep sync
            SharedPreferences sharedPreferences = context.getSharedPreferences("PlayerData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("coins_" + email, newCoins);
            editor.apply();
        }
        cursor.close();
        db.close();
    }

    private void updateDailyStats(String email, boolean isWin, int coinsWon, int coinsLost) {
        SQLiteDatabase db = this.getWritableDatabase();
        long today = System.currentTimeMillis() / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000); // Start of today

        // Get current daily stats
        Cursor cursor = db.query(TABLE_DAILY_STATS,
                new String[]{COLUMN_GAMES_PLAYED, COLUMN_COINS_EARNED, COLUMN_COINS_SPENT, COLUMN_WIN_RATE},
                COLUMN_EMAIL + "=? AND " + COLUMN_STAT_DATE + "=?",
                new String[]{email, String.valueOf(today)},
                null, null, null);

        ContentValues values = new ContentValues();
        if (cursor.moveToFirst()) {
            // Update existing stats
            int gamesPlayed = cursor.getInt(0) + 1;
            int coinsEarned = cursor.getInt(1) + coinsWon;
            int coinsSpent = cursor.getInt(2) + coinsLost;
            float winRate = cursor.getFloat(3);
            int totalWins = (int)(winRate * (gamesPlayed - 1));
            if (isWin) totalWins++;
            winRate = (float)totalWins / gamesPlayed;

            values.put(COLUMN_GAMES_PLAYED, gamesPlayed);
            values.put(COLUMN_COINS_EARNED, coinsEarned);
            values.put(COLUMN_COINS_SPENT, coinsSpent);
            values.put(COLUMN_WIN_RATE, winRate);

            db.update(TABLE_DAILY_STATS, values,
                    COLUMN_EMAIL + "=? AND " + COLUMN_STAT_DATE + "=?",
                    new String[]{email, String.valueOf(today)});
        } else {
            // Create new daily stats
            values.put(COLUMN_EMAIL, email);
            values.put(COLUMN_STAT_DATE, today);
            values.put(COLUMN_GAMES_PLAYED, 1);
            values.put(COLUMN_COINS_EARNED, coinsWon);
            values.put(COLUMN_COINS_SPENT, coinsLost);
            values.put(COLUMN_WIN_RATE, isWin ? 1.0f : 0.0f);

            db.insert(TABLE_DAILY_STATS, null, values);
        }
        cursor.close();
        db.close();
    }

    // New method to get user statistics
    public UserStats getUserStats(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        UserStats stats = new UserStats();
        
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_TOTAL_WINS, COLUMN_TOTAL_LOSSES, COLUMN_HIGHEST_COINS, COLUMN_TOTAL_PLAYTIME},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        if (cursor.moveToFirst()) {
            stats.totalWins = cursor.getInt(0);
            stats.totalLosses = cursor.getInt(1);
            stats.highestCoins = cursor.getInt(2);
            stats.totalPlaytime = cursor.getLong(3);
        }
        cursor.close();

        // Get daily stats
        cursor = db.query(TABLE_DAILY_STATS,
                new String[]{COLUMN_GAMES_PLAYED, COLUMN_COINS_EARNED, COLUMN_COINS_SPENT, COLUMN_WIN_RATE},
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null, COLUMN_STAT_DATE + " DESC",
                "1");

        if (cursor.moveToFirst()) {
            stats.todayGamesPlayed = cursor.getInt(0);
            stats.todayCoinsEarned = cursor.getInt(1);
            stats.todayCoinsSpent = cursor.getInt(2);
            stats.todayWinRate = cursor.getFloat(3);
        }
        cursor.close();
        db.close();
        
        return stats;
    }

    // New method to get game history
    public Cursor getGameHistory(String email, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_GAME_HISTORY,
                null,
                COLUMN_EMAIL + "=?",
                new String[]{email},
                null, null,
                COLUMN_GAME_DATE + " DESC",
                String.valueOf(limit));
    }

    // New method to check and award achievements
    public void checkAchievements(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        UserStats stats = getUserStats(email);
        
        // Define achievements
        Achievement[] achievements = {
            new Achievement("FIRST_WIN", "First Victory", "Win your first game", stats.totalWins >= 1),
            new Achievement("WINNING_STREAK", "Winning Streak", "Win 5 games in a row", stats.totalWins >= 5),
            new Achievement("HIGH_ROLLER", "High Roller", "Reach 10,000 coins", stats.highestCoins >= 10000),
            new Achievement("CASINO_MASTER", "Casino Master", "Play for 24 hours total", stats.totalPlaytime >= 24 * 60 * 60 * 1000),
            new Achievement("DAILY_CHAMPION", "Daily Champion", "Win 10 games in one day", stats.todayGamesPlayed >= 10 && stats.todayWinRate >= 0.5f)
        };

        for (Achievement achievement : achievements) {
            // Check if achievement already awarded
            Cursor cursor = db.query(TABLE_ACHIEVEMENTS,
                    new String[]{COLUMN_ACHIEVEMENT_ID},
                    COLUMN_EMAIL + "=? AND " + COLUMN_ACHIEVEMENT_NAME + "=?",
                    new String[]{email, achievement.name},
                    null, null, null);

            if (!cursor.moveToFirst() && achievement.achieved) {
                // Award new achievement
                ContentValues values = new ContentValues();
                values.put(COLUMN_EMAIL, email);
                values.put(COLUMN_ACHIEVEMENT_NAME, achievement.name);
                values.put(COLUMN_ACHIEVEMENT_DATE, System.currentTimeMillis());
                values.put(COLUMN_ACHIEVEMENT_DESCRIPTION, achievement.description);
                db.insert(TABLE_ACHIEVEMENTS, null, values);

                // Give bonus coins for achievement
                updateUserStats(email, true, 500, 0, 0); // 500 coins bonus
                
                // Show achievement notification
                showAchievementNotification(achievement.name, achievement.description);
            }
            cursor.close();
        }
        db.close();
    }

    private void showAchievementNotification(String name, String description) {
        // Implementation for showing achievement notification
        // This would typically use NotificationManager to show a notification
        Log.d("Achievements", "Achievement Unlocked: " + name + " - " + description);
    }

    // Helper classes for data structures
    public static class UserStats {
        public int totalWins;
        public int totalLosses;
        public int highestCoins;
        public long totalPlaytime;
        public int todayGamesPlayed;
        public int todayCoinsEarned;
        public int todayCoinsSpent;
        public float todayWinRate;
    }

    private static class Achievement {
        String name;
        String title;
        String description;
        boolean achieved;

        Achievement(String name, String title, String description, boolean achieved) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.achieved = achieved;
        }
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