package com.mm.debugassistant.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;

/**
 * Created by elegant.wang on 2017/8/4.
 */

public abstract class DebugDialog extends Dialog {
    protected Context mContext;

    private View mRootView;

    public DebugDialog(@NonNull Context context) {
        super(context);
        this.mContext = context;
        mRootView = LayoutInflater.from(context).inflate(getLayoutRes(), null);
        ButterKnife.bind(this, mRootView);
        setContentView(mRootView);
        setTitle(getTitle());
        Toasty.Config.getInstance().setTextSize(16).apply();
    }

    public DebugDialog(@NonNull Context context, @StyleRes int themeResId) {
        this(context);
    }

    protected DebugDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        this(context);
    }

    protected abstract
    @LayoutRes
    int getLayoutRes();

    protected abstract String getTitle();

    protected void showErrorToast(String message) {
        Toasty.error(mContext, message, Toast.LENGTH_SHORT, true).show();
    }

    protected void showSuccessToast(String message) {
        Toasty.success(mContext, message, Toast.LENGTH_SHORT, true).show();
    }

    protected void showInfoToast(String message) {
        Toasty.info(mContext, message, Toast.LENGTH_SHORT, true).show();
    }

    protected void showNormalToast(String message) {
        Toasty.normal(mContext, message, Toast.LENGTH_SHORT).show();
    }

    protected void runOnMain(Runnable action) {
        ((Activity) mContext).runOnUiThread(action);
    }
}
