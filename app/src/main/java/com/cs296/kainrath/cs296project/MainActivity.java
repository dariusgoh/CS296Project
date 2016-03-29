package com.cs296.kainrath.cs296project;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (((GlobalVars) this.getApplication()).getFailed()) {
            System.exit(1);
        }
        if (((GlobalVars) this.getApplication()).getUser() == null) {
            startActivity(new Intent(this, CreateUser.class));
        }

    }

    public void onClickMyInterests(View view) {
        startActivity(new Intent(this, DisplayInterests.class));
    }

    public void onClickActivate(View view) {
        startActivity(new Intent(this, ActivateSearch.class));
    }
}

