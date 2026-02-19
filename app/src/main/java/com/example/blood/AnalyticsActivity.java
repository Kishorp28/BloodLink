package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private DatabaseReference db;
    
    private TextView tvUserCount, tvDonationCount, tvRequestCount, tvBankCount, tvEfficiencyScore, tvEfficiencyStatus;
    private ProgressBar efficiencyProgress;
    private View vUrgencyNormal, vUrgencyUrgent, vUrgencyImmediate;
    private LinearLayout inventoryGraph, insightContainer, regionalImpactList;
    
    // Stats storage
    private long userCount = 0;
    private long donationCount = 0;
    private long requestCount = 0;
    private long bankCount = 0;
    private Map<String, Integer> bloodStats = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        db = FirebaseDatabase.getInstance().getReference();
        
        initViews();
        setupNavigation();
        loadAnalyticsData();
        
        findViewById(R.id.btnDownload).setOnClickListener(v -> generateReport());
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvDonationCount = findViewById(R.id.tvDonationCount);
        tvRequestCount = findViewById(R.id.tvRequestCount);
        tvBankCount = findViewById(R.id.tvBankCount);
        inventoryGraph = findViewById(R.id.inventoryGraph);
        tvEfficiencyScore = findViewById(R.id.tvEfficiencyScore);
        tvEfficiencyStatus = findViewById(R.id.tvEfficiencyStatus);
        efficiencyProgress = findViewById(R.id.efficiencyProgress);
        vUrgencyNormal = findViewById(R.id.vUrgencyNormal);
        vUrgencyUrgent = findViewById(R.id.vUrgencyUrgent);
        vUrgencyImmediate = findViewById(R.id.vUrgencyImmediate);
        insightContainer = findViewById(R.id.insightContainer);
        regionalImpactList = findViewById(R.id.regionalImpactList);
        
        findViewById(R.id.toolbar).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, Dashboard.class));
                finish();
            } else if (id == R.id.nav_request) {
                startActivity(new Intent(this, RequestBlood.class));
                finish();
            } else if (id == R.id.nav_inventory) {
                startActivity(new Intent(this, BloodInventoryActivity.class));
                finish();
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, DonationHistory.class));
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profile.class));
            } else if (id == R.id.nav_analytics) {
                return true;
            } else if (id == R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyRequestActivity.class));
            }
            return true;
        });
    }

    private void loadAnalyticsData() {
        // Run in background implicitly via Firebase listeners
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.hasChild("blood_banks")) {
                    com.example.blood.firebase.FirebaseManager fm = new com.example.blood.firebase.FirebaseManager();
                    fm.initializeCoimbatoreData();
                    fm.seedSampleRequests();
                    // Re-fetch after initialization
                    loadAnalyticsData();
                    return;
                }
                
                userCount = snapshot.child("users").getChildrenCount();
                donationCount = snapshot.child("blood_donations").getChildrenCount();
                requestCount = snapshot.child("blood_requests").getChildrenCount();
                bankCount = snapshot.child("blood_banks").getChildrenCount();

                tvUserCount.setText(String.valueOf(userCount));
                tvDonationCount.setText(String.valueOf(donationCount));
                tvRequestCount.setText(String.valueOf(requestCount));
                tvBankCount.setText(String.valueOf(bankCount));

                // INNOVATION: System Balance Check
                // INNOVATION: System Efficiency Score calculation
                double balanceRatio = requestCount > 0 ? (double) donationCount / requestCount : (donationCount > 0 ? 1.0 : 0.0);
                int score = (int) (Math.min(balanceRatio, 1.5) * 66); // 1.5 ratio = 100% efficiency
                if (score > 100) score = 100;
                
                efficiencyProgress.setProgress(score);
                tvEfficiencyScore.setText(score + "%");
                
                String healthStatus = "CRITICAL";
                int statusColor = android.graphics.Color.parseColor("#F44336");
                if (score >= 80) {
                    healthStatus = "EXCELLENT";
                    statusColor = android.graphics.Color.parseColor("#4CAF50");
                } else if (score >= 50) {
                    healthStatus = "STABLE";
                    statusColor = android.graphics.Color.parseColor("#FF9800");
                }
                
                tvEfficiencyStatus.setText(healthStatus);
                tvEfficiencyStatus.setTextColor(statusColor);

                // Process inventory stats
                bloodStats.clear();
                for (DataSnapshot bank : snapshot.child("blood_banks").getChildren()) {
                    DataSnapshot stock = bank.child("inventory");
                    for (DataSnapshot type : stock.getChildren()) {
                        String bloodType = type.getKey();
                        int units = type.getValue(Integer.class);
                        bloodStats.put(bloodType, bloodStats.getOrDefault(bloodType, 0) + units);
                    }
                }
                
                // Urgency Distribution
                long normalCount = 0, urgentCount = 0, immediateCount = 0;
                for (DataSnapshot req : snapshot.child("blood_requests").getChildren()) {
                    String urgency = req.child("urgency").getValue(String.class);
                    if (urgency != null) {
                        if (urgency.equalsIgnoreCase("NORMAL")) normalCount++;
                        else if (urgency.equalsIgnoreCase("URGENT")) urgentCount++;
                        else if (urgency.equalsIgnoreCase("IMMEDIATE")) immediateCount++;
                    }
                }
                updateUrgencyChart(normalCount, urgentCount, immediateCount);
                updateInsights();
                updateRegionalImpact(snapshot);
                updateVisualGraph();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AnalyticsActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUrgencyChart(long normal, long urgent, long immediate) {
        long total = normal + urgent + immediate;
        if (total == 0) {
            total = 1;
            normal = 1; // Default to normal for visual
        }
        
        LinearLayout.LayoutParams lpNormal = (LinearLayout.LayoutParams) vUrgencyNormal.getLayoutParams();
        lpNormal.weight = (float) normal / total;
        vUrgencyNormal.setLayoutParams(lpNormal);

        LinearLayout.LayoutParams lpUrgent = (LinearLayout.LayoutParams) vUrgencyUrgent.getLayoutParams();
        lpUrgent.weight = (float) urgent / total;
        vUrgencyUrgent.setLayoutParams(lpUrgent);

        LinearLayout.LayoutParams lpImmediate = (LinearLayout.LayoutParams) vUrgencyImmediate.getLayoutParams();
        lpImmediate.weight = (float) immediate / total;
        vUrgencyImmediate.setLayoutParams(lpImmediate);
    }

    private void updateInsights() {
        insightContainer.removeAllViews();
        boolean foundLow = false;
        for (Map.Entry<String, Integer> entry : bloodStats.entrySet()) {
            if (entry.getValue() < 50) {
                TextView tv = new TextView(this);
                tv.setText("⚠️ Low supply: " + entry.getKey() + " (" + entry.getValue() + " units remaining)");
                tv.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                tv.setPadding(0, 4, 0, 4);
                tv.setTextSize(12);
                insightContainer.addView(tv);
                foundLow = true;
            }
        }
        if (!foundLow) {
            TextView tv = new TextView(this);
            tv.setText("✓ All blood types are currently at safe levels.");
            tv.setTextColor(android.graphics.Color.parseColor("#424242"));
            tv.setTextSize(12);
            insightContainer.addView(tv);
        }
    }

    private void updateRegionalImpact(DataSnapshot snapshot) {
        regionalImpactList.removeAllViews();
        Map<String, Integer> cityStats = new HashMap<>();
        for (DataSnapshot user : snapshot.child("users").getChildren()) {
            String city = user.child("city").getValue(String.class);
            if (city == null || city.isEmpty()) city = "Unspecified";
            cityStats.put(city, cityStats.getOrDefault(city, 0) + 1);
        }

        int max = 0;
        for (int val : cityStats.values()) if (val > max) max = val;
        
        for (Map.Entry<String, Integer> entry : cityStats.entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView cityName = new TextView(this);
            cityName.setText(entry.getKey() + " (" + entry.getValue() + " users)");
            cityName.setTextSize(12);
            
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(max);
            pb.setProgress(entry.getValue());
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#A52A2A")));
            
            row.addView(cityName);
            row.addView(pb);
            regionalImpactList.addView(row);
        }
    }

    private void updateVisualGraph() {
        inventoryGraph.removeAllViews();
        int maxUnits = 0;
        for (int units : bloodStats.values()) if (units > maxUnits) maxUnits = units;
        if (maxUnits == 0) maxUnits = 100;

        for (Map.Entry<String, Integer> entry : bloodStats.entrySet()) {
            View bar = new View(this);
            int height = (entry.getValue() * 300) / maxUnits; // Increased scaling for better visibility
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height + 20);
            lp.weight = 1;
            lp.setMargins(4, 0, 4, 0);
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(getColor(R.color.primary_red));
            
            LinearLayout barContainer = new LinearLayout(this);
            barContainer.setOrientation(LinearLayout.VERTICAL);
            barContainer.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            barContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
            
            TextView label = new TextView(this);
            label.setText(entry.getKey());
            label.setTextSize(10);
            label.setGravity(android.view.Gravity.CENTER);
            label.setTextColor(android.graphics.Color.BLACK);
            label.setPadding(0, 4, 0, 0);
            
            TextView value = new TextView(this);
            value.setText(String.valueOf(entry.getValue()));
            value.setTextSize(8);
            value.setGravity(android.view.Gravity.CENTER);
            value.setTextColor(getColor(R.color.primary_red));
            
            barContainer.addView(value);
            barContainer.addView(bar);
            barContainer.addView(label);
            inventoryGraph.addView(barContainer);
        }
    }

    private void generateReport() {
        String timeStamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        String report = "========================================\n" +
                "       BLOOD BOND ANALYTICS REPORT\n" +
                "========================================\n" +
                "Generated on: " + timeStamp + "\n\n" +
                "CORE SYSTEM METRICS:\n" +
                "----------------------------------------\n" +
                "Total Community Members: " + userCount + "\n" +
                "Successful Life Donations: " + donationCount + "\n" +
                "Active Blood Requests: " + requestCount + "\n" +
                "Partner Blood Banks: " + bankCount + "\n\n" +
                "INNOVATIVE INSIGHTS:\n" +
                "----------------------------------------\n" +
                "Supply/Demand Ratio: " + String.format("%.2f", (double)donationCount/Math.max(1, requestCount)) + "\n" +
                "Estimated Lives Impacted: " + (donationCount * 3) + " souls\n\n" +
                "BLOOD TYPE AVAILABILITY (SYSTEM-WIDE):\n" +
                "----------------------------------------\n";
        
        for (Map.Entry<String, Integer> entry : bloodStats.entrySet()) {
            report += String.format("%-10s : %3d units\n", entry.getKey(), entry.getValue());
        }
        report += "\n========================================\n" +
                  "         END OF OFFICIAL REPORT\n" +
                  "========================================";

        try {
            File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File file = new File(path, "BloodBond_Report.txt");
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(report.getBytes());
            stream.close();
            Toast.makeText(this, "Report saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating report", Toast.LENGTH_SHORT).show();
        }
    }
}
