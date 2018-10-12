package com.mm.debugassistant;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mm.debugassistant.dcon.ConnectionManager;
import com.mm.debugassistant.dialog.CatchLog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

public class DebugConnFragment extends Fragment {
    private Context mContext;
    private View mRootView;
    private boolean mIsRunning = false;
    @BindView(R.id.btn_start)
    Button mStartBtn;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_debug_conn, container, false);
        }
        ButterKnife.bind(this, mRootView);
        return mRootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
    }

    @OnClick(R.id.btn_start)
    void start(View v) {
        if (mIsRunning) {
            Toasty.error(mContext, getString(R.string.dcon_already_running)).show();
        } else {
            ConnectionManager.getInstance().connect();
            Toasty.success(mContext, getString(R.string.dcon_ok)).show();
            mStartBtn.setText(R.string.dcon_running);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsRunning) {
            mStartBtn.setText(R.string.dcon_running);
        } else {
            mStartBtn.setText(R.string.dcon_start);
        }
    }
}
