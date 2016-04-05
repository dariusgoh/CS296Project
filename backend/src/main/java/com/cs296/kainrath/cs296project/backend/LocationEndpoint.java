package com.cs296.kainrath.cs296project.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
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
    private static final String url = "jdbc:google:mysql://cs296-backend:cs296-app-location-data/location?user=root";
    private static final double DIST = 50;
    private static final double RAD_EARTH = 6371000;

    /**
     * This method gets the <code>Location</code> object associated with the specified <code>id</code>.
     *
     * @param id The id of the object to be returned.
     * @return The <code>Location</code> associated with <code>id</code>.
     */
    @ApiMethod(name = "getLocation")
    public Location getLocation(@Named("id") Long id) {
        // TODO: Implement this function
        logger.info("Calling getLocation method");
        return null;
    }

    /**
     * This inserts a new <code>Location</code> object.
     *
     * @param user_id The user to be updated.
     * @param latitude The latitude of the user.
     * @param longitude The longitude of the user.
     * @return The object to be added.
     */
    @ApiMethod(name = "updateLocation")
    public Location updateLocation(@Named("user_id") String user_id, @Named("latitude") double latitude, @Named("longitude") double longitude) {
        Location location = new Location(user_id, latitude, longitude);
        logger.info("Calling insertLocation method");
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
            dao.createOrUpdate(location);
        } catch (SQLException e) {
            return null;
        }
        // Search for nearby users
        //findUsersInRadius(dao);
        try {
            connectionSource.close();
        } catch (SQLException e) {

        }
        return location;
    }

    @ApiMethod(name = "deactiveUser")
    public Location deactivateUser(@Named("user_id") String user_id) {
        Location location = new Location();
        location.setUser_id(user_id);
        logger.info("Calling deactiveUser method");
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

    /*
    private void findUsersInRadius(Dao<Location, String> dao, Location location) {
        double ang_dist = DIST / RAD_EARTH;
        double sin_lat1 = Math.sin()
        double dLat = ;
        double dLong = ;
        QueryBuilder<Location, String> queryBuilder = dao.queryBuilder();

    }*/
}