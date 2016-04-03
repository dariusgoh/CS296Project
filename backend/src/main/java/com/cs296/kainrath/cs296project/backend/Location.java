package com.cs296.kainrath.cs296project.backend;

/**
 * Created by kainrath on 4/3/16.
 */
public class Location {

    private String user_id;
    private double latitude;
    private double longitude;

    public Location (double latitude, double longitude, String user_id) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.user_id = user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}