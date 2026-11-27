package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.location.LocationTracker;
import com.reynanfc.sistema_gerenciador_trilha.util.MapIcons;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

public class RecordTrailActivity extends AppCompatActivity {

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final float INITIAL_ZOOM = 17f;

    private SettingsPrefs settingsPrefs;
    @Nullable
    private GoogleMap googleMap;
    private Chip chipAccuracy;
    private LocationTracker locationTracker;
    private boolean hasLocationPermission;
    private boolean cameraCentered;
    @Nullable
    private Marker positionMarker;

    // Must be registered before the Activity is started, so it is a field and not created on demand
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> onLocationPermissionResult());

    private final ActivityResultLauncher<IntentSenderRequest> locationSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    onDeviceLocationOff();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record_trail);

        settingsPrefs = new SettingsPrefs(this);
        chipAccuracy = findViewById(R.id.chip_accuracy);
        locationTracker = new LocationTracker(this, this::onLocation);

        setupWindowInsets();
        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        showCurrentSettings();
        setupMap();
        checkLocationPermission();
    }

    // GPS only runs while the screen is visible; the permission result arrives before onResume
    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission) {
            locationTracker.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationTracker.stop();
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkLocationPermission() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            onLocationPermissionGranted();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationale();
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS);
        }
    }

    // Fine and coarse are requested together: since Android 12 the user may grant only the approximate one
    private void onLocationPermissionResult() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            onLocationPermissionGranted();
        } else if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Toast.makeText(this, R.string.permission_location_approximate, Toast.LENGTH_LONG).show();
            onLocationPermissionGranted();
        } else {
            onLocationPermissionDenied();
        }
    }

    private void onLocationPermissionGranted() {
        hasLocationPermission = true;
        chipAccuracy.setText(R.string.record_accuracy_waiting);
        checkDeviceLocation();
    }

    // If the device location is off, Play Services shows a dialog that turns it on with one tap
    private void checkDeviceLocation() {
        locationTracker.checkLocationSettings().addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                PendingIntent resolution = ((ResolvableApiException) e).getResolution();
                locationSettingsLauncher.launch(new IntentSenderRequest.Builder(resolution).build());
            } else {
                onDeviceLocationOff();
            }
        });
    }

    private void onDeviceLocationOff() {
        chipAccuracy.setText(R.string.record_accuracy_gps_off);
        Toast.makeText(this, R.string.location_disabled_message, Toast.LENGTH_LONG).show();
    }

    private void onLocation(Location location) {
        chipAccuracy.setText(getString(R.string.record_accuracy, Math.round(location.getAccuracy())));

        if (googleMap == null) {
            return;
        }
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        updatePositionMarker(position, location);

        if (cameraCentered) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(position));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, INITIAL_ZOOM));
            cameraCentered = true;
        }
    }

    // One marker for the whole screen: created on the first fix and only moved afterwards
    private void updatePositionMarker(LatLng position, Location location) {
        if (positionMarker == null) {
            positionMarker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(MapIcons.fromVector(this, R.drawable.ic_position_marker))
                    .anchor(0.5f, 0.5f)
                    .flat(true));
        } else {
            positionMarker.setPosition(position);
        }

        // Flat marker: rotation is relative to north, so the arrow keeps pointing the right way
        // even when the map rotates (Course Up). Standing still has no bearing, so keep the last one
        if (positionMarker != null && location.hasBearing()) {
            positionMarker.setRotation(location.getBearing());
        }
    }

    private void onLocationPermissionDenied() {
        chipAccuracy.setText(R.string.record_accuracy_no_permission);

        // After "deny" twice (or "don't ask again") Android stops showing the system dialog,
        // so the only way back is the app settings screen
        boolean permanentlyDenied =
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_location_title)
                .setMessage(R.string.permission_location_denied_message)
                .setNegativeButton(R.string.action_back, (d, which) -> finish());
        if (permanentlyDenied) {
            dialog.setPositiveButton(R.string.permission_open_settings, (d, which) -> openAppSettings());
        } else {
            dialog.setPositiveButton(R.string.permission_try_again,
                    (d, which) -> locationPermissionLauncher.launch(LOCATION_PERMISSIONS));
        }
        dialog.setCancelable(false).show();
    }

    private void showPermissionRationale() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_location_title)
                .setMessage(R.string.permission_location_rationale)
                .setNegativeButton(R.string.action_back, (d, which) -> finish())
                .setPositiveButton(R.string.permission_continue,
                        (d, which) -> locationPermissionLauncher.launch(LOCATION_PERMISSIONS))
                .setCancelable(false)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
        finish();
    }

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this::onMapReady);
        }
    }

    private void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(toGoogleMapType(settingsPrefs.getMapType()));

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setMapToolbarEnabled(false);
    }

    private int toGoogleMapType(SettingsPrefs.MapType mapType) {
        switch (mapType) {
            case SATELLITE:
                return GoogleMap.MAP_TYPE_SATELLITE;
            case VECTOR:
            default:
                return GoogleMap.MAP_TYPE_NORMAL;
        }
    }

    private void showCurrentSettings() {
        Chip chipSettings = findViewById(R.id.chip_settings);
        chipSettings.setText(getString(R.string.record_settings_summary,
                getString(settingsPrefs.getMapType().labelResId),
                getString(settingsPrefs.getNavigationMode().labelResId)));
    }

    private void setupWindowInsets() {
        View overlay = findViewById(R.id.record_overlay);

        int initialLeft = overlay.getPaddingLeft();
        int initialTop = overlay.getPaddingTop();
        int initialRight = overlay.getPaddingRight();
        int initialBottom = overlay.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );
            return insets;
        });
    }
}
