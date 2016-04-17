package com.cs296.kainrath.cs296project.backend;

import static com.googlecode.objectify.ObjectifyService.ofy;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final Logger logger = Logger.getLogger(UserEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        ObjectifyService.register(User.class);
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
        Map<String, User> users_map = ofy().load().type(User.class).ids(user_ids);
        List<User> users_list = new ArrayList<User>();
        users_list.addAll(users_map.values());
        UserList users = new UserList();
        users.setUsers(users_list);
        return users;
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
        User user = ofy().load().type(User.class).id(user_id).now();
        //if (user == null) {
        // throw new NotFoundException("Could not find User with ID: " + user_id);
        //}
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
        User user = ofy().load().type(User.class).id(user_id).now();
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
    }
    //

    /**
     * Inserts a new {@code User}.
     */
    @ApiMethod(
            name = "insert",
            path = "user",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public User insert(User user) {
        ofy().save().entity(user).now();
        logger.info("Created User.");

        return ofy().load().entity(user).now();
    }

    /**
     * Updates an existing {@code User}.
     *
     * @param user_id the ID of the entity to be updated
     * @param user    the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code user_id} does not correspond to an existing
     *                           {@code User}
     */
    @ApiMethod(
            name = "update",
            path = "user/{user_id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public User update(@Named("user_id") String user_id, User user) throws NotFoundException {
        checkExists(user_id);
        ofy().save().entity(user).now();
        logger.info("Updated User: " + user);
        return ofy().load().entity(user).now();
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
        checkExists(user_id);
        ofy().delete().type(User.class).id(user_id).now();
        logger.info("Deleted User with ID: " + user_id);
    }

    private void checkExists(String user_id) throws NotFoundException {
        try {
            ofy().load().type(User.class).id(user_id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find User with ID: " + user_id);
        }
    }
}