package com.reynanfc.sistema_gerenciador_trilha.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.model.TrackPoint;

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
