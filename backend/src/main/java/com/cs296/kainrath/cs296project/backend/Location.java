package com.cs296.kainrath.cs296project.backend;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by kainrath on 4/3/16.
 */

@DatabaseTable(tableName = "Location")
public class Location {

    @DatabaseField(id = true)
    private String user_id;

    @DatabaseField(index = true)
    private double latitude;

    @DatabaseField(index = true)
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
}
