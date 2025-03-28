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

public class sign_up extends AppCompatActivity {

    private EditText etEmail, etPlayerName, etAge;
    private Button btnSignUp;
    private TextView tvMessage;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etEmail = findViewById(R.id.etEmail);
        etPlayerName = findViewById(R.id.etPlayerName);
        etAge = findViewById(R.id.etAge);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvMessage = findViewById(R.id.tvMessage);

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        // Check if the user is already signed up
        String registeredEmail = sharedPreferences.getString("email", null);

        // אם המייל כבר קיים, זה אומר שהמשתמש כבר נרשם
        if (registeredEmail != null) {
            // אם המייל כבר רשום, תציג הודעה שהמשתמש כבר רשום
            Toast.makeText(sign_up.this, "You are already signed up!", Toast.LENGTH_LONG).show();
            finish(); // Close sign-up screen if already signed up
            return;
        }

        // כפתור להרשמה
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String name = etPlayerName.getText().toString().trim();
                String ageStr = etAge.getText().toString().trim();

                // בדיקה אם יש שדות ריקים
                if (email.isEmpty() || name.isEmpty() || ageStr.isEmpty()) {
                    Toast.makeText(sign_up.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // בדיקה אם הגיל תקין
                int age = Integer.parseInt(ageStr);
                if (age < 18) {
                    tvMessage.setText("You must be 18 or older to sign up.");
                    return;
                } else if (age > 100) {
                    tvMessage.setText("Please set your real age!");
                    return;
                }

                // Save user data to SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("email", email);  // Save the email so we know it's a registered user
                editor.putString("name", name);  // Save the player name
                editor.putInt("age", age);  // Save the age
                editor.putInt("coins", 0);  // Initialize the coins count (or any other data)
                editor.apply();

                tvMessage.setText("Player " + name + " signed up successfully!");
                Toast.makeText(sign_up.this, "Sign up successful!", Toast.LENGTH_SHORT).show();

                // חזרה למסך הראשי לאחר הרשמה
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}
