package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.database.TrailDao;
import com.reynanfc.sistema_gerenciador_trilha.dialog.RenameTrailDialog;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.model.TrackPoint;
import com.reynanfc.sistema_gerenciador_trilha.util.Async;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;
import com.reynanfc.sistema_gerenciador_trilha.util.MapIcons;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Trail detail screen: loads one trail with its points by id, draws the route with start/end
 * pins, frames the camera on it and shows the summary. The name can be edited here too.
 * Flow and diagrams: docs/visualizar-trilha.md
 */
public class TrailDetailActivity extends AppCompatActivity {

    /** Id of the trail to show (long). Only the id travels in the Intent; the trail is loaded from the database */
    public static final String EXTRA_TRAIL_ID = "com.reynanfc.sistema_gerenciador_trilha.EXTRA_TRAIL_ID";

    private static final long NO_ID = -1;
    private static final float SINGLE_POINT_ZOOM = 17f;

    private long trailId;
    private SettingsPrefs settingsPrefs;
    private TrailDao trailDao;
    @Nullable
    private GoogleMap googleMap;
    @Nullable
    private Trail trail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trailId = getIntent().getLongExtra(EXTRA_TRAIL_ID, NO_ID);
        if (trailId == NO_ID) {
            closeWithError();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trail_detail);
        settingsPrefs = new SettingsPrefs(this);
        trailDao = new TrailDao(this);

        setupWindowInsets();
        findViewById(R.id.button_detail_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_detail_edit).setOnClickListener(v -> renameTrail());
        showSettingsSummary();
        setupMap();
        loadTrail();
    }

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(map -> {
                googleMap = map;
                googleMap.setMapType(settingsPrefs.getMapType() == SettingsPrefs.MapType.SATELLITE
                        ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                drawRouteWhenReady();
            });
        }
    }

    private void loadTrail() {
        Async.run(() -> trailDao.findById(trailId),
                loaded -> {
                    if (loaded == null) {
                        closeWithError();
                        return;
                    }
                    trail = loaded;
                    showInfo(loaded);
                    drawRouteWhenReady();
                },
                error -> closeWithError());
    }

    private void showInfo(Trail trail) {
        ((TextView) findViewById(R.id.text_detail_name)).setText(trail.getName());
        ((TextView) findViewById(R.id.text_detail_start)).setText(Formatters.dateTime(trail.getStartTime()));
        ((TextView) findViewById(R.id.text_detail_distance)).setText(
                getString(R.string.value_km, Formatters.distanceKm(trail.getDistance())));
        ((TextView) findViewById(R.id.text_detail_duration)).setText(Formatters.shortDuration(trail.getDurationMillis()));
        ((TextView) findViewById(R.id.text_detail_average_speed)).setText(
                getString(R.string.value_kmh, Formatters.speedKmh(trail.getAverageSpeed())));
        ((TextView) findViewById(R.id.text_detail_max_speed)).setText(
                getString(R.string.value_kmh, Formatters.speedKmh(trail.getMaxSpeed())));
    }

    private void showSettingsSummary() {
        ((TextView) findViewById(R.id.text_detail_settings)).setText(getString(R.string.record_settings_summary,
                getString(settingsPrefs.getMapType().labelResId),
                getString(settingsPrefs.getNavigationMode().labelResId)));

        // The legend line takes the same color as the route
        Drawable legendLine = ContextCompat.getDrawable(this, R.drawable.legend_line_route);
        if (legendLine != null) {
            legendLine = legendLine.mutate();
            legendLine.setTint(ContextCompat.getColor(this, routeColorRes()));
            ((TextView) findViewById(R.id.text_legend_route))
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(legendLine, null, null, null);
        }
    }

    // Same dialog as the list screen; the new name is saved here and shown right away
    private void renameTrail() {
        if (trail == null) {
            return;
        }
        Trail current = trail;
        RenameTrailDialog.show(this, current, newName ->
                Async.run(() -> trailDao.rename(current.getId(), newName),
                        renamed -> {
                            current.setName(newName);
                            ((TextView) findViewById(R.id.text_detail_name)).setText(newName);
                            Toast.makeText(this, R.string.rename_saved, Toast.LENGTH_SHORT).show();
                        },
                        error -> Toast.makeText(this, R.string.rename_error, Toast.LENGTH_LONG).show()));
    }

    // The map and the trail arrive asynchronously and in any order: draw once both exist
    private void drawRouteWhenReady() {
        if (googleMap == null || trail == null) {
            return;
        }
        List<TrackPoint> points = trail.getPoints();
        if (points.isEmpty()) {
            Toast.makeText(this, R.string.detail_no_points, Toast.LENGTH_LONG).show();
            return;
        }

        List<LatLng> route = new ArrayList<>(points.size());
        for (TrackPoint point : points) {
            route.add(new LatLng(point.latitude(), point.longitude()));
        }

        googleMap.addPolyline(new PolylineOptions()
                .addAll(route)
                .color(ContextCompat.getColor(this, routeColorRes()))
                .width(getResources().getDimension(R.dimen.route_width))
                .geodesic(true));

        // With a single point the start and end pins would overlap: show only the start
        addPin(route.get(0), R.drawable.ic_pin_start);
        if (route.size() > 1) {
            addPin(route.get(route.size() - 1), R.drawable.ic_pin_end);
        }

        // newLatLngBounds needs the map to have a size, so wait until it finished loading
        googleMap.setOnMapLoadedCallback(() -> frameRoute(route));
    }

    /**
     * Fits the whole route on screen. The map padding reserves the areas covered by the top bar and
     * the info panel, so the route is framed in the visible part between them.
     */
    private void frameRoute(List<LatLng> route) {
        View overlay = findViewById(R.id.detail_overlay);
        View topCard = findViewById(R.id.card_detail_top);
        View infoCard = findViewById(R.id.card_detail_info);
        googleMap.setPadding(0, topCard.getBottom(), 0, overlay.getHeight() - infoCard.getTop());

        if (route.size() == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(route.get(0), SINGLE_POINT_ZOOM));
            return;
        }
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (LatLng position : route) {
            bounds.include(position);
        }
        int padding = getResources().getDimensionPixelSize(R.dimen.detail_bounds_padding);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding));
    }

    // Blue reads well on the light vector map; on the dark satellite imagery yellow stands out
    private int routeColorRes() {
        return settingsPrefs.getMapType() == SettingsPrefs.MapType.SATELLITE
                ? R.color.route_satellite : R.color.route_vector;
    }

    private void addPin(LatLng position, int drawableResId) {
        googleMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(MapIcons.fromVector(this, drawableResId))
                .anchor(0.5f, 0.5f));
    }

    private void closeWithError() {
        Toast.makeText(this, R.string.detail_not_found, Toast.LENGTH_LONG).show();
        finish();
    }

    private void setupWindowInsets() {
        View overlay = findViewById(R.id.detail_overlay);

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
