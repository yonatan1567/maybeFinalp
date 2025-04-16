package com.example.maybefinalp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FreeGameActivity extends AppCompatActivity {
    private TextView coinCountTextView;
    private int coins = 0;
    private Button playButton;
    private Button returnButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_game);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        // Initialize views
        coinCountTextView = findViewById(R.id.coinCount);
        playButton = findViewById(R.id.playButton);
        returnButton = findViewById(R.id.returnButton);

        // Get current user's email
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            // Get current coins
            coins = sharedPreferences.getInt("coins_" + currentUserEmail, 0);
            coinCountTextView.setText("Coins: " + coins);
        }

        // Setup play button
        playButton.setOnClickListener(v -> {
            // Generate random coins between 1 and 100
            int wonCoins = (int) (Math.random() * 100) + 1;
            
            // Update coins
            coins += wonCoins;
            
            // Update SharedPreferences
            if (currentUserEmail != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("coins_" + currentUserEmail, coins);
                editor.apply();
            }
            
            // Update UI
            coinCountTextView.setText("Coins: " + coins);
            Toast.makeText(FreeGameActivity.this, "You won " + wonCoins + " coins!", Toast.LENGTH_SHORT).show();
        });

        // Setup return button
        returnButton.setOnClickListener(v -> {
            // Return to MainActivity with updated coins
            Intent returnIntent = new Intent();
            returnIntent.putExtra("coins", coins);
            setResult(RESULT_OK, returnIntent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Return to MainActivity with updated coins
        Intent returnIntent = new Intent();
        returnIntent.putExtra("coins", coins);
        setResult(RESULT_OK, returnIntent);
        finish();
    }
} 