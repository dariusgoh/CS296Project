package com.cs296.kainrath.cs296project;

import android.app.Service;
import android.content.Context;
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
            Log.d(TAG, "A user is leaving a group");
            int chatId = Integer.parseInt(data.getString("ChatId"));
            GlobalVars.removeFromGroup(chatId, data.getString("UserId"));
        } else if (action.equals("JoiningGroup")) {
            Log.d(TAG, "A user is joining a group");
            int chatId = Integer.parseInt(data.getString("ChatId"));
            GlobalVars.addToGroup(chatId, data.getString("UserId"));
        } else if (action.equals("NewMessage")) {
            Log.d(TAG, "Received a message from a chat group");
            newMessage(from, data);
        } else {
            Log.d(TAG, "Received unknown gcm message");
            return;
        }
        this.sendBroadcast(new Intent("ChatUpdate"));
    }

    private void newMessage(String from, Bundle data) {
        String user = data.getString("UserId");
        int chatId = Integer.parseInt(data.getString("ChatId"));
        String message = data.getString("Message");
        // DO SOMETHING WITH MESSAGE
    }
}
