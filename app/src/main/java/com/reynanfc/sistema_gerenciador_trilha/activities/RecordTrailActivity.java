package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.material.chip.Chip;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

public class RecordTrailActivity extends AppCompatActivity {

    private SettingsPrefs settingsPrefs;
    @Nullable
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record_trail);

        settingsPrefs = new SettingsPrefs(this);

        setupWindowInsets();
        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        showCurrentSettings();
        setupMap();
    }

    // The map loads asynchronously: googleMap is only usable after onMapReady
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

        // The screen has its own compass button and controls
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
