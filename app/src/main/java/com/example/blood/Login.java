package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood.utils.FirebaseHelper;
import com.example.blood.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ViewFlipper;

public class Login extends AppCompatActivity {

    EditText email, password;
    Button login, signup;
    ViewFlipper imageCarousel;
    View dot1, dot2, dot3;
    View[] dots;
    Handler carouselHandler;

    FirebaseAuth firebaseAuth;
    FirebaseHelper firebaseHelper;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        signup = findViewById(R.id.signup);

        // Setup Carousel
        imageCarousel = findViewById(R.id.imageCarousel);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dots = new View[]{dot1, dot2, dot3};
        setupCarouselDots();

        // Initialize Firebase Auth and Helper
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper(this);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        login.setOnClickListener(view -> {
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPassword)) {
                Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            signInUser(userEmail, userPassword);
        });

        signup.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, Signup.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void setupCarouselDots() {
        carouselHandler = new Handler(Looper.getMainLooper());
        carouselHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int current = imageCarousel.getDisplayedChild();
                updateDots(current);
                carouselHandler.postDelayed(this, 500);
            }
        }, 500);
    }

    private void updateDots(int activeIndex) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(i == activeIndex ? 
                R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (carouselHandler != null) carouselHandler.removeCallbacksAndMessages(null);
    }

    private void signInUser(String email, String password) {
        // Show loading
        login.setEnabled(false);
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();

        // Sign in with Firebase Auth
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            
                            // Check if user profile exists in Firebase
                            checkAndCreateUserProfile(userId, email);
                        } else {
                            Toast.makeText(Login.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            login.setEnabled(true);
                        }
                    } else {
                        // Login failed
                        String errorMessage = task.getException() != null ? 
                            task.getException().getMessage() : "Login failed";
                        Toast.makeText(Login.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        login.setEnabled(true);
                    }
                });
    }

    private void checkAndCreateUserProfile(String userId, String email) {
        // Check if user profile exists in Firebase Realtime Database
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User profile exists, get name and navigate to Dashboard
                    UserProfile profile = snapshot.getValue(UserProfile.class);
                    String userName = profile != null && profile.getName() != null ? 
                        profile.getName() : email.split("@")[0];
                    
                    navigateToDashboard(userId, userName);
                } else {
                    // User profile doesn't exist, create a basic one
                    createBasicUserProfile(userId, email);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Login.this, "Error checking profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                login.setEnabled(true);
            }
        });
    }

    private void createBasicUserProfile(String userId, String email) {
        // Create a basic user profile from Firebase Auth data
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            login.setEnabled(true);
            return;
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setEmail(email);
        profile.setName(firebaseUser.getDisplayName() != null ? 
            firebaseUser.getDisplayName() : email.split("@")[0]);
        profile.setPhone(firebaseUser.getPhoneNumber() != null ? 
            firebaseUser.getPhoneNumber() : "");
        profile.setRegistrationDate(System.currentTimeMillis());
        profile.setDonor(true);
        profile.setTotalDonations(0);

        // Save profile to Firebase
        firebaseHelper.saveUserProfile(profile, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess(String message) {
                String userName = profile.getName();
                navigateToDashboard(userId, userName);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(Login.this, "Profile created with basic info. Error: " + error, Toast.LENGTH_SHORT).show();
                // Still navigate to dashboard even if profile save had issues
                String userName = profile.getName();
                navigateToDashboard(userId, userName);
            }
        });
    }

    private void navigateToDashboard(String userId, String userName) {
        Toast.makeText(Login.this, "Login Successful!", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(Login.this, Dashboard.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0); // Seamless transition
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Removed auto-login to prevent automatic navigation
        // Users should explicitly log in through the login screen
    }
}
