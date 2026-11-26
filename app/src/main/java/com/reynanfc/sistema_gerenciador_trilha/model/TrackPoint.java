package com.reynanfc.sistema_gerenciador_trilha.model;

// One GPS fix of a trail: speed in m/s, accuracy in meters, timestamp in epoch milliseconds
public class TrackPoint {

    private long id;
    private long trailId;
    private double latitude;
    private double longitude;
    private float speed;
    private float accuracy;
    private long timestamp;

    public TrackPoint() {
    }

    public TrackPoint(double latitude, double longitude, float speed, float accuracy, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTrailId() {
        return trailId;
    }

    public void setTrailId(long trailId) {
        this.trailId = trailId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
