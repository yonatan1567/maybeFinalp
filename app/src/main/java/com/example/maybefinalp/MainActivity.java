package com.example.maybefinalp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.maybefinalp.DatabaseHelper;
import com.example.maybefinalp.TapGameActivity;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "game_reminder";
    private static final int NOTIFICATION_ID = 1;
    private int coins = 1000;
    private TextView coinCountTextView;
    private SharedPreferences sharedPreferences;
    private TextView usernameDisplay;
    private Button logoutButton;
    private Button blackjackButton;
    private Button freeGameButton;
    private TextView noCoinsMessage;
    private LinearLayout llMain;
    private ImageView rankImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        scheduleReminder();

        coinCountTextView = findViewById(R.id.coinCount);
        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);
        usernameDisplay = findViewById(R.id.usernameDisplay);
        logoutButton = findViewById(R.id.logoutButton);
        blackjackButton = findViewById(R.id.blackjackButton);
        freeGameButton = findViewById(R.id.freeGameButton);
        noCoinsMessage = findViewById(R.id.noCoinsMessage);
        llMain = findViewById(R.id.llMain);
        rankImageView = findViewById(R.id.rankImageView);

        // Get sign in and sign up buttons
        Button signInButton = findViewById(R.id.logInButton);
        Button signUpButton = findViewById(R.id.signUpButton);

        updateUsernameDisplay();

        // Initialize coins based on logged-in user
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            coins = sharedPreferences.getInt("coins_" + currentUserEmail, 1000);
        } else {
            coins = 1000;
        }
        coinCountTextView.setText("Coins: " + coins);
        updateBackground();

        // Update button states based on login status
        boolean isLoggedIn = isUserLoggedIn();
        signInButton.setEnabled(!isLoggedIn);
        signInButton.setAlpha(isLoggedIn ? 0.5f : 1.0f);
        signUpButton.setEnabled(!isLoggedIn);
        signUpButton.setAlpha(isLoggedIn ? 0.5f : 1.0f);
        logoutButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);

        // Setup logout button
        logoutButton.setOnClickListener(v -> {
            // Clear login state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("currentUserEmail");
            editor.apply();

            // Reset UI elements
            usernameDisplay.setText("Player: Guest");
            coins = 1000;
            coinCountTextView.setText("Coins: " + coins);
            llMain.setBackgroundResource(R.drawable.lobby);
            if (rankImageView != null) {
                rankImageView.setImageResource(R.drawable.lobby_r);
            }
            
            // Reset button states
            signInButton.setEnabled(true);
            signInButton.setAlpha(1.0f);
            signUpButton.setEnabled(true);
            signUpButton.setAlpha(1.0f);
            logoutButton.setVisibility(View.GONE);
            blackjackButton.setEnabled(false);
            blackjackButton.setAlpha(0.5f);
            freeGameButton.setEnabled(false);
            freeGameButton.setAlpha(0.5f);
            noCoinsMessage.setVisibility(View.GONE);

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });

        // כפתור לשחק במשחק
        blackjackButton.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                String userEmail = sharedPreferences.getString("currentUserEmail", null);
                if (userEmail != null) {
                    int userCoins = sharedPreferences.getInt("coins_" + userEmail, 0);
                    if (userCoins > 0) {
                        Intent intent = new Intent(MainActivity.this, BlackjackActivity.class);
                        startActivityForResult(intent, 1);
                    } else {
                        showToast("אין לך מספיק מטבעות! נסה את המשחק החינמי");
                        noCoinsMessage.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                showToast("Please log in first");
            }
        });

        freeGameButton.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                String userEmail = sharedPreferences.getString("currentUserEmail", null);
                if (userEmail != null) {
                    int userCoins = sharedPreferences.getInt("coins_" + userEmail, 0);
                    if (userCoins == 0) {
                        Intent intent = new Intent(MainActivity.this, TapGameActivity.class);
                        startActivityForResult(intent, 1);
                    } else {
                        showToast("You can only play the free game when you have 0 coins!");
                    }
                }
            } else {
                showToast("Please log in first");
            }
        });

        // כפתור להרשמה
        signUpButton.setOnClickListener(v -> {
            if (!isUserLoggedIn()) {
                Intent intent = new Intent(MainActivity.this, sign_up.class);
                startActivityForResult(intent, 2);
            }
        });

        // כפתור לכניסה
        signInButton.setOnClickListener(v1 -> {
            if (!isUserLoggedIn()) {
                Intent intent1 = new Intent(MainActivity.this, log_in.class);
                startActivity(intent1);
            }
        });

        // כפתור ל-leaderboard
        Button leaderboardButton = findViewById(R.id.leaderboard);
        leaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update coins from SharedPreferences
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            coins = sharedPreferences.getInt("coins_" + currentUserEmail, 1000);
            coinCountTextView.setText("Coins: " + coins);
        }
        
        // Update UI elements
        updateUsernameDisplay();
        updateButtonsState();
        updateBackground();
        checkBonuses();
    }

    private boolean isUserLoggedIn() {
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        return currentUserEmail != null;
    }

    private void updateUsernameDisplay() {
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            String username = sharedPreferences.getString("username_" + currentUserEmail, "Guest");
            usernameDisplay.setText("Player: " + username);
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            usernameDisplay.setText("Player: Guest");
            logoutButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            int updatedCoins = data.getIntExtra("coins", coins);
            coins = updatedCoins;
            coinCountTextView.setText("Coins: " + coins);
            
            // Save updated coins for current user
            String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
            if (currentUserEmail != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("coins_" + currentUserEmail, coins);
                editor.apply();
                
                // Update database
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("coins", coins);
                db.update("users", values, "email = ?", new String[]{currentUserEmail});
                db.close();
            }
            
            // Update button states based on new coin count
            updateButtonsState();
            updateBackground();
        }
        
        // Handle login success
        if (resultCode == RESULT_OK && data != null && data.getBooleanExtra("login_success", false)) {
            // Update UI elements
            updateUsernameDisplay();
            updateButtonsState();
            updateBackground();
            
            // Update coins
            String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
            if (currentUserEmail != null) {
                coins = sharedPreferences.getInt("coins_" + currentUserEmail, 1000);
                coinCountTextView.setText("Coins: " + coins);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Game Reminder";
            String description = "Reminds player to return to the game";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleReminder() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    // BroadcastReceiver for handling the reminder
    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Come back to play!")
                .setContentText("We miss you! Come back and try to win more coins!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void checkBonuses() {
        String email = sharedPreferences.getString("currentUserEmail", null);
        if (email != null && !email.isEmpty()) {
            // For now, we'll just use SharedPreferences for bonuses
            // We can implement the database version later if needed
            
            // Check 4-hour bonus
            long lastBonusTime = sharedPreferences.getLong("last_bonus_time_" + email, 0);
            long currentTime = System.currentTimeMillis();
            long fourHours = 4 * 60 * 60 * 1000; // 4 hours in milliseconds
            
            if (currentTime - lastBonusTime >= fourHours) {
                int currentCoins = sharedPreferences.getInt("coins_" + email, 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("coins_" + email, currentCoins + 100);
                editor.putLong("last_bonus_time_" + email, currentTime);
                editor.apply();
                showToast("You received 100 coins bonus!");
            }

            // Check consecutive days
            long lastLogin = sharedPreferences.getLong("last_login_" + email, 0);
            int consecutiveDays = sharedPreferences.getInt("consecutive_days_" + email, 0);
            long oneDay = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

            // Only show message if this is a new login (last login was more than 12 hours ago)
            boolean isNewLogin = (currentTime - lastLogin) > (12 * 60 * 60 * 1000);

            if (currentTime - lastLogin >= oneDay && currentTime - lastLogin < 2 * oneDay) {
                consecutiveDays++;
            } else if (currentTime - lastLogin >= 2 * oneDay) {
                consecutiveDays = 1;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("last_login_" + email, currentTime);
            editor.putInt("consecutive_days_" + email, consecutiveDays);
            editor.apply();

            // Only show the message if this is a new login
            if (isNewLogin && consecutiveDays > 0) {
                showToast("Day " + consecutiveDays + " of 7 - Keep logging in daily!");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String getStoredEmail() {
        return sharedPreferences.getString("currentUserEmail", "");
    }

    private void updateButtonsState() {
        boolean isLoggedIn = isUserLoggedIn();
        Button signInButton = findViewById(R.id.logInButton);
        Button signUpButton = findViewById(R.id.signUpButton);

        signInButton.setEnabled(!isLoggedIn);
        signInButton.setAlpha(isLoggedIn ? 0.5f : 1.0f);
        signUpButton.setEnabled(!isLoggedIn);
        signUpButton.setAlpha(isLoggedIn ? 0.5f : 1.0f);
        logoutButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);

        if (!isLoggedIn) {
            // If no user is logged in, disable game buttons and hide no coins message
            blackjackButton.setEnabled(false);
            blackjackButton.setAlpha(0.5f);
            freeGameButton.setEnabled(false);
            freeGameButton.setAlpha(0.5f);
            noCoinsMessage.setVisibility(View.GONE);
            return;
        }

        // Only check coins if user is logged in
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            int coins = sharedPreferences.getInt("coins_" + currentUserEmail, 0);
            
            if (coins == 0) {
                blackjackButton.setEnabled(false);
                blackjackButton.setAlpha(0.5f);
                freeGameButton.setEnabled(true);
                freeGameButton.setAlpha(1.0f);
                noCoinsMessage.setVisibility(View.VISIBLE);
            } else {
                blackjackButton.setEnabled(true);
                blackjackButton.setAlpha(1.0f);
                freeGameButton.setEnabled(false);
                freeGameButton.setAlpha(0.5f);
                noCoinsMessage.setVisibility(View.GONE);
            }
        }
    }

    private void updateBackground() {
        int drawable;
        int rankDrawable;

        if (coins > 50000) {
            drawable = R.drawable.dimond;
            rankDrawable = R.drawable.dimond_r;
        }
        else if (coins > 30000) {
            drawable = R.drawable.roby1;
            rankDrawable = R.drawable.roby_r;
        }
        else if (coins > 20000) {
            drawable = R.drawable.gold;
            rankDrawable = R.drawable.gold_r;
        }
        else if (coins > 10000) {
            drawable = R.drawable.silver;
            rankDrawable = R.drawable.silver_r;
        }
        else if (coins > 5000) {
            drawable = R.drawable.bronze2;
            rankDrawable = R.drawable.bronze_r;
        }
        else {
            drawable = R.drawable.lobby;
            rankDrawable = R.drawable.lobby_r;
        }

        // Update display
        llMain.setBackgroundResource(drawable);
        
        // Update rank image
        if (rankImageView != null) {
            rankImageView.setImageResource(rankDrawable);
        }
    }
}
