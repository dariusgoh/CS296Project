package com.cs296.kainrath.cs296project;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.android.gms.gcm.GcmListenerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class UserListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        User new_user = new User();
        new_user.setId(data.getString("userId"));
        new_user.setEmail(data.getString("userEmail"));

        String new_user_interests = data.getString("interests");

        // Need to breaks apart interest string
        List<String> interest_list = new ArrayList<String>();
        String[] interest_array = new_user_interests.split("\n");
        for (int i = 0; i < interest_array.length; ++i) {
            interest_list.add(interest_array[i]);
        }
        new_user.setInterests(interest_list);

        // Store new user data
        GlobalVars.addNearbyUser(new_user);
    }
}
