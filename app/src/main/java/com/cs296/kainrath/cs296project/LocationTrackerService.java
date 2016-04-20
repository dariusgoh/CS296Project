package com.cs296.kainrath.cs296project;

import android.app.Activity;
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

public class LocationTrackerService extends Service {

    private static final String TAG = "LocTrackServ";
    private static final String USER_ID = "USER_ID";
    private String user_id;
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
            new AsyncUpdateLocation(user_id, appContext).execute(location.getLatitude(), location.getLongitude());
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
            Toast.makeText(getBaseContext(), "GPS is on! ",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "Provider has been disabled");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            Toast.makeText(getBaseContext(), "turn GPS on! ",
                    Toast.LENGTH_SHORT).show();
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
                new AsyncDeactivateUser().execute(user_id);
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
        super.onStartCommand(intent, flags, startId);
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
        try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 20, locListener);
            Log.d(TAG, "Requested location updates");
        } catch (SecurityException e) {
            Log.d(TAG, "Security exception when creating location tracking service");
            // Already checked for permissions in MainActivity
        }
    }
}
