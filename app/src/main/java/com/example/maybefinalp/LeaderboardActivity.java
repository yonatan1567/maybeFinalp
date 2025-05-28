package com.example.maybefinalp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<LeaderboardEntry> entries;

    public LeaderboardAdapter(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);
        holder.rankTextView.setText(String.valueOf(position + 1));
        holder.usernameTextView.setText(entry.getUsername());
        holder.coinsTextView.setText(String.valueOf(entry.getCoins()));

        // Set rank image based on coins
        int rankDrawable = getRankDrawable(entry.getCoins());
        if (rankDrawable != 0) {
            holder.rankImageView.setImageResource(rankDrawable);
            holder.rankImageView.setVisibility(View.VISIBLE);
        } else {
            holder.rankImageView.setVisibility(View.GONE);
        }

        // Set background color for top 3 entries
        if (position == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFD700")); // Gold
        } else if (position == 1) {
            holder.itemView.setBackgroundColor(Color.parseColor("#C0C0C0")); // Silver
        } else if (position == 2) {
            holder.itemView.setBackgroundColor(Color.parseColor("#CD7F32")); // Bronze
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
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
            return R.drawable.lobby_r;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView;
        ImageView rankImageView;
        TextView usernameTextView;
        TextView coinsTextView;

        ViewHolder(View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.rankTextView);
            rankImageView = itemView.findViewById(R.id.rankImageView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            coinsTextView = itemView.findViewById(R.id.coinsTextView);
        }
    }
}

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView leaderboardRecyclerView;
    private Button returnButton;
    private SharedPreferences sharedPreferences;
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Initialize views
        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView);
        returnButton = findViewById(R.id.returnButton);
        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        // Setup RecyclerView with proper configuration
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        leaderboardRecyclerView.setLayoutManager(layoutManager);
        leaderboardRecyclerView.setHasFixedSize(true);
        leaderboardRecyclerView.setNestedScrollingEnabled(true);
        
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
        Collections.sort(entries, (e1, e2) -> Integer.compare(e2.getCoins(), e1.getCoins()));

        // Create and set adapter with sorted entries
        adapter = new LeaderboardAdapter(entries);
        leaderboardRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the leaderboard when returning to this activity
        loadLeaderboard();
    }
} 