package com.example.maybefinalp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public class MainActivity extends AppCompatActivity {
    private int coins = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView coinCountTextView = findViewById(R.id.coinCount);
        coinCountTextView.setText("Coins: " + coins);

        Button playGameButton = findViewById(R.id.playGameButton);
        playGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BlackjackActivity.class);
            intent.putExtra("coins", coins);
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            coins = data.getIntExtra("coins", coins);
            TextView coinCountTextView = findViewById(R.id.coinCount);
            coinCountTextView.setText("Coins: " + coins);
        }
    }
}