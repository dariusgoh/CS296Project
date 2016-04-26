package com.cs296.kainrath.cs296project.backend;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import static com.googlecode.objectify.ObjectifyService.ofy;

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

    /*
    static {
        ObjectifyService.register(User.class);
    }*/

    @ApiMethod(name = "sendMessage")
    public void sendMessage(@Named("userId") String userId, @Named("chatId") Integer chatId, @Named("message") String message) {
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            ResultSet tokenSet = conn.createStatement().executeQuery("SELECT Token FROM ChatUsers WHERE ChatId=" + chatId);
            List<String> tokens = new ArrayList<>();
            while (tokenSet.next()) {
                tokens.add(tokenSet.getString("Token"));
            }

            Sender sender = new Sender(API_KEY);
            Message msg = new Message.Builder().addData("Action", "NewMessage").addData("UserId", userId)
                    .addData("ChatId", "" + chatId).addData("Message", message).build();

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

    /**
     * Returns a UserList with matching users
     *
     * @param
     */
    @ApiMethod(
            name = "getAll",
            path = "user",
            httpMethod = ApiMethod.HttpMethod.GET
    )
    public static UserList getAll(@Named("user_ids") List<String> user_ids) {
        // Get all users with id in list
        /*
        Map<String, User> users_map = ofy().load().type(User.class).ids(user_ids);
        List<User> users_list = new ArrayList<User>();
        users_list.addAll(users_map.values());
        UserList users = new UserList();
        users.setUsers(users_list);
        return users;
        */
        UserList userList = null;
        Set<User> users = new TreeSet<User>();
        for (String user : user_ids) {
            users.add(getOne(user));
        }
        return userList;
    }

    /**
     * Returns the {@link User} with the corresponding ID.
     *
     * @param user_id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code User} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public static User getOne(@Named("user_id") String user_id) {
        logger.info("Getting User with ID: " + user_id);
        //User user = ofy().load().type(User.class).id(user_id).now();
        //if (user == null) {
        // throw new NotFoundException("Could not find User with ID: " + user_id);
        //}
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
     * Returns the {@link User} with the corresponding ID.
     *
     * @param user_id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code User} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public User get(@Named("user_id") String user_id, @Named("email") String email, @Named("token") String token) {
        logger.info("Getting User with ID: " + user_id);
        //User user = ofy().load().type(User.class).id(user_id).now();
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
            if (result.next()) {
                ResultSet int_result = conn.createStatement().executeQuery("SELECT Interest FROM UserInterests WHERE UserId=\"" +
                        user_id + "\"");
                while (int_result.next()) {
                    interest_set.add(int_result.getString("Interest"));
                }
                conn.createStatement().executeUpdate("UPDATE UserInfo SET Token=\"" + token + "\" WHERE UserId=\""
                        + user_id + "\"");
            } else { // NEW USER, NOT IN DATABASE
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

        /*
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setId(user_id);
        }
        user.setToken(token);
        ofy().save().entity(user).now();
        //if (user == null) {
            // throw new NotFoundException("Could not find User with ID: " + user_id);
        //}
        return user;
        */
    }
    //

    /**
     * Inserts a new {@code User}.
     */
    @ApiMethod(
            name = "insert",
            path = "user",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void insert(User user) {
        /*
        ofy().save().entity(user).now();
        logger.info("Created User.");
        */
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            String insertInfo = "INSERT INTO UserInfo (UserId, Email, Token) VALUES (\"" + user.getId() +
                    "\", \"" + user.getEmail() + "\", \"" + user.getToken() + "\")";
            conn.createStatement().executeUpdate(insertInfo);

            List<String> interests = new ArrayList<String>(user.getInterests());
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
     * Updates an existing {@code User}.
     *
     * @param user_id the ID of the entity to be updated
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code user_id} does not correspond to an existing
     *                           {@code User}
     */
    @ApiMethod(
            name = "update",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public void update(@Named("user_id") String user_id, @Named("add") List<String> add, @Named("remove") List<String> remove) throws NotFoundException {
        /*
        checkExists(user_id);
        ofy().save().entity(user).now();
        logger.info("Updated User: " + user);
        return ofy().load().entity(user).now();
        */
        // USER MUST EXIST IF THEY ARE ABLE TO CALL UPDATE (AT LEAST FROM THE APP)
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            if (add.get(0) != "") {
                String adds = "INSERT INTO UserInterests (UserId, Interest) VALUES (\"" +
                        user_id + "\", \"" + add.get(0) + "\")";
                for (int i = 1; i < add.size(); ++i) {
                    adds += ", (\"" + user_id + "\", \"" + add.get(i) + "\")";
                }
                conn.createStatement().executeUpdate(adds);
            }

            if (remove.get(0) != "") {
                String delete = "DELETE FROM UserInterests WHERE UserId=\"" + user_id + "\" AND Interest IN (\"" +
                        remove.get(0) + "\"";
                for (int i = 1; i < remove.size(); ++i) {
                    delete += ", \"" + remove.get(i) + "\"";
                }
                delete += ")";
                conn.createStatement().execute(delete);
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
     * Deletes the specified {@code User}.
     *
     * @param user_id the ID of the entity to delete
     * @throws NotFoundException if the {@code user_id} does not correspond to an existing
     *                           {@code User}
     */
    @ApiMethod(
            name = "remove",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("user_id") String user_id) throws NotFoundException {
        /*
        checkExists(user_id);
        ofy().delete().type(User.class).id(user_id).now();
        logger.info("Deleted User with ID: " + user_id);
        */
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url);

            String delete = "DELETE FROM UserInfo, UserInterests WHERE UserId=\"" + user_id + "\"";
            conn.createStatement().execute(delete);
            conn.close();

        } catch (ClassNotFoundException e) {
            String error = e.getMessage();
            return;
        } catch (SQLException e) {
            String error = e.getSQLState();
            return;
        }
    }

    /*
    private void checkExists(String user_id) throws NotFoundException {
        try {
            ofy().load().type(User.class).id(user_id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find User with ID: " + user_id);
        }
    }*/
}