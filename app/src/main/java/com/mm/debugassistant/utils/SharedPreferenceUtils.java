package com.mm.debugassistant.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by elegant.wang on 2017/8/5.
 */

public class SharedPreferenceUtils {
    private static SharedPreferences sp;

    public static void init(Context context) {
        sp = context.getSharedPreferences("basic", Context.MODE_PRIVATE);
    }

    public static String getString(String key, String def) {
        return sp.getString(key, def);
    }

    public static void put(String key, Object val) {
        SharedPreferences.Editor editor = sp.edit();
        if (val == null) {
            editor.putString(key, "");
        } else {
            editor.putString(key, val.toString());
        }
        editor.apply();
    }
}
