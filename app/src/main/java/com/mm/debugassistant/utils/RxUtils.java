package com.mm.debugassistant.utils;

import android.os.Looper;

public class RxUtils {
    public static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}
