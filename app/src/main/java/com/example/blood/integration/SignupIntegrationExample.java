package com.example.blood.integration;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.blood.R;
import com.example.blood.Dashboard;
import com.example.blood.firebase.FirebaseManager;
import com.example.blood.models.UserProfile;
import com.example.blood.utils.FirebaseHelper;

/**
 * EXAMPLE: How to integrate Firebase in your Signup Activity
 *
 * This shows how to:
 * 1. Create user with Firebase Auth
 * 2. Save user profile to Realtime Database
 * 3. Navigate to Dashboard on success
 */
public class SignupIntegrationExample extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, mobileInput, passwordInput,
                              cityInput, addressInput;
    private AutoCompleteTextView bloodGroupInput;
    private MaterialButton signupButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseHelper firebaseHelper;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup); // Your signup layout

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper(this);
        firebaseManager = new FirebaseManager();

        // Initialize views
        nameInput = findViewById(R.id.name);
        emailInput = findViewById(R.id.email);
        mobileInput = findViewById(R.id.mobile);
        passwordInput = findViewById(R.id.password);
        cityInput = findViewById(R.id.city);
        addressInput = findViewById(R.id.address);
        bloodGroupInput = findViewById(R.id.bloodgroup);
        signupButton = findViewById(R.id.btnSignup);

        // Setup blood group dropdown
        setupBloodGroupDropdown();

        // Sign up button click listener
        signupButton.setOnClickListener(v -> handleSignup());
    }

    /**
     * Setup blood group dropdown with all blood types
     */
    private void setupBloodGroupDropdown() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, bloodTypes);
        bloodGroupInput.setAdapter(adapter);
    }

    /**
     * Handle signup process
     * 1. Get form inputs
     * 2. Create Firebase Auth user
     * 3. Save profile to Realtime Database
     */
    private void handleSignup() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String mobile = mobileInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String bloodType = bloodGroupInput.getText().toString().trim();

        // Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
            mobile.isEmpty() || city.isEmpty() || bloodType.isEmpty()) {
            showToast("Please fill all fields");
            return;
        }

        if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return;
        }

        // Show loading
        signupButton.setEnabled(false);
        signupButton.setText("Creating Account...");

        // Step 1: Create Firebase Auth user
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Step 2: Save profile to Realtime Database
                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                        if (currentUser != null) {
                            saveUserProfile(currentUser.getUid(), name, email, mobile,
                                          bloodType, address, city);
                        }
                    } else {
                        // Auth failed
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        showToast("Signup failed: " + errorMessage);
                        signupButton.setEnabled(true);
                        signupButton.setText("Create Account");
                    }
                });
    }

    /**
     * Save user profile to Firebase Realtime Database
     */
    private void saveUserProfile(String userId, String name, String email, String phone,
                                String bloodType, String address, String city) {

        // Create user profile object
        UserProfile profile = new UserProfile(name, email, phone, bloodType);
        profile.setUserId(userId);
        profile.setAddress(address);
        profile.setCity(city);
        profile.setRegistrationDate(System.currentTimeMillis());
        profile.setDonor(true);

        // Save to Firebase
        firebaseManager.saveUserProfile(userId, profile,
                new FirebaseManager.OnCompleteListener() {
                    @Override
                    public void onSuccess(String message) {
                        showToast("✓ Welcome to BloodLink, " + name + "!");

                        // Navigate to Dashboard
                        Intent intent = new Intent(SignupIntegrationExample.this, Dashboard.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        showToast("Error saving profile: " + error);
                        signupButton.setEnabled(true);
                        signupButton.setText("Create Account");
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
