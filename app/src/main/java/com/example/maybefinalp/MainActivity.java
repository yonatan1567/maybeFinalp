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
import android.util.Log;
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

import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "game_reminder";
    private static final int NOTIFICATION_ID = 1;
    private static final String ALARM_ACTION = "com.example.maybefinalp.GAME_REMINDER";
    private static final int ALARM_REQUEST_CODE = 123;
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

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        createNotificationChannel();
        scheduleReminder();

        // Initialize UI components first
        coinCountTextView = findViewById(R.id.coinCount);
        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);
        usernameDisplay = findViewById(R.id.usernameDisplay);
        logoutButton = findViewById(R.id.logoutButton);
        blackjackButton = findViewById(R.id.blackjackButton);
        freeGameButton = findViewById(R.id.freeGameButton);
        noCoinsMessage = findViewById(R.id.noCoinsMessage);
        llMain = findViewById(R.id.llMain);
        rankImageView = findViewById(R.id.rankImageView);

        // Now that all UI elements are initialized, we can reset coins
        resetCoins();

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
            coins = sharedPreferences.getInt("coins_" + currentUserEmail, 0);
            coinCountTextView.setText("Coins: " + coins);
            
            // Update UI elements
            updateUsernameDisplay();
            updateButtonsState();
            updateBackground();
            checkBonuses();
        }
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
        
        try {
            if (requestCode == 1) { // Game result
                if (resultCode == RESULT_OK && data != null) {
                    int updatedCoins = data.getIntExtra("coins", coins);
                    
                    // Only update if we got valid coins
                    if (updatedCoins >= 0) {
                        coins = updatedCoins;
                        coinCountTextView.setText("Coins: " + coins);
                        
                        // Save updated coins for current user
                        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
                        if (currentUserEmail != null) {
                            try {
                                // Update SharedPreferences
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
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error updating coins: " + e.getMessage());
                            }
                        }
                        
                        // Update UI
                        updateButtonsState();
                        updateBackground();
                    }
                }
            } else if (requestCode == 2) { // Login/Signup result
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
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onActivityResult: " + e.getMessage());
            // Try to recover UI state
            try {
                updateUsernameDisplay();
                updateButtonsState();
                updateBackground();
            } catch (Exception ex) {
                Log.e("MainActivity", "Error recovering UI state: " + ex.getMessage());
            }
        }
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                    CharSequence name = "Game Reminder";
                    String description = "Reminds player to return to the game";
                    int importance = NotificationManager.IMPORTANCE_DEFAULT;
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                    channel.setDescription(description);
                    notificationManager.createNotificationChannel(channel);
                    Log.d("NotificationSystem", "Notification channel created successfully");
                } else {
                    Log.d("NotificationSystem", "Notification channel already exists");
                }
            }
        } catch (Exception e) {
            Log.e("NotificationSystem", "Error creating notification channel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void scheduleReminder() {
        try {
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.setAction(ALARM_ACTION);
            
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, intent, 
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT);
            }

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                long triggerTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
                Log.d("NotificationSystem", "Alarm scheduled successfully for: " + new Date(triggerTime));
            } else {
                Log.e("NotificationSystem", "Failed to get AlarmManager service");
            }
        } catch (Exception e) {
            Log.e("NotificationSystem", "Error scheduling alarm: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Log.d("NotificationSystem", "ReminderReceiver received broadcast");
                if (ALARM_ACTION.equals(intent.getAction())) {
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    
                    // Check notification permission
                    boolean canNotify = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        canNotify = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                            == android.content.pm.PackageManager.PERMISSION_GRANTED;
                        Log.d("NotificationSystem", "Notification permission status: " + canNotify);
                    }

                    if (canNotify) {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("Come back to play!")
                            .setContentText("We miss you! Come back and try to win more coins!")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true);

                        try {
                            notificationManager.notify(NOTIFICATION_ID, builder.build());
                            Log.d("NotificationSystem", "Notification sent successfully");
                        } catch (SecurityException e) {
                            Log.e("NotificationSystem", "Security exception while sending notification: " + e.getMessage());
                            e.printStackTrace();
                        }

                        // Schedule next reminder
                        try {
                            Intent newIntent = new Intent(context, ReminderReceiver.class);
                            newIntent.setAction(ALARM_ACTION);
                            
                            PendingIntent newPendingIntent;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                newPendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, 
                                    newIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                            } else {
                                newPendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, 
                                    newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                            }

                            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            if (alarmManager != null) {
                                long nextTriggerTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
                                
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, newPendingIntent);
                                } else {
                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, newPendingIntent);
                                }
                                Log.d("NotificationSystem", "Next alarm scheduled for: " + new Date(nextTriggerTime));
                            }
                        } catch (Exception e) {
                            Log.e("NotificationSystem", "Error scheduling next alarm: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Log.w("NotificationSystem", "Cannot send notification - permission denied");
                    }
                }
            } catch (Exception e) {
                Log.e("NotificationSystem", "Error in ReminderReceiver: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void checkBonuses() {
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail == null) return;

        // Get last login date
        long lastLogin = sharedPreferences.getLong("last_login_" + currentUserEmail, 0);
        long currentTime = System.currentTimeMillis();
        
        // Check if it's a new day
        boolean isNewDay = (currentTime - lastLogin) > 24 * 60 * 60 * 1000;
        
        if (isNewDay) {
            // Update last login time
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("last_login_" + currentUserEmail, currentTime);
            
            // Get consecutive days
            int consecutiveDays = sharedPreferences.getInt("consecutive_days_" + currentUserEmail, 0);
            consecutiveDays++;
            editor.putInt("consecutive_days_" + currentUserEmail, consecutiveDays);
            
            // Calculate bonus coins
            int bonusCoins = 100 * consecutiveDays;
            
            // Get current coins
            int currentCoins = sharedPreferences.getInt("coins_" + currentUserEmail, 0);
            int newCoins = currentCoins + bonusCoins;
            
            // Update coins
            editor.putInt("coins_" + currentUserEmail, newCoins);
            editor.apply();
            
            // Update database
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("coins", newCoins);
            db.update("users", values, "email = ?", new String[]{currentUserEmail});
            db.close();
            
            // Update UI
            coins = newCoins;
            coinCountTextView.setText("Coins: " + coins);
            updateButtonsState();
            updateBackground();
            
            // Show bonus message
            showToast("Daily bonus: " + bonusCoins + " coins! (Day " + consecutiveDays + " of 7)");
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
            int userCoins = sharedPreferences.getInt("coins_" + currentUserEmail, 0);
            
            // Enable Blackjack if user has coins
            blackjackButton.setEnabled(userCoins > 0);
            blackjackButton.setAlpha(userCoins > 0 ? 1.0f : 0.5f);
            
            // Enable free game if user has no coins
            freeGameButton.setEnabled(userCoins == 0);
            freeGameButton.setAlpha(userCoins == 0 ? 1.0f : 0.5f);
            
            // Show/hide no coins message
            noCoinsMessage.setVisibility(userCoins == 0 ? View.VISIBLE : View.GONE);
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

    private void resetCoins() {
        try {
            String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
            if (currentUserEmail != null) {
                // Reset coins in SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("coins_" + currentUserEmail, 1000);
                editor.apply();

                // Reset coins in database
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("coins", 1000);
                db.update("users", values, "email = ?", new String[]{currentUserEmail});
                db.close();

                // Update UI
                coins = 1000;
                if (coinCountTextView != null) {
                    coinCountTextView.setText("Coins: " + coins);
                }
                updateButtonsState();
                updateBackground();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error resetting coins: " + e.getMessage());
            // Try to recover
            try {
                coins = 1000;
                if (coinCountTextView != null) {
                    coinCountTextView.setText("Coins: " + coins);
                }
            } catch (Exception ex) {
                Log.e("MainActivity", "Error in recovery: " + ex.getMessage());
            }
        }
    }
}
