package com.reynanfc.sistema_gerenciador_trilha.location;

import android.location.Location;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.reynanfc.sistema_gerenciador_trilha.model.TrackPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * State of the trail being recorded, independent of any View: elapsed time with pauses, speeds,
 * distance and the accepted points.
 * Lives in a ViewModel so it survives screen rotation.
 *
 * Time uses SystemClock.elapsedRealtime(): it only moves forward, even if the user changes
 * the phone clock. The wall clock (System.currentTimeMillis) is used only for the start/end
 * dates saved in the database.
 */
public class TrailRecorder {

    private static final long NOT_RUNNING = -1;

    /** Fixes less accurate than this (meters) do not count for max speed, distance or points */
    public static final float MAX_ACCEPTED_ACCURACY = 25f;

    /** Smallest movement (meters) that counts as distance, so GPS jitter does not add up while standing */
    private static final float MIN_STEP_METERS = 5f;

    private boolean started;
    private long startTime;
    private long accumulatedMillis;
    private long runningSince = NOT_RUNNING;
    private float currentSpeed;
    private float maxSpeed;
    private double distance;
    private final List<TrackPoint> points = new ArrayList<>();
    @Nullable
    private Location lastAccepted;

    public boolean isStarted() {
        return started;
    }

    public boolean isRunning() {
        return runningSince != NOT_RUNNING;
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        startTime = System.currentTimeMillis();
        runningSince = SystemClock.elapsedRealtime();
    }

    public void pause() {
        if (!isRunning()) {
            return;
        }
        accumulatedMillis += SystemClock.elapsedRealtime() - runningSince;
        runningSince = NOT_RUNNING;
    }

    public void resume() {
        if (!started || isRunning()) {
            return;
        }
        runningSince = SystemClock.elapsedRealtime();
        // What the user walked while paused must not count: the next fix starts a new segment
        lastAccepted = null;
    }

    /** Feeds a GPS fix into the recording. Paused fixes still update the current speed only. */
    public void addLocation(Location location) {
        // Not every fix carries a speed (e.g. network location); treat it as standing still
        currentSpeed = location.hasSpeed() ? location.getSpeed() : 0f;

        if (!isRunning() || location.getAccuracy() > MAX_ACCEPTED_ACCURACY) {
            return;
        }
        // Inaccurate fixes are skipped above because they produce speed spikes and fake distance
        maxSpeed = Math.max(maxSpeed, currentSpeed);

        if (lastAccepted == null) {
            acceptPoint(location);
            return;
        }

        // A fix only counts once we moved more than its own error radius (and at least MIN_STEP),
        // otherwise standing still would slowly add distance from the GPS wandering around
        float step = lastAccepted.distanceTo(location);
        if (step >= Math.max(MIN_STEP_METERS, location.getAccuracy())) {
            distance += step;
            acceptPoint(location);
        }
    }

    private void acceptPoint(Location location) {
        points.add(new TrackPoint(location.getLatitude(), location.getLongitude(),
                currentSpeed, location.getAccuracy(), location.getTime()));
        lastAccepted = location;
    }

    /** Meters */
    public double getDistance() {
        return distance;
    }

    /** Accepted points in recording order, to be saved with the trail */
    public List<TrackPoint> getPoints() {
        return points;
    }

    /** m/s */
    public float getCurrentSpeed() {
        return currentSpeed;
    }

    /** m/s */
    public float getMaxSpeed() {
        return maxSpeed;
    }

    /** Recorded time without the pauses */
    public long getElapsedMillis() {
        long current = isRunning() ? SystemClock.elapsedRealtime() - runningSince : 0;
        return accumulatedMillis + current;
    }

    /** Wall clock time of the start, in epoch millis */
    public long getStartTime() {
        return startTime;
    }
}
