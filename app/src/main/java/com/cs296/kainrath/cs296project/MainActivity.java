package com.cs296.kainrath.cs296project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import android.location.LocationProvider;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;

public class MainActivity extends AppCompatActivity {

    private User user;
    private Button activate;
    private Button deactivate;

    // For debug purposes
    private TextView location_text;
    private TextView avail_text;
    private TextView user_text;
    private TextView count_text;

    int count;

    private LocationManager locationManager;

    private MyLocationListener mylistener;

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            // send information to the database

            count_text.setText("" + count);
            location_text.setText(location.getLatitude() + ", " + location.getLongitude());
            avail_text.setText("Available");
            ++count;
            new AsyncUpdateLocation(user.getId(), MainActivity.this).execute(location.getLatitude(), location.getLongitude());

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                avail_text.setText("Out of service");
            } else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                avail_text.setText("Temporarily Unavailable");
            } else { // status == LocationProvider.AVAILABLE
                avail_text.setText("Available");
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

        activate = (Button) findViewById(R.id.button_activate);
        deactivate = (Button) findViewById(R.id.button_deactivate);
        deactivate.setEnabled(false);

        location_text = (TextView) findViewById(R.id.location_text);
        avail_text = (TextView) findViewById(R.id.availability_text);
        user_text = (TextView) findViewById(R.id.user_id_text);
        count_text = (TextView) findViewById(R.id.update_count_text);
        //location_text.setText("Deactivated");
        //avail_text.setText("Deactivated");
        user_text.setText(user.getEmail());
        mylistener = new MyLocationListener();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
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

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == permissionCheck) {
            count = 1;
            activate.setEnabled(false);
            deactivate.setEnabled(true);
            count_text.setText("0");
            location_text.setText("Searching");
            avail_text.setText("Searching");
            //
            //                                                                   // 600000
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1609, mylistener);
        } else {
            deactivate.setText("0");
            location_text.setText("Failed to activate");
            avail_text.setText("Failed to activate");
        }
    }

    public void onClickDeactivate(View view) {
        deactivate.setEnabled(false);
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == permissionCheck) {
            locationManager.removeUpdates(mylistener);

            new AsyncDeactivateUser().execute(user.getId());

            count_text.setText("Deactivated");
            location_text.setText("Deactivated");
            avail_text.setText("Deactivated");
        }
        else {
            Toast.makeText(getBaseContext(), "please allow location permissions ",
                    Toast.LENGTH_SHORT).show();
        }

        activate.setEnabled(true);

    }
}



