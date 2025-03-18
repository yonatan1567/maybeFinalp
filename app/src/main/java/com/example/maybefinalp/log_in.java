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

public class log_in extends AppCompatActivity {

    private EditText etEmailLogin, etPlayerNameLogin;
    private Button btnLogIn;
    private TextView tvLoginMessage;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPlayerNameLogin = findViewById(R.id.etPlayerNameLogin);
        btnLogIn = findViewById(R.id.btnLogIn);
        tvLoginMessage = findViewById(R.id.tvLoginMessage);

        sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

        btnLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmailLogin.getText().toString().trim();
                String name = etPlayerNameLogin.getText().toString().trim();

                if (email.isEmpty() || name.isEmpty()) {
                    Toast.makeText(log_in.this, "Please enter all details", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Retrieve saved credentials
                String savedEmail = sharedPreferences.getString("email", null);
                String savedName = sharedPreferences.getString("name", null);

                if (email.equals(savedEmail) && name.equals(savedName)) {
                    tvLoginMessage.setText("Login successful! Welcome back, " + name + "!");
                    Toast.makeText(log_in.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                    // Return to MainActivity
                    Intent intent = new Intent(log_in.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    tvLoginMessage.setText("Log in first! No account found.");
                }
            }
        });
    }
}
