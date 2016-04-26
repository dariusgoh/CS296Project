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
    //private LocationApi locationService = null;
    private double lat, lon;
    private String TAG = "AsyncDeact";
    private List<Integer> chatIds;

    public AsyncDeactivateUser(double lat, double lon) {
        this.chatIds = new ArrayList<>();
        for (ChatGroup group : GlobalVars.getChatGroups()) {
            chatIds.add(group.getChatId());
        }
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
            locationService.deactivateUser(params[0], lat, lon, chatIds).execute();
            Log.d(TAG, "Back from server function");
            GlobalVars.setChatGroups(null);
        } catch (IOException e) {

        }
        return null;
    }

}
