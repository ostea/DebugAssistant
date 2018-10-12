package com.mm.debugassistant.dialog;

import android.content.Context;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;

/**
 * Created by elegant.wang on 2017/8/5.
 */

public class DialogManager {
    public static final String DIALOG_CATCH_LOG = "catch_log";
    public static final String DIALOG_SETTING = "setting";

    private static DialogManager sInstance;
    private Context mContext;
    public static final Map<String, String> DIALOG_CLASS = new HashMap<>();
    public static final Map<String, DebugDialog> DEBUG_DIALOGS = new HashMap<>();

    public static void init(Context context) {
        sInstance = new DialogManager(context);
    }

    public static DialogManager getInstance() {
        if (sInstance == null) throw new RuntimeException("DialogManager not initialized!");
        return sInstance;
    }

    private DialogManager(Context context) {
        mContext = context;

        DIALOG_CLASS.put(DIALOG_CATCH_LOG, "com.mm.debugassistant.dialog.CatchLog");
        DIALOG_CLASS.put(DIALOG_SETTING, "com.mm.debugassistant.dialog.Setting");
    }

    public void show(String which) {
        try {
            DebugDialog debugDialog = DEBUG_DIALOGS.get(which);
            if (debugDialog == null) {
                Class clazz = Class.forName(DIALOG_CLASS.get(which));
                Constructor constructor = clazz.getDeclaredConstructor(new Class[]{Context.class});
                debugDialog = (DebugDialog) constructor.newInstance(new Object[]{mContext});
                DEBUG_DIALOGS.put(which, debugDialog);
            }
            debugDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toasty.error(mContext, "Error when init debug dialog!", Toast.LENGTH_SHORT).show();
        }
    }
}
