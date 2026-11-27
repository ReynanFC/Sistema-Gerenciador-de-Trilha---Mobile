package com.reynanfc.sistema_gerenciador_trilha.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

/**
 * Delivers GPS fixes to a listener while started. The screen decides when to start and stop
 * (onResume/onPause), so the GPS is off whenever the screen is not visible.
 */
public class LocationTracker {

    public interface Listener {
        void onLocation(@NonNull Location location);
    }

    private static final long UPDATE_INTERVAL_MILLIS = 2000;
    private static final long FASTEST_INTERVAL_MILLIS = 1000;

    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private final LocationRequest locationRequest;
    private final LocationCallback locationCallback;
    private boolean running;

    public LocationTracker(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        fusedClient = LocationServices.getFusedLocationProviderClient(this.context);

        // High accuracy uses the GPS chip; a fix every 1-2 s is enough detail for walking
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MILLIS)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) {
                    listener.onLocation(location);
                }
            }
        };
    }

    /** Fails with a ResolvableApiException when the device location is off; the screen can ask to turn it on. */
    public Task<LocationSettingsResponse> checkLocationSettings() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build();
        return LocationServices.getSettingsClient(context).checkLocationSettings(request);
    }

    // Callers only start after the location permission was granted
    @SuppressLint("MissingPermission")
    public void start() {
        if (running) {
            return;
        }
        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        running = true;
    }

    public void stop() {
        if (!running) {
            return;
        }
        fusedClient.removeLocationUpdates(locationCallback);
        running = false;
    }
}
