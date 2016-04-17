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
import android.widget.Toast;

public class LocationTrackerService extends Service {

    private static final String USER_ID = "USER_ID";
    private String user_id;

    private LocationManager locationManager;

    private UserLocationListener locListener;

    private static LocationTrackerService instance = null;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private class UserLocationListener implements LocationListener {

        Context appContext = null;

        public UserLocationListener(Context appContext) {
            this.appContext = appContext;
        }

        @Override
        public void onLocationChanged(Location location) {
            // send information to the database
            new AsyncUpdateLocation(user_id, appContext).execute(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                // no service
            } else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                // temporarily no service
            } else { // status == LocationProvider.AVAILABLE
                // service is available
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(getBaseContext(), "GPS is on! ",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            Toast.makeText(getBaseContext(), "turn GPS on! ",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locListener);
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
        user_id = intent.getStringExtra(USER_ID);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize location listener
        instance = this;
        locListener = new UserLocationListener(this.getApplicationContext());
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 20, locListener);
        } catch (SecurityException e) {
            // Already checked for permissions in MainActivity
        }
    }
}
