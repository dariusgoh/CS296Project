package com.cs296.kainrath.cs296project;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class ActivateSearch extends AppCompatActivity
       /* implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener*/ {

    GoogleApiClient location_client = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate_search);
/*
        if (location_client == null) {
            location_client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }*/
    }
/*
    @Override
    protected void onStart() {
        location_client.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        location_client.disconnect();
        super.onStop();
    }*/

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        ((GlobalVars) this.getApplication()).saveState(savedInstanceState);
    }


    public void onClickToMainMenu(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }
}
