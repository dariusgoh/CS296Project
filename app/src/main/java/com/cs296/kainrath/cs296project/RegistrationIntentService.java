package com.cs296.kainrath.cs296project;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.userApi.UserApi;
import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class RegistrationIntentService extends IntentService {

    public static final String REQUEST_RESULT = "user_received";
    public static final String ACTION_GET_TOKEN = "com.cs296.kainrath.cs296project.action.getToken";
    public static final String ACTION_RESET_TOKEN = "com.cs296.kainrath.cs296project.action.resetToken";
    private static final String TAG = "RegistIntent";

    private static final int MAX_DELAY = 16000;

    private UserApi userApi = null;
    private String token = null;
    private int delayTime = 1000;  // Milliseconds

    public RegistrationIntentService() {
        super("RegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Log.d(TAG, "Received Intent");
            if (intent.getAction() == ACTION_GET_TOKEN || intent.getAction() == ACTION_RESET_TOKEN) {
                Log.d(TAG, "Action request GCM token");
                delayTime = 1000;
                User user = null;
                String email = intent.getStringExtra(CreateUser.EMAIL);
                String user_id = intent.getStringExtra(CreateUser.USER_ID);
                while ((user = getTokenAndUserData(user_id, email)) == null & delayTime < MAX_DELAY) {
                    try {
                        Thread.sleep(delayTime);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread sleep failed");
                    }
                    delayTime *= 2;
                }
                if (user == null) {  // Can't set token, exit
                    Log.d(TAG, "Failed to generate GCM token");
                    System.exit(1);
                } else {
                    GlobalVars.setUser(user);
                    if (intent.getAction() == ACTION_GET_TOKEN) {
                        ResultReceiver userResult = intent.getParcelableExtra(REQUEST_RESULT);
                        userResult.send(1, null);
                    }
                    Log.d(TAG, "Successfully generated GCM token");
                }
                token = null; // For if reset token is called
            }
        }
    }

    private User getTokenAndUserData(String user_id, String email) {
        if (token == null) {
            try {
                InstanceID instanceID = InstanceID.getInstance(this);
                token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.d(TAG, "SENDER_ID " + getString(R.string.gcm_defaultSenderId));
                Log.d(TAG, "Received token from GCM");
            } catch (IOException e) {
                return null;
            }
        }

        // Send it to the DB and store it
        if (userApi == null) {
            Log.d(TAG, "userApi connection is NULL");
            UserApi.Builder builder = new UserApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null)
                    .setRootUrl("https://cs296-backend.appspot.com/_ah/api/");

            userApi = builder.build();
        } else {
            Log.d(TAG, "userApi is connected");
        }

        User user = null;
        try {
            Log.d(TAG, "Attempting to send token to DB");
            user = userApi.get(user_id, email, token).execute();
            Log.d(TAG, "Sent token to DB result, user = " + user);
        } catch (IOException e) {
            Log.d(TAG, "Failed to connect to backend");
        }
        return user;
    }

}
