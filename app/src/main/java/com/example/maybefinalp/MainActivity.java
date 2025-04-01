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
        coinCountTextView.setText("Coins: " + coins);

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);
        checkIfSignedUp(); // Check if user already signed up

        // כפתור לשחק במשחק
        Button playGameButton = findViewById(R.id.playGameButton);
        playGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BlackjackActivity.class);
            intent.putExtra("coins", coins);
            startActivityForResult(intent, 1);
        });

        // כפתור להרשמה
        Button signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, sign_up.class);
            startActivityForResult(intent, 2); // Request code 2 for sign-up
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
            // יצירת Intent עבור מעבר לדף Leaderboard
            Intent intent = new Intent(MainActivity.this, Leaderboard.class);
            startActivity(intent);
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            coins = data.getIntExtra("coins", coins);
            coinCountTextView.setText("Coins: " + coins);
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            checkIfSignedUp(); // Recheck if the user signed up
        }
    }

    private void checkIfSignedUp() {
        boolean isSignedUp = sharedPreferences.getBoolean("isSignedUp", false);
        if (isSignedUp) {
            Toast.makeText(this, "You are already signed up!", Toast.LENGTH_LONG).show();
        }
    }

}
