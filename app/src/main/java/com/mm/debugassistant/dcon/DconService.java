package com.mm.debugassistant.dcon;

import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mm.debugassistant.dcon.ConnectionManager;

public class DconService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ConnectionManager.getInstance().connect();
        return Service.START_STICKY;
    }
}
