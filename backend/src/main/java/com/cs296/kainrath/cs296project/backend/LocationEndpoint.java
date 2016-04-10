package com.cs296.kainrath.cs296project.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    private static final int MYSQL_DUPLICATE_CODE = 1062;
    private static final Logger logger = Logger.getLogger(LocationEndpoint.class.getName());
    private static final String url = "jdbc:google:mysql://cs296-backend:cs296-app-location-data/UserLocation?user=root";
    private static final double DIST = 50;
    private static final double RAD_EARTH = 6371000;
    private static final double ANG_DIST = DIST / RAD_EARTH;

    /**
     * This method gets the <code>Location</code> object associated with the specified <code>id</code>.
     *
     * @param user_id The id of the object to be returned.
     * @return The <code>Location</code> associated with <code>id</code>.
     */
    @ApiMethod(name = "getLocation")
    public Location getLocation(@Named("user_id") String user_id) {
        logger.info("Calling getLocation method");
        Location location = null;
        try {
            // Connect
            Class.forName("com.mysql.jdbc.GoogleDriver");
            Connection conn = DriverManager.getConnection(url);

            String find_query = "SELECT * FROM Location WHERE user_id=\"" + user_id + "\"";
            Statement stmt = conn.createStatement();
            ResultSet result = stmt.executeQuery(find_query);
            if (result.next()) {
                location = new Location();
                location.setUser_id(user_id);
                location.setLatitude(result.getDouble(Location.LAT_FIELD));
                location.setLongitude(result.getDouble(Location.LONG_FIELD));
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
    public LocationList updateLocation(@Named("user_id") String user_id, @Named("latitude") double latitude, @Named("longitude") double longitude) {
        Location location = new Location(user_id, latitude, longitude);
        logger.info("Calling insertLocation method");

        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.GoogleDriver");
            conn = DriverManager.getConnection(url);

            Statement stmt_insert = conn.createStatement();
            String insert = "INSERT INTO Location (user_id, latitude, longitude) VALUES " +
                            "(\"" + user_id + "\", " + latitude + ", " + longitude + ")";
            stmt_insert.executeUpdate(insert);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_CODE && conn != null) { // Already in database, just update
                try {
                    String update = "UPDATE Location SET latitude=" + latitude + ", longitude=" + longitude +
                                            " WHERE user_id=\"" + user_id + "\"";
                    Statement stmt_update = conn.createStatement();
                    stmt_update.executeUpdate(update);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return null;
                }
            } else {
                e.printStackTrace();
                return null;
            }
        }

        // Find nearby users
        LocationList locations = findUsersInRadius(conn, location);

        // Close connection
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return locations;
    }

    @ApiMethod(name = "deactivateUser")
    public void deactivateUser(@Named("user_id") String user_id) {
        // Location location = new Location();
        // location.setUser_id(user_id);
        logger.info("Calling deactivateUser method");
        try {
            // Connect
            Class.forName("com.mysql.jdbc.GoogleDriver");
            Connection conn = DriverManager.getConnection(url);

            // Delete
            String delete = "DELETE FROM Location WHERE user_id=\"" + user_id + "\"";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(delete);

            // Close connection
            conn.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            int err_code = e.getErrorCode();
            String err_msg = e.getMessage();
            e.printStackTrace();
        }
    }


    private LocationList findUsersInRadius(Connection conn, Location user_loc) {
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

        String query = "SELECT * FROM Location WHERE latitude BETWEEN " + lat2 + " AND " + lat1 +
                        " AND longitude BETWEEN " + long2 + " AND " + long1;
        List<Location> nearby_users = new ArrayList<Location>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next() && !rs.getString(Location.ID_FIELD).equals(user_loc.getUser_id())) {
                Location loc = new Location();
                loc.setUser_id(rs.getString(Location.ID_FIELD));
                loc.setLatitude(rs.getDouble(Location.LAT_FIELD));
                loc.setLongitude(rs.getDouble(Location.LONG_FIELD));
                nearby_users.add(loc);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Remove users who are not in radius
        for (int i = nearby_users.size() - 1; i >= 0; --i) {
            if (user_loc.distanceTo(nearby_users.get(i)) > DIST) {
                nearby_users.remove(i);
            }
        }
        LocationList users = new LocationList();
        users.setLocations(nearby_users);
        return users;
    }
}



