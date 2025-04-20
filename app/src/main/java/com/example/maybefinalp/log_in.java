package com.example.maybefinalp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class log_in extends AppCompatActivity {
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button returnButton;
    private Button goToSignUpButton;
    private Button forgotPasswordButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        returnButton = findViewById(R.id.returnButton);
        goToSignUpButton = findViewById(R.id.goToSignUpButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(log_in.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get all registered emails
            Set<String> emails = sharedPreferences.getStringSet("emails", new HashSet<>());
            boolean found = false;

            // Check each email's username and password
            for (String email : emails) {
                String storedUsername = sharedPreferences.getString("username_" + email, "");
                String storedPassword = sharedPreferences.getString("password_" + email, "");

                if (username.equals(storedUsername) && password.equals(storedPassword)) {
                    // Login successful
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("currentUserEmail", email);
                    editor.apply();
                    found = true;
                    break;
                }
            }

            if (found) {
                Toast.makeText(log_in.this, "Login successful!", Toast.LENGTH_SHORT).show();
                // Return to MainActivity with success result
                Intent returnIntent = new Intent();
                returnIntent.putExtra("login_success", true);
                setResult(RESULT_OK, returnIntent);
                finish();
            } else {
                Toast.makeText(log_in.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(log_in.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        goToSignUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(log_in.this, sign_up.class);
            startActivity(intent);
            finish();
        });

        forgotPasswordButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            if (username.isEmpty()) {
                Toast.makeText(log_in.this, "Please enter your username first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get all registered emails
            Set<String> emails = sharedPreferences.getStringSet("emails", new HashSet<>());
            String userEmail = null;

            // Find the email associated with the username
            for (String email : emails) {
                String storedUsername = sharedPreferences.getString("username_" + email, "");
                if (username.equals(storedUsername)) {
                    userEmail = email;
                    break;
                }
            }

            if (userEmail != null) {
                // Show the password in a toast (in a real app, you would send an email)
                String password = sharedPreferences.getString("password_" + userEmail, "");
                Toast.makeText(log_in.this, "Your password is: " + password, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(log_in.this, "Username not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
