package com.cs296.kainrath.cs296project;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.iid.InstanceIDListenerService;

public class TokenRefreshListener extends InstanceIDListenerService {

    // Regenerate token if needed
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(CreateUser.USER_ID, GlobalVars.getUser().getId());
        startService(intent);
    }
}
