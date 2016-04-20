package com.cs296.kainrath.cs296project;

import android.Manifest;
import android.content.BroadcastReceiver;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String USER_ID = "USER_ID";

    private User user;
    private Button activate;
    private Button deactivate;

    // For debug purposes

    private TextView nearby_user_names;
    private TextView user_info;
    private TextView lat_long_info;

    int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            ((GlobalVars) this.getApplication()).restoreState(savedInstanceState);
        }

        if (GlobalVars.getFailed()) {
            System.exit(1);
        }
        user = GlobalVars.getUser();
        if (user == null) {
            startActivity(new Intent(this, CreateUser.class));
        }
        setContentView(R.layout.activity_main);

        activate = (Button) findViewById(R.id.button_activate);
        deactivate = (Button) findViewById(R.id.button_deactivate);
        nearby_user_names = (TextView) findViewById(R.id.nearby_user_names);
        user_info = (TextView) findViewById(R.id.user_info_text);
        user_info.setText(user.getEmail());
        lat_long_info = (TextView) findViewById(R.id.lat_long_text);
        nearby_user_names.setText(GlobalVars.getNearbyUserString());
        if (LocationTrackerService.isInstanceCreated()) {
            activate.setEnabled(false);
        } else {
            deactivate.setEnabled(false);
        }
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
            activate.setEnabled(false);
            deactivate.setEnabled(true);
            Intent intent = new Intent(this, LocationTrackerService.class);
            intent.putExtra(USER_ID, user.getId());
            startService(intent);
        } else {
            Toast.makeText(this, "Please enable location services for this app", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickDeactivate(View view) {

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED == permissionCheck) {
            stopService(new Intent(this, LocationTrackerService.class));
            deactivate.setEnabled(false);
            activate.setEnabled(true);
        }
        else {
            Toast.makeText(this, "Please enable location services for this app", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickRefresh(View view) {
        nearby_user_names.setText(GlobalVars.getNearbyUserString());
        String loc = "Lat/Long: " + GlobalVars.getLat() + ":" + GlobalVars.getLong();
        lat_long_info.setText(loc);
    }
    /*
    public void onClickRefresh(View view) {

        List<User> nearby_users = ((GlobalVars) this.getApplication()).getNearbyUsers();
        if (nearby_users == null | nearby_users.isEmpty()) {
            nearby_user_text.setText("No nearby users");
        } else {
            String user_list = "";
            for (User user : nearby_users) {
                if (user != null) {
                    user_list += user.getId() + " ";
                }
            }
            nearby_user_text.setText(user_list);
        }
    }*/

    /*
    // For getting the gcm token
    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }
    */
}



