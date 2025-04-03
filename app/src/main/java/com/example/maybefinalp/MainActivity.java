package com.example.maybefinalp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private int coins = 1000;
    private TextView coinCountTextView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coinCountTextView = findViewById(R.id.coinCount);
        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        // Clear any existing login state
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("currentUserEmail");
        editor.apply();

        // Reset coins to default value since no user is logged in
        coins = 1000;
        coinCountTextView.setText("Coins: " + coins);

        // כפתור לשחק במשחק
        Button playGameButton = findViewById(R.id.playGameButton);
        playGameButton.setOnClickListener(v -> {
            // Check if user is logged in
            String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
            if (currentUserEmail == null) {
                Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, BlackjackActivity.class);
            intent.putExtra("coins", coins);
            startActivityForResult(intent, 1);
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
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            checkIfSignedUp();
        }
    }

    private void checkIfSignedUp() {
        boolean isSignedUp = sharedPreferences.getBoolean("isSignedUp", false);
        if (isSignedUp) {
            Toast.makeText(this, "You are already signed up!", Toast.LENGTH_LONG).show();
        }
    }
}
