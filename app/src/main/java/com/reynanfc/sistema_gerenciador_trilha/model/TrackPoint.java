package com.reynanfc.sistema_gerenciador_trilha.model;

/**
 * One GPS fix of a trail: speed in m/s, accuracy in meters, timestamp in epoch milliseconds.
 * Immutable record: the database id and the owning trail id are not part of the value,
 * the DAO receives the trail id when inserting the points.
 */
public record TrackPoint(double latitude, double longitude, float speed, float accuracy, long timestamp) {
}
