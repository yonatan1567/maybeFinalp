package com.example.maybefinalp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class sign_up extends AppCompatActivity {

    private EditText ageEditText;
    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signUpButton;
    private Button goToLoginButton;
    private Button returnButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        ageEditText = findViewById(R.id.ageEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        goToLoginButton = findViewById(R.id.goToLoginButton);
        returnButton = findViewById(R.id.returnButton);

        signUpButton.setOnClickListener(v -> {
            String ageStr = ageEditText.getText().toString();
            String username = usernameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (ageStr.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(sign_up.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int age = Integer.parseInt(ageStr);
            if (age < 18) {
                Toast.makeText(sign_up.this, "You must be at least 18 years old to play", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if email already exists
            Set<String> emails = sharedPreferences.getStringSet("emails", new HashSet<>());
            if (emails.contains(email)) {
                Toast.makeText(sign_up.this, "Email already registered", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save user data
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username_" + email, username);
            editor.putInt("age_" + email, age);
            editor.putString("password_" + email, password);
            editor.putInt("coins_" + email, 1000); // Starting coins
            editor.putString("currentUserEmail", email); // Set as logged in

            // Add email to set of registered emails
            Set<String> updatedEmails = new HashSet<>(emails);
            updatedEmails.add(email);
            editor.putStringSet("emails", updatedEmails);
            editor.apply();

            // Save to database
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("email", email);
            values.put("username", username);
            values.put("age", age);
            values.put("password", password);
            values.put("coins", 1000);
            db.insert("users", null, values);
            db.close();

            Toast.makeText(sign_up.this, "Registration successful! You are now logged in.", Toast.LENGTH_SHORT).show();
            finish();
        });

        goToLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(sign_up.this, log_in.class);
            startActivity(intent);
            finish();
        });

        returnButton.setOnClickListener(v -> {
            finish();
        });
    }
}
