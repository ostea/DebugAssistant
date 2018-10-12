package com.mm.debugassistant.api;

import com.mm.debugassistant.utils.Constant;
import com.mm.debugassistant.utils.SharedPreferenceUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by elegant.wang on 2017/8/5.
 */

public class DebugServer {
    private static DebugServer sInstance;
    private ApiService mApi;
    private String mDid;
    private String mServerIp;
    private SimpleDateFormat mSdf;

    public static DebugServer getInstance() {
        if (sInstance == null) {
            synchronized (DebugServer.class) {
                if (sInstance == null)
                    sInstance = new DebugServer();
            }
        }
        return sInstance;
    }

    private DebugServer() {
        try {
            mServerIp = SharedPreferenceUtils.getString(Constant.SERVER_IP, Constant.DEF_SERVER_IP);
            mApi = ServiceGenerator.createService(ApiService.class, String.format("http://%s:5188", mServerIp));
            mDid = StringUtils.replace(FileUtils.readFileToString(new File("/sys/class/net/eth0/address")).trim(), ":", "");
            mSdf = new SimpleDateFormat("yyyyMMddHHmmss");
        } catch (IOException e) {
            e.printStackTrace();
            mDid = "unknown";
        }
    }

    public <T> void uploadFile(File file, Callback<ResponseBody> callback) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        String date = mSdf.format(new Date());
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", mDid + "_" + date + "_" + file.getName(), requestBody);
        String descriptionString = String.format("log file from %s @ %s", mDid, date);
        RequestBody description =
                RequestBody.create(
                        okhttp3.MultipartBody.FORM, descriptionString);
        Call<ResponseBody> call = mApi.upload(description, body);
        call.enqueue(callback);
    }

    public static void reset() {
        sInstance = null;
    }
}
