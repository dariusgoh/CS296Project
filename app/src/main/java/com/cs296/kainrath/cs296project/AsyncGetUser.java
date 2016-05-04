package com.cs296.kainrath.cs296project;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.cs296.kainrath.cs296project.backend.userApi.UserApi;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;


// DON'T USE THIS CLASS ANYMORE!!!
/*

public class AsyncGetUser extends AsyncTask<String, Void, User> {
    private Activity activity = null;

    public AsyncGetUser(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected User doInBackground(String... params) {
        UserApi userService = GlobalVars.userApi;
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
                    });

            // End options for devappserver

            userService = builder.build();
            GlobalVars.userApi = userService;
        }
        User user = null;
        try {
            //user = userService.get(params[0]).execute();
            if (user == null) {
                user = new User();
                user.setId(params[0]);
                user.setEmail(params[1]);
                userService.insert(user).execute();
            }
            GlobalVars.setUser(user);
        } catch (IOException e) {
            GlobalVars.setFailed(true);
        }
        return user;
    }

    @Override
    protected void onPostExecute(User user) {
        if (user == null) {
            activity.startActivity(new Intent(activity.getBaseContext(), CreateUser.class));
        } else {
            activity.startActivity(new Intent(activity.getBaseContext(), MainActivity.class));
        }
    }
}
*/
