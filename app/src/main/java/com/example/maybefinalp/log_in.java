package com.example.maybefinalp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class log_in extends AppCompatActivity {

    private EditText etEmailLogin, etUsernameLogin;
    private Button btnLogIn, returnButton;
    private TextView tvLoginMessage;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        etEmailLogin = findViewById(R.id.etEmailLogin);
        etUsernameLogin = findViewById(R.id.etUsernameLogin);
        btnLogIn = findViewById(R.id.btnLogIn);
        returnButton = findViewById(R.id.returnButton);
        tvLoginMessage = findViewById(R.id.tvLoginMessage);

        // Clear any existing message
        tvLoginMessage.setText("");

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        btnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmailLogin.getText().toString().trim();
                String username = etUsernameLogin.getText().toString().trim();

                if (email.isEmpty() || username.isEmpty()) {
                    tvLoginMessage.setText("Please enter all details");
                    return;
                }

                // Get all registered emails and usernames
                Set<String> registeredEmails = sharedPreferences.getStringSet("emails", new HashSet<String>());
                Set<String> registeredUsernames = sharedPreferences.getStringSet("usernames", new HashSet<String>());

                // Check if email exists and username matches
                if (registeredEmails.contains(email)) {
                    String savedUsername = sharedPreferences.getString("username_" + email, "");
                    if (username.equals(savedUsername)) {
                        // Load user's coins
                        int userCoins = sharedPreferences.getInt("coins_" + email, 0);
                        
                        // Save current user's email for future reference
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("currentUserEmail", email);
                        editor.apply();

                        tvLoginMessage.setText("Login successful! Welcome back, " + username + "!");
                        Toast.makeText(log_in.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                        // Return to MainActivity with user's coins
                        Intent intent = new Intent(log_in.this, MainActivity.class);
                        intent.putExtra("coins", userCoins);
                        startActivity(intent);
                        finish();
                    } else {
                        tvLoginMessage.setText("Invalid username for this email.");
                    }
                } else {
                    tvLoginMessage.setText("Email not found. Please sign up first!");
                }
            }
        });

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(log_in.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
