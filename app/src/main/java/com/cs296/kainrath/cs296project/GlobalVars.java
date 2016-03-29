package com.cs296.kainrath.cs296project;

import android.app.Application;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by kainrath on 3/24/16.
 */
public class GlobalVars extends Application{
    private User user = null;
    private boolean failed = false;

    public void setUser(User user) { this.user = user; }

    public User getUser() { return this.user; }

    public boolean getFailed() { return failed; }

    public void setFailed(boolean result) { this.failed = result; }

}
