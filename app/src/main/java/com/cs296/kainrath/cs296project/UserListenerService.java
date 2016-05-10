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

/*
 * Service that receives the GCM notifications
 */
public class UserListenerService extends GcmListenerService {
    private static String TAG = "GCM Listener Service";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (!LocationTrackerService.isInstanceCreated()) {
            Log.d(TAG, "Received GCM while inactive, ignore");
            return;
        }

        String action = data.getString("Action");
        int chatId = Integer.parseInt(data.getString("ChatId"));
        String recvd_email = data.getString("Email");
        String user_email = GlobalVars.getUser().getEmail();
        if (action.equals("LeavingGroup") && !recvd_email.equals(user_email)) {
            Log.d(TAG, "A user is leaving a group");
            GlobalVars.removeFromGroup(chatId, recvd_email);
            this.sendBroadcast(new Intent("ChatUpdate")); // send broadcast to MainActivity list
        } else if (action.equals("JoiningGroup") && !recvd_email.equals(user_email)) {
            Log.d(TAG, "A user is joining a group");
            GlobalVars.addToGroup(chatId, recvd_email);
            this.sendBroadcast(new Intent("ChatUpdate")); // Send broadcast to MainActivity list
        } else if (action.equals("NewMessage")) {
            Log.d(TAG, "Received a message from a chat group");
            GlobalVars.addMessage(chatId, recvd_email, data.getString("Message"));
            Intent intent = new Intent("MessageUpdate");
            intent.putExtra("ChatId", chatId);
            this.sendBroadcast(intent);                   // Send broadcast to ChatGroupActivity list
        } else {
            Log.d(TAG, "Received unknown gcm message");
            return;
        }
    }
}
