package com.reynanfc.sistema_gerenciador_trilha.location;

import android.os.SystemClock;

/**
 * State of the trail being recorded, independent of any View: elapsed time with pauses.
 * Lives in a ViewModel so it survives screen rotation.
 *
 * Time uses SystemClock.elapsedRealtime(): it only moves forward, even if the user changes
 * the phone clock. The wall clock (System.currentTimeMillis) is used only for the start/end
 * dates saved in the database.
 */
public class TrailRecorder {

    private static final long NOT_RUNNING = -1;

    private boolean started;
    private long startTime;
    private long accumulatedMillis;
    private long runningSince = NOT_RUNNING;

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
