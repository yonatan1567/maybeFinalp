package com.example.maybefinalp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

        updateUsernameDisplay();

        // Initialize coins based on logged-in user
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            coins = sharedPreferences.getInt("coins_" + currentUserEmail, 1000);
        } else {
            coins = 1000;
        }
        coinCountTextView.setText("Coins: " + coins);

        // Setup logout button
        logoutButton.setOnClickListener(v -> {
            // Clear login state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("currentUserEmail");
            editor.apply();

            // Reset coins to default
            coins = 1000;
            coinCountTextView.setText("Coins: " + coins);

            // Update UI
            updateUsernameDisplay();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        });

        // כפתור לשחק במשחק
        blackjackButton.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                String email = getStoredEmail();
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                int coins = dbHelper.getUserCoins(email);
                
                if (coins > 0) {
                    startActivity(new Intent(MainActivity.this, BlackjackActivity.class));
                } else {
                    showToast("אין לך מספיק מטבעות! נסה את המשחק החינמי");
                    noCoinsMessage.setVisibility(View.VISIBLE);
                }
            } else {
                showToast("Please log in first");
            }
        });

        freeGameButton.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                startActivity(new Intent(MainActivity.this, FreeGameActivity.class));
            } else {
                showToast("Please log in first");
            }
        });

        // כפתור להרשמה
        Button signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, sign_up.class);
            startActivityForResult(intent, 2);
        });

        // כפתור לכניסה
        Button logInButton = findViewById(R.id.logInButton);
        logInButton.setOnClickListener(v1 -> {
            Intent intent1 = new Intent(MainActivity.this, log_in.class);
            startActivity(intent1);
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
        checkBonuses();
        updateButtonsState();
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
        if (requestCode == 1 && resultCode == RESULT_OK) {
            coins = data.getIntExtra("coins", coins);
            coinCountTextView.setText("Coins: " + coins);
            
            // Save updated coins for current user
            String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
            if (currentUserEmail != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("coins_" + currentUserEmail, coins);
                editor.apply();
            }
        }
        // Update username display after returning from signup/login
        updateUsernameDisplay();
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
        String email = getStoredEmail();
        if (email != null && !email.isEmpty()) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            
            // Check 4-hour bonus
            if (dbHelper.checkAndGiveBonus(email)) {
                showToast("You received 100 coins bonus!");
            }

            // Check consecutive days
            int consecutiveDays = dbHelper.checkAndUpdateConsecutiveDays(email);
            if (consecutiveDays > 0) {
                showToast("Day " + consecutiveDays + " of 7 - Keep logging in daily!");
            }

            // Check comeback bonus
            if (dbHelper.checkAndGiveComebackBonus(email)) {
                showToast("Welcome back! You received 200 coins comeback bonus!");
            }

            // Check VIP bonus for players who had high amounts before
            if (dbHelper.checkAndGiveVIPBonus(email)) {
                showToast("VIP Bonus: You received 200 coins to continue playing!");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String getStoredEmail() {
        SharedPreferences prefs = getSharedPreferences("BlackjackPrefs", MODE_PRIVATE);
        return prefs.getString("email", "");
    }

    private void updateButtonsState() {
        if (isUserLoggedIn()) {
            String email = getStoredEmail();
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            int coins = dbHelper.getUserCoins(email);
            
            if (coins == 0) {
                blackjackButton.setEnabled(false);
                blackjackButton.setAlpha(0.5f);
                noCoinsMessage.setVisibility(View.VISIBLE);
            } else {
                blackjackButton.setEnabled(true);
                blackjackButton.setAlpha(1.0f);
                noCoinsMessage.setVisibility(View.GONE);
            }
        }
    }
}
