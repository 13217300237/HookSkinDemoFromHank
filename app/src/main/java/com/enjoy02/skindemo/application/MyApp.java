package com.enjoy02.skindemo.application;

import android.app.Application;

import com.enjoy02.skindemo.skin.SkinEngine;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化换肤引擎
        SkinEngine.getInstance().init(this);
    }
}
