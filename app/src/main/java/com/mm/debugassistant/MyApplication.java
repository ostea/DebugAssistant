package com.mm.debugassistant;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import com.mm.debugassistant.dcon.DconService;
import com.mm.debugassistant.utils.SharedPreferenceUtils;

import timber.log.Timber;

/**
 * Created by elegant.wang on 2017/8/5.
 */

public class MyApplication extends Application {
    private static MyApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        SharedPreferenceUtils.init(this);
        // DialogManager.init(this);
        startService(new Intent(this, DconService.class));
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());
    }

    public static MyApplication getInstance() {
        return sInstance;
    }
}
