package com.cs296.kainrath.cs296project;

import android.os.AsyncTask;
import android.util.Log;

import com.cs296.kainrath.cs296project.backend.userApi.UserApi;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kainrath on 3/28/16.
 */
public class AsyncUpdateUser extends AsyncTask<String, Void, Void> {
    private UserApi userService = null;
    private List<String> removedInterests = null;
    private List<String> addedInterests = null;
    private static String TAG = "AsyncUpdateUser";

    public AsyncUpdateUser(List<String> add, List<String> remove) {
        removedInterests = remove;
        addedInterests = add;
        Log.d(TAG, "removing " + removedInterests.size() + ", adding: " + addedInterests.size());
        Log.d(TAG, "removing(0): " + removedInterests.get(0) + ", adding(0): " + addedInterests.get(0));
    }

    @Override
    protected Void doInBackground(String... params) {
        if (userService == null) {
            Log.d(TAG, "userService is null, need to generate");
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
            Log.d(TAG, "userService has been generated");
        } else {
            Log.d(TAG, "userService is not null");
        }

        try {
            Log.d(TAG, "Calling server update function");
            userService.update(params[0], addedInterests, removedInterests).execute();
            Log.d(TAG, "Returned from server function");
        } catch (IOException e) {
            // Exception...
        }
        return null;
    }
}
