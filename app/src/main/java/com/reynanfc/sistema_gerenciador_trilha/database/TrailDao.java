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
        this(new DbHelper(context));
    }

    public TrailDao(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Salva a trilha e todos os seus pontos de forma atômica: se algum insert falhar, nada é salvo.
     * A transação única também deixa a inserção em massa rápida, porque o SQLite grava no disco
     * uma vez, no commit, em vez de uma vez por linha.
     *
     * @return o id gerado da trilha, também definido em {@code trail}
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

    // Os pontos não são carregados aqui: a lista só mostra o resumo
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

    /** @return a trilha com seus pontos, ou {@code null} se nenhuma trilha tiver esse id */
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

    /** @return quantas trilhas foram renomeadas: 1, ou 0 se nenhuma trilha tiver esse id */
    public int rename(long trailId, String newName) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_TRAIL_NAME, newName);
        return dbHelper.getWritableDatabase().update(
                DbHelper.TABLE_TRAIL, values,
                DbHelper.COLUMN_TRAIL_ID + " = ?", new String[]{String.valueOf(trailId)});
    }

    // Os métodos de apagar só mexem na tabela trail: o ON DELETE CASCADE remove os pontos
    public int delete(long trailId) {
        return dbHelper.getWritableDatabase().delete(
                DbHelper.TABLE_TRAIL,
                DbHelper.COLUMN_TRAIL_ID + " = ?", new String[]{String.valueOf(trailId)});
    }

    /**
     * Apaga as trilhas que começaram entre os dois instantes, ambos incluídos (SQL BETWEEN).
     * A tela converte as datas escolhidas para o início do primeiro dia e o fim do último.
     *
     * @return quantas trilhas foram apagadas
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
