package com.example.blood;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.blood.firebase.FirebaseManager;
import com.example.blood.models.BloodBank;
import com.google.firebase.database.DataSnapshot;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Map;

public class BloodBanksMapActivity extends AppCompatActivity {

    private MapView map;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required for osmdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_blood_banks_map);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(13.0);
        
        // Start center - Coimbatore
        GeoPoint startPoint = new GeoPoint(11.0168, 76.9558);
        mapController.setCenter(startPoint);

        firebaseManager = new FirebaseManager();
        loadMarkers();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadMarkers() {
        firebaseManager.getAllBloodBanks(new FirebaseManager.OnDataReceiveListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot snapshot) {
                for (DataSnapshot bankSnap : snapshot.getChildren()) {
                    BloodBank bank = bankSnap.getValue(BloodBank.class);
                    if (bank != null && bank.getLatitude() != 0) {
                        addMarker(bank);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BloodBanksMapActivity.this, "Error loading markers: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarker(BloodBank bank) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(bank.getLatitude(), bank.getLongitude()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(bank.getName());
        
        StringBuilder stockInfo = new StringBuilder();
        stockInfo.append(bank.getAddress()).append("\n\nStock:\n");
        for (Map.Entry<String, Integer> entry : bank.getInventory().entrySet()) {
            stockInfo.append(entry.getKey()).append(": ").append(entry.getValue()).append(" U | ");
        }
        
        marker.setSnippet(stockInfo.toString());
        map.getOverlays().add(marker);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}
