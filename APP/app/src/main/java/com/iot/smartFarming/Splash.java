package com.iot.smartFarming;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Splash extends AppCompatActivity {
    ImageView logo;
    TextView upper, bottom;
    Animation topAnim, bottomAnim, textFadeIn;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Edge-to-edge handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupAnimations();
        checkAuthState();
    }

    private void initializeViews() {
        logo = findViewById(R.id.logo_image);
        upper = findViewById(R.id.upper_text);
        bottom = findViewById(R.id.bottom_text);
        auth = FirebaseAuth.getInstance();
    }

    private void setupAnimations() {
        // Bounce effect for logo
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(1000);
        scaleAnimation.setInterpolator(new DecelerateInterpolator());

        // Text fade-in animation
        textFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        textFadeIn.setStartOffset(400);

        // Start animations
        logo.startAnimation(scaleAnimation);
        upper.startAnimation(textFadeIn);
        bottom.startAnimation(textFadeIn);
    }

    private void checkAuthState() {
        new Handler().postDelayed(() -> {
            Class<?> destination = auth.getCurrentUser() != null ?
                    MainActivity.class : Login.class;
            startActivity(new Intent(Splash.this, destination));
            finish();
        }, 2000); // Increased duration for better animation display
    }

    // Optional: Add these to preserve animation smoothness
    @Override
    protected void onPause() {
        super.onPause();
        logo.clearAnimation();
        upper.clearAnimation();
        bottom.clearAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupAnimations();
    }
}