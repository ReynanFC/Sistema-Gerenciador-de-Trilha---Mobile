package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.database.TrailDao;
import com.reynanfc.sistema_gerenciador_trilha.location.LocationTracker;
import com.reynanfc.sistema_gerenciador_trilha.location.TrailRecorder;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Async;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;
import com.reynanfc.sistema_gerenciador_trilha.util.MapIcons;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

import java.util.ArrayList;

public class RecordTrailActivity extends AppCompatActivity {

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final float INITIAL_ZOOM = 17f;
    private static final long TIMER_TICK_MILLIS = 500;
    /** m/s (~1.8 km/h): below this the GPS bearing is noise */
    private static final float MIN_SPEED_FOR_BEARING = 0.5f;

    private SettingsPrefs settingsPrefs;
    @Nullable
    private GoogleMap googleMap;
    private Chip chipAccuracy;
    private LocationTracker locationTracker;
    private boolean hasLocationPermission;
    private boolean cameraCentered;
    @Nullable
    private Marker positionMarker;
    @Nullable
    private Circle accuracyCircle;
    private TrailRecorder recorder;
    private TextView textTimer;
    private TextView textSpeed;
    private TextView textMaxSpeed;
    private TextView textDistance;
    private TextView textRecordingStatus;
    private MaterialButton buttonPause;
    private ImageButton buttonCompass;

    // Leaving after the recording started would lose the trail, so ask first
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (recorder.isStarted()) {
                confirmDiscard();
            } else {
                finish();
            }
        }
    };

    // Refreshes the timer text while the screen is visible (started in onResume, removed in onPause)
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            textTimer.setText(Formatters.duration(recorder.getElapsedMillis()));
            timerHandler.postDelayed(this, TIMER_TICK_MILLIS);
        }
    };

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
        textTimer = findViewById(R.id.text_timer);
        textSpeed = findViewById(R.id.text_speed);
        textMaxSpeed = findViewById(R.id.text_max_speed);
        textDistance = findViewById(R.id.text_distance);
        textRecordingStatus = findViewById(R.id.text_recording_status);
        buttonPause = findViewById(R.id.button_pause);
        buttonCompass = findViewById(R.id.button_compass);
        recorder = new ViewModelProvider(this).get(RecordTrailViewModel.class).recorder;
        locationTracker = new LocationTracker(this, this::onLocation);

        setupWindowInsets();
        findViewById(R.id.button_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, backCallback);
        findViewById(R.id.button_finish).setOnClickListener(v -> onFinishClicked());
        showCurrentSettings();
        buttonPause.setOnClickListener(v -> togglePause());
        buttonCompass.setOnClickListener(v -> resetBearing());
        renderRecordingState();
        renderStats();
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
        timerHandler.post(timerTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationTracker.stop();
        timerHandler.removeCallbacks(timerTick);
    }

    private void onFinishClicked() {
        if (recorder.getPoints().isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.finish_no_points_title)
                    .setMessage(R.string.finish_no_points_message)
                    .setNegativeButton(R.string.finish_discard, (d, which) -> finish())
                    .setPositiveButton(R.string.finish_keep_recording, null)
                    .show();
            return;
        }

        // Freeze the numbers while the user types the name; cancelling the sheet resumes
        boolean wasRunning = recorder.isRunning();
        recorder.pause();
        renderRecordingState();
        showFinishSheet(wasRunning, System.currentTimeMillis());
    }

    private void showFinishSheet(boolean wasRunning, long endTime) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        // null parent: the dialog becomes the parent when the view is set on it
        View content = getLayoutInflater().inflate(R.layout.bottom_sheet_finish_trail, null);
        sheet.setContentView(content);
        sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        sheet.getBehavior().setSkipCollapsed(true);

        long elapsed = recorder.getElapsedMillis();
        double averageSpeed = averageSpeed(recorder.getDistance(), elapsed);

        ((TextView) content.findViewById(R.id.text_finish_start)).setText(Formatters.dateTime(recorder.getStartTime()));
        ((TextView) content.findViewById(R.id.text_finish_end)).setText(Formatters.dateTime(endTime));
        // Duration = end - start (what the list and detail show); the timer above shows moving time only
        ((TextView) content.findViewById(R.id.text_finish_duration)).setText(
                Formatters.duration(endTime - recorder.getStartTime()));
        ((TextView) content.findViewById(R.id.text_finish_distance)).setText(
                getString(R.string.value_km, Formatters.distanceKm(recorder.getDistance())));
        ((TextView) content.findViewById(R.id.text_finish_average_speed)).setText(
                getString(R.string.value_kmh, Formatters.speedKmh(averageSpeed)));
        ((TextView) content.findViewById(R.id.text_finish_max_speed)).setText(
                getString(R.string.value_kmh, Formatters.speedKmh(recorder.getMaxSpeed())));

        TextInputLayout layoutName = content.findViewById(R.id.layout_trail_name);
        TextInputEditText editName = content.findViewById(R.id.edit_trail_name);
        MaterialButton buttonSave = content.findViewById(R.id.button_save_trail);

        sheet.setOnCancelListener(d -> {
            if (wasRunning) {
                recorder.resume();
                renderRecordingState();
            }
        });
        content.findViewById(R.id.button_discard).setOnClickListener(v -> confirmDiscard());
        buttonSave.setOnClickListener(v -> {
            String name = editName.getText() == null ? "" : editName.getText().toString().trim();
            if (name.isEmpty()) {
                layoutName.setError(getString(R.string.finish_name_required));
                return;
            }
            layoutName.setError(null);
            buttonSave.setEnabled(false);
            saveTrail(buildTrail(name, endTime, averageSpeed), sheet, buttonSave);
        });

        sheet.show();
    }

    /** Distance over moving time (pauses excluded), in m/s */
    private static double averageSpeed(double distanceMeters, long elapsedMillis) {
        return elapsedMillis > 0 ? distanceMeters / (elapsedMillis / 1000.0) : 0;
    }

    private Trail buildTrail(String name, long endTime, double averageSpeed) {
        Trail trail = new Trail(name, recorder.getStartTime(), endTime,
                averageSpeed, recorder.getMaxSpeed(), recorder.getDistance());
        trail.setPoints(new ArrayList<>(recorder.getPoints()));
        return trail;
    }

    // The insert of hundreds of points runs on a background thread so the UI never freezes
    private void saveTrail(Trail trail, BottomSheetDialog sheet, MaterialButton buttonSave) {
        TrailDao trailDao = new TrailDao(this);
        Async.run(() -> trailDao.insert(trail),
                trailId -> {
                    Toast.makeText(this, R.string.finish_saved, Toast.LENGTH_SHORT).show();
                    sheet.dismiss();
                    finish();
                },
                error -> {
                    buttonSave.setEnabled(true);
                    Toast.makeText(this, R.string.finish_save_error, Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDiscard() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.finish_discard_title)
                .setMessage(R.string.finish_discard_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.finish_discard, (d, which) -> finish())
                .show();
    }

    private void togglePause() {
        if (recorder.isRunning()) {
            recorder.pause();
        } else {
            recorder.resume();
        }
        renderRecordingState();
    }

    // Status pill and Pause/Resume button; before the first GPS fix there is nothing to pause
    private void renderRecordingState() {
        if (!recorder.isStarted()) {
            setRecordingStatus(R.string.record_waiting_gps, R.drawable.dot_waiting);
        } else if (recorder.isRunning()) {
            setRecordingStatus(R.string.record_recording, R.drawable.dot_recording);
        } else {
            setRecordingStatus(R.string.record_paused, R.drawable.dot_waiting);
        }

        buttonPause.setEnabled(recorder.isStarted());
        boolean paused = recorder.isStarted() && !recorder.isRunning();
        buttonPause.setText(paused ? R.string.record_resume : R.string.record_pause);
        buttonPause.setIconResource(paused ? R.drawable.ic_play : R.drawable.ic_pause);
    }

    private void renderStats() {
        textSpeed.setText(Formatters.speedKmh(recorder.getCurrentSpeed()));
        textMaxSpeed.setText(Formatters.speedKmh(recorder.getMaxSpeed()));
        textDistance.setText(Formatters.distanceKm(recorder.getDistance()));
    }

    private void setRecordingStatus(@StringRes int textResId, @DrawableRes int dotResId) {
        textRecordingStatus.setText(textResId);
        textRecordingStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(dotResId, 0, 0, 0);
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
        // Recording starts with the first fix, so the start time and the first point match
        if (!recorder.isStarted()) {
            recorder.start();
            renderRecordingState();
        }
        recorder.addLocation(location);
        renderStats();

        chipAccuracy.setText(getString(R.string.record_accuracy, Math.round(location.getAccuracy())));

        if (googleMap == null) {
            return;
        }
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        updatePositionMarker(position, location);
        updateAccuracyCircle(position, location.getAccuracy());

        if (cameraCentered) {
            followCamera(position, location);
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, INITIAL_ZOOM));
            cameraCentered = true;
        }
    }

    /**
     * Keeps the user in the center. North Up keeps bearing 0; Course Up turns the map so the
     * walking direction points up. The bearing is only trusted while moving, because standing
     * still makes the GPS heading jump around.
     */
    private void followCamera(LatLng position, Location location) {
        CameraPosition current = googleMap.getCameraPosition();
        float bearing = 0f;
        if (isCourseUp()) {
            boolean reliableBearing = location.hasBearing() && recorder.getCurrentSpeed() >= MIN_SPEED_FOR_BEARING;
            bearing = reliableBearing ? location.getBearing() : current.bearing;
        }
        CameraPosition target = new CameraPosition.Builder(current)
                .target(position)
                .bearing(bearing)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(target));
    }

    private boolean isCourseUp() {
        return settingsPrefs.getNavigationMode() == SettingsPrefs.NavigationMode.COURSE_UP;
    }

    // Compass: back to north (Course Up turns again on the next fix while walking)
    private void resetBearing() {
        if (googleMap == null) {
            return;
        }
        CameraPosition north = new CameraPosition.Builder(googleMap.getCameraPosition()).bearing(0f).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(north));
    }

    // Radius = GPS accuracy in meters: the real position is inside the circle with ~68% probability
    private void updateAccuracyCircle(LatLng center, float accuracyMeters) {
        if (accuracyCircle == null) {
            accuracyCircle = googleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(accuracyMeters)
                    .fillColor(ContextCompat.getColor(this, R.color.accuracy_fill))
                    .strokeColor(ContextCompat.getColor(this, R.color.accuracy_stroke))
                    .strokeWidth(getResources().getDimension(R.dimen.accuracy_stroke_width)));
        } else {
            accuracyCircle.setCenter(center);
            accuracyCircle.setRadius(accuracyMeters);
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
        // The orientation comes from the North Up / Course Up setting, not from two-finger rotation
        uiSettings.setRotateGesturesEnabled(false);

        // The compass needle points to the real north: rotate the icon against the map bearing
        googleMap.setOnCameraMoveListener(() ->
                buttonCompass.setRotation(-googleMap.getCameraPosition().bearing));
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
