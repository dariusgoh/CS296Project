package com.cs296.kainrath.cs296project;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by kainrath on 4/17/16.
 */
@SuppressLint("ParcelCreator")
public class UserInfoReceiver extends ResultReceiver {

    private Receiver userReceiver;

    public UserInfoReceiver(Handler handler) {
        super(handler);
    }

    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }

    public void setReceiver(Receiver receiver) {
        userReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (userReceiver != null) {
            userReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}
