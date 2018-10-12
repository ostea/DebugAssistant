package com.mm.debugassistant.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.mm.debugassistant.R;
import com.mm.debugassistant.api.DebugServer;
import com.mm.debugassistant.utils.Constant;
import com.mm.debugassistant.utils.SharedPreferenceUtils;
import com.mm.debugassistant.utils.Utils;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by elegant.wang on 2017/8/5.
 */

public class Setting extends DebugDialog {
    @BindView(R.id.et_server_ip)
    EditText mEtServerIp;

    private String mServerIp;

    public Setting(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.dialog_setting;
    }

    @Override
    protected String getTitle() {
        return "Setting";
    }

    @Override
    public void show() {
        mServerIp = SharedPreferenceUtils.getString(Constant.SERVER_IP, Constant.DEF_SERVER_IP);
        mEtServerIp.setText(mServerIp);
        super.show();
    }

    @OnClick(R.id.bt_ok)
    void onOK(View v) {
        String ip = mEtServerIp.getText().toString();
        if (!Utils.isValidIp(ip)) {
            showErrorToast("Invalid IP!");
            return;
        }
        SharedPreferenceUtils.put(Constant.SERVER_IP, ip);
        DebugServer.reset();
        cancel();
    }

    @OnClick(R.id.bt_cancel)
    void onCancel() {
        this.cancel();
    }
}
