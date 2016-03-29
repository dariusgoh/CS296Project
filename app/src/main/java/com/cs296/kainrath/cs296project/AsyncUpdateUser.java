package com.cs296.kainrath.cs296project;

import android.os.AsyncTask;

import com.cs296.kainrath.cs296project.backend.userApi.UserApi;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;

/**
 * Created by kainrath on 3/28/16.
 */
public class AsyncUpdateUser extends AsyncTask<User, Void, Void> {
    private UserApi userService = null;

    @Override
    protected Void doInBackground(User... params) {
        if (userService == null) {
            UserApi.Builder builder = new UserApi.Builder(AndroidHttp.newCompatibleTransport(),
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

            userService = builder.build();
        }
        try {
            userService.update(params[0].getId(), params[0]).execute();
        } catch (IOException e) {
            // Exception...
        }
        return null;
    }
}
