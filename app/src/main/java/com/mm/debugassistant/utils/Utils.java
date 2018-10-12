package com.mm.debugassistant.utils;

import android.text.TextUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Created by elegant.wang on 2017/8/5.
 */

public class Utils {
    private static Pattern IP_PATTERN = Pattern.compile("^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$");

    public static String sizeOfKb(File file, boolean appendUnit) {
        long size = FileUtils.sizeOf(file);
        //final DecimalFormat df = new DecimalFormat(".##");
        //return df.format(size / 1024.0);
        return String.format("%.2f", size / 1024.0) + (appendUnit ? "KB" : "");
    }


    public static boolean isValidIp(String ip) {
        return !TextUtils.isEmpty(ip) && ip.length() >= 7 && ip.length() <= 15 && IP_PATTERN.matcher(ip).matches();
    }
}
