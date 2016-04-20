package com.cs296.kainrath.cs296project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cs296.kainrath.cs296project.backend.userApi.model.User;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String USER_ID = "USER_ID";

    private User user;
    private Button activate;
    private Button deactivate;
    private ListView chat_list;


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

        chat_list = (ListView) findViewById(R.id.chat_list);
        lat_long_info = (TextView) findViewById(R.id.lat_long_text);
        if (LocationTrackerService.isInstanceCreated()) {
            activate.setEnabled(false);
            // Make chat list visible
        } else {
            deactivate.setEnabled(false);
            // Make chat list invisible
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_actionbar_layout, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        ((GlobalVars) this.getApplication()).saveState(savedInstanceState);
    }

    // TODO: DISABLE IF THE USER IS ACTIVATED
    public void onClickMyInterests(View view) {
        startActivity(new Intent(this, DisplayInterests.class));
    }


    // TODO: DISABLE IF THE USER HAS NO INTERESTS
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

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        Log.d(TAG, "clicked options menu");
        switch (menu.getItemId()) {
            case R.id.logout_button:
                Log.d(TAG, "clicked logout");
                logout();
                return true;
            default:
                Log.d(TAG, "clicked default");
                return super.onOptionsItemSelected(menu);
        }
    }

    private void logout() {
        Log.d(TAG, "starting logout");
        GlobalVars.setUser(null);
        GlobalVars.setNearbyUsers(null);
        GlobalVars.setLatLong(0,0);
        if (LocationTrackerService.isInstanceCreated()) {
            Log.d(TAG, "stopping location service");
            stopService(new Intent(this, LocationTrackerService.class));
        } else {
            Log.d(TAG, "location service was not running");
        }
        startActivity(new Intent(this, CreateUser.class));
    }

    private void enableChatList() {

    }
}



