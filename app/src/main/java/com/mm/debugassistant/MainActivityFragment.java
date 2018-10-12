package com.mm.debugassistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mm.debugassistant.api.DebugServer;
import com.mm.debugassistant.dialog.CatchLog;
import com.mm.debugassistant.utils.ProcessUtils;
import com.mm.debugassistant.utils.ZipUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private Activity mActivity;
    private View mRootView;
    private CardView mCvRecordLog;
    private CardView mCvRemoteCon;
    private CardView mCvSysInfo;
    private CardView mCvUpdateLog;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.layout_fragment_main, container, false);
        mCvRecordLog = (CardView) mRootView.findViewById(R.id.cv_record_log);
        mCvRemoteCon = (CardView) mRootView.findViewById(R.id.cv_remote_connect);
        mCvSysInfo = (CardView) mRootView.findViewById(R.id.cv_system_info);
        mCvUpdateLog = (CardView) mRootView.findViewById(R.id.cv_update_log);
        setUpView();
        ButterKnife.bind(this, mRootView);
        return mRootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
    }

    private void setUpView() {
    }

    @OnClick(R.id.cv_record_log)
    void catchLog(View v) {
        CatchLog catchLog = new CatchLog(mActivity);
        catchLog.show();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.cv_update_log)
    void updateLog(View v) {
        // show progress
        final String zipFileName = "/sdcard/update_log.zip";
        final ProgressDialog progressDialog = ProgressDialog.show(getActivity(), "", "升级日志");
        Observable.create(new ObservableOnSubscribe<ImmutablePair<Boolean, String>>() {
            @Override
            public void subscribe(@NonNull final ObservableEmitter<ImmutablePair<Boolean, String>> e) throws Exception {
                e.onNext(new ImmutablePair<>(false, "拷贝升级日志..."));
                ProcessUtils.runCmd(true, 60, "cp -r /cache/recovery /data/local/tmp/recovery", "chmod -R 777 /data/local/tmp/recovery");
                e.onNext(new ImmutablePair<>(false, "压缩升级日志..."));
                FileUtils.deleteQuietly(new File(zipFileName));// 如果存在则删除
                ZipUtils.zipFileAtPath("/data/local/tmp/recovery", zipFileName);
                ProcessUtils.runCmd(true, 60, "rm -r /data/local/tmp/recovery");
                e.onNext(new ImmutablePair<>(false, "上传到服务器..."));
                File updateLogFile = new File(zipFileName);
                if (!updateLogFile.exists()) {
                    e.onError(new RuntimeException("生成日志压缩文件失败！"));
                    return;
                }

                DebugServer.getInstance().uploadFile(updateLogFile, new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        e.onNext(new ImmutablePair<>(true, "上传完成"));
                        e.onComplete();
                        // FileUtils.deleteQuietly(new File(zipFileName));// 如果存在则删除
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                        e.onError(throwable);
                        // FileUtils.deleteQuietly(new File(zipFileName));// 如果存在则删除
                    }
                });
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ImmutablePair<Boolean, String>>() {
                    @Override
                    public void accept(ImmutablePair<Boolean, String> s) throws Exception {
                        progressDialog.setMessage(s.getRight());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        progressDialog.dismiss();
                        Toasty.error(getActivity(), "上传失败," + throwable.getMessage()).show();
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        progressDialog.dismiss();
                        Toasty.success(getActivity(), "上传成功").show();
                    }
                });
    }

    @OnClick(R.id.cv_system_info)
    void systemInfo(View v) {
        final Context context = getActivity();
        try {
            final String TAG = "XXXXXXXXX";
            PackageManager pm = context.getPackageManager();
            final List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);
            String[] items = new String[installedApps.size()];
            for (int i = 0; i < installedApps.size(); i++) {
                ApplicationInfo applicationInfo = installedApps.get(i);
                items[i] = applicationInfo.packageName;
            }
            new AlertDialog.Builder(context)
                    .setTitle("请选择")
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            File sourceDir = new File(installedApps.get(which).sourceDir);
                            Log.e(TAG, "" + sourceDir.isDirectory());
                            Log.e(TAG, sourceDir.getAbsolutePath());
                            File sdcardDir = Environment.getExternalStorageDirectory();
                            try {
                                FileUtils.copyFileToDirectory(sourceDir, sdcardDir);
                            } catch (IOException e) {
                                Toasty.error(context, "失败了：" + e.getMessage()).show();
                            }
                        }
                    }).create().show();

        } catch (Exception e) {
            Toasty.error(context, "失败了：" + e.getMessage()).show();
        }
    }
}
