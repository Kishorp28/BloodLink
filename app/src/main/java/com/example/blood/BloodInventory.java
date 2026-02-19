package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BloodInventory extends AppCompatActivity {

    LinearLayout inventoryContainer;
    TextView tvTotalUnits;
    BottomNavigationView bottomNav;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blood_inventory);

        inventoryContainer = findViewById(R.id.inventoryContainer);
        tvTotalUnits = findViewById(R.id.tvTotalUnits);
        bottomNav = findViewById(R.id.bottomNav);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        loadInventory();
        setupBottomNavigation();
    }

    private void loadInventory() {
        databaseReference.child("BloodAvailability")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        inventoryContainer.removeAllViews();
                        int totalUnits = 0;

                        for (DataSnapshot bloodSnap : snapshot.getChildren()) {
                            String group = bloodSnap.getKey();
                            String units = bloodSnap.getValue(String.class);

                            // Create inventory card
                            com.google.android.material.card.MaterialCardView card = 
                                new com.google.android.material.card.MaterialCardView(BloodInventory.this);
                            card.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            card.setCardBackgroundColor(android.graphics.Color.WHITE);
                            card.setRadius(12);
                            card.setCardElevation(4);

                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
                            params.setMargins(0, 0, 0, 12);
                            card.setLayoutParams(params);

                            LinearLayout layout = new LinearLayout(BloodInventory.this);
                            layout.setOrientation(LinearLayout.HORIZONTAL);
                            layout.setPadding(16, 16, 16, 16);
                            layout.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ));

                            // Blood group
                            TextView groupText = new TextView(BloodInventory.this);
                            groupText.setText(group);
                            groupText.setTextSize(18);
                            groupText.setTypeface(groupText.getTypeface(), android.graphics.Typeface.BOLD);
                            groupText.setTextColor(android.graphics.Color.BLACK);
                            groupText.setLayoutParams(new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                            ));

                            // Units
                            TextView unitsText = new TextView(BloodInventory.this);
                            unitsText.setText(units + " ml");
                            unitsText.setTextSize(16);
                            unitsText.setTextColor(getResources().getColor(R.color.primary_red, null));

                            layout.addView(groupText);
                            layout.addView(unitsText);
                            card.addView(layout);
                            inventoryContainer.addView(card);

                            try {
                                totalUnits += Integer.parseInt(units.replaceAll("[^0-9]", ""));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        tvTotalUnits.setText(totalUnits + " ml");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_inventory);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                finish();
                return true;
            } else if (itemId == R.id.nav_request) {
                startActivity(new Intent(BloodInventory.this, RequestBlood.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_inventory) {
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(BloodInventory.this, DonationHistory.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(BloodInventory.this, Profile.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
