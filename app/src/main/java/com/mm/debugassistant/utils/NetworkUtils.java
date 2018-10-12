package com.mm.debugassistant.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.text.TextUtils;

import com.mm.debugassistant.MyApplication;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import timber.log.Timber;

/**
 * Created by elegant.wang on 2016/5/26.
 */
public class NetworkUtils {
    public static String ETHERNET_INTERFACE = "eth0";
    private static String ethernetMac;
    private static String did;

    public static String getEthernetMac() {
        if (TextUtils.isEmpty(ethernetMac)) {
            synchronized (NetworkUtils.class) {
                if (TextUtils.isEmpty(ethernetMac)) {
                    try {
                        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                        while (networkInterfaces.hasMoreElements()) {
                            NetworkInterface networkInterface = networkInterfaces.nextElement();
                            if (ETHERNET_INTERFACE.equals(networkInterface.getDisplayName())) {
                                ethernetMac = parseMac(networkInterface.getHardwareAddress());
                                break;
                            }
                        }
                        if (ethernetMac == null || ethernetMac.equals("")) {
                            //// TODO: 2016/5/26  其他方式得到MAC地址？
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream("/sys/class/net/eth0/address")));
                            ethernetMac = bufferedReader.readLine();
                        }
                    } catch (SocketException e) {
                        Timber.e(e, "getEthernetMac error:");
                        //Log.e("NetworkUtils", "getEthernetMac error:" + e);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return ethernetMac;
    }

    public static String getIpAddress() {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ipAddress = inetAddress.getHostAddress().toString();
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }

    public static String getDid() {
        try {
            if (TextUtils.isEmpty(did)) {
                synchronized (NetworkUtils.class) {
                    if (TextUtils.isEmpty(did)) {
                        did = getEthernetMac().replace(":", "").toLowerCase();
                    }
                }
            }
            return did;
        } catch (Exception e) {
            return "";
        }

    }

    //解析mac
    private static String parseMac(byte[] mac) {
        StringBuffer sb = new StringBuffer();
        if (mac != null) {
            sb.delete(0, sb.length());
            for (int i = 0; i < mac.length; i++) {
                sb.append(parseByte(mac[i]));
            }
            return sb.substring(0, sb.length() - 1);
        }
        return null;
    }

    //字节转字符
    private static String parseByte(byte b) {
        String s = "00" + Integer.toHexString(b) + ":";
        return s.substring(s.length() - 3);
    }

    public static boolean isNetworkConnected(Context context) {
//        return isEthernetConnected() || isWifiConnected();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static String getMacFromFile() {

        FileInputStream localFileInputStream;
        String mac = "";
        try {
            localFileInputStream = new FileInputStream(
                    "/sys/class/net/eth0/address");
            byte[] arrayOfByte = new byte[17];
            localFileInputStream.read(arrayOfByte, 0, 17);
            mac = new String(arrayOfByte);
            localFileInputStream.close();
            if (mac.contains(":"))
                mac = mac.replace(":", "").trim();
            if (mac.contains("-"))
                mac = mac.replace("-", "").trim();
        } catch (Exception e) {
            Timber.e(e, "getMac by file error!");
            return "";
        }
        if (TextUtils.isEmpty(mac)) {
            Timber.e("getMac by file return empty!");
            return "";
        }

        return mac.toLowerCase();
    }

    public static String getAndroidID() {
        try {
            return Settings.Secure.getString(MyApplication.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            return "";
        }
    }
}
