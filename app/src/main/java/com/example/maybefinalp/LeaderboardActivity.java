package com.example.maybefinalp;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Gravity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class LeaderboardEntry {
    private String username;
    private int coins;

    public LeaderboardEntry(String username, int coins) {
        this.username = username;
        this.coins = coins;
    }

    public String getUsername() {
        return username;
    }

    public int getCoins() {
        return coins;
    }
}

public class LeaderboardActivity extends AppCompatActivity {
    private LinearLayout leaderboardLayout;
    private Button returnButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Initialize views
        leaderboardLayout = findViewById(R.id.leaderboardLayout);
        returnButton = findViewById(R.id.returnButton);
        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        // Load and display leaderboard
        loadLeaderboard();

        // Setup return button
        returnButton.setOnClickListener(v -> finish());
    }

    private void loadLeaderboard() {
        // Get all registered emails
        Set<String> registeredEmails = sharedPreferences.getStringSet("emails", new HashSet<>());
        List<LeaderboardEntry> entries = new ArrayList<>();

        // Create LeaderboardEntry objects for each registered email
        for (String email : registeredEmails) {
            String username = sharedPreferences.getString("username_" + email, "");
            // Get the pre-bet coins amount if it exists, otherwise use current coins
            int preBetCoins = sharedPreferences.getInt("pre_bet_coins_" + email, -1);
            int coins = (preBetCoins != -1) ? preBetCoins : sharedPreferences.getInt("coins_" + email, 0);
            entries.add(new LeaderboardEntry(username, coins));
        }

        // Sort entries by coins in descending order
        Collections.sort(entries, new Comparator<LeaderboardEntry>() {
            @Override
            public int compare(LeaderboardEntry e1, LeaderboardEntry e2) {
                return Integer.compare(e2.getCoins(), e1.getCoins());
            }
        });

        // Display the leaderboard
        displayLeaderboard(entries);
    }

    private int getRankDrawable(int coins) {
        if (coins > 50000) {
            return R.drawable.dimond_r;
        }
        else if (coins > 30000) {
            return R.drawable.roby_r;
        }
        else if (coins > 20000) {
            return R.drawable.gold_r;
        }
        else if (coins > 10000) {
            return R.drawable.silver_r;
        }
        else if (coins > 1050) {
            return R.drawable.bronze_r;
        }
        else {
            return R.drawable.lobby_r; // Show lobby_r for players below bronze level
        }
    }

    private void displayLeaderboard(List<LeaderboardEntry> entries) {
        leaderboardLayout.removeAllViews();

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            View entryView = getLayoutInflater().inflate(R.layout.leaderboard_entry, leaderboardLayout, false);

            TextView rankTextView = entryView.findViewById(R.id.rankTextView);
            ImageView rankImageView = entryView.findViewById(R.id.rankImageView);
            TextView usernameTextView = entryView.findViewById(R.id.usernameTextView);
            TextView coinsTextView = entryView.findViewById(R.id.coinsTextView);

            rankTextView.setText(String.valueOf(i + 1));
            usernameTextView.setText(entry.getUsername());
            coinsTextView.setText(String.valueOf(entry.getCoins()));

            // Set rank image based on coins
            int rankDrawable = getRankDrawable(entry.getCoins());
            if (rankDrawable != 0) {
                rankImageView.setImageResource(rankDrawable);
                rankImageView.setVisibility(View.VISIBLE);
            } else {
                rankImageView.setVisibility(View.GONE);
            }

            // Set background color for top 3 entries
            if (i == 0) {
                entryView.setBackgroundColor(Color.parseColor("#FFD700")); // Gold
            } else if (i == 1) {
                entryView.setBackgroundColor(Color.parseColor("#C0C0C0")); // Silver
            } else if (i == 2) {
                entryView.setBackgroundColor(Color.parseColor("#CD7F32")); // Bronze
            }

            leaderboardLayout.addView(entryView);
        }
    }
} 