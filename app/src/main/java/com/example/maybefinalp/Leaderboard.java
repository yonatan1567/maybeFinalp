package com.example.maybefinalp;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Leaderboard extends AppCompatActivity {
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<User> userList;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(R.id.leaderboardRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(userList);
        recyclerView.setAdapter(adapter);

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);
        fetchLeaderboard();
    }

    private void fetchLeaderboard() {
        Map<String, ?> allEntries = sharedPreferences.getAll();
        userList.clear();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().contains("name")) { // בודקים אם זה שם של שחקן
                String playerName = entry.getValue().toString();
                int coins = sharedPreferences.getInt("coins_" + playerName, 0); // שליפת המטבעות

                userList.add(new User(playerName, coins));
            }
        }

        // ממיינים את המשתמשים לפי כמות המטבעות (מהגבוה לנמוך)
        Collections.sort(userList, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                return Integer.compare(u2.getCoins(), u1.getCoins());
            }
        });

        adapter.notifyDataSetChanged();
    }
}
