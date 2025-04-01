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

    private EditText etEmail, etPlayerName, etAge;
    private Button btnSignUp, returnButton;
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
        returnButton = findViewById(R.id.returnButton);  // כפתור חזרה
        tvMessage = findViewById(R.id.tvMessage);

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        // בדיקה אם השחקן כבר נרשם
        String savedEmail = sharedPreferences.getString("email", null);
        String savedName = sharedPreferences.getString("name", null);

        if (savedEmail != null && savedName != null) {
            tvMessage.setText("You are already signed up!");
        }

        // קבלת רשימת המיילים שנרשמו בעבר
        Set<String> registeredEmails = sharedPreferences.getStringSet("emails", new HashSet<String>());

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

                // בדיקה אם המייל כבר קיים
                if (registeredEmails.contains(email)) {
                    Toast.makeText(sign_up.this, "This email is already registered!", Toast.LENGTH_SHORT).show();
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

                // הוספת המייל לרשימת המיילים הנרשמים
                registeredEmails.add(email);

                // שמירה מחדש של רשימת המיילים ב-SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet("emails", registeredEmails);  // עדכון הרשימה
                editor.putString("name", name);  // שמירה של שם השחקן
                editor.putString("email", email);  // שמירה של מייל
                editor.putInt("age", age);  // שמירה של גיל
                editor.putInt("coins", 0);  // אתחול מספר המטבעות (או כל מידע אחר)
                editor.apply();

                tvMessage.setText("Player " + name + " signed up successfully!");
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
                // מעבר ל- MainActivity
                Intent intent = new Intent(sign_up.this, MainActivity.class);
                startActivity(intent);
                finish(); // לסיים את הפעילות הנוכחית כדי לא להותיר אותה בהיסטוריית הפעילויות
            }
        });
    }
}
