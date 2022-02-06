package com.dima;

import android.app.Application;

import com.dima.Receiver.RoboErrorReporter;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        RoboErrorReporter.bindReporter(this);
        super.onCreate();
    }
}