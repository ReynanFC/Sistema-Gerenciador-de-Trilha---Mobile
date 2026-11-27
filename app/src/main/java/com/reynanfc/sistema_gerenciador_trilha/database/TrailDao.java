package com.reynanfc.sistema_gerenciador_trilha.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.model.TrackPoint;

import java.util.ArrayList;
import java.util.List;

public class TrailDao {

    private final DbHelper dbHelper;

    public TrailDao(Context context) {
        dbHelper = new DbHelper(context);
    }

    /**
     * Saves the trail and all its points atomically: if any insert fails, nothing is saved.
     * The single transaction also makes bulk inserts fast, since SQLite writes to disk once
     * at the commit instead of once per row.
     *
     * @return the generated trail id, also set on {@code trail}
     */
    public long insert(Trail trail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long trailId;

        db.beginTransaction();
        try {
            trailId = db.insertOrThrow(DbHelper.TABLE_TRAIL, null, toContentValues(trail));

            ContentValues pointValues = new ContentValues();
            for (TrackPoint point : trail.getPoints()) {
                fillPointValues(pointValues, trailId, point);
                db.insertOrThrow(DbHelper.TABLE_TRACK_POINT, null, pointValues);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        trail.setId(trailId);
        return trailId;
    }

    // Points are not loaded here: the list screen only shows the summary
    public List<Trail> findAll() {
        List<Trail> trails = new ArrayList<>();
        try (Cursor cursor = dbHelper.getReadableDatabase().query(
                DbHelper.TABLE_TRAIL, null, null, null, null, null,
                DbHelper.COLUMN_TRAIL_START_TIME + " DESC")) {
            while (cursor.moveToNext()) {
                trails.add(toTrail(cursor));
            }
        }
        return trails;
    }

    /** @return the trail with its points, or {@code null} if no trail has this id */
    @Nullable
    public Trail findById(long trailId) {
        Trail trail;
        try (Cursor cursor = dbHelper.getReadableDatabase().query(
                DbHelper.TABLE_TRAIL, null,
                DbHelper.COLUMN_TRAIL_ID + " = ?", new String[]{String.valueOf(trailId)},
                null, null, null)) {

            if (!cursor.moveToFirst()) return null;

            trail = toTrail(cursor);
        }
        trail.setPoints(findPoints(trailId));
        return trail;
    }

    public List<TrackPoint> findPoints(long trailId) {
        List<TrackPoint> points = new ArrayList<>();
        try (Cursor cursor = dbHelper.getReadableDatabase().query(
                DbHelper.TABLE_TRACK_POINT, null,
                DbHelper.COLUMN_POINT_TRAIL_ID + " = ?", new String[]{String.valueOf(trailId)},
                null, null,
                DbHelper.COLUMN_POINT_TIMESTAMP + " ASC")) {
            while (cursor.moveToNext()) {
                points.add(toTrackPoint(cursor));
            }
        }
        return points;
    }

    private Trail toTrail(Cursor cursor) {
        Trail trail = new Trail(
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TRAIL_NAME)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TRAIL_START_TIME)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TRAIL_END_TIME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TRAIL_AVERAGE_SPEED)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TRAIL_MAX_SPEED)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TRAIL_DISTANCE))
        );
        trail.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_TRAIL_ID)));
        return trail;
    }

    private TrackPoint toTrackPoint(Cursor cursor) {
        return new TrackPoint(
                cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_POINT_LATITUDE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_POINT_LONGITUDE)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_POINT_SPEED)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_POINT_ACCURACY)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.COLUMN_POINT_TIMESTAMP))
        );
    }

    public long count() {
        return DatabaseUtils.queryNumEntries(dbHelper.getReadableDatabase(), DbHelper.TABLE_TRAIL);
    }

    /** @return the number of renamed trails: 1, or 0 if no trail has this id */
    public int rename(long trailId, String newName) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_TRAIL_NAME, newName);
        return dbHelper.getWritableDatabase().update(
                DbHelper.TABLE_TRAIL, values,
                DbHelper.COLUMN_TRAIL_ID + " = ?", new String[]{String.valueOf(trailId)});
    }

    // The delete methods only touch the trail table: ON DELETE CASCADE removes the points
    public int delete(long trailId) {
        return dbHelper.getWritableDatabase().delete(
                DbHelper.TABLE_TRAIL,
                DbHelper.COLUMN_TRAIL_ID + " = ?", new String[]{String.valueOf(trailId)});
    }

    /**
     * Deletes the trails that started between the two instants, both inclusive (SQL BETWEEN).
     * The screen converts the picked dates to the start of the first day and the end of the last one.
     *
     * @return the number of deleted trails
     */
    public int deleteBetween(long fromMillis, long toMillis) {
        return dbHelper.getWritableDatabase().delete(
                DbHelper.TABLE_TRAIL,
                DbHelper.COLUMN_TRAIL_START_TIME + " BETWEEN ? AND ?",
                new String[]{String.valueOf(fromMillis), String.valueOf(toMillis)});
    }

    public int deleteAll() {
        return dbHelper.getWritableDatabase().delete(DbHelper.TABLE_TRAIL, "1", null);
    }

    private ContentValues toContentValues(Trail trail) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_TRAIL_NAME, trail.getName());
        values.put(DbHelper.COLUMN_TRAIL_START_TIME, trail.getStartTime());
        values.put(DbHelper.COLUMN_TRAIL_END_TIME, trail.getEndTime());
        values.put(DbHelper.COLUMN_TRAIL_AVERAGE_SPEED, trail.getAverageSpeed());
        values.put(DbHelper.COLUMN_TRAIL_MAX_SPEED, trail.getMaxSpeed());
        values.put(DbHelper.COLUMN_TRAIL_DISTANCE, trail.getDistance());
        return values;
    }

    private void fillPointValues(ContentValues values, long trailId, TrackPoint point) {
        values.put(DbHelper.COLUMN_POINT_TRAIL_ID, trailId);
        values.put(DbHelper.COLUMN_POINT_LATITUDE, point.latitude());
        values.put(DbHelper.COLUMN_POINT_LONGITUDE, point.longitude());
        values.put(DbHelper.COLUMN_POINT_SPEED, point.speed());
        values.put(DbHelper.COLUMN_POINT_ACCURACY, point.accuracy());
        values.put(DbHelper.COLUMN_POINT_TIMESTAMP, point.timestamp());
    }
}
