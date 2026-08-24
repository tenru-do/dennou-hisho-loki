package com.example.lokiaudiorelay;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

public final class MainActivity extends Activity {
    static final String PREFS = "loki_audio_relay";
    static final String KEY_RELAY_HOST = "relay_host";
    static final String KEY_RELAY_TOKEN = "relay_token";
    static final String KEY_CAPTURE_ACTIVE = "capture_active";
    static final String KEY_CAPTURE_STATUS = "capture_status";
    static final int PORT = 8765;
    private static final int REQUEST_AUDIO = 10;
    private static final int REQUEST_CAPTURE = 20;

    private EditText hostInput;
    private EditText tokenInput;
    private TextView statusView;
    private Button captureButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stateUpdater = new Runnable() {
        @Override public void run() {
            refreshState();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(stateUpdater);
        handler.post(stateUpdater);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(stateUpdater);
        super.onPause();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 28, 30, 28);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("ロキ Audio Relay");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("Bluetooth送信元の端末で再生音を取得し、電脳秘書ロキへ渡します。\n"
                + "1. Galaxyの秘書アプリで「音声PAIR」\n"
                + "2. この端末で「Galaxyを検索」\n"
                + "3. 「再生音声 ON」→共有を許可\n\n"
                + "音声は保存しません。DRMなど取得を禁止したアプリの音声は対象外です。");
        help.setTextSize(14);
        help.setTextColor(Color.DKGRAY);
        help.setPadding(0, 16, 0, 14);
        root.addView(help);

        statusView = new TextView(this);
        statusView.setTextSize(17);
        statusView.setTextColor(Color.rgb(20, 120, 40));
        statusView.setPadding(0, 8, 0, 14);
        root.addView(statusView);

        hostInput = new EditText(this);
        hostInput.setSingleLine(true);
        hostInput.setHint("GalaxyのIP（例 192.168.43.1）");
        hostInput.setInputType(InputType.TYPE_CLASS_PHONE);
        hostInput.setText(getPreferences().getString(KEY_RELAY_HOST, ""));
        root.addView(hostInput);

        tokenInput = new EditText(this);
        tokenInput.setSingleLine(true);
        tokenInput.setHint("連携トークン（検索時は自動設定）");
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenInput.setText(getPreferences().getString(KEY_RELAY_TOKEN, ""));
        root.addView(tokenInput);

        LinearLayout pairRow = new LinearLayout(this);
        pairRow.setOrientation(LinearLayout.HORIZONTAL);

        Button discover = new Button(this);
        discover.setText("Galaxyを検索");
        discover.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                discoverGalaxy();
            }
        });
        pairRow.addView(discover, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button save = new Button(this);
        save.setText("接続保存");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (saveConnection()) {
                    setStatus("接続先を保存しました", false);
                }
            }
        });
        pairRow.addView(save, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(pairRow);

        captureButton = new Button(this);
        captureButton.setText("再生音声 ON");
        captureButton.setTextSize(18);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (PlaybackCaptureService.isActive(MainActivity.this)) {
                    stopCapture();
                } else {
                    requestCapture();
                }
            }
        });
        root.addView(captureButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        refreshState();
    }

    private void requestCapture() {
        if (Build.VERSION.SDK_INT < 29) {
            setStatus("Android 10以降が必要です", true);
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            setStatus("マイク権限を許可してください", true);
            return;
        }
        if (!saveConnection()) {
            return;
        }
        MediaProjectionManager manager = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            setStatus("再生音声の取得に対応していません", true);
            return;
        }
        setStatus("次の画面で音声共有を許可してください", false);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE) return;
        if (resultCode != RESULT_OK || data == null) {
            setStatus("再生音声の取得が許可されませんでした", true);
            return;
        }
        Intent service = new Intent(this, PlaybackCaptureService.class);
        service.setAction(PlaybackCaptureService.ACTION_START);
        service.putExtra(PlaybackCaptureService.EXTRA_RESULT_CODE, resultCode);
        service.putExtra(PlaybackCaptureService.EXTRA_RESULT_DATA, data);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        setStatus("再生音声を開始しています", false);
    }

    private void stopCapture() {
        Intent service = new Intent(this, PlaybackCaptureService.class);
        service.setAction(PlaybackCaptureService.ACTION_STOP);
        startService(service);
        setStatus("再生音声を停止しました", false);
    }

    private boolean saveConnection() {
        String host = normalizeHost(hostInput.getText().toString());
        String token = tokenInput.getText().toString().trim();
        if (host.length() == 0 || token.length() < 16) {
            setStatus("先にGalaxy側で音声PAIRを押し、Galaxyを検索してください", true);
            return false;
        }
        hostInput.setText(host);
        getPreferences().edit()
                .putString(KEY_RELAY_HOST, host)
                .putString(KEY_RELAY_TOKEN, token)
                .apply();
        return true;
    }

    private void discoverGalaxy() {
        setStatus("Galaxyを検索中…", false);
        new Thread(new Runnable() {
            @Override public void run() {
                Exception last = null;
                for (String host : buildCandidateHosts()) {
                    try {
                        String token = requestPair(host);
                        if (token.length() >= 16) {
                            final String foundHost = host;
                            final String foundToken = token;
                            getPreferences().edit()
                                    .putString(KEY_RELAY_HOST, foundHost)
                                    .putString(KEY_RELAY_TOKEN, foundToken)
                                    .apply();
                            runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    hostInput.setText(foundHost);
                                    tokenInput.setText(foundToken);
                                    setStatus("Galaxyと接続しました: " + foundHost, false);
                                }
                            });
                            return;
                        }
                    } catch (Exception error) {
                        last = error;
                    }
                }
                final String detail = last == null ? "" : last.getMessage();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        setStatus("Galaxyが見つかりません。Galaxy側の音声PAIRを押して60秒以内に再試行してください。"
                                + (detail == null || detail.length() == 0 ? "" : " (" + detail + ")"), true);
                    }
                });
            }
        }, "RelayDiscovery").start();
    }

    private String requestPair(String host) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://" + host + ":" + PORT + "/pair").openConnection();
        connection.setConnectTimeout(500);
        connection.setReadTimeout(900);
        int code = connection.getResponseCode();
        String response = readAll(code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();
        if (code != 200) throw new IllegalStateException("HTTP " + code);
        return new JSONObject(response).optString("token", "").trim();
    }

    private ArrayList<String> buildCandidateHosts() {
        LinkedHashSet<String> hosts = new LinkedHashSet<String>();
        // When Galaxy itself is the Bluetooth source, the secretary bridge and
        // Audio Relay run on the same device.
        hosts.add("127.0.0.1");
        String gateway = getGatewayIp();
        if (gateway.length() > 0) hosts.add(gateway);
        String local = getLocalIp();
        int dot = local.lastIndexOf('.');
        if (dot > 0) {
            String prefix = local.substring(0, dot + 1);
            int[] common = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                    15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 30, 50, 100, 101};
            for (int value : common) {
                String candidate = prefix + value;
                if (!candidate.equals(local)) hosts.add(candidate);
            }
        }
        return new ArrayList<String>(hosts);
    }

    private String getGatewayIp() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            DhcpInfo info = wifi == null ? null : wifi.getDhcpInfo();
            if (info != null && info.gateway != 0) return intToIp(info.gateway);
        } catch (Exception ignored) {
        }
        return "";
    }

    private String getLocalIp() {
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (java.net.InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        String value = address.getHostAddress();
                        if (value != null && !value.startsWith("169.254.")) return value;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String intToIp(int value) {
        return (value & 255) + "." + ((value >> 8) & 255) + "."
                + ((value >> 16) & 255) + "." + ((value >> 24) & 255);
    }

    private String normalizeHost(String value) {
        String host = value == null ? "" : value.trim();
        host = host.replaceFirst("^https?://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        return host.trim();
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        reader.close();
        return result.toString();
    }

    private void refreshState() {
        boolean active = PlaybackCaptureService.isActive(this);
        if (captureButton != null) captureButton.setText(active ? "再生音声 OFF" : "再生音声 ON");
        String serviceStatus = getPreferences().getString(KEY_CAPTURE_STATUS, "");
        if (active && serviceStatus.length() > 0) setStatus(serviceStatus, false);
    }

    private void setStatus(String value, boolean error) {
        if (statusView == null) return;
        statusView.setText(value == null ? "" : value);
        statusView.setTextColor(error ? Color.rgb(190, 40, 30) : Color.rgb(20, 120, 40));
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }
}
