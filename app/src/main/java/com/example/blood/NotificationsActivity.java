package com.example.blood;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.content.Intent;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NotificationsActivity extends AppCompatActivity {

    private LinearLayout notifContainer;
    private DatabaseReference databaseReference;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notifContainer = findViewById(R.id.notifContainer);

        databaseReference = FirebaseDatabase.getInstance().getReference("Notifications");

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();

        loadNotifications();
    }

    private void loadNotifications() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        notifContainer.removeAllViews();

        databaseReference.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                refreshAllNotifications(userId, inflater);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        FirebaseDatabase.getInstance().getReference("PublicNotifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        refreshAllNotifications(userId, inflater);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void refreshAllNotifications(String userId, LayoutInflater inflater) {
        notifContainer.removeAllViews();
        FirebaseDatabase.getInstance().getReference("PublicNotifications").limitToLast(20)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot s : snapshot.getChildren()) addNotifView(s, inflater, true);
                databaseReference.child(userId).limitToLast(20)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot sn) {
                        for (DataSnapshot s : sn.getChildren()) addNotifView(s, inflater, false);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addNotifView(DataSnapshot snapshot, LayoutInflater inflater, boolean isPublic) {
        String title = snapshot.child("title").getValue(String.class);
        String body = snapshot.child("body").getValue(String.class);
        String time = snapshot.child("time").getValue(String.class);

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(12);
        card.setCardElevation(2);
        
        card.setCardBackgroundColor(isPublic ? android.graphics.Color.parseColor("#FFF3F3") : android.graphics.Color.WHITE);

        View content = inflater.inflate(R.layout.notification_item, card, false);
        ((TextView)content.findViewById(R.id.tvNotifTitle)).setText(title != null ? title : "Emergency");
        ((TextView)content.findViewById(R.id.tvNotifBody)).setText(body != null ? body : "");
        ((TextView)content.findViewById(R.id.tvNotifTime)).setText(time != null ? time : "");

        card.addView(content);
        notifContainer.addView(card, 0); 
    }

    private void setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, Dashboard.class));
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
}


