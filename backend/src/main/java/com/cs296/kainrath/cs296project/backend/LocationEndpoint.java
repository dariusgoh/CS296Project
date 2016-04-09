package com.cs296.kainrath.cs296project.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

/**
 * An endpoint class we are exposing
 */
@Api(
        name = "locationApi",
        version = "v1",
        resource = "location",
        namespace = @ApiNamespace(
                ownerDomain = "backend.cs296project.kainrath.cs296.com",
                ownerName = "backend.cs296project.kainrath.cs296.com",
                packagePath = ""
        )
)
public class LocationEndpoint {

    private static final Logger logger = Logger.getLogger(LocationEndpoint.class.getName());
    private static final String url = "jdbc:mysql://cs296-backend:cs296-app-location-data/UserLocation/location?user=root";
    private static final double DIST = 50;
    private static final double RAD_EARTH = 6371000;
    private static final double ANG_DIST = DIST / RAD_EARTH;

    /**
     * This method gets the <code>Location</code> object associated with the specified <code>id</code>.
     *
     * @param id The id of the object to be returned.
     * @return The <code>Location</code> associated with <code>id</code>.
     */
    @ApiMethod(name = "getLocation")
    public Location getLocation(@Named("id") String id) {
        logger.info("Calling getLocation method");
        ConnectionSource connectionSource;
        try {
            connectionSource = new JdbcConnectionSource(url);
        } catch (SQLException e) {
            return null;
        }
        Dao<Location, String> dao;
        try {
            dao = DaoManager.createDao(connectionSource, Location.class);
        } catch (SQLException e) {
            return null;
        }
        Location location;
        try {
            location = dao.queryForId(id);
        } catch (SQLException e) {
            return null;
        }
        try {
            connectionSource.close();
        } catch (SQLException e) {

        }
        return location;

    }

    /**
     * This inserts a new <code>Location</code> object.
     *
     * @param user_id   The user to be updated.
     * @param latitude  The latitude of the user.
     * @param longitude The longitude of the user.
     * @return The object to be added.
     */
    @ApiMethod(name = "updateLocation")
    public List<Location> updateLocation(@Named("user_id") String user_id, @Named("latitude") double latitude, @Named("longitude") double longitude) {
        Location location = new Location(user_id, latitude, longitude);
        logger.info("Calling insertLocation method");
        ConnectionSource connectionSource;
        List<Location> nearby_users = new ArrayList<Location>();
        nearby_users.add(location); // 1
        String err_msg;
        int err_code;
        String err_sql;
        try {
            connectionSource = new JdbcConnectionSource(url);
        } catch (SQLException e) {
            // return null
            return nearby_users;
        }

        Dao<Location, String> dao;
        nearby_users.add(location); // 2
        try {
            dao = DaoManager.createDao(connectionSource, Location.class);
        } catch (SQLException e) {
            // return null
            return nearby_users;
        }

        nearby_users.add(location); // 3
        try {
            dao.createOrUpdate(location);
        } catch (SQLException e) {
            err_msg = e.getMessage();
            err_code = e.getErrorCode();
            err_sql = e.getSQLState();
        }
        // Search for nearby users every time you update your position
        //List<Location> nearby_users = findUsersInRadius(dao, location);
        nearby_users.add(location);  // 4
        try {
            connectionSource.close();
        } catch (SQLException e) {
            // return null
            return nearby_users;
        }
        nearby_users.add(location);
        return nearby_users;
        //return null;

    }

    @ApiMethod(name = "deactivateUser")
    public Location deactivateUser(@Named("user_id") String user_id) {
        Location location = new Location();
        location.setUser_id(user_id);
        logger.info("Calling deactivateUser method");
        ConnectionSource connectionSource;
        try {
            connectionSource = new JdbcConnectionSource(url);
        } catch (SQLException e) {
            return null;
        }

        Dao<Location, String> dao;
        try {
            dao = DaoManager.createDao(connectionSource, Location.class);
        } catch (SQLException e) {
            return null;
        }
        try {
            dao.delete(location);
        } catch (SQLException e) {
            return null;
        }
        try {
            connectionSource.close();
        } catch (SQLException e) {

        }
        return location;
    }


    private List<Location> findUsersInRadius(Dao<Location, String> dao, Location user_loc) {
        double latitude = user_loc.getLatitude();
        double longitude = user_loc.getLongitude();
        double ang_cos = Math.cos(ANG_DIST);
        double ang_sin = Math.sin(ANG_DIST);
        double sin_lat1 = Math.sin(latitude * Math.PI / 180); // Need radians
        double cos_lat1 = Math.cos(latitude * Math.PI / 180); // Need radians    1 = Cos(0)
        double dLat = Math.abs(Math.asin(sin_lat1 * ang_cos + cos_lat1 * ang_sin * 1) * 180 / Math.PI - latitude);
        double dLong = Math.abs(Math.atan2(1 * ang_sin * cos_lat1, ang_cos - sin_lat1 * sin_lat1) * 180 / Math.PI); // Lat2 = Lat 1;
                                        // 1 = sin(90)
        double lat1 = latitude + dLat;
        double lat2 = latitude - dLat;
        double long1 = longitude + dLong;
        double long2 = longitude - dLong;

        Where<Location, String> where = dao.queryBuilder().where();
        List<Location> nearby_users;
        try {
            nearby_users = where.between(Location.LAT_FIELD, lat2, lat1)
                    .between(Location.LONG_FIELD, long2, long1)
                    .and(2) // and the previous 2 betweens
                    .query();
        } catch (SQLException e) {
            return null;
        }

        // Remove users who are not in radius
        for (int i = nearby_users.size() - 1; i >= 0; --i) {
            if (user_loc.distanceTo(nearby_users.get(i)) > DIST) {
                nearby_users.remove(i);
            }
        }
        return nearby_users;
    }

    /*
    SELECT * FROM UserLocation.Location
    WHERE Latidude BETWEEN lat2 AND lat1
    AND Longitude BETWEEN long2 and long1
     */
}



