package com.example.blood;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.blood.utils.NotificationHelper;

/**
 * Base activity to handle global notifications while the app is running.
 */
public class BaseActivity extends AppCompatActivity {
    protected DatabaseReference mDatabase;
    protected String mUserId;
    private ChildEventListener bloodRequestListener;

    private static boolean isPersistenceEnabledSet = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Firebase Persistence must be set BEFORE any getInstance call
        if (!isPersistenceEnabledSet) {
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                isPersistenceEnabledSet = true;
            } catch (Exception e) {
                Log.e("BaseActivity", "Firebase persistence error: " + e.getMessage());
            }
        }
        
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            mUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startBloodRequestListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopBloodRequestListener();
    }

    private void startBloodRequestListener() {
        if (bloodRequestListener != null || mDatabase == null) return;

        // Only listen for requests created AFTER this activity started
        long startTime = System.currentTimeMillis();

        bloodRequestListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String bloodGroup = snapshot.child("bloodType").getValue(String.class);
                String patientName = snapshot.child("hospitalName").getValue(String.class);
                String urgency = snapshot.child("urgency").getValue(String.class);
                String creatorId = snapshot.child("createdBy").getValue(String.class);

                Log.d("BaseActivity", "New blood request: " + bloodGroup + " for " + patientName);

                // Do NOT notify if I am the one who created this request
                if (mUserId != null && mUserId.equals(creatorId)) {
                    return;
                }

                // Show a Toast to prove it's happening in real-time
                android.widget.Toast.makeText(BaseActivity.this, 
                        "🚨 REAL-TIME ALERT: " + bloodGroup + " needed!", android.widget.Toast.LENGTH_LONG).show();

                // Notify ALL users (System Notification Pop-up)
                String title = "Urgent " + (bloodGroup != null ? bloodGroup : "") + " Blood Needed!";
                String message = (patientName != null ? patientName : "Someone") + " needs blood (" + (urgency != null ? urgency : "NORMAL") + "). Tap to help!";
                NotificationHelper.showNotification(BaseActivity.this, title, message);
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        // Use createdTime index to only get new requests
        mDatabase.child("blood_requests")
                .orderByChild("createdTime")
                .startAt(startTime)
                .addChildEventListener(bloodRequestListener);
    }

    private void stopBloodRequestListener() {
        if (bloodRequestListener != null && mDatabase != null) {
            mDatabase.child("blood_requests").removeEventListener(bloodRequestListener);
            bloodRequestListener = null;
        }
    }
}
