package com.example.blood.integration;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blood.utils.FirebaseHelper;
import com.google.android.material.button.MaterialButton;

/**
 * EXAMPLE: How to record blood donation
 */
public class DonateBloodIntegrationExample extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private String selectedBloodType = "A+";
    private MaterialButton submitButton;
}