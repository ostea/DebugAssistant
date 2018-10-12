package com.mm.debugassistant.dialog;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.mm.debugassistant.R;
import com.mm.debugassistant.api.DebugServer;
import com.mm.debugassistant.utils.ProcessUtils;
import com.mm.debugassistant.utils.Utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mm.debugassistant.dialog.CatchLog.CatchState.CATCHING;
import static com.mm.debugassistant.dialog.CatchLog.CatchState.IDLE;
import static com.mm.debugassistant.dialog.CatchLog.CatchState.REPORTING;
import static com.mm.debugassistant.utils.ProcessUtils.runCmd;

/**
 * Created by elegant.wang on 2017/8/4.
 */

public class CatchLog extends DebugDialog {
    public static final String CMD_LOGCAT = "logcat -v threadtime > /data/local/tmp/logcat.log";
    public static final String CMD_KERNEL = "cat /proc/kmsg > /data/local/tmp/kernel.log";
    @BindView(R.id.tv_log_state)
    TextView mTvState;
    @BindView(R.id.tv_logcat_file_size)
    TextView mTvLogcatSize;
    @BindView(R.id.tv_kernel_log_size)
    TextView mTvKernelSize;

    private CatchState mState;
    private boolean mLogFileWatching;

    public CatchLog(@NonNull Context context) {
        super(context);
    }

    @Override
    public void show() {
        super.show();
        mState = isCatching() ? CATCHING : IDLE;
        showState();
        if (mState == CATCHING) {
            watchLogFile();
        } else {
            showLogFile();
        }
    }

    @Override
    public void cancel() {
        mLogFileWatching = false;
        super.cancel();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.dialog_catch_log;
    }

    @Override
    protected String getTitle() {
        return "Catch Log";
    }

    @OnClick(R.id.btn_start)
    void start(View v) {
        if (mState != IDLE) {
            showErrorToast("State is not idle, please stop first!");
            return;
        }
        mState = CATCHING;
        showState();
        ProcessUtils.runCmdRsync(CMD_LOGCAT);
        ProcessUtils.runCmdRsync(CMD_KERNEL);
        watchLogFile();
    }

    @OnClick(R.id.btn_stop)
    void stop(View v) {
        if (mState == CATCHING) {
            String killLogcatCmd = "lsof|grep /data/local/tmp/logcat.log|awk '{print $2}'|xargs kill -9";
            String killKernelCmd = "lsof|grep /data/local/tmp/kernel.log|awk '{print $2}'|xargs kill -9";
            runCmd(true, 5, killKernelCmd, killLogcatCmd);
            mLogFileWatching = false;
            mState = IDLE;
            showState();
        } else if (mState == REPORTING) {
            mState = IDLE;
            showState();
        } else {
            showErrorToast("Not catching or reporting!");
        }
    }

    @OnClick(R.id.btn_report)
    void report(View v) {
        if (mState != IDLE) {
            showErrorToast("State is not idle, please stop first!");
            return;
        }

        final File logcatFile = new File("/data/local/tmp/logcat.log");
        File logcatGz = new File("/data/local/tmp/logcat.log.gz");
        final File kernelFile = new File("/data/local/tmp/kernel.log");
        File kernelGz = new File("/data/local/tmp/kernel.log.gz");
        if ((!logcatFile.exists() && !logcatGz.exists()) || (!kernelFile.exists() && !kernelGz.exists())) {
            showErrorToast("Log file not exists, please catch first!");
            return;
        }

        mState = REPORTING;
        showState();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> cmd = new ArrayList<>();
                if (logcatFile.exists()) {
                    cmd.add("gzip /data/local/tmp/logcat.log");
                }
                if (kernelFile.exists()) {
                    cmd.add("gzip /data/local/tmp/kernel.log");
                }
                String[] cmdArr = cmd.toArray(new String[]{});
                if (cmdArr.length > 0)
                    runCmd(true, 60, cmdArr);
                Callback<ResponseBody> callback = new Callback<ResponseBody>() {
                    private int total = 2;
                    private int success = 0;
                    private int failed = 0;

                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        success++;
                        onResult();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        failed++;
                        onResult();
                    }

                    private void onResult() {
                        if (success + failed >= total) {
                            mState = IDLE;
                            showState();
                            if (failed == 0) {
                                showSuccessToast("Upload success");
                                ProcessUtils.runCmd(true, 10, "rm /data/local/tmp/logcat.log.gz", "rm /data/local/tmp/kernel.log.gz");
                                showLogFile();
                            } else {
                                showErrorToast("Upload failed, please retry");
                            }
                        }
                    }
                };
                DebugServer.getInstance().uploadFile(new File("/data/local/tmp/logcat.log.gz"), callback);
                DebugServer.getInstance().uploadFile(new File("/data/local/tmp/kernel.log.gz"), callback);
            }
        }).start();

    }

    private boolean isCatching() {
        try {
            /*File procDir = new File("/proc");
            File[] procFiles = procDir.listFiles();
            for (int i = 0; i < procFiles.length; i++) {
                String fileName = procFiles[i].getName();
                if (procFiles[i].isDirectory() && fileName.matches("\\d+")) {
                    File cmdlineFile = new File(procFiles[i], "cmdline");
                    String cmdline = FileUtils.readFileToString(cmdlineFile).trim();
                    if (StringUtils.equalsIgnoreCase("cat /proc/kmsg", cmdline)) {
                        return true;
                    }
                }
            }*/
            String counter = runCmd(true, 5, "lsof|grep /data/local/tmp/kernel.log|wc -l").outMessaage();
            return StringUtils.equals("1", counter);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void watchLogFile() {
        if (mLogFileWatching) return;
        mLogFileWatching = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mLogFileWatching && mState == CATCHING) {
                    showLogFile();
                    SystemClock.sleep(5000);
                }
            }
        }).start();
    }

    private void showLogFile() {
        runOnMain(new Runnable() {
            @Override
            public void run() {
                File logcatFile = new File("/data/local/tmp/logcat.log");
                File kernelFile = new File("/data/local/tmp/kernel.log");

                File logcatGz = new File("/data/local/tmp/logcat.log.gz");
                File kernelGz = new File("/data/local/tmp/kernel.log.gz");
                mTvLogcatSize.setText(logcatFile.exists() ? Utils.sizeOfKb(logcatFile, true) : (logcatGz.exists() ? "Compressed:" + Utils.sizeOfKb(logcatGz, true) : "not exists"));
                mTvKernelSize.setText(kernelFile.exists() ? Utils.sizeOfKb(kernelFile, true) : (kernelGz.exists() ? "Compressed:" + Utils.sizeOfKb(kernelGz, true) : "not exists"));
            }
        });
    }

    public void showState() {
        runOnMain(new Runnable() {
            @Override
            public void run() {
                mTvState.setText(mState.toString());
            }
        });
    }

    enum CatchState {
        IDLE,
        CATCHING,
        REPORTING
    }
}
