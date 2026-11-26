package com.reynanfc.sistema_gerenciador_trilha.model;

import java.util.ArrayList;
import java.util.List;

// Times in epoch milliseconds, speeds in m/s and distance in meters (converted only for display)
public class Trail {

    private long id;
    private String name;
    private long startTime;
    private long endTime;
    private double averageSpeed;
    private double maxSpeed;
    private double distance;
    private List<TrackPoint> points = new ArrayList<>();

    public Trail() {
    }

    public Trail(String name, long startTime, long endTime,
                 double averageSpeed, double maxSpeed, double distance) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.averageSpeed = averageSpeed;
        this.maxSpeed = maxSpeed;
        this.distance = distance;
    }

    public long getDurationMillis() {
        return endTime - startTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public List<TrackPoint> getPoints() {
        return points;
    }

    public void setPoints(List<TrackPoint> points) {
        this.points = points;
    }
}
