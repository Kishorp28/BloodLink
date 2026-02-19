package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.example.blood.utils.FirebaseHelper;
import com.example.blood.models.BloodInventory;

public class Dashboard extends AppCompatActivity {

    TextView user, tvQuote;
    ViewGroup bloodContainer, donorContainer;
    BottomNavigationView bottomNav;
    Button donateNowBtn;
    FrameLayout notificationBtn;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    private LinearLayout emergencyContainer, emergencyAlertsSection;

    DatabaseReference databaseReference;
    FirebaseAuth firebaseAuth;
    FirebaseHelper firebaseHelper;
    String userId;
    double currentLat = 0, currentLon = 0;
    String myBloodGroup;
    private com.google.firebase.database.ChildEventListener bloodRequestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);  // make sure file name is dashboard.xml

        // ✅ Initialize views
        user = findViewById(R.id.user);
        bloodContainer = findViewById(R.id.bloodContainer);
        donorContainer = findViewById(R.id.donorContainer);
        bottomNav = findViewById(R.id.bottomNav);        donateNowBtn = findViewById(R.id.donateNowBtn);
        notificationBtn = findViewById(R.id.notificationBell);
        tvQuote = findViewById(R.id.tvQuote);
        emergencyContainer = findViewById(R.id.emergencyContainer);
        emergencyAlertsSection = findViewById(R.id.emergencyAlertsSection);
        
        displayRandomQuote();

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();

        // Add click listeners
        donateNowBtn.setOnClickListener(v -> {
            Intent donateIntent = new Intent(Dashboard.this, DonateActivity.class);
            donateIntent.putExtra("userId", userId);
            startActivity(donateIntent);
        });

        notificationBtn.setOnClickListener(v -> {
            Intent notifIntent = new Intent(Dashboard.this, NotificationsActivity.class);
            startActivity(notifIntent);
        });

        // ✅ Get name and id from Login activity or Firebase Auth
        String nameFromLogin = getIntent().getStringExtra("userName");
        userId = getIntent().getStringExtra("userId");
        
        // If userId not in intent, get from Firebase Auth
        if (userId == null || userId.isEmpty()) {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
                // Try to get name from profile if not provided
                if (nameFromLogin == null || nameFromLogin.isEmpty()) {
                    databaseReference.child("users").child(userId).child("name")
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                String profileName = snapshot.getValue(String.class);
                                if (profileName != null && !profileName.isEmpty()) {
                                    user.setText(profileName);
                                } else if (currentUser.getEmail() != null) {
                                    user.setText(currentUser.getEmail().split("@")[0]);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                                if (currentUser.getEmail() != null) {
                                    user.setText(currentUser.getEmail().split("@")[0]);
                                }
                            }
                        });
                }
            }
        }
        
        if (nameFromLogin != null && !nameFromLogin.isEmpty()) {
            user.setText(nameFromLogin);
        }

        fetchCurrentUserLocationAndLoadDonors();
        loadBloodInventory();
        loadEmergencyRequests();
        checkNotificationPermission();
        setupBottomNavigation();
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, 
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void fetchCurrentUserLocationAndLoadDonors() {
        if (userId == null) return;
        
        databaseReference.child("users").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Double lat = snapshot.child("latitude").getValue(Double.class);
                    Double lon = snapshot.child("longitude").getValue(Double.class);
                    if (lat != null && lon != null) {
                        currentLat = lat;
                        currentLon = lon;
                    }
                    myBloodGroup = snapshot.child("bloodType").getValue(String.class);
                    loadNearbyDonors();
                    startBloodRequestListener();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadNearbyDonors();
                }
            });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344; // Convert to Kilometers
        return dist;
    }

    private void loadEmergencyRequests() {
        databaseReference.child("blood_requests")
            .orderByChild("urgency").equalTo("EMERGENCY")
            .limitToLast(5)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (emergencyContainer == null) return;
                    emergencyContainer.removeAllViews();
                    
                    if (!snapshot.exists()) {
                        emergencyAlertsSection.setVisibility(View.GONE);
                        return;
                    }
                    
                    emergencyAlertsSection.setVisibility(View.VISIBLE);
                    for (DataSnapshot postSnap : snapshot.getChildren()) {
                        addEmergencyCard(postSnap);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void addEmergencyCard(DataSnapshot snap) {
        String type = snap.child("bloodType").getValue(String.class);
        String hospital = snap.child("hospitalName").getValue(String.class);
        String loc = snap.child("location").getValue(String.class);

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(600, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 24, 0);
        card.setLayoutParams(params);
        card.setRadius(16);
        card.setCardElevation(4);
        card.setCardBackgroundColor(android.graphics.Color.WHITE);
        card.setStrokeColor(android.graphics.Color.parseColor("#FFCDD2")); // Light Red
        card.setStrokeWidth(2);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        TextView tvType = new TextView(this);
        tvType.setText(type + " NEEDED");
        tvType.setTextColor(android.graphics.Color.RED);
        tvType.setTextSize(18);
        tvType.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvHospital = new TextView(this);
        tvHospital.setText(hospital);
        tvHospital.setTextColor(android.graphics.Color.BLACK);
        tvHospital.setTextSize(14);
        tvHospital.setSingleLine(true);
        tvHospital.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView tvLoc = new TextView(this);
        tvLoc.setText("📍 " + loc);
        tvLoc.setTextSize(12);
        tvLoc.setTextColor(android.graphics.Color.GRAY);

        layout.addView(tvType);
        layout.addView(tvHospital);
        layout.addView(tvLoc);
        card.addView(layout);
        
        emergencyContainer.addView(card, 0); // Add at beginning
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh inventory when user returns from Donate activity
        loadBloodInventory();
    }

    private void loadBloodInventory() {
        firebaseHelper.loadBloodInventory(new FirebaseHelper.OnInventoryLoadedListener() {
            @Override
            public void onInventoryLoaded(DataSnapshot snapshot) {
                if (bloodContainer instanceof GridLayout) {
                    GridLayout gridLayout = (GridLayout) bloodContainer;
                    gridLayout.removeAllViews();

                    int row = 0;
                    int col = 0;
                    for (DataSnapshot bloodSnapshot : snapshot.getChildren()) {
                        // Manually parse to handle type mismatches (lastUpdated might be String or long)
                        String bloodType = bloodSnapshot.child("bloodType").getValue(String.class);
                        Integer units = bloodSnapshot.child("units").getValue(Integer.class);
                        String status = bloodSnapshot.child("status").getValue(String.class);
                        
                        // Handle lastUpdated which might be String or Long
                        long lastUpdated = 0;
                        Object lastUpdatedObj = bloodSnapshot.child("lastUpdated").getValue();
                        if (lastUpdatedObj != null) {
                            if (lastUpdatedObj instanceof Long) {
                                lastUpdated = (Long) lastUpdatedObj;
                            } else if (lastUpdatedObj instanceof String) {
                                try {
                                    lastUpdated = Long.parseLong((String) lastUpdatedObj);
                                } catch (NumberFormatException e) {
                                    lastUpdated = System.currentTimeMillis();
                                }
                            } else if (lastUpdatedObj instanceof Number) {
                                lastUpdated = ((Number) lastUpdatedObj).longValue();
                            }
                        }

                        if (bloodType != null && units != null && status != null) {
                            // Create BloodInventory object manually
                            BloodInventory inventory = new BloodInventory(bloodType, units, status);
                            inventory.setLastUpdated(lastUpdated);
                            // Create card
                            MaterialCardView card = new MaterialCardView(Dashboard.this);
                            card.setCardBackgroundColor(android.graphics.Color.WHITE);
                            card.setRadius(12);
                            card.setCardElevation(4);

                            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                            params.rowSpec = GridLayout.spec(row);
                            params.columnSpec = GridLayout.spec(col, 1f);
                            params.width = 0;
                            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                            params.setMargins(8, 8, 8, 16);
                            card.setLayoutParams(params);

                            LinearLayout layout = new LinearLayout(Dashboard.this);
                            layout.setOrientation(LinearLayout.VERTICAL);
                            layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                            layout.setPadding(16, 16, 16, 16);

                            // Blood type name
                            TextView groupText = new TextView(Dashboard.this);
                            groupText.setText(inventory.getBloodType());
                            groupText.setTextSize(28);
                            groupText.setTypeface(null, android.graphics.Typeface.BOLD);
                            groupText.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            textParams.bottomMargin = 8;
                            groupText.setLayoutParams(textParams);

                            // Units and status
                            TextView unitsText = new TextView(Dashboard.this);
                            unitsText.setText(inventory.getUnits() + " units");
                            unitsText.setTextSize(14);
                            unitsText.setTextColor(firebaseHelper.getStatusColor(inventory.getStatus()));

                            layout.addView(groupText);
                            layout.addView(unitsText);
                            card.addView(layout);
                            gridLayout.addView(card);

                            col++;
                            if (col == 2) {
                                col = 0;
                                row++;
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (bloodContainer instanceof GridLayout) {
                    GridLayout gridLayout = (GridLayout) bloodContainer;
                    gridLayout.removeAllViews();
                    
                    TextView errorText = new TextView(Dashboard.this);
                    errorText.setText("❌ Error loading inventory\n" + error + "\n\n⚠️ Check if Firebase rules are deployed");
                    errorText.setTextSize(14);
                    errorText.setPadding(20, 20, 20, 20);
                    errorText.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                    gridLayout.addView(errorText);
                }
                Toast.makeText(Dashboard.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNearbyDonors() {
        databaseReference.child("users").orderByChild("donor").equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (donorContainer == null) return;
                        donorContainer.removeAllViews();
                        
                        // Limit to top 15 donors to prevent UI lag
                        int count = 0;
                        java.util.List<DonorData> donorList = new java.util.ArrayList<>();

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            if (firebaseAuth.getCurrentUser() != null && 
                                userSnap.getKey().equals(firebaseAuth.getCurrentUser().getUid())) {
                                continue;
                            }

                            String name = userSnap.child("name").getValue(String.class);
                            String bloodGroup = userSnap.child("bloodType").getValue(String.class);
                            Double dLat = userSnap.child("latitude").getValue(Double.class);
                            Double dLon = userSnap.child("longitude").getValue(Double.class);

                            double distance = -1;
                            if (currentLat != 0 && currentLon != 0 && dLat != null && dLon != null) {
                                distance = calculateDistance(currentLat, currentLon, dLat, dLon);
                            }
                            
                            donorList.add(new DonorData(name, bloodGroup, distance));
                        }

                        // Sort by proximity
                        java.util.Collections.sort(donorList, (d1, d2) -> {
                            if (d1.distance == -1) return 1;
                            if (d2.distance == -1) return -1;
                            return Double.compare(d1.distance, d2.distance);
                        });

                        LayoutInflater inflater = LayoutInflater.from(Dashboard.this);
                        for (DonorData donor : donorList) {
                            if (count >= 15) break; 
                            
                            View cardView = inflater.inflate(R.layout.donor_item, donorContainer, false);
                            TextView donorInitial = cardView.findViewById(R.id.donorInitial);
                            TextView donorNameView = cardView.findViewById(R.id.donorName);
                            TextView donorDistanceView = cardView.findViewById(R.id.donorDistance);
                            TextView donorBloodTypeView = cardView.findViewById(R.id.donorBloodType);

                            String safeName = donor.name != null ? donor.name : "Donor";
                            donorInitial.setText(String.valueOf(safeName.charAt(0)).toUpperCase());
                            donorNameView.setText(safeName);
                            donorBloodTypeView.setText(donor.bloodGroup != null ? donor.bloodGroup : "--");
                            
                            if (donor.distance != -1) {
                                donorDistanceView.setText(String.format("%.1f km away", donor.distance));
                            } else {
                                donorDistanceView.setText("Distance N/A");
                            }

                            donorContainer.addView(cardView);
                            count++;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void startBloodRequestListener() {
        if (bloodRequestListener != null) return;
        
        databaseReference.child("blood_requests").limitToLast(1)
            .addChildEventListener(new com.google.firebase.database.ChildEventListener() {
                private boolean isInitialLoad = true;
                
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {
                    if (isInitialLoad) {
                        isInitialLoad = false;
                        return;
                    }
                    
                    String bloodGroup = snapshot.child("bloodGroup").getValue(String.class);
                    String patientName = snapshot.child("patientName").getValue(String.class);
                    String urgency = snapshot.child("urgency").getValue(String.class);
                    String creatorId = snapshot.child("createdBy").getValue(String.class);
                    
                    // Do NOT notify if I am the one who created this request
                    if (userId != null && userId.equals(creatorId)) {
                        return;
                    }

                    // Notify ALL users as requested
                    String title = "Urgent " + bloodGroup + " Blood Needed!";
                    String message = patientName + " needs " + bloodGroup + " blood (" + urgency + "). Tap to help!";
                    com.example.blood.utils.NotificationHelper.showNotification(Dashboard.this, title, message);
                    
                    // Visual cue on the notification bell
                    notificationBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#FFCDD2"))); // Light Red
                }

                @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
                @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
                @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bloodRequestListener != null) {
            databaseReference.child("BloodRequests").removeEventListener(bloodRequestListener);
        }
    }

    // Helper class for sorting
    private static class DonorData {
        String name, bloodGroup;
        double distance;
        DonorData(String name, String bloodGroup, double distance) {
            this.name = name;
            this.bloodGroup = bloodGroup;
            this.distance = distance;
        }
    }

    private void setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_request) {
                startActivity(new Intent(this, RequestBlood.class));
            } else if (id == R.id.nav_inventory) {
                startActivity(new Intent(this, BloodInventoryActivity.class));
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, DonationHistory.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalyticsActivity.class));
            } else if (id == R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyRequestActivity.class));
            }
            return true;
        });
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_request) {
                Intent intent = new Intent(Dashboard.this, RequestBlood.class);
                startActivity(intent);
                overridePendingTransition(0, 0); // Seamless transition
                return true;
            } else if (itemId == R.id.nav_inventory) {
                Intent inventoryIntent = new Intent(Dashboard.this, BloodInventoryActivity.class);
                if (userId != null) {
                    inventoryIntent.putExtra("userId", userId);
                }
                startActivity(inventoryIntent);
                overridePendingTransition(0, 0); // Seamless transition
                return true;
            } else if (itemId == R.id.nav_history) {
                Intent historyIntent = new Intent(Dashboard.this, DonationHistory.class);
                if (userId != null) {
                    historyIntent.putExtra("userId", userId);
                }
                startActivity(historyIntent);
                overridePendingTransition(0, 0); // Seamless transition
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent profileIntent = new Intent(Dashboard.this, Profile.class);
                if (userId != null) {
                    profileIntent.putExtra("userId", userId);
                }
                startActivity(profileIntent);
                overridePendingTransition(0, 0); // Seamless transition
                return true;
            }
            return false;
        });
    }

    private void displayRandomQuote() {
        String[] quotes = {
            "Donate blood, save a life.",
            "Your blood can be someone’s tomorrow.",
            "A drop of blood is a drop of hope.",
            "Give blood, give life.",
            "Be a hero — donate blood.",
            "Blood donors are real-life superheroes.",
            "One pint can save three lives.",
            "Donate blood today, be someone’s miracle tomorrow.",
            "Your kindness flows in your veins.",
            "Share your strength, donate blood.",
            "Every drop counts.",
            "Save lives with a simple act.",
            "Blood donation is the gift of life.",
            "Give blood and keep the world beating.",
            "Donate blood, spread love.",
            "A small prick, a big difference.",
            "Blood donation: the most precious gift.",
            "You don’t need a cape to be a hero.",
            "Life flows from you.",
            "One donation, countless smiles.",
            "Give hope, give blood.",
            "Blood donors make life possible.",
            "Donate blood, because life matters.",
            "Your blood type is the best type.",
            "Saving lives starts with you.",
            "Donate blood and be proud.",
            "Be someone’s reason to live.",
            "The gift of blood is the gift of life.",
            "Donate blood — it costs nothing but means everything.",
            "Strong hearts donate blood.",
            "Give blood, give strength.",
            "Heroes donate blood regularly.",
            "Blood donation is humanity in action.",
            "Donate blood and feel the difference.",
            "Let your blood be someone’s lifeline.",
            "Make every heartbeat count.",
            "Donate blood, spread humanity.",
            "The power to save lives is in your veins.",
            "Be brave, donate blood.",
            "Blood donors are life savers.",
            "A few minutes for you, a lifetime for someone.",
            "Donate blood and be a blessing.",
            "Life is in your hands — and your veins.",
            "One act, many lives saved.",
            "Blood connects us all.",
            "Donate blood, inspire others.",
            "Give blood, give hope, give life.",
            "Your donation can rewrite someone’s story.",
            "Be the reason someone survives.",
            "Donate blood — because every life is precious."
        };
        
        int randomIndex = new java.util.Random().nextInt(quotes.length);
        if (tvQuote != null) {
            tvQuote.setText(quotes[randomIndex]);
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow back button on Dashboard - exit app instead
        moveTaskToBack(true);
    }
}
