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

        // TODO: RETRIEVE AND STORE EMAIL
        String action = data.getString("Action");
        int chatId = Integer.parseInt(data.getString("ChatId"));
        String email = data.getString("Email");
        if (action.equals("LeavingGroup")) {
            Log.d(TAG, "A user is leaving a group");
            GlobalVars.removeFromGroup(chatId, email);
            this.sendBroadcast(new Intent("ChatUpdate"));
        } else if (action.equals("JoiningGroup")) {
            Log.d(TAG, "A user is joining a group");
            GlobalVars.addToGroup(chatId, email);
            this.sendBroadcast(new Intent("ChatUpdate"));
        } else if (action.equals("NewMessage")) {
            Log.d(TAG, "Received a message from a chat group");
            GlobalVars.addMessage(chatId, email, data.getString("Message"));
            Intent intent = new Intent("MessageUpdate");
            intent.putExtra("ChatId", chatId);
            this.sendBroadcast(intent);
        } else {
            Log.d(TAG, "Received unknown gcm message");
            return;
        }

    }
}
