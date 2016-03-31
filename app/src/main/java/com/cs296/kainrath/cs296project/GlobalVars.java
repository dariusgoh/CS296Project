package com.cs296.kainrath.cs296project;

import android.app.Application;
import android.os.Bundle;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;

import java.util.ArrayList;

/**
 * Created by kainrath on 3/24/16.
 */
public class GlobalVars extends Application {
    private User user = null;
    private boolean failed = false;

    private static final String email = "user_email";
    private static final String id = "user_id";
    private static final String interests = "interests";

    public void setUser(User user) { this.user = user; }

    public User getUser() { return this.user; }

    public boolean getFailed() { return failed; }

    public void setFailed(boolean result) { this.failed = result; }


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
