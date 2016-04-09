package com.cs296.kainrath.cs296project;

import android.os.AsyncTask;

import com.cs296.kainrath.cs296project.backend.locationApi.LocationApi;
import com.cs296.kainrath.cs296project.backend.userApi.UserApi;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;

/**
 * Created by kainrath on 4/5/16.
 */
public class AsyncDeactivateUser extends AsyncTask<String, Void, Void> {
    LocationApi locationService = null;

    @Override
    protected Void doInBackground(String... params) {
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
        try {
            locationService.deactivateUser(params[0]).execute();
        } catch (IOException e) {

        }
        return null;
    }

}
