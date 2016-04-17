package com.cs296.kainrath.cs296project;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class CreateUser extends AppCompatActivity implements
        UserInfoReceiver.Receiver,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    GoogleApiClient apiClient;

    public static final String EMAIL = "EMAIL";
    public static final String USER_ID = "USER_ID";
    private static final int GOOGLE_SIGN_IN = 9001;
    private static final String TAG = "SignInActivity";

    public UserInfoReceiver userInfoReceiver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (GlobalVars.getUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
        }

        setContentView(R.layout.activity_create_user);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().build();
        apiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.sign_in_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(apiClient);
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }


    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess()); // For debug version only
        if (result.isSuccess()) {
            // Signed in successfully
            GoogleSignInAccount acct = result.getSignInAccount();
            Toast.makeText(this, "Welcome " + acct.getEmail(), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.setAction(RegistrationIntentService.ACTION_GET_TOKEN);
            intent.putExtra(USER_ID, acct.getId());
            intent.putExtra(EMAIL, acct.getEmail());
            userInfoReceiver = new UserInfoReceiver(new Handler());
            userInfoReceiver.setReceiver(this);
            intent.putExtra(RegistrationIntentService.REQUEST_RESULT, userInfoReceiver);
            startService(intent); // Get registration token
            // new AsyncGetUser(this).execute(acct.getId(), acct.getEmail());
            setContentView(R.layout.activity_loading_user);
            //startActivity(new Intent(this, MainActivity.class));
        } else {
            Toast.makeText(this, "Unable to sign in", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == 1) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            Log.d(TAG, "Creating token and receiving user data failed");
            System.exit(1);
        }
    }
}