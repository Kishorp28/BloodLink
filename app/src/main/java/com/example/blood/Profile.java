package com.example.blood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.example.blood.utils.FirebaseHelper;
import com.example.blood.models.UserProfile;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class Profile extends AppCompatActivity {

    private TextView tvName, tvEmail, tvMobile, tvCity, tvAddress, tvBloodGroup, tvInitial, tvTotalDonations;
    private Button btnLogout;
    private BottomNavigationView bottomNav;
    private FirebaseHelper firebaseHelper;
    private String userId;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private android.widget.EditText etMobile, etCity, etAddress;
    private android.widget.LinearLayout myRequestsContainer;
    private com.google.android.material.button.MaterialButton btnEditToggle;
    private boolean isEditMode = false;
    private UserProfile currentProfile;
    private com.google.android.material.imageview.ShapeableImageView ivProfile;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabEditImage, fabDeleteImage;
    private androidx.activity.result.ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvMobile = findViewById(R.id.tvMobile);
        tvCity = findViewById(R.id.tvCity);
        tvAddress = findViewById(R.id.tvAddress);
        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvInitial = findViewById(R.id.tvInitial);
        bottomNav = findViewById(R.id.bottomNav);
        etMobile = findViewById(R.id.etMobile);
        etCity = findViewById(R.id.etCity);
        etAddress = findViewById(R.id.etAddress);
        myRequestsContainer = findViewById(R.id.myRequestsContainer);
        btnEditToggle = findViewById(R.id.btnEditToggle);
        btnLogout = findViewById(R.id.btnLogout);
        ivProfile = findViewById(R.id.ivProfile);
        fabEditImage = findViewById(R.id.fabEditImage);
        fabDeleteImage = findViewById(R.id.fabDeleteImage);
        
        setupImageCRUD();

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        setupDrawerNavigation();

        // Get userId from intent or current auth
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                userId = auth.getCurrentUser().getUid();
            }
        }

        if (userId != null && !userId.isEmpty()) {
            loadUserProfile();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupBottomNavigation();

        btnLogout.setOnClickListener(v -> logout());
        btnEditToggle.setOnClickListener(v -> toggleEditMode());
        
        loadMyRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile when user returns
        if (userId != null && !userId.isEmpty()) {
            loadUserProfile();
        }
    }

    private void loadUserProfile() {
        firebaseHelper.loadUserProfile(new FirebaseHelper.OnProfileLoadedListener() {
            @Override
            public void onProfileLoaded(UserProfile profile) {
                if (profile != null) {
                    currentProfile = profile;
                    tvName.setText(profile.getName() != null ? profile.getName() : "N/A");
                    tvEmail.setText(profile.getEmail() != null ? profile.getEmail() : "N/A");
                    tvMobile.setText(profile.getPhone() != null ? profile.getPhone() : "N/A");
                    tvCity.setText(profile.getCity() != null ? profile.getCity() : "N/A");
                    tvAddress.setText(profile.getAddress() != null ? profile.getAddress() : "N/A");
                    tvBloodGroup.setText(profile.getBloodType() != null ? profile.getBloodType() : "N/A");
                    
                    etMobile.setText(profile.getPhone());
                    etCity.setText(profile.getCity());
                    etAddress.setText(profile.getAddress());
                    
                    if (profile.getName() != null && profile.getName().length() > 0) {
                        tvInitial.setText(String.valueOf(profile.getName().charAt(0)).toUpperCase());
                    }
                    
                        if (profile.getProfilePhotoUrl() != null && !profile.getProfilePhotoUrl().isEmpty()) {
                            try {
                                byte[] decodedString = android.util.Base64.decode(profile.getProfilePhotoUrl(), android.util.Base64.DEFAULT);
                                android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                ivProfile.setImageBitmap(decodedByte);
                                tvInitial.setVisibility(android.view.View.GONE);
                                fabDeleteImage.setVisibility(android.view.View.VISIBLE);
                            } catch (Exception e) {
                                tvInitial.setVisibility(android.view.View.VISIBLE);
                                fabDeleteImage.setVisibility(android.view.View.GONE);
                            }
                        } else {
                            ivProfile.setImageResource(R.drawable.avatar_bg);
                            tvInitial.setVisibility(android.view.View.VISIBLE);
                            fabDeleteImage.setVisibility(android.view.View.GONE);
                        }
                    } else {
                    Toast.makeText(Profile.this, "Profile not found", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(Profile.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        
        if (isEditMode) {
            btnEditToggle.setText("SAVE");
            btnEditToggle.setIconResource(android.R.drawable.ic_menu_save);
            
            tvMobile.setVisibility(android.view.View.GONE);
            tvCity.setVisibility(android.view.View.GONE);
            tvAddress.setVisibility(android.view.View.GONE);
            
            etMobile.setVisibility(android.view.View.VISIBLE);
            etCity.setVisibility(android.view.View.VISIBLE);
            etAddress.setVisibility(android.view.View.VISIBLE);
        } else {
            saveProfileUpdates();
        }
    }

    private void saveProfileUpdates() {
        if (currentProfile == null) return;
        
        String phone = etMobile.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        
        currentProfile.setPhone(phone);
        currentProfile.setCity(city);
        currentProfile.setAddress(address);
        
        firebaseHelper.saveUserProfile(currentProfile, new FirebaseHelper.OnCompleteListener() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(Profile.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                btnEditToggle.setText("EDIT");
                btnEditToggle.setIconResource(android.R.drawable.ic_menu_edit);
                
                tvMobile.setVisibility(android.view.View.VISIBLE);
                tvCity.setVisibility(android.view.View.VISIBLE);
                tvAddress.setVisibility(android.view.View.VISIBLE);
                
                etMobile.setVisibility(android.view.View.GONE);
                etCity.setVisibility(android.view.View.GONE);
                etAddress.setVisibility(android.view.View.GONE);
                
                loadUserProfile(); // Refresh UI
            }

            @Override
            public void onError(String error) {
                Toast.makeText(Profile.this, "Update failed: " + error, Toast.LENGTH_SHORT).show();
                isEditMode = true; // Keep in edit mode if failed
                btnEditToggle.setText("SAVE");
            }
        });
    }

    private void loadMyRequests() {
        if (userId == null) return;
        
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("blood_requests")
                .orderByChild("createdBy").equalTo(userId)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                myRequestsContainer.removeAllViews();
                if (!snapshot.exists()) {
                    TextView tv = new TextView(Profile.this);
                    tv.setText("No active blood requests.");
                    tv.setTextColor(android.graphics.Color.GRAY);
                    myRequestsContainer.addView(tv);
                    return;
                }
                
                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    addRequestCard(snap);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void setupImageCRUD() {
        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                            android.graphics.Bitmap resized = android.graphics.Bitmap.createScaledBitmap(bitmap, 256, 256, true);
                            
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
                            String encodedImage = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
                            
                            updateProfileImage(encodedImage);
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        fabEditImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        fabDeleteImage.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to remove your profile picture?")
                .setPositiveButton("Remove", (dialog, id) -> updateProfileImage(""))
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void updateProfileImage(String encodedImage) {
        if (userId == null) return;
        
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("profilePhotoUrl").setValue(encodedImage)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, encodedImage.isEmpty() ? "Image Deleted" : "Image Updated", Toast.LENGTH_SHORT).show();
                    loadUserProfile();
                });
    }

    private void addRequestCard(com.google.firebase.database.DataSnapshot snap) {
        String requestId = snap.child("requestId").getValue(String.class);
        String bloodType = snap.child("bloodType").getValue(String.class);
        String hospital = snap.child("hospitalName").getValue(String.class);
        String status = snap.child("status").getValue(String.class);
        
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(12 * getResources().getDisplayMetrics().density);
        card.setCardElevation(2 * getResources().getDisplayMetrics().density);
        card.setCardBackgroundColor(android.graphics.Color.WHITE);
        
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, 0, (int)(12 * getResources().getDisplayMetrics().density));
        card.setLayoutParams(clp);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(bloodType + " Needed at " + hospital);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(android.graphics.Color.BLACK);
        
        TextView tvStatus = new TextView(this);
        tvStatus.setText("Status: " + status);
        tvStatus.setTextSize(12);

        textLayout.addView(tvTitle);
        textLayout.addView(tvStatus);
        
        android.widget.ImageButton btnDelete = new android.widget.ImageButton(this);
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
        btnDelete.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnDelete.setColorFilter(android.graphics.Color.parseColor("#D32F2F"));
        btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Request")
                .setMessage("Are you sure you want to delete this blood request?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("blood_requests")
                        .child(requestId).removeValue()
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Request deleted (CRUD - Delete)", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
        });

        layout.addView(textLayout);
        layout.addView(btnDelete);
        card.addView(layout);
        myRequestsContainer.addView(card);
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(Profile.this, Dashboard.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_request) {
                Intent intent = new Intent(Profile.this, RequestBlood.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_inventory) {
                Intent intent = new Intent(Profile.this, BloodInventoryActivity.class);
                String userId = getIntent().getStringExtra("userId");
                if (userId != null) {
                    intent.putExtra("userId", userId);
                }
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                Intent historyIntent = new Intent(Profile.this, DonationHistory.class);
                historyIntent.putExtra("userId", getIntent().getStringExtra("userId"));
                startActivity(historyIntent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        // Navigate back to Dashboard
        Intent intent = new Intent(Profile.this, Dashboard.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void logout() {
        // Sign out from Firebase Auth
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        
        Intent intent = new Intent(Profile.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        finish();
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
                return true;
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalyticsActivity.class));
            } else if (id == R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyRequestActivity.class));
            }
            return true;
        });
    }
}
