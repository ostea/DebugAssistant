package com.mm.debugassistant.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by elegant.wang on 2017/8/4.
 */

public class ProcessUtils {

    /***
     * @param withSu  是否root身份
     * @param timeout 执行命令超时时间，单位秒
     * @param cmds    执行的命令列表
     * @return
     */
    public static ProcessResult runCmd(boolean withSu, long timeout, String... cmds) {
        ProcessResult result = new ProcessResult();
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final StringBuilder sb = new StringBuilder();
            ProcessBuilder processBuilder = new ProcessBuilder(withSu ? "su" : "sh");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            DataOutputStream cmdStream = new DataOutputStream(process.getOutputStream());
            final InputStream stdout = process.getInputStream();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                    boolean firstLine = true;
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            if (firstLine) {
                                firstLine = false;
                            } else {
                                sb.append(System.getProperty("line.separator"));
                            }
                            sb.append(line);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                        try {
                            stdout.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();
            for (String cmd : cmds) {
                String realCmd = cmd;
                if (!cmd.endsWith("\n"))
                    realCmd = cmd + "\n";
                cmdStream.writeBytes(realCmd);
            }
            cmdStream.writeBytes("exit\n");
            cmdStream.flush();
            cmdStream.close();
            int execResult = process.waitFor();
            latch.await(timeout, TimeUnit.SECONDS);
            result.isSuccess = (execResult == 0);
            result.outMessaage = sb.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            result.isSuccess = false;
            result.outMessaage = "";
        }
        return result;
    }

    public static void runCmdRsync(final String... cmds) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("su");
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    DataOutputStream cmdStream = new DataOutputStream(process.getOutputStream());
                    for (String cmd : cmds) {
                        String realCmd = cmd;
                        if (!cmd.endsWith("\n"))
                            realCmd = cmd + "\n";
                        cmdStream.writeBytes(realCmd);
                    }
                    cmdStream.writeBytes("exit\n");
                    cmdStream.flush();
                    cmdStream.close();
                    int execResult = process.waitFor();
                } catch (Exception e) {
                    Log.e("ProcessUtils", "run cmd rsync error", e);
                }
            }
        }).start();
    }

    public static class ProcessResult {
        private boolean isSuccess;
        private String outMessaage;

        public boolean isSuccess() {
            return isSuccess;
        }

        public String outMessaage() {
            return outMessaage;
        }
    }
}
