package com.example.blood;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.blood.utils.FirebaseHelper;
import com.example.blood.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.FirebaseException;
import androidx.annotation.NonNull;
import java.util.concurrent.TimeUnit;

public class Signup extends AppCompatActivity {

    EditText name, mobile, email, password, city, address, otpInput;
    com.google.android.material.textfield.MaterialAutoCompleteTextView bloodgroup;
    Button btnSignup, btnPickLocation, btnSendOTP, btnVerifyOTP;
    com.google.android.material.textfield.TextInputLayout otpLayout;
    double selectedLat = 0, selectedLon = 0;
    
    // Phone Auth
    private String verificationId;
    private boolean isPhoneVerified = false;
    private com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken resendToken;

    FirebaseAuth firebaseAuth;
    FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        // Connect Views
        name = findViewById(R.id.name);
        mobile = findViewById(R.id.mobile);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        city = findViewById(R.id.city);
        address = findViewById(R.id.address);
        bloodgroup = findViewById(R.id.bloodgroup);
        btnSignup = findViewById(R.id.btnSignup);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP);
        otpLayout = findViewById(R.id.otpLayout);
        otpInput = findViewById(R.id.otp);

        // Setup AutoCompleteTextView Data
        String[] groups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                groups
        );

        bloodgroup.setAdapter(adapter);
        bloodgroup.setOnClickListener(v -> ((com.google.android.material.textfield.MaterialAutoCompleteTextView)v).showDropDown());

        // Initialize Firebase Auth and Helper
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper(this);

        btnSendOTP.setOnClickListener(v -> {
            String m = mobile.getText().toString().trim();
            if (m.isEmpty()) {
                Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!m.startsWith("+")) {
                m = "+91" + m; // Default to India if no code
                mobile.setText(m);
            }
            sendVerificationCode(m);
        });

        btnVerifyOTP.setOnClickListener(v -> {
            String code = otpInput.getText().toString().trim();
            if (code.length() < 6) {
                Toast.makeText(this, "Enter 6-digit code", Toast.LENGTH_SHORT).show();
                return;
            }
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneCredential(credential);
        });

        // Button Click
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String n = name.getText().toString().trim();
                String m = mobile.getText().toString().trim();
                String e = email.getText().toString().trim();
                String p = password.getText().toString().trim();
                String c = city.getText().toString().trim();
                String a = address.getText().toString().trim();
                String b = bloodgroup.getText().toString().trim();

                if (n.isEmpty() || m.isEmpty() || e.isEmpty() || p.isEmpty()
                        || c.isEmpty() || a.isEmpty() || b.isEmpty()) {
                    Toast.makeText(Signup.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!m.startsWith("+")) {
                    Toast.makeText(Signup.this, "Phone must start with country code (e.g., +91)", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isPhoneVerified) {
                    Toast.makeText(Signup.this, "Please verify your mobile number first", Toast.LENGTH_SHORT).show();
                } else if (p.length() < 6) {
                    Toast.makeText(Signup.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else {
                    registerUser(e, p, n, m, c, a, b);
                }
            }
        });

        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(Signup.this, LocationPickerActivity.class);
            startActivityForResult(intent, 200);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("lat", 0);
            selectedLon = data.getDoubleExtra("lon", 0);
            String fullAddress = data.getStringExtra("address");
            String cityName = data.getStringExtra("city");

            if (fullAddress != null && !fullAddress.isEmpty()) address.setText(fullAddress);
            if (cityName != null && !cityName.isEmpty()) city.setText(cityName);
            
            Toast.makeText(this, "Location set from map!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendVerificationCode(String phoneNumber) {
        btnSendOTP.setEnabled(false);
        Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show();
        
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                String code = credential.getSmsCode();
                                if (code != null) otpInput.setText(code);
                                signInWithPhoneCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                btnSendOTP.setEnabled(true);
                                Log.e("PhoneAuth", "Error: " + e.getMessage(), e);
                                if (e.getMessage().contains("App Check")) {
                                    Toast.makeText(Signup.this, "Security check failed. Please test on a real device.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(Signup.this, "OTP failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onCodeSent(@NonNull String vid,
                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                verificationId = vid;
                                resendToken = token;
                                otpLayout.setVisibility(View.VISIBLE);
                                btnVerifyOTP.setVisibility(View.VISIBLE);
                                btnSendOTP.setText("Resend");
                                btnSendOTP.setEnabled(true);
                                Toast.makeText(Signup.this, "OTP Sent! Please enter it below.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCodeAndRegister(String code, String email, String password, String name, 
                                     String mobile, String city, String address, String bloodType) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneCredential(credential, email, password, name, mobile, city, address, bloodType);
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        signInWithPhoneCredential(credential, null, null, null, null, null, null, null);
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential, String email, String password, 
                                         String name, String mobile, String city, String address, String bloodType) {
        Toast.makeText(this, "Verifying OTP...", Toast.LENGTH_SHORT).show();
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    isPhoneVerified = true;
                    btnSendOTP.setText("Verified ✓");
                    btnSendOTP.setEnabled(false);
                    btnVerifyOTP.setVisibility(View.GONE);
                    otpLayout.setVisibility(View.GONE);
                    
                    if (email != null) {
                        Toast.makeText(Signup.this, "Phone verified! Creating account...", Toast.LENGTH_SHORT).show();
                        registerUser(email, password, name, mobile, city, address, bloodType);
                    } else {
                        Toast.makeText(Signup.this, "Phone verified successfully!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Signup.this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void registerUser(String email, String password, String name, String mobile,
                             String city, String address, String bloodType) {
        // Create Firebase Auth user
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = firebaseAuth.getCurrentUser().getUid();

                        // Create UserProfile object
                        UserProfile profile = new UserProfile();
                        profile.setUserId(userId);
                        profile.setName(name);
                        profile.setEmail(email);
                        profile.setPhone(mobile);
                        profile.setBloodType(bloodType);
                        profile.setAddress(address);
                        profile.setCity(city);
                        profile.setLatitude(selectedLat);
                        profile.setLongitude(selectedLon);
                        profile.setRegistrationDate(System.currentTimeMillis());
                        profile.setDonor(true);
                        profile.setTotalDonations(0);

                        // Save profile to Firebase
                        firebaseHelper.saveUserProfile(profile, new FirebaseHelper.OnCompleteListener() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(Signup.this, "Registered Successfully!", Toast.LENGTH_SHORT).show();

                                // Clear fields
                                Signup.this.name.setText("");
                                Signup.this.mobile.setText("");
                                Signup.this.email.setText("");
                                Signup.this.password.setText("");
                                Signup.this.city.setText("");
                                Signup.this.address.setText("");
                                bloodgroup.setText("");

                                // Navigate to Login
                                Intent intent = new Intent(Signup.this, Login.class);
                                startActivity(intent);
                                overridePendingTransition(0, 0); // Seamless transition
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(Signup.this, "Error saving profile: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        // Handle auth errors
                        Exception exception = task.getException();
                        if (exception != null) {
                            String errorMessage = exception.getMessage();
                            
                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(Signup.this, "Email already registered", Toast.LENGTH_SHORT).show();
                            } else if (errorMessage != null && errorMessage.contains("sign-in provider is disabled")) {
                                Toast.makeText(Signup.this, "Email/Password authentication is disabled. Please enable it in Firebase Console.", Toast.LENGTH_LONG).show();
                            } else if (errorMessage != null && errorMessage.contains("network")) {
                                Toast.makeText(Signup.this, "Network error. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                            } else if (errorMessage != null && errorMessage.contains("invalid-email")) {
                                Toast.makeText(Signup.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                            } else if (errorMessage != null && errorMessage.contains("weak-password")) {
                                Toast.makeText(Signup.this, "Password is too weak", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Signup.this, "Signup failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(Signup.this, "Signup failed. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
