package com.example.blood;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity {

    private MapView map;
    private IMapController mapController;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required for osmdroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_location_picker);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(15.0);
        
        // Start center - Coimbatore
        GeoPoint startPoint = new GeoPoint(11.0168, 76.9558);
        mapController.setCenter(startPoint);

        checkPermissions();

        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> {
            GeoPoint center = (GeoPoint) map.getMapCenter();
            returnLocation(center.getLatitude(), center.getLongitude());
        });

        findViewById(R.id.fabCurrentLocation).setOnClickListener(v -> {
            getCurrentLocation();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

        com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = 
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
            
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapController.animateTo(currentPoint);
            } else {
                Toast.makeText(this, "Could not get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void returnLocation(double lat, double lon) {
        String addressLine = "";
        String city = "";
        
        // Try to get address from Lat/Lon
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                addressLine = addr.getAddressLine(0);
                city = addr.getLocality();
                if (city == null) city = addr.getSubAdminArea();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("lat", lat);
        resultIntent.putExtra("lon", lon);
        resultIntent.putExtra("address", addressLine);
        resultIntent.putExtra("city", city);
        setResult(RESULT_OK, resultIntent);
        finish();
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
