package com.cs296.kainrath.cs296project.backend;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import com.google.android.gcm.server.Sender;

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

    private static final int MYSQL_DUPLICATE_CODE = 1062;
    private static final Logger logger = Logger.getLogger(LocationEndpoint.class.getName());
    private static final String url = "jdbc:google:mysql://cs296-backend:cs296-app-location-data/UserLocation?user=root";
    private static final double DIST = 200;
    private static final double RAD_EARTH = 6371000;
    private static final double ANG_DIST = DIST / RAD_EARTH;
    private static final String API_KEY = "AIzaSyAJuwfy0EoirghnDaThupzrqNTDVxsm650";
    private static final String GCM_URL = "https://gcm-http.googleapis.com/gcm/send";

    /**
     * This method gets the location of the user with the specified ID.  If the user is offline,
     * the location will be null.
     *
     * @param user_id The id of the object to be returned.
     * @return The location of the user
     */
    @ApiMethod(name = "getLocation")
    public Location getLocation(@Named("user_id") String user_id) {
        logger.info("Calling getLocation method");
        Location location = null;
        try {
            // Connect
            Class.forName("com.mysql.jdbc.GoogleDriver");
            Connection conn = DriverManager.getConnection(url);

            String find_query = "SELECT * FROM UserInfo WHERE UserId=\"" + user_id + "\"";
            Statement stmt = conn.createStatement();
            ResultSet result = stmt.executeQuery(find_query);
            if (result.next() && result.getString("Online").equals("Y")) {
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
     * This activates/updates a User's location in the database as well as looks for groups to join/leave
     *
     * @params ChatGroupList is the list of current chat groups the user is in, or null if the user is activating.
     * @return List of chat groups
     */
    @ApiMethod(name = "updateLocation")
    public ChatGroupList updateLocation(@Named("user_id") String user_id, @Named("lat") double lat,
                                        @Named("lon") double lon, @Named("interests") List<String> interests,
                                        @Named("token") String token, ChatGroupList chatGroupList) {
        if (interests == null || interests.isEmpty()) { // Need to have some interests
            return null;
        }
        // Interests is comma separated, single String, not a list for some reason
        String ints = interests.get(0);
        String[] split_interests = ints.split(",");
        interests.clear();
        for (String s : split_interests) {
            interests.add(s);
        }

        boolean firstUpdate = (chatGroupList == null) || (chatGroupList.getChatGroups() == null) || (chatGroupList.getChatGroups().isEmpty());
        Location userLocation = new Location(user_id, lat, lon);
        Connection conn = null;
        double oldLat = -200, oldLong = -200;
        try {
            Class.forName("com.mysql.jdbc.GoogleDriver");
            conn = DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            String error = e.getSQLState();
            return null;
        }

        // UPDATE LOCATION IN THE DATABASE
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
        try {
            conn.createStatement().executeUpdate(updateLoc);
        } catch (SQLException e) {
            String error = e.getSQLState();
            return null;
        }

        List<ChatGroup> currChatGroups;
        if (!firstUpdate) {
            currChatGroups = updateCurrentChatGroups(conn, chatGroupList.getChatGroups(), userLocation, oldLat, oldLong, token);
        } else {
            currChatGroups = new ArrayList<>();
        }

        // Get nearby chats that you are not already in
        List<ChatGroup> newNearbyChats = findNewChatGroupsInRadius(conn, userLocation, currChatGroups);

        // Compare interests to see if they match with any new nearby chats
        if (newNearbyChats != null && !newNearbyChats.isEmpty()) {
            Iterator<ChatGroup> iter = newNearbyChats.iterator();
            while (iter.hasNext()) {
                ChatGroup chat = iter.next();
                if (!interests.contains(chat.getInterest())) {
                    iter.remove();
                }
            }

            // Update new groups and notify users
            if (!newNearbyChats.isEmpty()) {
                joinNewGroups(conn, newNearbyChats, userLocation, token);
            }
        }
        currChatGroups.addAll(newNearbyChats);

        // Start a chat group for any interests that don't already have a chatgroup
        if (firstUpdate) {
            List<String> unmatchedInterests = new ArrayList<>();
            unmatchedInterests.addAll(interests);
            for (ChatGroup chat : currChatGroups) {
                unmatchedInterests.remove(chat.getInterest());
            }
            if (!unmatchedInterests.isEmpty()) {
                currChatGroups.addAll(startNewChatGroups(conn, unmatchedInterests, lat, lon, token, user_id));
            }
        }

        chatGroupList.setChatGroups(currChatGroups);

        try {
            conn.close();
        } catch (SQLException e) {
            String error = e.getSQLState();
        }
        return chatGroupList;
    }

    @ApiMethod(name = "deactivateUser")
    public void deactivateUser(@Named("user_id") String user_id, @Named("lat") double lat,
                               @Named("lon") double lon, @Named("chatIdString") String chatIdString) {

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

            // GET CHATGROUP INFORMATION
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

            // Should not be null or empty, but make sure
            if (currChatGroups != null || !currChatGroups.isEmpty()) {
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
                    } else { // Remove use from a larger group
                        group.removeUserFromGroup(user_id, lat, lon);
                        leaveChatGroup(conn, user_id, group);
                    }
                }
                if (removed) {
                    Statement stmt = conn.createStatement();
                    stmt.addBatch(destroyChatGroupUpdate);
                    stmt.addBatch(destroyChatUserUpdate);
                    stmt.executeBatch();
                }
            }


            conn.close();

            /*
            // Delete
            String delete = "DELETE FROM Location WHERE user_id=\"" + user_id + "\"";
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(delete);

            // Close connection
            conn.close();
            */
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return; // For debugging purposes
        } catch (SQLException e) {
            String error = e.getSQLState();
            return; // For debugging purposes
        }

    }


    private List<ChatGroup> startNewChatGroups(Connection conn, List<String> interests, double lat, double lon, String userToken,
                                               String userId) {
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
                group.putCurrMember(userId);
                newGroups.add(group);
            }

            // Insert into ChatUsers Tables
            String updateChatUsers = "INSERT INTO ChatUsers (ChatId, UserId, Token) VALUES (" + newGroups.get(0).getChatId() +
                    ", \"" + userId + "\", \"" + userToken + "\")";
            for (int i = 1; i < newGroups.size(); ++i) {
                updateChatUsers += ", (" + newGroups.get(i).getChatId() + ", \"" + userId + "\", \"" + userToken + "\")";
            }
            conn.createStatement().executeUpdate(updateChatUsers);

        } catch (SQLException e) {
            String error = e.getSQLState();
            return null;
        }
        return newGroups;
    }


    private void joinNewGroups(Connection conn, List<ChatGroup> chatGroups, Location userLoc, String token) {
        try {
            ChatGroup chat = chatGroups.get(0);
            chat.addUserToGroup(userLoc.getUser_id(), userLoc.getLatitude(), userLoc.getLongitude());
            String chatUserUpdate = "INSERT INTO ChatUsers (ChatId, UserId, Token) VALUES (" + chat.getChatId() +", " +
                    "\"" + userLoc.getUser_id() + "\", \"" + token + "\")";
            String chatGroupUpdate = "INSERT INTO ChatGroups (ChatId, Interest, Latitude, Longitude, GroupSize) VALUES (" +
                    chat.getChatId() + ", \"" + chat.getInterest() + "\", " + chat.getLatitude() + ", " +
                    chat.getLongitude() + ", " + chat.getGroupSize() + ")";
            String tokenQuery = "SELECT * FROM ChatUsers WHERE ChatId=" + chat.getChatId();

            // Update chat groups
            for (int i = 1; i < chatGroups.size(); ++i) {
                chat = chatGroups.get(i);
                chat.addUserToGroup(userLoc.getUser_id(), userLoc.getLatitude(), userLoc.getLongitude());
                tokenQuery += " OR ChatId=" + chat.getChatId();
                chatUserUpdate += ", (" + chat.getChatId() + ", \"" + userLoc.getUser_id() + "\", \"" + token + "\")";
                chatGroupUpdate += ", (" + chat.getChatId() + ", \"" + chat.getInterest() + "\", " + chat.getLatitude() +
                        ", " + chat.getLongitude() + ", " + chat.getGroupSize() + ")";
            }
            chatGroupUpdate += "ON DUPLICATE KEY UPDATE Interest=VALUES(Interest), Latitude=VALUES(Latitude), " +
                    "Longitude=VALUES(Longitude), GroupSize=VALUES(GroupSize)";

            ResultSet tokenSet = conn.createStatement().executeQuery(tokenQuery);
            Statement stmt = conn.createStatement();
            stmt.addBatch(chatUserUpdate);
            stmt.addBatch(chatGroupUpdate);
            stmt.executeBatch();

            // Notify chat users
            Map<Integer, List<String>> userTokens = new TreeMap<>();
            while (tokenSet.next()) {
                int chatId = tokenSet.getInt("ChatId");
                List<String> tokens;
                if (userTokens.containsKey(chatId)) {
                    tokens = userTokens.get(chatId);
                } else {
                    tokens = new ArrayList<>();
                }
                tokens.add(tokenSet.getString("Token"));
                userTokens.put(chatId, tokens);

                // Get user id
                for (ChatGroup group : chatGroups) {
                    if (group.getChatId() == chatId) {
                        group.putCurrMember(tokenSet.getString("UserId"));
                        break;
                    }
                }
            }

            for (Integer chatId : userTokens.keySet()) {
                List<String> info = userTokens.get(chatId);
                Sender sender = new Sender(API_KEY);
                Message msg = new Message.Builder().addData("UserId", userLoc.getUser_id())
                        .addData("ChatId", "" + chatId).addData("Action", "JoiningGroup").build();
                try {
                    MulticastResult result = sender.send(msg, info, 3);
                } catch (IOException e) {
                    String error = e.getMessage();
                }
            }

        } catch (SQLException e) {
            String error = e.getSQLState();
        }
    }

    private List<ChatGroup> findNewChatGroupsInRadius(Connection conn, Location userLoc, List<ChatGroup> currGroups) {
        double latitude = userLoc.getLatitude();
        double longitude = userLoc.getLongitude();
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


        List<ChatGroup> nearbyChatGroups = new ArrayList<ChatGroup>();
        ChatGroup nearbyGroup; // Placed here for debug purposes
        try {
            // GET CHATGROUPS NEAR USER
            String groupQuery = "SELECT * FROM ChatGroups WHERE Latitude BETWEEN " + lat2 + " AND " + lat1 +
                    " AND Longitude BETWEEN " + long2 + " AND " + long1;
            if (currGroups != null) {
                for (int i = 0; i < currGroups.size(); ++i) {
                    groupQuery += " AND ChatId != " + currGroups.get(i).getChatId();
                }
            }

            ResultSet rs = conn.createStatement().executeQuery(groupQuery);

            while (rs.next()) {
                nearbyGroup = new ChatGroup(rs.getString("Interest"), rs.getInt("ChatId"), rs.getInt("GroupSize"),
                        rs.getDouble("Latitude"), rs.getDouble("Longitude"));
                nearbyChatGroups.add(nearbyGroup);
            }

        } catch (SQLException e) {
            String error = e.getSQLState();
            return null;
        }
        return nearbyChatGroups;
    }

    private List<ChatGroup> updateCurrentChatGroups(Connection conn, List<ChatGroup> currGroups, Location userLoc,
                                                    double oldLat, double oldLong, String token) {
        /*
        String chatIdQuery = "SELECT ChatId FROM ChatUsers WHERE UserId=\"" + userLoc.getUser_id() + "\"";
        ResultSet chatIds = conn.createStatement().executeQuery(chatIdQuery);

        currChatGroups = new ArrayList<>();
        while (chatIds.next()) {
            String chatQuery = "SELECT * FROM ChatGroups WHERE ChatId=" + chatIds.getInt("ChatId");
            ResultSet chatGroups = conn.createStatement().executeQuery(chatQuery);
            if (chatGroups.next()) {
                ChatGroup chatGroup = new ChatGroup(chatGroups.getString("Interest"), chatGroups.getInt("ChatId"),
                        chatGroups.getInt("GroupSize"), chatGroups.getDouble("Latitude"), chatGroups.getDouble("Longitude"));

                double newDist = userLoc.distanceTo(chatGroup.getLatitude(), chatGroup.getLongitude());
                if (newDist > DIST) {  // CAN ONLY OCCUR IF THERE IS SOMEONE ELSE IN GROUP
                    // Remove from group and notify users
                    leaveChatGroup(conn, userLoc.getUser_id(), chatGroup.getChatId());
                } else {
                    currChatGroups.add(chatGroup);
                }
            }

        }*/
        for (ChatGroup group : currGroups) {
            group.moveMember(oldLat, oldLong, userLoc.getLatitude(), userLoc.getLongitude());
            double newDist = userLoc.distanceTo(group.getLatitude(), group.getLongitude());
            if (newDist > DIST && group.getGroupSize() > 1) {
                group.removeUserFromGroup(userLoc.getUser_id(), oldLat, oldLong);
                leaveChatGroup(conn, userLoc.getUser_id(), group);
            }
        }
        return currGroups;
    }

    private void leaveChatGroup(Connection conn, String user_id, ChatGroup group) {
        try {
            // Remove from DB
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

            /*
            String tokenQuery = "SELECT Token FROM UserInfo WHERE UserId IN (\"" + userIds.get(0) + "\"";
            for (int i = 1; i < userIds.size(); ++i) {
                tokenQuery += ", \"" + userIds.get(i) + "\"";
            }
            tokenQuery += ")";
            ResultSet tokenSet = conn.createStatement().executeQuery(tokenQuery);

            List<String> userTokens = new ArrayList<>();
            while (tokenSet.next()) {
                userTokens.add(tokenSet.getString("Token"));
            }
            */

            // NOTIFY THEM ALL THROUGH GCM MESSAGE
            Sender sender = new Sender(API_KEY);
            Message message = new Message.Builder().addData("Action", "LeavingGroup").addData("UserId", user_id)
                    .addData("ChatId", "" + group.getChatId()).build();
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
