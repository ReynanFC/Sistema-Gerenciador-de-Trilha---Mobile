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
 * Entrega as posições do GPS a um listener enquanto estiver ligado. A tela decide quando ligar e
 * desligar (onResume/onPause), então o GPS fica desligado sempre que a tela não está visível.
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

        // Alta precisão usa o chip de GPS; uma posição a cada 1-2 s é detalhe suficiente para caminhada
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

    /** Falha com ResolvableApiException quando a localização do aparelho está desligada; a tela pode pedir para ligar. */
    public Task<LocationSettingsResponse> checkLocationSettings() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build();
        return LocationServices.getSettingsClient(context).checkLocationSettings(request);
    }

    // Quem chama só liga depois que a permissão de localização foi concedida
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
