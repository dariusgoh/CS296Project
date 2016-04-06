package com.cs296.kainrath.cs296project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private User user = null;
    private Button activate;
    private Button deactivate;

    private AsyncUpdateLocation asyncUpdateLoc;

    private LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    private MyLocationListener mylistener;

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            // send information to the database
            String msg = location.getLatitude() + " : "
                    + location.getLongitude() + "location updated!";

            if (asyncUpdateLoc == null) {
                asyncUpdateLoc = new AsyncUpdateLocation(user.getId());
            }
            asyncUpdateLoc.execute(location.getLatitude(), location.getLongitude());

            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
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
    };

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

        activate = (Button)findViewById(R.id.button_activate);
        deactivate = (Button)findViewById(R.id.button_deactivate);
        deactivate.setEnabled(false);
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
        activate.setEnabled(false);
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == permissionCheck) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600000, 1609, mylistener);

            Toast.makeText(getBaseContext(), "activated! ",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getBaseContext(), "please allow location permissions ",
                    Toast.LENGTH_SHORT).show();
        }
        deactivate.setEnabled(true);
    }

    public void onClickDeactivate(View view) {
        deactivate.setEnabled(false);
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == permissionCheck) {
            locationManager.removeUpdates(mylistener);

            new AsyncDeactivateUser().execute(user.getId());

            Toast.makeText(getBaseContext(), "Deactivated! ",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getBaseContext(), "please allow location permissions ",
                    Toast.LENGTH_SHORT).show();
        }

        activate.setEnabled(true);

    }

}



