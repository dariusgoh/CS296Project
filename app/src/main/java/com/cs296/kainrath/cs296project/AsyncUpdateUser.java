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
    private String addAll = "";
    private String remAll = "";
    private static String TAG = "AsyncUpdateUser";

    public AsyncUpdateUser(List<String> add, List<String> remove) {
        if (!add.get(0).equals("")) {
            addAll += add.get(0);
            for (int i = 1; i < add.size(); ++i) {
                addAll += ",,," + add.get(i);
            }
        }
        if (!remove.get(0).equals("")) {
            remAll += remove.get(0);
            for (int i = 1; i < remove.size(); ++i) {
                remAll += ",,," + remove.get(i);
            }
        }
    }

    @Override
    protected Void doInBackground(String... params) {
        if (userService == null) {
            Log.d(TAG, "userService is null, need to generate");
            UserApi.Builder builder = new UserApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null)
                    .setRootUrl("https://cs296-backend.appspot.com/_ah/api/");

            userService = builder.build();
            Log.d(TAG, "userService has been generated");
        } else {
            Log.d(TAG, "userService is not null");
        }

        try {
            Log.d(TAG, "Calling server update function");
            userService.update(params[0], addAll, remAll).execute();
            Log.d(TAG, "Returned from server function");
        } catch (IOException e) {

        }
        return null;
    }
}
