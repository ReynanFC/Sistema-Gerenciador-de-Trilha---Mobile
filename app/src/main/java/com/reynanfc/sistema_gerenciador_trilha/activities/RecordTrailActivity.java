package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

public class RecordTrailActivity extends AppCompatActivity {

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private SettingsPrefs settingsPrefs;
    @Nullable
    private GoogleMap googleMap;
    private Chip chipAccuracy;

    // Must be registered before the Activity is started, so it is a field and not created on demand
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> onLocationPermissionResult());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record_trail);

        settingsPrefs = new SettingsPrefs(this);
        chipAccuracy = findViewById(R.id.chip_accuracy);

        setupWindowInsets();
        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        showCurrentSettings();
        setupMap();
        checkLocationPermission();
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
        chipAccuracy.setText(R.string.record_accuracy_waiting);
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
