package com.cs296.kainrath.cs296project;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private User user = null;

    private LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // send information to the database
            String msg = location.getLatitude() + " : "
                    + location.getLongitude() + "location updated!";

            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) {

            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            Toast.makeText(getBaseContext(), "turn GPS on! ",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            ((GlobalVars) this.getApplication()).restoreState(savedInstanceState);
        }

        if (((GlobalVars) this.getApplication()).getFailed()) {
            System.exit(1);
        }
        user = ((GlobalVars) this.getApplication()).getUser();
        if (user == null) {
            startActivity(new Intent(this, CreateUser.class));
        }
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        ((GlobalVars) this.getApplication()).saveState(savedInstanceState);
    }

    public void onClickMyInterests(View view) {
        startActivity(new Intent(this, DisplayInterests.class));
    }

    public void onClickActivate(View view) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600000, 1609, locationListener);

        Toast.makeText(getBaseContext(), "activated! ",
                Toast.LENGTH_SHORT).show();
    }

    public void onClickDeactivate(View view) {
        locationManager.removeUpdates(locationListener);

        Toast.makeText(getBaseContext(), "Deactivated! ",
                Toast.LENGTH_SHORT).show();
    }

}

