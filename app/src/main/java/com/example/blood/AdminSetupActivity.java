package com.example.blood;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blood.firebase.InitialDataLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Admin panel to initialize Firebase data
 * Shows current inventory and provides buttons to load sample data
 */
public class AdminSetupActivity extends AppCompatActivity {

    private TextView inventoryDisplay;
    private Button loadDataBtn, refreshBtn, clearDataBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_setup);

        inventoryDisplay = findViewById(R.id.inventoryDisplay);
        loadDataBtn = findViewById(R.id.loadDataBtn);
        refreshBtn = findViewById(R.id.refreshBtn);
        clearDataBtn = findViewById(R.id.clearDataBtn);

        // Load Data Button - Initialize Firebase with sample inventory
        loadDataBtn.setOnClickListener(v -> {
            loadDataBtn.setEnabled(false);
            inventoryDisplay.setText("Loading sample data into Firebase...");

            InitialDataLoader.initializeInventory();
            Toast.makeText(this, "✓ Inventory data loaded to Firebase!", Toast.LENGTH_LONG).show();

            // Refresh display after 1 second
            refreshInventoryDisplay();
            loadDataBtn.setEnabled(true);
        });

        // Refresh Button - Fetch current data from Firebase
        refreshBtn.setOnClickListener(v -> {
            refreshBtn.setEnabled(false);
            inventoryDisplay.setText("Fetching data from Firebase...");
            refreshInventoryDisplay();
            refreshBtn.setEnabled(true);
        });

        // Clear Data Button - Delete all inventory
        clearDataBtn.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Clear All Data?")
                    .setMessage("This will delete all inventory data from Firebase")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        FirebaseDatabase.getInstance()
                                .getReference("inventory")
                                .removeValue((error, ref) -> {
                                    if (error == null) {
                                        Toast.makeText(AdminSetupActivity.this, "✓ Data cleared!", Toast.LENGTH_SHORT).show();
                                        inventoryDisplay.setText("All data cleared. Click 'Load Data' to reinitialize.");
                                    } else {
                                        Toast.makeText(AdminSetupActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Initial load
        refreshInventoryDisplay();
    }

    private void refreshInventoryDisplay() {
        FirebaseDatabase.getInstance()
                .getReference("inventory")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            inventoryDisplay.setText("❌ No data in Firebase yet\n\nClick 'Load Data' to initialize");
                            return;
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("📊 Current Inventory in Firebase\n");
                        sb.append("════════════════════════════════\n\n");

                        for (DataSnapshot bloodSnapshot : snapshot.getChildren()) {
                            String bloodType = bloodSnapshot.getKey();
                            int units = bloodSnapshot.child("units").getValue(Integer.class) != null ?
                                    bloodSnapshot.child("units").getValue(Integer.class) : 0;
                            String status = bloodSnapshot.child("status").getValue(String.class) != null ?
                                    bloodSnapshot.child("status").getValue(String.class) : "UNKNOWN";

                            String statusEmoji = "AVAILABLE".equals(status) ? "🟢" :
                                    "LOW".equals(status) ? "🟡" : "🔴";

                            sb.append(String.format("%s %s: %d units [%s]\n",
                                    statusEmoji, bloodType, units, status));
                        }

                        sb.append("\n════════════════════════════════\n");
                        sb.append("Total types: " + snapshot.getChildrenCount());

                        inventoryDisplay.setText(sb.toString());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        inventoryDisplay.setText("❌ Error loading data: " + error.getMessage());
                    }
                });
    }
}
