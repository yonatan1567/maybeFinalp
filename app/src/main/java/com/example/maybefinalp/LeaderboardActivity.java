package com.example.maybefinalp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<User> users = new ArrayList<>();

        // Create User objects for each registered email
        for (String email : registeredEmails) {
            String username = sharedPreferences.getString("username_" + email, "");
            int coins = sharedPreferences.getInt("coins_" + email, 0);
            users.add(new User(username, coins));
        }

        // Sort users by coins in descending order
        Collections.sort(users, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                return Integer.compare(u2.getCoins(), u1.getCoins());
            }
        });

        // Display top 10 users
        int count = Math.min(users.size(), 10);
        for (int i = 0; i < count; i++) {
            User user = users.get(i);
            TextView userView = new TextView(this);
            userView.setText((i + 1) + ". " + user.getName() + " - " + user.getCoins() + " coins");
            userView.setTextSize(18);
            userView.setTextColor(getResources().getColor(android.R.color.white));
            userView.setPadding(16, 8, 16, 8);
            leaderboardLayout.addView(userView);
        }
    }
} 