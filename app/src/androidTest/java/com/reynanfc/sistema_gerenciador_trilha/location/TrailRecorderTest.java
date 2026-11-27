package com.reynanfc.sistema_gerenciador_trilha.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.location.Location;
import android.location.LocationManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TrailRecorderTest {

    // ~1,11 m a cada 0,00001 grau de latitude
    private static final double BASE_LAT = -12.9;
    private static final double BASE_LNG = -38.3;

    private TrailRecorder recorder;

    @Before
    public void setUp() {
        recorder = new TrailRecorder();
        recorder.start();
    }

    @Test
    public void standingStillDoesNotAddDistance() {
        // GPS oscilando ~3 m em volta do mesmo lugar, com 8 m de precisão
        double[] jitter = {0, 0.00002, -0.00001, 0.00003, -0.00002, 0.00001};
        for (double offset : jitter) {
            recorder.addLocation(fix(BASE_LAT + offset, BASE_LNG, 8f, 0f));
        }

        assertEquals(0.0, recorder.getDistance(), 0.0);
        assertEquals(1, recorder.getPoints().size());
    }

    @Test
    public void walkingAddsTheDistanceBetweenAcceptedPoints() {
        for (int i = 0; i <= 10; i++) {
            recorder.addLocation(fix(BASE_LAT + i * 0.0001, BASE_LNG, 5f, 1.4f)); // passos de ~11 m
        }

        assertEquals(111.0, recorder.getDistance(), 3.0);
        assertEquals(11, recorder.getPoints().size());
    }

    @Test
    public void inaccurateFixesAreIgnored() {
        recorder.addLocation(fix(BASE_LAT, BASE_LNG, 5f, 1f));
        recorder.addLocation(fix(BASE_LAT + 0.001, BASE_LNG, 60f, 40f)); // salto de 111 m, precisão de 60 m

        assertEquals(0.0, recorder.getDistance(), 0.0);
        assertEquals(1f, recorder.getMaxSpeed(), 0f);
    }

    @Test
    public void walkingWhilePausedIsNotCounted() {
        recorder.addLocation(fix(BASE_LAT, BASE_LNG, 5f, 1f));
        recorder.pause();
        recorder.addLocation(fix(BASE_LAT + 0.001, BASE_LNG, 5f, 1f));
        recorder.resume();
        recorder.addLocation(fix(BASE_LAT + 0.001, BASE_LNG, 5f, 1f));
        recorder.addLocation(fix(BASE_LAT + 0.0011, BASE_LNG, 5f, 1f));

        assertEquals(11.0, recorder.getDistance(), 1.0);
    }

    @Test
    public void pauseFreezesTheTimer() throws InterruptedException {
        recorder.pause();
        long frozen = recorder.getElapsedMillis();
        Thread.sleep(300);

        assertEquals(frozen, recorder.getElapsedMillis());
        recorder.resume();
        Thread.sleep(300);
        assertTrue(recorder.getElapsedMillis() >= frozen + 250);
    }

    private Location fix(double lat, double lng, float accuracy, float speed) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setAccuracy(accuracy);
        location.setSpeed(speed);
        location.setTime(System.currentTimeMillis());
        return location;
    }
}
