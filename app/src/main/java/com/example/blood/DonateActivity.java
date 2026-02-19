package com.example.blood;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;

public class DonateActivity extends AppCompatActivity {
    
    private static final String TAG = "DonateActivity";

    private FirebaseHelper firebaseHelper;
    private AutoCompleteTextView bloodTypeDropdown;
    private EditText unitsInput, centerNameInput;
    private Button donateBtn;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate); // You'll need to create this layout

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Get userId from intent
        userId = getIntent().getStringExtra("userId");

        // Initialize views
        bloodTypeDropdown = findViewById(R.id.bloodTypeDropdown);
        unitsInput = findViewById(R.id.unitsInput);
        centerNameInput = findViewById(R.id.centerNameInput);
        donateBtn = findViewById(R.id.donateBtn);

        // Setup blood type dropdown
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                bloodTypes
        );
        bloodTypeDropdown.setAdapter(adapter);

        // Donate button click listener
        donateBtn.setOnClickListener(v -> recordDonation());
    }

    private void recordDonation() {
        // CHECK AUTH STATUS
        String userId = FirebaseAuth.getInstance().getUid();
        Log.d(TAG, "=== DONATE ATTEMPT ===");
        Log.d(TAG, "Current User ID: " + userId);
        Log.d(TAG, "User Authenticated: " + (FirebaseAuth.getInstance().getCurrentUser() != null));
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "❌ NOT LOGGED IN! Please login first.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "USER NOT AUTHENTICATED - CANNOT DONATE");
            finish();
            return;
        }
        
        String selectedBloodType = bloodTypeDropdown.getText().toString().trim();
        String unitsStr = unitsInput.getText().toString().trim();
        String centerName = centerNameInput.getText().toString().trim();

        // Validation
        if (selectedBloodType.isEmpty()) {
            Toast.makeText(this, "Please select blood type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (unitsStr.isEmpty()) {
            Toast.makeText(this, "Please enter units", Toast.LENGTH_SHORT).show();
            return;
        }

        if (centerName.isEmpty()) {
            Toast.makeText(this, "Please enter donation center", Toast.LENGTH_SHORT).show();
            return;
        }

        int units = Integer.parseInt(unitsStr);

        // Show loading state
        donateBtn.setEnabled(false);
        donateBtn.setText("Recording...");
        
        Log.d(TAG, "Submitting donation: " + selectedBloodType + " (" + units + " units) at " + centerName);

        // This automatically updates inventory with callback!
        firebaseHelper.recordDonation(selectedBloodType, units, centerName, 
            new FirebaseHelper.OnCompleteListener() {
                @Override
                public void onSuccess(String message) {
                    Log.d(TAG, "✓ Donation recorded successfully: " + message);
                    Toast.makeText(DonateActivity.this, "✓ Donation recorded! Thank you!", Toast.LENGTH_LONG).show();
                    // Clear fields
                    bloodTypeDropdown.setText("");
                    unitsInput.setText("");
                    centerNameInput.setText("");
                    // Go back to Dashboard
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "✗ Error recording donation: " + error);
                    Toast.makeText(DonateActivity.this, "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    donateBtn.setEnabled(true);
                    donateBtn.setText("Submit Donation");
                }
            });
    }
}
