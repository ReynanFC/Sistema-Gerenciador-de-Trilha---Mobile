package com.reynanfc.sistema_gerenciador_trilha.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates and upgrades the SQLite schema: one trail has many track points (1:N).
 * Android calls onCreate only the first time the database file is opened, and onUpgrade
 * when DATABASE_VERSION is higher than the version stored in the file.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trilha.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRAIL = "trail";
    public static final String COLUMN_TRAIL_ID = "id";
    public static final String COLUMN_TRAIL_NAME = "name";
    public static final String COLUMN_TRAIL_START_TIME = "start_time";
    public static final String COLUMN_TRAIL_END_TIME = "end_time";
    public static final String COLUMN_TRAIL_AVERAGE_SPEED = "average_speed";
    public static final String COLUMN_TRAIL_MAX_SPEED = "max_speed";
    public static final String COLUMN_TRAIL_DISTANCE = "distance";

    public static final String TABLE_TRACK_POINT = "track_point";
    public static final String COLUMN_POINT_ID = "id";
    public static final String COLUMN_POINT_TRAIL_ID = "trail_id";
    public static final String COLUMN_POINT_LATITUDE = "latitude";
    public static final String COLUMN_POINT_LONGITUDE = "longitude";
    public static final String COLUMN_POINT_SPEED = "speed";
    public static final String COLUMN_POINT_ACCURACY = "accuracy";
    public static final String COLUMN_POINT_TIMESTAMP = "timestamp";

    private static final String SQL_CREATE_TRAIL =
            "CREATE TABLE " + TABLE_TRAIL + " ("
                    + COLUMN_TRAIL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TRAIL_NAME + " TEXT NOT NULL, "
                    + COLUMN_TRAIL_START_TIME + " INTEGER NOT NULL, "
                    + COLUMN_TRAIL_END_TIME + " INTEGER NOT NULL, "
                    + COLUMN_TRAIL_AVERAGE_SPEED + " REAL NOT NULL, "
                    + COLUMN_TRAIL_MAX_SPEED + " REAL NOT NULL, "
                    + COLUMN_TRAIL_DISTANCE + " REAL NOT NULL)";


    private static final String SQL_CREATE_TRACK_POINT =
            "CREATE TABLE " + TABLE_TRACK_POINT + " ("
                    + COLUMN_POINT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_POINT_TRAIL_ID + " INTEGER NOT NULL, "
                    + COLUMN_POINT_LATITUDE + " REAL NOT NULL, "
                    + COLUMN_POINT_LONGITUDE + " REAL NOT NULL, "
                    + COLUMN_POINT_SPEED + " REAL NOT NULL, "
                    + COLUMN_POINT_ACCURACY + " REAL NOT NULL, "
                    + COLUMN_POINT_TIMESTAMP + " INTEGER NOT NULL, "
                    + "FOREIGN KEY (" + COLUMN_POINT_TRAIL_ID + ") REFERENCES "
                    + TABLE_TRAIL + "(" + COLUMN_TRAIL_ID + ") ON DELETE CASCADE)";

    private static final String SQL_CREATE_POINT_INDEX =
            "CREATE INDEX idx_point_trail ON " + TABLE_TRACK_POINT + "(" + COLUMN_POINT_TRAIL_ID + ")";

    public DbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TRAIL);
        db.execSQL(SQL_CREATE_TRACK_POINT);
        db.execSQL(SQL_CREATE_POINT_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACK_POINT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAIL);
        onCreate(db);
    }
}
