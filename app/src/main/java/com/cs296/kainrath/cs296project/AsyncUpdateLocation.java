package com.cs296.kainrath.cs296project;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.locationApi.LocationApi;
import com.cs296.kainrath.cs296project.backend.locationApi.model.Location;
import com.cs296.kainrath.cs296project.backend.locationApi.model.LocationCollection;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Darius on 4/5/2016.
 */
public class AsyncUpdateLocation extends AsyncTask<Double, Void, Integer> {
    private LocationApi locationService = null;
    private String userID = null;
    private Context context = null;

    public AsyncUpdateLocation(String userID, Context context) {
        this.userID = userID;
        this.context = context;
    }

    @Override  // Runs in a separate thread
    protected Integer doInBackground(Double... params) {
        if (locationService == null) {
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
        }
        LocationCollection nearby_users = null;
        try {
            // locationService.updateLocation(userID, params[0], params[1]).execute();
            nearby_users = locationService.updateLocation(userID, params[0], params[1]).execute();
        } catch (IOException e) {

        }
        if (nearby_users == null) {
            return -1;
        } else {
            return nearby_users.size();
        }
    }

    @Override
    protected void onPostExecute(Integer count) {
        if (count == -1) { // Failed to check nearby users
            Toast.makeText(context, "Failed to check for nearby users", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Found " + count + " users nearby", Toast.LENGTH_SHORT).show();
        }
    }
}
