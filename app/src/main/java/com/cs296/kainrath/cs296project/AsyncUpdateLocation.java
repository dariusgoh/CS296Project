package com.cs296.kainrath.cs296project;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.locationApi.LocationApi;
import com.cs296.kainrath.cs296project.backend.locationApi.model.ChatGroup;
import com.cs296.kainrath.cs296project.backend.locationApi.model.ChatGroupList;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by Darius on 4/5/2016.
 */
public class AsyncUpdateLocation extends AsyncTask<Double, Void, List<ChatGroup>> {
    private String userID = null;
    private String email = null;
    private Context context = null;
    private String token = null;
    private List<String> interests = null;
    private String chatIdString = " ";
    private static final String TAG = "AsyncLocUpdate";

    public AsyncUpdateLocation(String userID, String email, Context context, String token,
                               List<String> interests, List<ChatGroup> chatGroups) {
        this.userID = userID;
        this.email = email;
        this.context = context;
        this.token = token;
        this.interests = interests;
        if (chatGroups != null && !chatGroups.isEmpty()) {
            chatIdString = "" + chatGroups.get(0).getChatId();
            for (int i = 1; i < chatGroups.size(); ++i) {
                chatIdString += "," + chatGroups.get(i).getChatId();
            }
        }
        Log.d(TAG, "userId: " + userID + ", token: " + token);
        if (chatGroups == null || chatGroups.isEmpty()) {
            Log.d(TAG, "chatGroups is null or empty");
        } else {
            Log.d(TAG, "" + chatIdString);
        }
    }

    @Override  // Runs in a separate thread
    protected List<ChatGroup> doInBackground(Double... params) {
        LocationApi locationService = GlobalVars.locationApi;
        if (locationService == null) {
            Log.d(TAG, "LocationApi is null, generating locationService");
            LocationApi.Builder builder = new LocationApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null)
                    .setRootUrl("https://cs296-backend.appspot.com/_ah/api/");

            locationService = builder.build();
            Log.d(TAG, "locationService generated");
            GlobalVars.locationApi = locationService;
        }
        ChatGroupList chatGroupList = null;
        try {
            chatGroupList = locationService.updateLocation(userID, email, params[0], params[1], interests, token, chatIdString).execute();
            Log.d(TAG, "Update location");
        } catch (IOException e) {
            Log.d(TAG, "IOException when trying to update location");
        }
        if (chatGroupList != null && chatGroupList.getChatGroups() != null) {
            List<ChatGroup> chatGroups = chatGroupList.getChatGroups();
            Log.d(TAG, "update location returned #" + chatGroups.size());
            return chatGroups;
        } else {
            Log.d(TAG, "update location return null");
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<ChatGroup> chatGroups) {
        if (chatGroups == null) {
            Toast.makeText(context, "Failed to find nearby chat groups", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "No nearby chat groups");
        } else {
            Log.d(TAG, "Found nearby chat groups");
            GlobalVars.addChatGroups(chatGroups);
            context.sendBroadcast(new Intent("ChatUpdate"));
        }
    }
}
