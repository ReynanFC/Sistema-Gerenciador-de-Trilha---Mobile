package com.reynanfc.sistema_gerenciador_trilha.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

/**
 * Cria e atualiza o esquema SQLite: uma trilha tem muitos pontos (1:N).
 * O Android chama onCreate só na primeira vez que o arquivo do banco é aberto, e onUpgrade
 * quando DATABASE_VERSION é maior que a versão gravada no arquivo.
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
        this(context, DATABASE_NAME);
    }

    /** @param databaseName nome do arquivo, ou null para um banco em memória (usado pelos testes) */
    public DbHelper(Context context, @Nullable String databaseName) {
        super(context.getApplicationContext(), databaseName, null, DATABASE_VERSION);
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
