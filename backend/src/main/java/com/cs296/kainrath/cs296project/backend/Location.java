package com.cs296.kainrath.cs296project.backend;



/**
 * Created by kainrath on 4/3/16.
 */

public class Location {
    public static final String LAT_FIELD = "Latitude";
    public static final String LONG_FIELD = "Longitude";
    public static final String ID_FIELD = "UserId";

    private String user_id;

    private double latitude;

    private double longitude;

    public Location () { }

    public Location (String user_id, double latitude, double longitude) {
        this.user_id = user_id;
        this.latitude = latitude;
        this.longitude = longitude;
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

    // Calculates distance between two locations in meters
    public double distanceTo(Location other) {
        double deg2rad = 0.017453292519943295;  // PI / 180 precomputed to save time
        double distance = 0.5 - Math.cos((other.latitude - this.latitude) * deg2rad)/2 +
                          Math.cos(this.latitude * deg2rad) * Math.cos(other.latitude * deg2rad) *
                          (1 - Math.cos((other.longitude - this.longitude) * deg2rad))/2;
        return 12742000 * Math.asin(Math.sqrt(distance)); // Earths radius in m (6371000) * 2 precomputed
    }

    // For comparing the distance between a User and a ChatGroup
    public double distanceTo(double lat, double lon) {
        double deg2rad = 0.017453292519943295;  // PI / 180 precomputed to save time
        double distance = 0.5 - Math.cos((lat - this.latitude) * deg2rad)/2 +
                Math.cos(this.latitude * deg2rad) * Math.cos(lat * deg2rad) *
                        (1 - Math.cos((lon - this.longitude) * deg2rad))/2;
        return 12742000 * Math.asin(Math.sqrt(distance)); // Earths radius in m (6371000) * 2 precomputed
    }
}
