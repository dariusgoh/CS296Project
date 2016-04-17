package com.cs296.kainrath.cs296project;

import android.app.Application;
import android.os.Bundle;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by kainrath on 3/24/16.
 */
public class GlobalVars extends Application {
    private static List<User> nearby_users = null;
    private static User user = null;
    private static boolean failed = false;

    private static double latit = 0;
    private static double longit = 0;
    // private boolean has_user = false;

    private static final String email = "user_email";
    private static final String id = "user_id";
    private static final String interests = "interests";

    public static void setLatLong(double latitude, double longitude) {
        latit = latitude;
        longit = longitude;
    }

    public static double getLat() {
        return latit;
    }

    public static double getLong() {
        return longit;
    }

    public static void setUser(User ouser) { user = ouser; }

    public static User getUser() { return user; }

    public static boolean getFailed() { return failed; }

    public static void setFailed(boolean result) { failed = result; }

    public static String getNearbyUserString() {
        if (nearby_users == null || nearby_users.isEmpty()) {
            return "No Users Nearby";
        }
        String result = "";
        for (User n_user : nearby_users) {
            result += n_user.getEmail() + "\n";
        }
        return result;
    }

    public static List<User> getNearbyUsers() {
        if (nearby_users == null) {
            nearby_users = new ArrayList<>();
        }
        return nearby_users;
    }

    public static void setNearbyUsers(List<User> onearby_users) {
        nearby_users = onearby_users;
    }

    public static void addNearbyUser(String id, String email, List<String> interests) {
        User nearby_user = new User();
        nearby_user.setId(id);
        nearby_user.setEmail(email);
        nearby_user.setInterests(interests);
        addNearbyUser(nearby_user);
    }

    public static void addNearbyUser(User nearby_user) {
        if (nearby_users == null) {
            nearby_users = new ArrayList<User>();
            nearby_users.add(nearby_user);
            return;
        }
        for (int i = 0; i < nearby_users.size(); ++i) {
            if (nearby_users.get(i).getId().equals(nearby_user.getId())) {
                nearby_users.add(i, nearby_user);
                nearby_users.remove(i + 1);
                return;
            }
        }
        nearby_users.add(nearby_user);
    }

    public Bundle saveState(Bundle instance) {
        if (user != null) {
            instance.putString(id, user.getId());
            instance.putString(email, user.getEmail());
            instance.putStringArrayList(interests, (ArrayList<String>) user.getInterests());
        }
        return instance;
    }

    public Bundle restoreState(Bundle instance) {
        if (instance.getString(id) != null) {
            user = new User();
            user.setEmail(instance.getString(email));
            user.setId(instance.getString(id));
            user.setInterests(instance.getStringArrayList(interests));
        }
        return instance;
    }


}
