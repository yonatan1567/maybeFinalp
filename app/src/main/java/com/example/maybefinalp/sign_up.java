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

public class sign_up extends AppCompatActivity {

    private EditText etEmail, etUsername, etAge;
    private Button btnSignUp, returnButton;
    private TextView tvMessage;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etAge = findViewById(R.id.etAge);
        btnSignUp = findViewById(R.id.btnSignUp);
        returnButton = findViewById(R.id.returnButton);
        tvMessage = findViewById(R.id.tvMessage);

        // Clear any existing message
        tvMessage.setText("");

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        // Get registered emails and usernames
        Set<String> registeredEmails = sharedPreferences.getStringSet("emails", new HashSet<String>());
        Set<String> registeredUsernames = sharedPreferences.getStringSet("usernames", new HashSet<String>());

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();

                if (email.isEmpty() || username.isEmpty() || ageStr.isEmpty()) {
                    tvMessage.setText("Please fill in all fields");
                    return;
                }

                if (registeredEmails.contains(email)) {
                    tvMessage.setText("This email is already registered!");
                    return;
                }

                if (registeredUsernames.contains(username)) {
                    tvMessage.setText("This username is already taken!");
                    return;
                }

                // Age validation
                int age;
                try {
                    age = Integer.parseInt(ageStr);
                    if (age < 18) {
                        tvMessage.setText("You must be 18 or older to sign up.");
                        return;
                    } else if (age > 100) {
                        tvMessage.setText("Please set your real age!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    tvMessage.setText("Please enter a valid age");
                    return;
                }

                // Add new user data
                registeredEmails.add(email);
                registeredUsernames.add(username);

                // Save all data
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet("emails", registeredEmails);
                editor.putStringSet("usernames", registeredUsernames);
                editor.putString("username_" + email, username);
                editor.putInt("age_" + email, age);
                editor.putInt("coins_" + email, 1000);
                editor.putString("currentUserEmail", email); // Auto-login after signup
                editor.apply();

                Toast.makeText(sign_up.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
