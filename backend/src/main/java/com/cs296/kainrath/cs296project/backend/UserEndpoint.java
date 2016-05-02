package com.cs296.kainrath.cs296project.backend;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.inject.Named;

@Api(
        name = "userApi",
        version = "v1",
        resource = "user",
        namespace = @ApiNamespace(
                ownerDomain = "backend.cs296project.kainrath.cs296.com",
                ownerName = "backend.cs296project.kainrath.cs296.com",
                packagePath = ""
        )
)
public class UserEndpoint {

    private static final String url = "jdbc:google:mysql://cs296-backend:cs296-app-location-data/UserLocation?user=root";
    private static final String driver = "com.mysql.jdbc.GoogleDriver";

    private static final Logger logger = Logger.getLogger(UserEndpoint.class.getName());

    private static final String API_KEY = "AIzaSyAJuwfy0EoirghnDaThupzrqNTDVxsm650";

    @ApiMethod(name = "sendMessage")
    public void sendMessage(@Named("email") String email, @Named("chatId") Integer chatId, @Named("message") String message) {
        if (message == null || message.isEmpty() || email == null || email.isEmpty()) {
            return;
        }
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            // Get users to send message to
            String tokenQuery = "SELECT Token FROM ChatUsers WHERE ChatId=" + chatId;
            ResultSet tokenSet = conn.createStatement().executeQuery(tokenQuery);
            List<String> tokens = new ArrayList<>();
            while (tokenSet.next()) {
                tokens.add(tokenSet.getString("Token"));
            }

            Sender sender = new Sender(API_KEY);
            Message msg = new Message.Builder().addData("Action", "NewMessage").addData("ChatId", "" + chatId)
                    .addData("Message", message).addData("Email", email).build();

            MulticastResult mr = sender.send(msg, tokens, 3);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (SQLException e) {
            String error = e.getSQLState();
            return;
        } catch (IOException e) {
            String error = e.getMessage();
            return;
        }
    }

    // Function not used anymore
    /**
     * @param user_id the ID of the entity to be retrieved
     * @return the user with the corresponding Id
     */
    @ApiMethod(
            name = "get",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public static User getOne(@Named("user_id") String user_id) {
        logger.info("Getting User with ID: " + user_id);

        User user = null;
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            String user_info = "SELECT * FROM UserInfo WHERE UserId=\"" + user_id + "\"";
            ResultSet info = conn.createStatement().executeQuery(user_info);
            if (!info.next()) { // USER NOT IN DATABASE
                conn.close();
                return null;
            }
            user = new User();
            user.setId(user_id);
            user.setEmail(info.getString("Email"));
            user.setToken(info.getString("Token"));

            String user_ints = "SELECT Interest FROM UserInterests WHERE UserId=\"" + user_id + "\"";
            ResultSet ints = conn.createStatement().executeQuery(user_ints);
            Set<String> interests = new TreeSet<String>();
            while (ints.next()) {
                interests.add(ints.getString("Interests"));
            }
            user.setInterests(interests);
            conn.close();
        } catch (ClassNotFoundException e) {
            String error = e.getMessage();
            return null;
        } catch (SQLException e) {
            String error = e.getSQLState();
            return null;
        }

        return user;
    }

    /**
     * Function is called when the app starts up
     * @param user_id the ID of the entity to be retrieved
     * @return the user with the corresponding ID
     */
    @ApiMethod(name = "get")
    public User get(@Named("user_id") String user_id, @Named("email") String email, @Named("token") String token) {
        logger.info("Getting User with ID: " + user_id);
        User user = null;
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);


            String query = "SELECT * FROM UserInfo WHERE UserId=\"" + user_id + "\"";
            ResultSet result = conn.createStatement().executeQuery(query);
            user = new User();
            user.setId(user_id);
            user.setEmail(email);
            user.setToken(token);
            Set<String> interest_set = new TreeSet<String>();
            if (result.next()) { // User is in the database, get interests

                // Make sure the user isn't in the chatgroup/chatuser table
                // This would happen if user doesn't deactivate and app is killed
                if (result.getString("Active").equals("Y")) {
                    ResultSet chats = conn.createStatement().executeQuery("SELECT ChatId FROM ChatUsers WHERE UserId=\"" + user_id + "\"");
                    if (chats.next()) {
                        String chatString = "" + chats.getInt("ChatId");
                        while (chats.next()) {
                            chatString += "," + chats.getInt("ChatId");
                        }
                        LocationEndpoint locationEndpoint = new LocationEndpoint();
                        locationEndpoint.deactivateUser(user_id, email, result.getDouble("Latitude"), result.getDouble("Longitude"), chatString);
                    }
                    conn.createStatement().executeUpdate("UPDATE UserInfo SET Active=\"N\" WHERE UserId=\"" + user_id + "\"");
                }


                ResultSet int_result = conn.createStatement().executeQuery("SELECT Interest FROM UserInterests WHERE UserId=\"" +
                        user_id + "\"");
                while (int_result.next()) {
                    interest_set.add(int_result.getString("Interest"));
                }

                // If the token is new, update the database
                if (!token.equals(result.getString("Token"))) {
                    conn.createStatement().executeUpdate("UPDATE UserInfo SET Token=\"" + token + "\" WHERE UserId=\""
                            + user_id + "\"");
                }



            } else { // New user, not in the database
                String add_user = "INSERT INTO UserInfo (UserId, Email, Token, Active) VALUES (\"" + user_id +
                        "\", \"" + email + "\", \"" + token + "\", \"N\")";
                conn.createStatement().executeUpdate(add_user);
            }
            user.setInterests(interest_set);
            conn.close();

        } catch (ClassNotFoundException e) {
            String error = e.getMessage();
            return null;
        } catch (SQLException e) {
            String error = e.getSQLState();
            return null;
        }
        return user;
    }


    // Function not used anymore
    /**
     * Inserts a new User
     */
    @ApiMethod(name = "insert")
    public void insert(User user) {
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            // Insert user data into table
            String insertInfo = "INSERT INTO UserInfo (UserId, Email, Token, Active) VALUES (\"" + user.getId() +
                    "\", \"" + user.getEmail() + "\", \"" + user.getToken() + "\", \"N\")";
            conn.createStatement().executeUpdate(insertInfo);

            // Insert user interests into table
            List<String> interests = new ArrayList<>(user.getInterests());
            if (!interests.isEmpty()) {
                String insertInts = "INSERT INTO UserInterests (UserId, Interest) VALUES (\"" + user.getId() + "\", \"" +
                        interests.get(0) + "\")";
                for (int i = 0; i < interests.size(); ++i) {
                    insertInts += ", (\"" + user.getId() + "\", \"" + interests.get(i) + "\")";
                }

                conn.createStatement().executeUpdate(insertInts);
            }

            conn.close();
        } catch (ClassNotFoundException e) {
            String error = e.getMessage();
            return;
        } catch (SQLException e) {
            String error = e.getSQLState();
            return;
        }

    }

    /**
     * @param user_id the ID of the entity to be updated
     * @return the updated version of the user
     * @throws NotFoundException if user is not in the database
     */
    @ApiMethod(
            name = "update",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void update(@Named("user_id") String user_id, @Named("add") String add, @Named("remove") String remove)
            throws NotFoundException {
        // Need to parse interests to add/remove since (due to weird formatting)
        boolean modAdd = false;
        boolean modRem = false;
        List<String> addAll = new ArrayList<>();
        if (!add.isEmpty() && !add.equals("")) {
            String[] new_ints_arr = add.split(",,,");
            for (String s : new_ints_arr) {
                addAll.add(s);
            }
            modAdd = true;
        }
        List<String> remAll = new ArrayList<>();
        if (!remove.isEmpty() && !remove.equals("")) {
            String[] old_ints_arr = remove.split(",,,");
            for (String s : old_ints_arr) {
                remAll.add(s);
            }
            modRem = true;
        }
        if (modAdd || modRem) {
            try {
                Class.forName(driver);
                Connection conn = DriverManager.getConnection(url);

                ResultSet validUser = conn.createStatement().executeQuery("SELECT UserId FROM UserInfo WHERE UserId=\"" + user_id + "\"");
                if (!validUser.next() || !validUser.getString("UserId").equals(user_id)) {
                    throw new NotFoundException("User Not in the Database");
                }

                if (modAdd) {
                    String adds = "INSERT INTO UserInterests (UserId, Interest) VALUES (\"" +
                            user_id + "\", \"" + addAll.get(0) + "\")";
                    for (int i = 1; i < addAll.size(); ++i) {
                        adds += ", (\"" + user_id + "\", \"" + addAll.get(i) + "\")";
                    }
                    conn.createStatement().executeUpdate(adds);
                }

                if (modRem) {
                    String delete = "DELETE FROM UserInterests WHERE UserId=\"" + user_id + "\" AND Interest IN (\"" +
                            remAll.get(0) + "\"";
                    for (int i = 1; i < remAll.size(); ++i) {
                        delete += ", \"" + remAll.get(i) + "\"";
                    }
                    delete += ")";
                    conn.createStatement().executeUpdate(delete);
                }

                conn.close();
            } catch (ClassNotFoundException e) {
                String error = e.getMessage();
                return;
            } catch (SQLException e) {
                String error = e.getSQLState();
                return;
            }
        }
    }

    /**
     * @param user_id the ID of the entity to delete
     * @throws NotFoundException if the user_id does not correspond to an existing
     */
    @ApiMethod(
            name = "remove",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("user_id") String user_id) throws NotFoundException {
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            String delete = "DELETE FROM UserInfo, UserInterests WHERE UserId=\"" + user_id + "\"";
            if (conn.createStatement().executeUpdate(delete) == -1) {
                // User will at least be in UserInfo, if not in UserInterests
                throw new NotFoundException("User Not in the Database");
            }
            conn.close();

        } catch (ClassNotFoundException e) {
            String error = e.getMessage();
            return;
        } catch (SQLException e) {
            String error = e.getSQLState();
            return;
        }
    }
}