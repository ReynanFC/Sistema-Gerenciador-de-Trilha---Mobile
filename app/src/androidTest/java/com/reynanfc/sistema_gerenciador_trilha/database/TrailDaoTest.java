package com.reynanfc.sistema_gerenciador_trilha.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.model.TrackPoint;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TrailDaoTest {

    private static final String DATABASE_NAME = "trilha.db";

    private Context context;
    private DbHelper dbHelper;
    private TrailDao trailDao;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
        dbHelper = new DbHelper(context);
        trailDao = new TrailDao(context);
    }

    @After
    public void tearDown() {
        dbHelper.close();
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void insertSavesTrailWithAllPointsQuickly() {
        Trail trail = newTrail(1000);

        long start = System.currentTimeMillis();
        long trailId = trailDao.insert(trail);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(trailId > 0);
        assertEquals(trailId, trail.getId());
        assertEquals(1, countRows(DbHelper.TABLE_TRAIL));
        assertEquals(1000, countRows(DbHelper.TABLE_TRACK_POINT));
        assertTrue("1000 points took " + elapsed + " ms", elapsed < 2000);
    }

    // SQLite stores NaN as NULL, so the last point violates NOT NULL after 999 successful inserts
    @Test
    public void failureInTheMiddleSavesNothing() {
        Trail trail = newTrail(999);
        trail.getPoints().add(new TrackPoint(Double.NaN, 0, 0, 0, 0));

        assertThrows(SQLiteConstraintException.class, () -> trailDao.insert(trail));

        assertEquals(0, countRows(DbHelper.TABLE_TRAIL));
        assertEquals(0, countRows(DbHelper.TABLE_TRACK_POINT));
    }

    @Test
    public void findAllReturnsNewestFirstWithoutPoints() {
        Trail older = newTrail(3);
        older.setStartTime(1000);
        Trail newer = newTrail(3);
        newer.setStartTime(5000);
        trailDao.insert(older);
        trailDao.insert(newer);

        List<Trail> trails = trailDao.findAll();

        assertEquals(2, trails.size());
        assertEquals(newer.getId(), trails.get(0).getId());
        assertEquals(older.getId(), trails.get(1).getId());
        assertTrue(trails.get(0).getPoints().isEmpty());
    }

    @Test
    public void findByIdReturnsTrailWithPointsSortedByTimestamp() {
        Trail trail = newTrail(0);
        trail.getPoints().add(new TrackPoint(2, 2, 0, 0, 2000));
        trail.getPoints().add(new TrackPoint(1, 1, 0, 0, 1000));
        long trailId = trailDao.insert(trail);

        Trail found = trailDao.findById(trailId);

        assertNotNull(found);
        assertEquals("Test trail", found.getName());
        assertEquals(trail.getDistance(), found.getDistance(), 0.0);
        assertEquals(2, found.getPoints().size());
        assertEquals(1000, found.getPoints().get(0).timestamp());
        assertEquals(2000, found.getPoints().get(1).timestamp());
    }

    @Test
    public void findByIdReturnsNullForUnknownId() {
        assertNull(trailDao.findById(42));
    }

    private Trail newTrail(int pointCount) {
        List<TrackPoint> points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            points.add(new TrackPoint(-12.9 + i * 0.0001, -38.3, 1.5f, 5f, i * 1000L));
        }
        Trail trail = new Trail("Test trail", 0, pointCount * 1000L, 1.2, 3.0, 500);
        trail.setPoints(points);
        return trail;
    }

    private long countRows(String table) {
        return DatabaseUtils.queryNumEntries(dbHelper.getReadableDatabase(), table);
    }
}
