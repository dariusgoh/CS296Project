package com.cs296.kainrath.cs296project.backend;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.response.NotFoundException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    private static final String url = "jdbc:google:mysql://cs296-backend:cs296-app-location-data/UserLocation?user=root";
    private static final String API_KEY = "AIzaSyAJuwfy0EoirghnDaThupzrqNTDVxsm650";

    // A user or chatgroup's radius
    private static final double DIST = 100;
    private static final double RAD_EARTH = 6371000; // In meters

    /**
     * This method gets the location of the user with the specified ID.  If the user is offline,
     * the location will be null.
     *
     * @param user_id The id of the object to be returned.
     * @return The location of the user
     * @throws NotFoundException if User is not in the Database
     */
    @ApiMethod(name = "getLocation")
    public Location getLocation(@Named("user_id") String user_id) throws NotFoundException {
        logger.info("Calling getLocation method");
        Location location = null;
        try {
            Class.forName("com.mysql.jdbc.GoogleDriver");
            Connection conn = DriverManager.getConnection(url);

            String find_query = "SELECT * FROM UserInfo WHERE UserId=\"" + user_id + "\"";
            Statement stmt = conn.createStatement();
            ResultSet result = stmt.executeQuery(find_query);
            if (result.next()) {
                if (result.getString("Online").equals("Y")) {
                    location = new Location();
                    location.setUser_id(user_id);
                    location.setLatitude(result.getDouble(Location.LAT_FIELD));
                    location.setLongitude(result.getDouble(Location.LONG_FIELD));
                }
            } else {
                throw new NotFoundException("User not in the database");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return location;
    }

    /**
     * This activates/updates a User's location in the database as well as looks for groups to join/leave
     *
     * @params ChatGroupList is the list of current chat groups the user is in, or null if the user is activating.
     * @return List of chat groups
     */
    @ApiMethod(name = "updateLocation")
    public ChatGroupList updateLocation(@Named("user_id") String user_id, @Named("email") String email,
                                        @Named("lat") double lat, @Named("lon") double lon,
                                        @Named("interests") List<String> interests,
                                        @Named("token") String token, @Named("chatIdsString") String chatIdsString) {
        if (interests == null || interests.isEmpty()) { // Need to have some interests
            return null;
        }

        // Interests is comma separated, single String, not a list of Strings for some reason
        String ints = interests.get(0);
        String[] split_interests = ints.split(",");
        interests.clear();
        for (String s : split_interests) {
            interests.add(s);
        }

        // Was sending a list of chatGroups, but the list was always empty
        // If chatIdsString is empty, the user just activated
        // If chatIdsString is not empty, the user's location updated but the user was already "active"
        boolean firstUpdate = chatIdsString.equals(" ");
        List<Integer> currChatIds = null;
        if (!firstUpdate) {
            String[] parsedChatIds = chatIdsString.split(",");
            currChatIds = new ArrayList<>();
            for (String s : parsedChatIds) {
                currChatIds.add(Integer.parseInt(s));
            }
        }

        Location userLocation = new Location(user_id, lat, lon);
        Connection conn;
        double oldLat = -200, oldLong = -200; // Values will be changed if the user was already active
        try { // Create a connection to the database
            Class.forName("com.mysql.jdbc.GoogleDriver");
            conn = DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            String error = e.getSQLState(); // This is here for debugging purposes (cloud debugger)
            return null;
        }

        String updateLoc;
        if (firstUpdate) { // Just activated
            updateLoc = "UPDATE UserInfo SET Latitude=" + lat + ", Longitude=" + lon +
                    ", Active=\"Y\" WHERE UserId=\"" + user_id + "\"";
        } else {
            // Get old location
           try {
               String oldLocQuery = "SELECT Latitude, Longitude FROM UserInfo WHERE UserId=\"" + user_id + "\"";
               ResultSet rs = conn.createStatement().executeQuery(oldLocQuery);
               if (rs.next()) {
                   oldLat = rs.getDouble("Latitude");
                   oldLong = rs.getDouble("Longitude");
               }
           } catch (SQLException e) {
               String error = e.getSQLState();
           }

            updateLoc = "UPDATE UserInfo SET Latitude=" + lat + ", Longitude=" + lon +
                    " WHERE UserId=\"" + user_id + "\"";
        }
        try { // Update location in the database
            conn.createStatement().executeUpdate(updateLoc);
        } catch (SQLException e) {
            String error = e.getSQLState();
            return null;
        }

        // Get nearby chat groups
        List<ChatGroup> nearbyChats = findChatGroupsInRadius(conn, userLocation);

        // Compare interests to see if they match with any new nearby chats
        if (!nearbyChats.isEmpty()) {
            int nearbyChatSize = nearbyChats.size(); // For debugging purposes
            int matchingSize = 0; // For debugging purposes
            String interest_debug = "";
            Iterator<ChatGroup> iter = nearbyChats.iterator();
            while (iter.hasNext()) {
                ChatGroup chat = iter.next();
                interest_debug += ":" + chat.getInterest();
                if (currChatIds != null && currChatIds.contains(chat.getChatId())) { // Already in chatGroup
                    if (updateCurrentChatGroup(conn, chat, userLocation, oldLat, oldLong, email)) {
                        iter.remove(); // User outside group range
                    }
                } else if (interests.contains(chat.getInterest())) { // New chatGroup with matching interest
                    joinNewGroup(conn, chat, userLocation, token, email);
                } else { // Non-matching group
                    iter.remove();
                }
            }
        }

        // Start a chat group for any interests that don't already have a chatgroup
        // This can only happen if the user just activated (not on a location update)
        //if (firstUpdate) {
        List<String> unmatchedInterests = new ArrayList<>();
        unmatchedInterests.addAll(interests);
        for (ChatGroup chat : nearbyChats) { // Remove interests that are already covered by another chatGroup
            unmatchedInterests.remove(chat.getInterest());
        }
        if (!unmatchedInterests.isEmpty()) { // Unmatched interests, start new chatGroup
            nearbyChats.addAll(startNewChatGroups(conn, unmatchedInterests, lat, lon, token, user_id, email));
        }
        //}

        ChatGroupList groupList = new ChatGroupList(nearbyChats);

        try { // Close connection
            conn.close();
        } catch (SQLException e) {
            String error = e.getSQLState();
        }
        return groupList;
    }

    @ApiMethod(name = "deactivateUser")
    public void deactivateUser(@Named("user_id") String user_id, @Named("email") String email,
                               @Named("lat") double lat, @Named("lon") double lon,
                               @Named("chatIdString") String chatIdString) {

        // chatIds was List<Integer>, but if there are multiple chatIds, then the
        // list would be a comma separated string of the integers and the call to this function
        // failed

        // chatIds is list of comma separated numbers, not actual list
        List<Integer> chatIds = new ArrayList<>();
        if (chatIdString != null) {
            String[] ids_split = chatIdString.split(",");
            for (String s : ids_split) {
                chatIds.add(Integer.parseInt(s));
            }
        }

        logger.info("Calling deactivateUser method");
        Connection conn;
        try {
            // Connect
            Class.forName("com.mysql.jdbc.GoogleDriver");
            conn = DriverManager.getConnection(url);

            // Get ChatGroup information
            String groupQuery = "SELECT * FROM ChatGroups WHERE ChatId=" + chatIds.get(0);
            for (int i = 1; i < chatIds.size(); ++i) {
                groupQuery += " OR ChatId=" + chatIds.get(i);
            }
            ResultSet groupSet = conn.createStatement().executeQuery(groupQuery);
            List<ChatGroup> currChatGroups = new ArrayList<>();
            while (groupSet.next()) {
                ChatGroup group = new ChatGroup(groupSet.getString("Interest"), groupSet.getInt("ChatId"),
                        groupSet.getInt("GroupSize"), groupSet.getDouble("Latitude"), groupSet.getDouble("Longitude"));
                currChatGroups.add(group);
            }

            // Make user "Offline"
            String offlineUpdate = "UPDATE UserInfo SET Active=\"N\" WHERE UserId=\"" + user_id + "\"";
            conn.createStatement().executeUpdate(offlineUpdate);

            // Should not be empty, but make sure
            if (!currChatGroups.isEmpty()) {
                // These destroy updates are only for chats with size 1
                String destroyChatGroupUpdate = "DELETE FROM ChatGroups WHERE ChatId=";
                String destroyChatUserUpdate = "DELETE FROM ChatUsers WHERE ChatId=";
                boolean removed = false;
                for (ChatGroup group : currChatGroups) {
                    if (group.getGroupSize() == 1) { // User is only member, destroy group
                        if (removed) {
                            destroyChatGroupUpdate += " OR ChatId=" + group.getChatId();
                            destroyChatUserUpdate += " OR ChatId=" + group.getChatId();
                        } else {
                            removed = true;
                            destroyChatGroupUpdate += group.getChatId();
                            destroyChatUserUpdate += group.getChatId();
                        }
                    } else { // Remove user from a larger group
                        group.removeUserFromGroup(lat, lon);
                        leaveChatGroup(conn, user_id, email, group);
                    }
                }
                if (removed) { // Destroy the chatGroups where the user is the only member
                    Statement stmt = conn.createStatement();
                    stmt.addBatch(destroyChatGroupUpdate);
                    stmt.addBatch(destroyChatUserUpdate);
                    stmt.executeBatch();
                }
            }

            conn.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return; // For debugging purposes
        } catch (SQLException e) {
            String error = e.getSQLState();
            return; // For debugging purposes
        }

    }

    private List<ChatGroup> startNewChatGroups(Connection conn, List<String> interests, double lat, double lon, String userToken,
                                               String userId, String email) {
        List<ChatGroup> newGroups = new ArrayList<>();
        String insertGroupUpdate = "INSERT INTO ChatGroups (Interest, Latitude, Longitude, GroupSize) VALUES (\"" +
                interests.get(0) + "\", " + lat + ", " + lon + ", " + 1 + ")";
        for (int i = 1; i < interests.size(); ++i) {
            insertGroupUpdate += ", (\"" + interests.get(i) + "\", " + lat + ", " + lon + ", " + 1 + ")";
        }
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(insertGroupUpdate, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            int index = 0;
            while (rs.next()) {
                int chatId = rs.getInt(1);
                ChatGroup group = new ChatGroup(interests.get(index++), chatId, 1, lat, lon);
                group.putCurrMember(email);
                newGroups.add(group);
            }

            // Insert into ChatUsers Tables
            String updateChatUsers = "INSERT INTO ChatUsers (ChatId, UserId, Token, Email) VALUES (" + newGroups.get(0).getChatId() +
                    ", \"" + userId + "\", \"" + userToken + "\", \"" + email + "\")";
            for (int i = 1; i < newGroups.size(); ++i) {
                updateChatUsers += ", (" + newGroups.get(i).getChatId() + ", \"" + userId + "\", \"" + userToken +
                        "\", \"" + email + "\")";
            }
            conn.createStatement().executeUpdate(updateChatUsers);

        } catch (SQLException e) {
            String error = e.getSQLState();
        }
        return newGroups;
    }

    private void joinNewGroup(Connection conn, ChatGroup chat, Location userLoc, String token, String email) {
        try {
            chat.addUserToGroup(email, userLoc.getLatitude(), userLoc.getLongitude());
            String chatUserUpdate = "INSERT INTO ChatUsers (ChatId, UserId, Token, Email) VALUES (" + chat.getChatId() +", " +
                    "\"" + userLoc.getUser_id() + "\", \"" + token + "\", \"" + email + "\")";
            String chatGroupUpdate = "UPDATE ChatGroups SET Latitude=" + chat.getLatitude() + ", Longitude=" + chat.getLongitude() +
                    ", GroupSize=" + chat.getGroupSize() + " WHERE ChatId=" + chat.getChatId();
            Statement stmt = conn.createStatement();
            stmt.addBatch(chatGroupUpdate);
            stmt.addBatch(chatUserUpdate);
            stmt.executeBatch();

            String tokenQuery = "SELECT Token, Email FROM ChatUsers WHERE ChatId=" + chat.getChatId();
            ResultSet tokenSet = conn.createStatement().executeQuery(tokenQuery);
            List<String> tokens = new ArrayList<>();
            while (tokenSet.next()) {
                tokens.add(tokenSet.getString("Token"));
                chat.putCurrMember(tokenSet.getString("Email"));
            }

            // Send GCM Message
            Sender sender = new Sender(API_KEY);
            Message msg = new Message.Builder().addData("UserId", userLoc.getUser_id()).addData("Email", email)
                    .addData("ChatId", "" + chat.getChatId()).addData("Action", "JoiningGroup").build();
            MulticastResult result = sender.send(msg, tokens, 3);
        } catch (SQLException e) {
            String error = e.getSQLState();
        } catch (IOException e) {
            String error = e.getMessage();
        }
    }

    private List<ChatGroup> findChatGroupsInRadius(Connection conn, Location userLoc) {
        double ang_dist = DIST / RAD_EARTH;
        double latitude = userLoc.getLatitude();
        double longitude = userLoc.getLongitude();
        double ang_cos = Math.cos(ang_dist);
        double ang_sin = Math.sin(ang_dist);
        double sin_lat1 = Math.sin(latitude * Math.PI / 180); // Need radians
        double cos_lat1 = Math.cos(latitude * Math.PI / 180); // Need radians    1 = Cos(0)
        double dLat = Math.abs(Math.asin(sin_lat1 * ang_cos + cos_lat1 * ang_sin * 1) * 180 / Math.PI - latitude);
        double dLong = Math.abs(Math.atan2(1 * ang_sin * cos_lat1, ang_cos - sin_lat1 * sin_lat1) * 180 / Math.PI); // Lat2 = Lat 1;
        // 1 = sin(90)
        double lat1 = latitude + dLat;
        double lat2 = latitude - dLat;
        double long1 = longitude + dLong;
        double long2 = longitude - dLong;


        List<ChatGroup> nearbyChatGroups = new ArrayList<>();
        ChatGroup nearbyGroup; // Placed here for debug purposes
        try {
            // Get chatGroups near user
            String groupQuery = "SELECT * FROM ChatGroups WHERE Latitude BETWEEN " + lat2 + " AND " + lat1 +
                    " AND Longitude BETWEEN " + long2 + " AND " + long1;

            ResultSet rs = conn.createStatement().executeQuery(groupQuery);

            while (rs.next()) {
                nearbyGroup = new ChatGroup(rs.getString("Interest"), rs.getInt("ChatId"), rs.getInt("GroupSize"),
                        rs.getDouble("Latitude"), rs.getDouble("Longitude"));
                nearbyChatGroups.add(nearbyGroup);
            }

        } catch (SQLException e) {
            String error = e.getSQLState();
        }
        return nearbyChatGroups;
    }

    // Returns true if the user left the group and should be removed from the group
    private boolean updateCurrentChatGroup(Connection conn, ChatGroup currGroup, Location userLoc,
                                                    double oldLat, double oldLong, String email) {
        // Get chats that the user is in
        currGroup.moveMember(oldLat, oldLong, userLoc.getLatitude(), userLoc.getLongitude());
        double newDist = userLoc.distanceTo(currGroup.getLatitude(), currGroup.getLongitude());
        if (newDist > DIST && currGroup.getGroupSize() > 1) { // Out of group range
            currGroup.removeUserFromGroup(email, oldLat, oldLong); // If only 1 user, then the location of the group will just be the users new location
            leaveChatGroup(conn, userLoc.getUser_id(), email, currGroup);
            return true;
        } else {
            try { // Still in group range
                String updateGroup = "UPDATE ChatGroups SET Latitude=" + currGroup.getLatitude() + ", Longitude=" + currGroup.getLongitude() +
                        " WHERE ChatId=" + currGroup.getChatId();
                conn.createStatement().executeUpdate(updateGroup);
            } catch (SQLException e) {
                String error = e.getSQLState();
            }
            return false;
        }
    }

    private void leaveChatGroup(Connection conn, String user_id, String email, ChatGroup group) {
        try {
            Statement stmt = conn.createStatement();
            String deleteChatUser = "DELETE FROM ChatUsers WHERE UserId=\"" + user_id + "\" AND ChatId=" +
                    group.getChatId();
            String updateChatGroup = "UPDATE ChatGroups SET Latitude=" + group.getLatitude() + ", Longitude=" + group.getLongitude() +
                    ", GroupSize=" + group.getGroupSize() + " WHERE ChatId=" + group.getChatId();
            stmt.addBatch(deleteChatUser);
            stmt.addBatch(updateChatGroup);
            stmt.executeBatch();

            // Get other users from DB
            String groupUsers = "SELECT Token FROM ChatUsers WHERE ChatId=" + group.getChatId();
            ResultSet userSet = conn.createStatement().executeQuery(groupUsers);

            List<String> userTokens = new ArrayList<>();
            while (userSet.next()) {
                userTokens.add(userSet.getString("Token"));
            }

            if (userTokens.isEmpty()) {
                return;
            }

            // Notify through GCM
            Sender sender = new Sender(API_KEY);
            Message message = new Message.Builder().addData("Action", "LeavingGroup").addData("ChatId", "" + group.getChatId())
                    .addData("Email", email).build();
            try {
                MulticastResult result = sender.send(message, userTokens, 3); // Retry sending 3 times
            } catch (IOException e) {
                String error = e.getMessage();
            }

        } catch (SQLException e) {
            String error = e.getSQLState();
        }
    }
}

// Old code
    /*
    private List<User> findMatchingUsers(User curr_user, List<User> nearby_users) {
        Set<String> curr_interests = curr_user.getInterests();
        Set<String> other_interests;
        for (int i = nearby_users.size() - 1; i >= 0; --i) {
            if (nearby_users.get(i).getId().equals(curr_user.getId())) {
                continue;
            }
            other_interests = nearby_users.get(i).getInterests();
            other_interests.retainAll(curr_interests);
            if (other_interests.size() == 0) {  // No matching interests
                nearby_users.remove(i);
            } else {
                nearby_users.get(i).setInterests(other_interests);
            }
        }
        return nearby_users;
    }*/

 /*
        // Find nearby users
    List<String> nearby_user_ids = findUsersInRadius(conn, location);
    UserList nearby_users = null;
    boolean isEmpty = nearby_user_ids.isEmpty();
    if (!isEmpty) {
        // Get users within radius
        nearby_users = UserEndpoint.getAll(nearby_user_ids);
        User curr_user = UserEndpoint.getOne(user_id);

        // Check for interest match, remove users who dont match
        nearby_users.setUsers(findMatchingUsers(curr_user, nearby_users.getUsers()));

        // Notify the nearby users (if any)
        notifyNearbyUsers(nearby_users.getUsers(), curr_user);

            /*
            final User curr_user = UserEndpoint.getOne(user_id);
            final List<User> other_users = nearby_users.getUsers();
            if (!nearby_users.getUsers().isEmpty()) {
                Thread notifyOthers = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        notifyNearbyUsers(other_users, curr_user);
                    }
                });
                notifyOthers.start();
            }
        }


        // Close connection
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nearby_users;
        */


/* Finding ChatGroups nearby
* Look for ChatGroups Between Lat1 and Lat2 and Long1 and Long2 (Square)
* Narrow down to circle
* If there is a match, add user to ChatGroup and notify members of ChatGroup
* Create a "ChatGroup" for each interest that doesn't have a ChatGroup
* Return a Set of ChatGroups
*/

// OLD CODE

    /*
    private List<String> findUsersInRadius(Connection conn, Location user_loc) {
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
        List<Location> nearby_users_loc = new ArrayList<Location>();
        Location loc;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String user_id = rs.getString(Location.ID_FIELD);
                if (user_id.equals(user_loc.getUser_id())) {
                    continue;
                }

                loc = new Location();
                loc.setUser_id(user_id);
                loc.setLatitude(rs.getDouble(Location.LAT_FIELD));
                loc.setLongitude(rs.getDouble(Location.LONG_FIELD));
                nearby_users_loc.add(loc);
            }
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<String> nearby_user_ids = new ArrayList<String>();
        // Add users who are in radius
        for (int i = nearby_users_loc.size() - 1; i >= 0; --i) {
            double dist = user_loc.distanceTo(nearby_users_loc.get(i));
            if (dist <= DIST) {
                nearby_user_ids.add(nearby_users_loc.get(i).getUser_id());
            }
        }
        return nearby_user_ids;
    }

    private void notifyNearbyUsers(List<User> nearby_users, User curr_user) {
        String token;
        for (User user : nearby_users) {
            token = user.getToken();
            if (token == null) { // Make sure they have a token
                continue;
            }

            String matching_interests = "";
            for (String match : user.getInterests()) {
                matching_interests += "\n" + match;
            }
            Sender sender = new Sender(API_KEY);
            Message message = new Message.Builder().addData("userId", curr_user.getId()).addData("userEmail", curr_user.getEmail())
                    .addData("interests", matching_interests).build();
            try {
                Result result = sender.send(message, token, 5); // Retry sending 5 times
            } catch (IOException e) {

            }
        }
    } */
