package com.iot.smartFarming;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    private Button login;
    private EditText email, password;
    private TextView signup, forgotpass;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                navigateToMainActivity();
            } else {
                auth.signOut();
            }
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        login = findViewById(R.id.login_button);
        email = findViewById(R.id.login_email);
        password = findViewById(R.id.login_password);
        signup = findViewById(R.id.signup_txt);
        forgotpass = findViewById(R.id.forgotpass_txt);
    }

    private void setupClickListeners() {
        signup.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, SignUP.class));
            finish();
        });

        forgotpass.setOnClickListener(v -> {
            String emailStr = email.getText().toString().trim();
            if (emailStr.isEmpty()) {
                Toast.makeText(this, "Enter email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(emailStr)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        login.setOnClickListener(v -> {
            String emailStr = email.getText().toString().trim().toLowerCase();
            String passwordStr = password.getText().toString().trim();

            if (!validateInputs(emailStr, passwordStr)) return;

            login.setEnabled(false);
            login.setText("Logging in...");

            auth.signInWithEmailAndPassword(emailStr, passwordStr)
                    .addOnCompleteListener(task -> {
                        login.setEnabled(true);
                        login.setText("Log in");

                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                navigateToMainActivity();
                            } else {
                                Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG).show();
                                auth.signOut();
                            }
                        } else {
                            String error = task.getException().getMessage();
                            if (error.contains("invalid login credentials")) {
                                Toast.makeText(this, "Invalid email/password", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;

        if (email.isEmpty()) {
            this.email.setError("Email required");
            valid = false;
        }

        if (password.isEmpty()) {
            this.password.setError("Password required");
            valid = false;
        } else if (password.length() < 8) {
            this.password.setError("Minimum 8 characters");
            valid = false;
        }

        return valid;
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }
}