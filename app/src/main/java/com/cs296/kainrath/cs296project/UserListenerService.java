package com.cs296.kainrath.cs296project;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.android.gms.gcm.GcmListenerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class UserListenerService extends GcmListenerService {
    private static String TAG = "GCM Listener Service";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (!LocationTrackerService.isInstanceCreated()) {
            Log.d(TAG, "Received GCM while inactive, ignore");
            return;
        }

        String action = data.getString("Action");
        if (action.equals("LeavingGroup")) {
            GlobalVars.removeFromGroup(data.getInt("ChatId"), data.getString("UserId"));
        } else if (action.equals("JoiningGroup")) {
            GlobalVars.addToGroup(data.getInt("ChatId"), data.getString("UserId"));
        } else if (action.equals("NewMessage")) {
            newMessage(from, data);
        } else {
            Log.d(TAG, "Received unknown gcm message");
        }
    }

    private void newMessage(String from, Bundle data) {
        String user = data.getString("UserId");
        int chatId = data.getInt("ChatId");
        String message = data.getString("Message");
        // DO SOMETHING WITH MESSAGE
    }
}
