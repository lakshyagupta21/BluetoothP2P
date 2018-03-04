package com.dexter.bluetoothp2p;

import android.app.Application;

/**
 * Created by laksh on 3/4/2018.
 */

public class Supervisor extends Application {

    private static Supervisor singleton;
    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }

    public static Supervisor getSingleton() {
        return singleton;
    }
}
