package com.cs296.kainrath.cs296project;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/* Service that tracks the users location
 * The service runs on the main thread but spawns
 * a new thread when the location changes in order
 * to connect to the server
 */

public class LocationTrackerService extends Service {

    private static final String TAG = "LocTrackServ";
    private static final String USER_ID = "USER_ID";
    private String user_id;
    private String email;
    private String provider;
    //private Activity activity;

    private LocationManager locationManager;

    private UserLocationListener locListener;

    private static LocationTrackerService instance = null;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private class UserLocationListener implements LocationListener {

        Context appContext = null;
        //Activity appActivity = null;

        public UserLocationListener(Context appContext/*, Activity appActivity*/) {
            this.appContext = appContext;
           // this.appActivity = appActivity;
        }

        @Override
        public void onLocationChanged(Location location) {
            // send information to the database
            Log.d(TAG, "Updating location");
            GlobalVars.setLatLong(location.getLatitude(), location.getLongitude());
            new AsyncUpdateLocation(user_id, email, appContext, GlobalVars.getUser().getToken(),
                    GlobalVars.getUser().getInterests(), GlobalVars.getChatGroups()).execute(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                Log.d(TAG, "Status change, out of service");
                // no service
            } else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                Log.d(TAG, "Status changed, temporarily unavailable");
                // temporarily no service
            } else { // status == LocationProvider.AVAILABLE
                Log.d(TAG, "Status changed, available");
                // service is available
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "Provider has been enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "Provider has been disabled");
            Toast.makeText(getBaseContext(), "Please enable GPS for this app",
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying location tracking service");
        instance = null;
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locListener);
                Log.d(TAG, "Starting AsyncDeactivateUser");
                new AsyncDeactivateUser(GlobalVars.getLat(), GlobalVars.getLong()).execute(user_id, email);
            } catch (SecurityException e) {
                // Already checked for permissions in MainActivity
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "starting location tracking service");
        user_id = intent.getStringExtra(USER_ID);
        email = GlobalVars.getUser().getEmail();
        provider = intent.getStringExtra("PROVIDER");
        super.onStartCommand(intent, flags, startId);
        try {
            locationManager.requestLocationUpdates(provider, 3000, 5, locListener);

            Log.d(TAG, "Requested location updates");
        } catch (SecurityException e) {
            Log.d(TAG, "Security exception when creating location tracking service");
            // Already checked for permissions in MainActivity
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize location listener
        Log.d(TAG, "Creating location tracking service");
        instance = this;
        locListener = new UserLocationListener(this.getApplicationContext());
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }
}
