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

        // בדיקה אם השחקן כבר נרשם
        String savedEmail = sharedPreferences.getString("email", null);

        if (savedEmail != null) {
            tvMessage.setText("You are already signed up!");
        }

        // קבלת רשימת המיילים שנרשמו בעבר
        Set<String> registeredEmails = sharedPreferences.getStringSet("emails", new HashSet<String>());
        // קבלת רשימת שמות המשתמשים שנרשמו בעבר
        Set<String> registeredUsernames = sharedPreferences.getStringSet("usernames", new HashSet<String>());

        // כפתור להרשמה
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();

                // בדיקה אם יש שדות ריקים
                if (email.isEmpty() || username.isEmpty() || ageStr.isEmpty()) {
                    tvMessage.setText("Please fill all fields");
                    return;
                }

                // בדיקה אם המייל כבר קיים
                if (registeredEmails.contains(email)) {
                    tvMessage.setText("This email is already registered!");
                    return;
                }

                // בדיקה אם שם המשתמש כבר קיים
                if (registeredUsernames.contains(username)) {
                    tvMessage.setText("This username is already taken!");
                    return;
                }

                // בדיקה אם הגיל תקין
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

                // הוספת המייל לרשימת המיילים הנרשמים
                registeredEmails.add(email);
                // הוספת שם המשתמש לרשימת שמות המשתמשים הנרשמים
                registeredUsernames.add(username);

                // שמירה מחדש של הנתונים ב-SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet("emails", registeredEmails);
                editor.putStringSet("usernames", registeredUsernames);
                editor.putString("username_" + email, username);
                editor.putString("email", email);
                editor.putInt("age_" + email, age);
                editor.putInt("coins_" + email, 1000);
                editor.putBoolean("isSignedUp", true);
                editor.apply();

                tvMessage.setText("Sign up successful! Welcome, " + username + "!");
                Toast.makeText(sign_up.this, "Sign up successful!", Toast.LENGTH_SHORT).show();

                // חזרה למסך הראשי לאחר הרשמה
                setResult(RESULT_OK);
                finish();
            }
        });

        // כפתור חזרה למסך הראשי
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(sign_up.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
