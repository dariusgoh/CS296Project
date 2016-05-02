package com.cs296.kainrath.cs296project;

import android.os.AsyncTask;
import android.util.Log;

import com.cs296.kainrath.cs296project.backend.locationApi.LocationApi;
import com.cs296.kainrath.cs296project.backend.locationApi.model.ChatGroup;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by kainrath on 4/5/16.
 */
public class AsyncDeactivateUser extends AsyncTask<String, Void, Void> {
    private double lat, lon;
    private String TAG = "AsyncDeact";
    private String chatIds = "";

    public AsyncDeactivateUser(double lat, double lon) {
        List<ChatGroup> chatGroups = GlobalVars.getChatGroups();
        if (chatGroups != null && !chatGroups.isEmpty()) {
            chatIds += chatGroups.get(0).getChatId();
            for (int i = 1; i < chatGroups.size(); ++i) {
                chatIds += "," + chatGroups.get(i).getChatId();
            }
        }

        GlobalVars.emptyChatGroup();
        this.lat = lat;
        this.lon = lon;
        Log.d(TAG, "Creating async object, lat: " + lat + ", lon: " + lon);
    }

    @Override
    protected Void doInBackground(String... params) {
        LocationApi locationService = GlobalVars.locationApi;
        if (locationService == null) {
            Log.d(TAG, "locationService is null, generating locationService");
            LocationApi.Builder builder = new LocationApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null)
                    .setRootUrl("https://cs296-backend.appspot.com/_ah/api/");
            // options for running against local devappserver
            // - 10.0.2.2 is localhost's IP address in Android Emulator
            // - turn off compression when running against local devappserver
            // for local testing
                    /*
                    .setRootUrl("http://10.0.2.2:8080/_ah/api/")
                    .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                        @Override
                        public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                            abstractGoogleClientRequest.setDisableGZipContent(true);
                        }
                    });*/

            // End options for devappserver

            locationService = builder.build();
            Log.d(TAG, "locationService generated");
            GlobalVars.locationApi = locationService;
        }
        try {
            Log.d(TAG, "Calling server function");
            locationService.deactivateUser(params[0], params[1], lat, lon, chatIds).execute();
            Log.d(TAG, "Back from server function");
        } catch (IOException e) {

        }
        return null;
    }

}
