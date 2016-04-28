package com.cs296.kainrath.cs296project;

import android.os.AsyncTask;
import android.util.Log;

import com.cs296.kainrath.cs296project.backend.userApi.UserApi;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;

/**
 * Created by kainrath on 4/25/16.
 */
public class AsyncSendMessage extends AsyncTask<Void, Void, Void> {
    private int chatId;
    private String message;
    private String email;
    private static String TAG = "AsyncSendMsg";

    public AsyncSendMessage(String email, int chatId, String message) {
        this.message = message;
        this.chatId = chatId;
        this.email = email;
    }

    @Override
    protected Void doInBackground(Void... params) {
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
                    });*/

            // End options for devappserver

            userService = builder.build();
            GlobalVars.userApi = userService;
        }

        // SEND MESSAGE
        try {
            Log.d(TAG, "Sending message");
            userService.sendMessage(email, chatId, message).execute();
            Log.d(TAG, "Sent message");
        } catch (IOException e) {
            Log.d(TAG, "Failed to sent message");
        }

        return null;
    }
}
