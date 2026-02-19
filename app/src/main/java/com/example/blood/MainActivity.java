package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    Button getstarted;
    FirebaseAuth firebaseAuth;

    // Animated views
    ImageView bgImage;
    TextView heartIcon, appTitle, tagline, bottomMessage;
    LinearLayout featuresSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        getstarted = findViewById(R.id.getstarted);

        // Find animated views
        bgImage = findViewById(R.id.bgImage);
        heartIcon = findViewById(R.id.heartIcon);
        appTitle = findViewById(R.id.appTitle);
        tagline = findViewById(R.id.tagline);
        featuresSection = findViewById(R.id.featuresSection);
        bottomMessage = findViewById(R.id.bottomMessage);

        // Start animations
        playEntryAnimations();

        // Check if user is already logged in
        checkLoginStatus();
    }

    private void playEntryAnimations() {
        // Background fade in
        Animation bgFade = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow);
        bgImage.startAnimation(bgFade);

        // Heart pulse animation (continuous)
        Animation heartbeat = AnimationUtils.loadAnimation(this, R.anim.heartbeat_pulse);
        heartIcon.startAnimation(heartbeat);

        // Staggered slide-up animations
        Handler handler = new Handler(Looper.getMainLooper());

        // Title slides up after 300ms
        handler.postDelayed(() -> {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up);
            appTitle.setVisibility(View.VISIBLE);
            appTitle.startAnimation(slideUp);
        }, 300);

        // Tagline slides up after 600ms
        handler.postDelayed(() -> {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up);
            tagline.setVisibility(View.VISIBLE);
            tagline.startAnimation(slideUp);
        }, 600);

        // Features cards appear after 900ms
        handler.postDelayed(() -> {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up);
            featuresSection.setVisibility(View.VISIBLE);
            featuresSection.startAnimation(slideUp);
        }, 900);

        // Get Started button after 1200ms
        handler.postDelayed(() -> {
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up);
            getstarted.setVisibility(View.VISIBLE);
            getstarted.startAnimation(slideUp);
        }, 1200);

        // Bottom message fades in after 1500ms
        handler.postDelayed(() -> {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_slow);
            bottomMessage.setVisibility(View.VISIBLE);
            bottomMessage.startAnimation(fadeIn);
        }, 1500);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check login status again when activity resumes
        checkLoginStatus();
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // User is logged in, go directly to Dashboard
            Intent intent = new Intent(MainActivity.this, Dashboard.class);
            intent.putExtra("userId", currentUser.getUid());
            // Get user name from Firebase
            com.google.firebase.database.DatabaseReference usersRef = 
                FirebaseDatabase.getInstance().getReference("users");
            usersRef.child(currentUser.getUid()).child("name")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        String userName = snapshot.getValue(String.class);
                        if (userName != null && !userName.isEmpty()) {
                            intent.putExtra("userName", userName);
                        } else if (currentUser.getEmail() != null) {
                            intent.putExtra("userName", currentUser.getEmail().split("@")[0]);
                        }
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        if (currentUser.getEmail() != null) {
                            intent.putExtra("userName", currentUser.getEmail().split("@")[0]);
                        }
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                });
        }
    }

    public void getstarted(View view) {
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}