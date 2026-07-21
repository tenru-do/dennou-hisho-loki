package com.example.rokidgeminisecretary;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.os.OutcomeReceiver;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.StepsRecord;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
    private static final String TAG = "RokidPhoneSecretary";
    private static final int PORT = 8765;
    private static final int MAX_LOGS = 30;
    private static final String PREFS = "phone_secretary";
    private static final String KEY_CUSTOM = "custom_instructions";
    private static final String KEY_BRIDGE_TOKEN = "bridge_token";
    private static final String KEY_HEALTH_COMPACT = "health_compact";
    private static final String KEY_HEALTH_TIME = "health_time";
    private static final String KEY_HEALTH_DATE = "health_date";
    private static final String KEY_WEATHER_LOCATION = "weather_location";
    private static final String KEY_WEATHER_CONDITION = "weather_condition";
    private static final String KEY_WEATHER_TEMPERATURE = "weather_temperature";
    private static final String KEY_WEATHER_FEELS_LIKE = "weather_feels_like";
    private static final String KEY_WEATHER_TIME = "weather_time";
    private static final String KEY_PENDING_COMMAND = "pending_command";
    private static final List<String> LOGS = new ArrayList<String>();
    private static MainActivity activeActivity;
    private static String pendingCommand = "";
    private static String pendingCustomInstructions = "";
    private static String pendingControl = "";
    private TextView status;
    private TextView details;
    private TextView logView;
    private TextView customInfo;
    private EditText commandInput;
    private EditText customInput;
    private EditText bridgeTokenInput;
    private LinearLayout customPanel;
    private LinearLayout toolsPanel;
    private Button toggleCustomButton;
    private Button toolsButton;
    private volatile boolean running;
    private volatile long pairingUntilMs;
    private ServerSocket serverSocket;
    private final Handler healthHandler = new Handler(Looper.getMainLooper());
    private final Handler weatherHandler = new Handler(Looper.getMainLooper());
    private volatile boolean weatherRefreshInFlight;
    private final Runnable healthRefresh = new Runnable() {
        @Override public void run() {
            refreshHealthConnectSteps();
            healthHandler.postDelayed(this, 60000L);
        }
    };
    private final Runnable weatherRefresh = new Runnable() {
        @Override public void run() {
            refreshWeatherAsync();
            weatherHandler.postDelayed(this, 600000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        activeActivity = this;
        ensureBridgeToken();
        buildUi();
        startBridgeForegroundService();
        ensureCalendarPermission();
        String oldHealth = getPreferences().getString(KEY_HEALTH_COMPACT, "");
        String savedHealthDate = getPreferences().getString(KEY_HEALTH_DATE, "");
        String today = LocalDate.now(ZoneId.systemDefault()).toString();
        if (oldHealth.startsWith("HEALBE") || !today.equals(savedHealthDate)) {
            getPreferences().edit().remove(KEY_HEALTH_COMPACT).remove(KEY_HEALTH_TIME)
                    .putString(KEY_HEALTH_DATE, today).apply();
        }
        ensureHealthConnectPermission();
        healthHandler.post(healthRefresh);
        weatherHandler.postDelayed(new Runnable() {
            @Override public void run() {
                ensureLocationPermission();
                weatherHandler.post(weatherRefresh);
            }
        }, 1600L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            weatherHandler.post(new Runnable() {
                @Override public void run() {
                    refreshWeatherAsync();
                }
            });
        }
    }

    private void startBridgeForegroundService() {
        try {
            Intent intent = new Intent(this, BridgeForegroundService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception error) {
            Log.w(TAG, "startBridgeForegroundService failed", error);
        }
    }

    @Override
    protected void onDestroy() {
        running = false;
        healthHandler.removeCallbacks(healthRefresh);
        weatherHandler.removeCallbacksAndMessages(null);
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {
        }
        if (activeActivity == this) {
            activeActivity = null;
        }
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 20, 24, 20);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Rokid Secretary Phone");
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(Color.rgb(30, 120, 30));
        status.setPadding(0, 14, 0, 6);
        root.addView(status);

        details = new TextView(this);
        details.setTextSize(11);
        details.setTextColor(Color.DKGRAY);
        details.setMaxLines(2);
        root.addView(details);

        Button notificationAccess = new Button(this);
        notificationAccess.setText("MAIL");
        notificationAccess.setTextSize(12);
        notificationAccess.setMinHeight(0);
        notificationAccess.setPadding(4, 0, 4, 0);
        notificationAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });

        commandInput = new EditText(this);
        commandInput.setSingleLine(false);
        commandInput.setMinLines(2);
        commandInput.setHint("スマホからグラスへ質問");
        commandInput.setTextSize(14);
        root.addView(commandInput);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button sendCommand = new Button(this);
        sendCommand.setText("送信");
        sendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = commandInput.getText().toString().trim();
                if (text.length() == 0) {
                    return;
                }
                synchronized (MainActivity.class) {
                    pendingCommand = text;
                }
                getPreferences().edit().putString(KEY_PENDING_COMMAND, text).apply();
                addAiLog("ユーザー: " + shortText(text, 160));
                updateStatus("送信待ち", "グラスが受け取るまで保持します: " + shortText(text, 80));
                commandInput.setText("");
            }
        });
        actionRow.addView(sendCommand, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toggleCustomButton = new Button(this);
        toggleCustomButton.setText("指示");
        toggleCustomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean show = customPanel.getVisibility() != View.VISIBLE;
                customPanel.setVisibility(show ? View.VISIBLE : View.GONE);
                refreshCustomInfo();
                toggleCustomButton.setText(show ? "閉じる" : "指示");
            }
        });
        actionRow.addView(toggleCustomButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        customPanel = new LinearLayout(this);
        customPanel.setOrientation(LinearLayout.VERTICAL);
        customPanel.setVisibility(View.GONE);

        customInfo = new TextView(this);
        customInfo.setTextSize(12);
        customInfo.setTextColor(Color.DKGRAY);
        customInfo.setPadding(0, 6, 0, 4);
        customPanel.addView(customInfo);

        customInput = new EditText(this);
        customInput.setSingleLine(false);
        customInput.setMinLines(2);
        customInput.setMaxLines(5);
        customInput.setHint("Geminiへのカスタム指示");
        customInput.setText(getPreferences().getString(KEY_CUSTOM, ""));
        customInput.setTextSize(14);
        customPanel.addView(customInput);

        bridgeTokenInput = new EditText(this);
        bridgeTokenInput.setSingleLine(true);
        bridgeTokenInput.setHint("グラス連携トークン");
        bridgeTokenInput.setText(getPreferences().getString(KEY_BRIDGE_TOKEN, ""));
        bridgeTokenInput.setTextSize(14);
        bridgeTokenInput.setVisibility(View.GONE);
        customPanel.addView(bridgeTokenInput);

        Button pairGlass = new Button(this);
        pairGlass.setText("グラスをペアリング（60秒）");
        pairGlass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String token = getPreferences().getString(KEY_BRIDGE_TOKEN, "").trim();
                if (token.length() < 16) {
                    token = createBridgeToken();
                    getPreferences().edit().putString(KEY_BRIDGE_TOKEN, token).apply();
                    bridgeTokenInput.setText(token);
                }
                pairingUntilMs = System.currentTimeMillis() + 60000L;
                updateStatus("ペアリング待機中", "60秒以内にグラスのSETを押してください。");
            }
        });
        customPanel.addView(pairGlass);

        Button sendCustom = new Button(this);
        sendCustom.setText("カスタム指示をグラスへ保存");
        sendCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = customInput.getText().toString().trim();
                String token = bridgeTokenInput.getText().toString().trim();
                if (token.length() < 16) {
                    token = createBridgeToken();
                    bridgeTokenInput.setText(token);
                }
                getPreferences().edit().putString(KEY_CUSTOM, text).putString(KEY_BRIDGE_TOKEN, token).apply();
                synchronized (MainActivity.class) {
                    pendingCustomInstructions = text;
                }
                refreshCustomInfo();
                addAiLog("カスタム指示を更新: " + shortText(text, 160));
                updateStatus("指示を保存", "次にグラスが同期したら反映します。");
            }
        });
        customPanel.addView(sendCustom);
        root.addView(customPanel);

        Button clearLogs = new Button(this);
        clearLogs.setText("削除");
        clearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                synchronized (LOGS) {
                    LOGS.clear();
                }
                refreshLogs();
            }
        });
        actionRow.addView(clearLogs, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        clearLogs.setVisibility(View.GONE);

        Button stopButton = new Button(this);
        stopButton.setText("停止");
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                synchronized (MainActivity.class) {
                    pendingControl = "stop";
                    pendingCommand = "";
                }
                getPreferences().edit().remove(KEY_PENDING_COMMAND).apply();
                addAiLog("操作: 停止 / 次の会話へ");
            }
        });
        actionRow.addView(stopButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolsButton = new Button(this);
        toolsButton.setText("操作");
        toolsButton.setTextSize(12);
        toolsButton.setMinHeight(0);
        toolsButton.setPadding(4, 0, 4, 0);
        toolsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean show = toolsPanel != null && toolsPanel.getVisibility() != View.VISIBLE;
                if (toolsPanel != null) {
                    toolsPanel.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (customPanel != null && !show) {
                    customPanel.setVisibility(View.GONE);
                }
                toolsButton.setText(show ? "閉じる" : "操作");
            }
        });
        actionRow.addView(toolsButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(actionRow);

        toolsPanel = new LinearLayout(this);
        toolsPanel.setOrientation(LinearLayout.VERTICAL);
        toolsPanel.setVisibility(View.GONE);

        LinearLayout miscRow = new LinearLayout(this);
        miscRow.setOrientation(LinearLayout.HORIZONTAL);

        Button customTool = new Button(this);
        customTool.setText("指示");
        customTool.setTextSize(12);
        customTool.setMinHeight(0);
        customTool.setPadding(4, 0, 4, 0);
        customTool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean show = customPanel.getVisibility() != View.VISIBLE;
                customPanel.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
        miscRow.addView(customTool, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        customTool.setVisibility(View.GONE);

        Button clearTool = new Button(this);
        clearTool.setText("削除");
        clearTool.setTextSize(12);
        clearTool.setMinHeight(0);
        clearTool.setPadding(4, 0, 4, 0);
        clearTool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                synchronized (LOGS) {
                    LOGS.clear();
                }
                refreshLogs();
            }
        });
        miscRow.addView(clearTool, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolsPanel.addView(miscRow);

        LinearLayout proRow = new LinearLayout(this);
        proRow.setOrientation(LinearLayout.HORIZONTAL);

        proRow.addView(notificationAccess, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button proactiveOn = new Button(this);
        proactiveOn.setVisibility(View.GONE);
        proactiveOn.setText("PRO ON");
        proactiveOn.setTextSize(12);
        proactiveOn.setMinHeight(0);
        proactiveOn.setPadding(4, 0, 4, 0);
        proactiveOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                synchronized (MainActivity.class) {
                    pendingControl = "pro_on";
                }
                addAiLog("操作: PRO ON");
            }
        });
        proRow.addView(proactiveOn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button proactiveOff = new Button(this);
        proactiveOff.setVisibility(View.GONE);
        proactiveOff.setText("PRO OFF");
        proactiveOff.setTextSize(12);
        proactiveOff.setMinHeight(0);
        proactiveOff.setPadding(4, 0, 4, 0);
        proactiveOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                synchronized (MainActivity.class) {
                    pendingControl = "pro_off";
                }
                addAiLog("操作: PRO OFF");
            }
        });
        proRow.addView(proactiveOff, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolsPanel.addView(proRow);

        LinearLayout wifiRow = new LinearLayout(this);
        wifiRow.setOrientation(LinearLayout.HORIZONTAL);

        Button wifiOn = new Button(this);
        wifiOn.setText("WiFi ON");
        wifiOn.setTextSize(12);
        wifiOn.setMinHeight(0);
        wifiOn.setPadding(4, 0, 4, 0);
        wifiOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl("wifi_on", "グラスWiFi ON");
            }
        });
        wifiRow.addView(wifiOn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button wifiReconnect = new Button(this);
        wifiReconnect.setText("再接続");
        wifiReconnect.setTextSize(12);
        wifiReconnect.setMinHeight(0);
        wifiReconnect.setPadding(4, 0, 4, 0);
        wifiReconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl("wifi_reconnect", "グラスWiFi再接続");
            }
        });
        wifiRow.addView(wifiReconnect, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button wifiStatus = new Button(this);
        wifiStatus.setText("状態");
        wifiStatus.setTextSize(12);
        wifiStatus.setMinHeight(0);
        wifiStatus.setPadding(4, 0, 4, 0);
        wifiStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl("wifi_status", "グラスWiFi状態確認");
            }
        });
        wifiRow.addView(wifiStatus, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolsPanel.addView(wifiRow);

        LinearLayout launchRow = new LinearLayout(this);
        launchRow.setOrientation(LinearLayout.HORIZONTAL);

        Button openAi = new Button(this);
        openAi.setText("AI起動");
        openAi.setTextSize(12);
        openAi.setMinHeight(0);
        openAi.setPadding(4, 0, 4, 0);
        openAi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl("open_ai", "RokidKeyboardAI起動");
            }
        });
        launchRow.addView(openAi, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button openManager = new Button(this);
        openManager.setText("Manager");
        openManager.setTextSize(12);
        openManager.setMinHeight(0);
        openManager.setPadding(4, 0, 4, 0);
        openManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendControl("open_manager", "RokidManager起動");
            }
        });
        launchRow.addView(openManager, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolsPanel.addView(launchRow);
        root.addView(toolsPanel);

        logView = new TextView(this);
        logView.setTextSize(13);
        logView.setTextColor(Color.DKGRAY);
        logView.setPadding(0, 8, 0, 0);
        logView.setTextIsSelectable(true);
        ScrollView logScroll = new ScrollView(this);
        logScroll.addView(logView);
        root.addView(logScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        updateStatus("準備中", "カレンダー権限を確認しています。\nメールは通知アクセス許可後に読めます。");
        refreshCustomInfo();
        refreshLogs();
    }

    private void refreshCustomInfo() {
        if (customInfo == null || customInput == null) return;
        String saved = getPreferences().getString(KEY_CUSTOM, "");
        if (saved == null || saved.trim().length() == 0) {
            customInfo.setText("現在のカスタム情報: 未設定\n下の欄に入力して保存すると、グラス側Geminiへ反映されます。");
        } else {
            customInfo.setText("現在のカスタム情報:\n" + saved.trim());
        }
        if (customPanel != null && customPanel.getVisibility() == View.VISIBLE) {
            customInput.setText(saved == null ? "" : saved);
        }
    }

    private void ensureCalendarPermission() {
        ArrayList<String> missingExtra = new ArrayList<String>();
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            missingExtra.add(Manifest.permission.READ_CALENDAR);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            missingExtra.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!missingExtra.isEmpty()) {
            requestPermissions(missingExtra.toArray(new String[missingExtra.size()]), 10);
            updateStatus("権限待ち", "カレンダーと音声認識の権限を許可してください。");
            return;
        }
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.READ_CALENDAR }, 10);
            updateStatus("権限待ち", "表示された画面でカレンダーの読み取りを許可してください。");
            return;
        }
        startServer();
    }

    private void sendControl(String control, String label) {
        synchronized (MainActivity.class) {
            pendingControl = control;
        }
        updateStatus("操作待ち", label + " をグラスへ送ります");
        addAiLog("操作: " + label);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 205) {
            refreshWeatherAsync();
            return;
        }
        ensureCalendarPermission();
    }

    private void startServer() {
        if (running) return;
        running = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT, 20, InetAddress.getByName("0.0.0.0"));
                    final String ip = getWifiIp();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus("起動中",
                                    "グラスから接続できます。\nhttp://" + ip + ":" + PORT + "/today");
                        }
                    });
                    while (running) {
                        final Socket client = serverSocket.accept();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                handleClient(client);
                            }
                        }, "CalendarServerClient").start();
                    }
                } catch (final Exception error) {
                    if (running) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus("停止しました", error.getMessage());
                            }
                        });
                    }
                }
            }
        }, "CalendarServer").start();
    }

    private void handleClient(Socket socket) {
        try {
            socket.setSoTimeout(20000);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String request = reader.readLine();
            boolean today = request != null && request.startsWith("GET /today");
            boolean schedule = request != null && request.startsWith("GET /schedule");
            boolean mail = request != null && request.startsWith("GET /mail");
            boolean news = request != null && request.startsWith("GET /news");
            boolean command = request != null && request.startsWith("GET /command");
            boolean ackCommand = request != null && request.startsWith("GET /ack_command");
            boolean control = request != null && request.startsWith("GET /control");
            boolean log = request != null && request.startsWith("GET /log");
            boolean postLog = request != null && request.startsWith("POST /log");
            boolean custom = request != null && request.startsWith("GET /custom");
            boolean health = request != null && request.startsWith("GET /health");
            boolean postHealth = request != null && request.startsWith("POST /health");
            boolean weather = request != null && request.startsWith("GET /weather");
            boolean stt = request != null && request.startsWith("POST /stt");
            boolean pair = request != null && request.startsWith("GET /pair");
            RequestPayload payload = readRequestPayload(reader);
            String bodyText = payload.body;
            if (pair) {
                if (System.currentTimeMillis() > pairingUntilMs) {
                    writeJsonResponse(socket, 403, "{\"ok\":false,\"error\":\"pairing_closed\"}");
                    return;
                }
                pairingUntilMs = 0L;
                JSONObject paired = new JSONObject();
                paired.put("ok", true);
                paired.put("token", getPreferences().getString(KEY_BRIDGE_TOKEN, ""));
                writeJsonResponse(socket, 200, paired.toString());
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        updateStatus("ペアリング完了", "グラスへ認証情報を安全に転送しました。");
                    }
                });
                return;
            }
            if (!constantTimeEquals(getPreferences().getString(KEY_BRIDGE_TOKEN, ""), payload.token)) {
                writeJsonResponse(socket, 401, "{\"ok\":false,\"error\":\"unauthorized\"}");
                return;
            }
            String body = today ? buildTodayJson().toString()
                    : schedule ? buildScheduleJson(parseIntQuery(request, "offset", 0),
                            parseIntQuery(request, "days", 1),
                            parseStringQuery(request, "q", "")).toString()
                    : mail ? buildMailJson().toString()
                    : news ? buildNewsJson(parseStringQuery(request, "q", "")).toString()
                    : command ? buildCommandJson().toString()
                    : ackCommand ? buildAckCommandJson().toString()
                    : control ? buildControlJson().toString()
                    : postHealth ? buildPostHealthResult(bodyText).toString()
                    : health ? buildHealthJson().toString()
                    : weather ? buildWeatherJson().toString()
                    : postLog ? buildPostLogResult(bodyText).toString()
                    : log ? buildLogResult(request).toString()
                    : custom ? buildCustomJson().toString()
                    : stt ? buildSpeechTextJson(bodyText).toString()
                    : "{\"ok\":true}";
            writeJsonResponse(socket, 200, body);
        } catch (Exception error) {
            Log.w(TAG, "handleClient failed", error);
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private JSONObject buildSpeechTextJson(String bodyText) throws Exception {
        JSONObject root = new JSONObject();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            root.put("ok", false);
            root.put("error", "record_audio_permission_missing");
            return root;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            root.put("ok", false);
            root.put("error", "speech_recognizer_not_available");
            return root;
        }
        JSONObject request = new JSONObject(bodyText == null || bodyText.trim().length() == 0 ? "{}" : bodyText);
        String encoded = request.optString("pcm", "");
        int sampleRate = request.optInt("sampleRate", 16000);
        if (encoded.length() == 0) {
            root.put("ok", false);
            root.put("error", "empty_audio");
            return root;
        }
        byte[] pcm = Base64.decode(encoded, Base64.NO_WRAP);
        if (pcm == null || pcm.length < 8000) {
            root.put("ok", false);
            root.put("error", "audio_too_short");
            return root;
        }
        String transcript;
        try {
            transcript = transcribePcmWithSpeechRecognizer(pcm, sampleRate);
        } catch (Exception e) {
            root.put("ok", false);
            root.put("error", e.getMessage() == null ? "speech_failed" : e.getMessage());
            return root;
        }
        root.put("ok", transcript.length() > 0);
        root.put("transcript", transcript);
        if (transcript.length() == 0) {
            root.put("error", "no_match");
        }
        addAiLog("音声入力: " + shortText(transcript.length() == 0 ? "(聞き取りなし)" : transcript, 160));
        return root;
    }

    private String transcribePcmWithSpeechRecognizer(final byte[] pcm, final int sampleRate) throws Exception {
        Log.i(TAG, "phone stt start pcmBytes=" + (pcm == null ? 0 : pcm.length) + " sampleRate=" + sampleRate);
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[] { "" };
        final String[] partial = new String[] { "" };
        final String[] error = new String[] { "" };
        final SpeechRecognizer[] recognizerHolder = new SpeechRecognizer[1];
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                    recognizerHolder[0] = recognizer;
                    final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                    final ParcelFileDescriptor readSide = pipe[0];
                    final ParcelFileDescriptor writeSide = pipe[1];
                    recognizer.setRecognitionListener(new RecognitionListener() {
                        @Override
                        public void onReadyForSpeech(Bundle params) {
                            updateStatus("音声認識中", "グラスから届いた音声を文字起こししています。");
                        }

                        @Override
                        public void onBeginningOfSpeech() {
                            Log.i(TAG, "phone stt onBeginningOfSpeech");
                        }

                        @Override
                        public void onRmsChanged(float rmsdB) {
                        }

                        @Override
                        public void onBufferReceived(byte[] buffer) {
                        }

                        @Override
                        public void onEndOfSpeech() {
                            Log.i(TAG, "phone stt onEndOfSpeech");
                        }

                        @Override
                        public void onError(int code) {
                            Log.w(TAG, "phone stt onError code=" + code + " text=" + speechErrorText(code));
                            error[0] = speechErrorText(code);
                            try {
                                recognizer.destroy();
                            } catch (Exception ignored) {
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onResults(Bundle results) {
                            String text = bestRecognitionText(results);
                            if (text.length() > 0) {
                                result[0] = text;
                            }
                            Log.i(TAG, "phone stt onResults textLength=" + result[0].length());
                            try {
                                recognizer.destroy();
                            } catch (Exception ignored) {
                            }
                            latch.countDown();
                        }

                        @Override
                        public void onPartialResults(Bundle partialResults) {
                            String text = bestRecognitionText(partialResults);
                            if (text.length() > 0) {
                                partial[0] = text;
                            }
                            Log.i(TAG, "phone stt onPartialResults textLength=" + partial[0].length());
                        }

                        @Override
                        public void onEvent(int eventType, Bundle params) {
                            Log.i(TAG, "phone stt onEvent type=" + eventType);
                        }

                        @Override
                        public void onSegmentResults(Bundle segmentResults) {
                            String text = bestRecognitionText(segmentResults);
                            if (text.length() > 0) {
                                result[0] = text;
                            }
                            Log.i(TAG, "phone stt onSegmentResults textLength=" + result[0].length());
                        }

                        @Override
                        public void onEndOfSegmentedSession() {
                            Log.i(TAG, "phone stt onEndOfSegmentedSession textLength=" + result[0].length()
                                    + " partialLength=" + partial[0].length());
                            try {
                                recognizer.destroy();
                            } catch (Exception ignored) {
                            }
                            latch.countDown();
                        }
                    });
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP");
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readSide);
                    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
                    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1);
                    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, sampleRate);
                    if (Build.VERSION.SDK_INT >= 33) {
                        intent.putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE);
                    }
                    recognizer.startListening(intent);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(writeSide);
                                byte[] leadingSilence = new byte[Math.max(8000, sampleRate / 4 * 2)];
                                byte[] trailingSilence = new byte[Math.max(12000, sampleRate / 2 * 2)];
                                out.write(leadingSilence);
                                out.write(pcm);
                                out.write(trailingSilence);
                                out.flush();
                                out.close();
                                Log.i(TAG, "phone stt audio pipe closed");
                            } catch (Exception e) {
                                Log.w(TAG, "phone stt audio pipe error", e);
                                error[0] = "audio_pipe_error: " + e.getMessage();
                                latch.countDown();
                            }
                        }
                    }, "PhoneSttAudioWriter").start();
                } catch (Exception e) {
                    error[0] = e.getMessage();
                    latch.countDown();
                }
            }
        });
        boolean completed = latch.await(24, TimeUnit.SECONDS);
        if (!completed) {
            Log.w(TAG, "phone stt timeout pcmBytes=" + (pcm == null ? 0 : pcm.length));
            String salvaged = result[0].length() > 0 ? result[0] : partial[0];
            if (salvaged.length() > 0) {
                Log.i(TAG, "phone stt timeout but salvaged textLength=" + salvaged.length());
                return salvaged;
            }
            final SpeechRecognizer recognizer = recognizerHolder[0];
            if (recognizer != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            recognizer.cancel();
                            recognizer.destroy();
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
            throw new IllegalStateException("speech_timeout");
        }
        if (result[0].length() == 0 && partial[0].length() > 0) {
            return partial[0];
        }
        if (result[0].length() == 0 && error[0].length() > 0) {
            throw new IllegalStateException(error[0]);
        }
        return result[0];
    }

    private String bestRecognitionText(Bundle bundle) {
        if (bundle == null) {
            return "";
        }
        ArrayList<String> list = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return "";
        }
        return list.get(0).trim();
    }

    private String speechErrorText(int code) {
        if (code == SpeechRecognizer.ERROR_AUDIO) {
            return "スマホ側の音声入力エラーです。";
        }
        if (code == SpeechRecognizer.ERROR_CLIENT) {
            return "スマホ側の音声認識サービスを開始できませんでした。";
        }
        if (code == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            return "スマホ側のマイク権限がありません。";
        }
        if (code == SpeechRecognizer.ERROR_NETWORK || code == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
            return "スマホ側の音声認識サービスがネットワークに接続できません。";
        }
        if (code == SpeechRecognizer.ERROR_NO_MATCH) {
            return "スマホ側で聞き取れる音声が見つかりませんでした。少し長めにはっきり話してください。";
        }
        if (code == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            return "スマホ側の音声認識サービスが使用中です。少し待ってください。";
        }
        if (code == SpeechRecognizer.ERROR_SERVER) {
            return "スマホ側の音声認識サービスでエラーが起きました。";
        }
        if (code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            return "スマホ側で話し始めを検出できませんでした。";
        }
        return "スマホ側音声認識エラー: " + code;
    }

    private JSONObject buildTodayJson() throws Exception {
        return buildScheduleJson(0, 1);
    }

    private JSONObject buildScheduleJson(int offsetDays, int days) throws Exception {
        return buildScheduleJson(offsetDays, days, "");
    }

    private JSONObject buildScheduleJson(int offsetDays, int days, String query) throws Exception {
        int safeDays = Math.max(1, Math.min(days, 730));
        String safeQuery = safe(query).trim();
        long start = startOfDayOffsetMillis(offsetDays);
        long end = endOfDayOffsetMillis(offsetDays + safeDays - 1);
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        android.content.ContentUris.appendId(builder, start);
        android.content.ContentUris.appendId(builder, end);
        String[] projection = new String[] {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        };
        Cursor cursor = getContentResolver().query(builder.build(), projection,
                CalendarContract.Instances.VISIBLE + "!=0",
                null,
                CalendarContract.Instances.BEGIN + " ASC");

        JSONArray events = new JSONArray();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    JSONObject event = new JSONObject();
                    event.put("title", shortText(cursor.getString(0), 60));
                    event.put("begin", cursor.getLong(1));
                    event.put("end", cursor.getLong(2));
                    event.put("allDay", cursor.getInt(3) != 0);
                    event.put("location", shortText(cursor.getString(4), 40));
                    event.put("calendar", safe(cursor.getString(5)));
                    if (matchesScheduleQuery(event, safeQuery)) {
                        events.put(event);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("timezone", TimeZone.getDefault().getID());
        root.put("date", DateFormat.format("yyyy-MM-dd", start).toString());
        root.put("startDate", DateFormat.format("yyyy-MM-dd", start).toString());
        root.put("endDate", DateFormat.format("yyyy-MM-dd", end).toString());
        root.put("offsetDays", offsetDays);
        root.put("days", safeDays);
        root.put("query", safeQuery);
        root.put("events", events);
        return root;
    }

    private boolean matchesScheduleQuery(JSONObject event, String query) {
        if (query == null || query.trim().length() == 0) {
            return true;
        }
        String haystack = (event.optString("title", "") + " "
                + event.optString("location", "") + " "
                + event.optString("calendar", "")).toLowerCase(Locale.JAPAN);
        String normalized = query.toLowerCase(Locale.JAPAN).trim();
        if (normalized.contains("病院")) {
            String[] hospitalWords = new String[] {
                    "病院", "医療", "医療センター", "クリニック",
                    "診察", "治療", "検査", "MRI", "CT",
                    "カンファレンス", "多摩総合", "東京都立多摩総合医療センター"
            };
            for (String word : hospitalWords) {
                if (haystack.contains(word.toLowerCase(Locale.JAPAN))) {
                    return true;
                }
            }
        }
        String[] words = normalized.split("[\\s　,、]+");
        for (String word : words) {
            if (word.length() > 0 && haystack.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private JSONObject buildControlJson() throws Exception {
        JSONObject root = new JSONObject();
        String control;
        synchronized (MainActivity.class) {
            control = pendingControl;
            pendingControl = "";
        }
        root.put("ok", true);
        root.put("control", control == null ? "" : control);
        return root;
    }

    private static final class RequestPayload {
        final String body;
        final String token;
        RequestPayload(String body, String token) {
            this.body = body;
            this.token = token;
        }
    }

    private RequestPayload readRequestPayload(BufferedReader reader) throws Exception {
        int contentLength = 0;
        String token = "";
        String line;
        while ((line = reader.readLine()) != null && line.length() > 0) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                } catch (Exception ignored) {
                }
            } else if (lower.startsWith("x-roki-token:")) {
                token = line.substring(line.indexOf(':') + 1).trim();
            }
        }
        if (contentLength <= 0) {
            return new RequestPayload("", token);
        }
        char[] chars = new char[contentLength];
        int offset = 0;
        while (offset < contentLength) {
            int read = reader.read(chars, offset, contentLength - offset);
            if (read < 0) break;
            offset += read;
        }
        return new RequestPayload(new String(chars, 0, offset), token);
    }

    private void writeJsonResponse(Socket socket, int statusCode, String body) throws Exception {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String statusText = statusCode == 200 ? "OK" : "Unauthorized";
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n"
                + "Content-Length: " + bytes.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    private void ensureBridgeToken() {
        if (getPreferences().getString(KEY_BRIDGE_TOKEN, "").trim().length() < 16) {
            getPreferences().edit().putString(KEY_BRIDGE_TOKEN, createBridgeToken()).apply();
        }
    }

    private String createBridgeToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] left = (expected == null ? "" : expected).getBytes(StandardCharsets.UTF_8);
        byte[] right = (actual == null ? "" : actual).getBytes(StandardCharsets.UTF_8);
        int diff = left.length ^ right.length;
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            byte a = i < left.length ? left[i] : 0;
            byte b = i < right.length ? right[i] : 0;
            diff |= a ^ b;
        }
        return diff == 0 && left.length >= 16;
    }

    private JSONObject buildCommandJson() throws Exception {
        JSONObject root = new JSONObject();
        String command;
        synchronized (MainActivity.class) {
            command = pendingCommand;
        }
        if ((command == null || command.length() == 0) && activeActivity != null) {
            command = activeActivity.getPreferences().getString(KEY_PENDING_COMMAND, "");
        }
        root.put("ok", true);
        root.put("command", command == null ? "" : command);
        return root;
    }

    private JSONObject buildAckCommandJson() throws Exception {
        JSONObject root = new JSONObject();
        synchronized (MainActivity.class) {
            pendingCommand = "";
        }
        if (activeActivity != null) {
            activeActivity.getPreferences().edit().remove(KEY_PENDING_COMMAND).apply();
            activeActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activeActivity.updateStatus("受信済み", "グラスがスマホ入力を受け取りました");
                }
            });
        }
        root.put("ok", true);
        root.put("ack", true);
        return root;
    }

    private JSONObject buildPostLogResult(String bodyText) throws Exception {
        if (bodyText != null && bodyText.length() > 0) {
            String kind;
            String message;
            if (bodyText.trim().startsWith("{")) {
                JSONObject body = new JSONObject(bodyText);
                kind = body.optString("kind", "");
                message = body.optString("message", "");
            } else {
                kind = formValue(bodyText, "kind");
                message = formValue(bodyText, "message");
            }
            if (message.length() > 0 && isAiLogKind(kind)) {
                addAiLog((kind.length() > 0 ? kind + ": " : "") + message);
            }
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        return root;
    }

    private String formValue(String bodyText, String key) {
        try {
            String[] parts = bodyText.split("&");
            for (String part : parts) {
                int eq = part.indexOf('=');
                String name = eq < 0 ? part : part.substring(0, eq);
                if (key.equals(name)) {
                    String value = eq < 0 ? "" : part.substring(eq + 1);
                    return URLDecoder.decode(value, "UTF-8");
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private JSONObject buildCustomJson() throws Exception {
        JSONObject root = new JSONObject();
        String custom;
        synchronized (MainActivity.class) {
            custom = pendingCustomInstructions;
            pendingCustomInstructions = "";
        }
        root.put("ok", true);
        root.put("custom", custom == null ? "" : custom);
        return root;
    }

    private void ensureLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 205);
        }
    }

    private JSONObject buildWeatherJson() throws Exception {
        SharedPreferences preferences = getPreferences();
        String location = preferences.getString(KEY_WEATHER_LOCATION, "").trim();
        String condition = preferences.getString(KEY_WEATHER_CONDITION, "").trim();
        String temperature = preferences.getString(KEY_WEATHER_TEMPERATURE, "").trim();
        String feelsLike = preferences.getString(KEY_WEATHER_FEELS_LIKE, "").trim();
        long updatedAt = preferences.getLong(KEY_WEATHER_TIME, 0L);
        if (updatedAt == 0L || System.currentTimeMillis() - updatedAt > 600000L) {
            refreshWeatherAsync();
        }
        JSONObject root = new JSONObject();
        root.put("ok", location.length() > 0 && condition.length() > 0 && temperature.length() > 0);
        root.put("location", location);
        root.put("condition", condition);
        root.put("temperature", temperature);
        root.put("feelsLike", feelsLike);
        root.put("time", updatedAt);
        root.put("source", "Open-Meteo");
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            root.put("error", "location_permission_missing");
        }
        return root;
    }

    private void refreshWeatherAsync() {
        if (weatherRefreshInFlight
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        weatherRefreshInFlight = true;
        Log.i(TAG, "weather refresh started");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    refreshWeatherNow();
                } catch (Exception error) {
                    Log.w(TAG, "weather refresh failed", error);
                } finally {
                    weatherRefreshInFlight = false;
                }
            }
        }, "WeatherRefresh").start();
    }

    private void refreshWeatherNow() throws Exception {
        Location location = getBestAvailableLocation();
        if (location == null) {
            throw new IllegalStateException("phone location unavailable");
        }
        String latitude = String.format(Locale.US, "%.5f", location.getLatitude());
        String longitude = String.format(Locale.US, "%.5f", location.getLongitude());
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,apparent_temperature,weather_code&timezone=auto";
        JSONObject current = new JSONObject(fetchText(url)).optJSONObject("current");
        if (current == null) {
            throw new IllegalStateException("weather response has no current data");
        }
        double temperature = current.optDouble("temperature_2m", Double.NaN);
        double feelsLike = current.optDouble("apparent_temperature", Double.NaN);
        int weatherCode = current.optInt("weather_code", -1);
        if (Double.isNaN(temperature) || weatherCode < 0) {
            throw new IllegalStateException("weather response is incomplete");
        }
        String place = getPreferences().getString(KEY_WEATHER_LOCATION, "").trim();
        if (place.length() == 0) place = "取得済";
        SharedPreferences.Editor editor = getPreferences().edit()
                .putString(KEY_WEATHER_LOCATION, shortInline(place, 20))
                .putString(KEY_WEATHER_CONDITION, weatherCondition(weatherCode))
                .putString(KEY_WEATHER_TEMPERATURE,
                        String.format(Locale.JAPAN, "%.1f", temperature))
                .putLong(KEY_WEATHER_TIME, System.currentTimeMillis());
        if (!Double.isNaN(feelsLike)) {
            editor.putString(KEY_WEATHER_FEELS_LIKE,
                    String.format(Locale.JAPAN, "%.1f", feelsLike));
        } else {
            editor.remove(KEY_WEATHER_FEELS_LIKE);
        }
        editor.apply();
        Log.i(TAG, "weather data cached: " + weatherCondition(weatherCode)
                + " " + String.format(Locale.JAPAN, "%.1f", temperature) + "C");
        String resolvedPlace = readMunicipality(location);
        if (resolvedPlace.length() > 0) {
            getPreferences().edit()
                    .putString(KEY_WEATHER_LOCATION, shortInline(resolvedPlace, 20))
                    .apply();
            Log.i(TAG, "weather municipality resolved");
        }
    }

    private Location getBestAvailableLocation() throws Exception {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }
        Location best = null;
        for (String provider : manager.getProviders(true)) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
            } catch (Exception ignored) {
            }
        }
        if (best != null && System.currentTimeMillis() - best.getTime() <= 900000L) {
            return best;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            String provider = null;
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            } else if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            }
            if (provider != null) {
                final Location[] result = new Location[1];
                final CountDownLatch latch = new CountDownLatch(1);
                final android.os.CancellationSignal cancellation = new android.os.CancellationSignal();
                manager.getCurrentLocation(provider, cancellation, getMainExecutor(),
                        new java.util.function.Consumer<Location>() {
                            @Override public void accept(Location value) {
                                result[0] = value;
                                latch.countDown();
                            }
                        });
                latch.await(8L, TimeUnit.SECONDS);
                cancellation.cancel();
                if (result[0] != null) {
                    return result[0];
                }
            }
        }
        return best;
    }

    private String readMunicipality(Location location) {
        try {
            if (!Geocoder.isPresent()) {
                return "";
            }
            List<Address> addresses = new Geocoder(this, Locale.JAPAN)
                    .getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses == null || addresses.isEmpty()) {
                return "";
            }
            Address address = addresses.get(0);
            String region = safe(address.getAdminArea()).trim();
            String municipality = safe(address.getLocality()).trim();
            if (municipality.length() == 0) {
                municipality = safe(address.getSubAdminArea()).trim();
            }
            if (municipality.length() == 0) {
                municipality = safe(address.getSubLocality()).trim();
            }
            if (region.length() == 0) {
                return municipality;
            }
            if (municipality.length() == 0 || region.equals(municipality)
                    || region.endsWith(municipality)) {
                return region;
            }
            return region + " " + municipality;
        } catch (Exception error) {
            Log.d(TAG, "reverse geocoding unavailable: " + error.getMessage());
            return "";
        }
    }

    private String weatherCondition(int code) {
        if (code == 0) return "快晴";
        if (code == 1) return "晴れ";
        if (code == 2) return "晴れ時々曇り";
        if (code == 3) return "曇り";
        if (code == 45 || code == 48) return "霧";
        if (code >= 51 && code <= 57) return "霧雨";
        if (code >= 61 && code <= 67) return "雨";
        if (code >= 71 && code <= 77) return "雪";
        if (code >= 80 && code <= 82) return "にわか雨";
        if (code == 85 || code == 86) return "にわか雪";
        if (code >= 95 && code <= 99) return "雷雨";
        return "天気不明";
    }

    private JSONObject buildHealthJson() throws Exception {
        String savedCompact = getPreferences().getString(KEY_HEALTH_COMPACT, "").trim();
        long savedTime = getPreferences().getLong(KEY_HEALTH_TIME, 0L);
        String today = LocalDate.now(ZoneId.systemDefault()).toString();
        String savedDate = getPreferences().getString(KEY_HEALTH_DATE, "");
        if (!today.equals(savedDate)) {
            savedCompact = "";
            savedTime = 0L;
            getPreferences().edit().remove(KEY_HEALTH_COMPACT).remove(KEY_HEALTH_TIME)
                    .putString(KEY_HEALTH_DATE, today).apply();
        }
        refreshHealthConnectSteps();
        String compact = savedCompact;
        long time = savedTime;
        JSONObject root = new JSONObject();
        root.put("ok", compact.length() > 0);
        root.put("compact", compact);
        root.put("time", time);
        root.put("source", "Health Connect");
        return root;
    }

    private void ensureHealthConnectPermission() {
        if (Build.VERSION.SDK_INT < 34) return;
        String permission = "android.permission.health.READ_STEPS";
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, 204);
        }
    }

    private void refreshHealthConnectSteps() {
        if (Build.VERSION.SDK_INT < 34) return;
        String permission = "android.permission.health.READ_STEPS";
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return;
        try {
            HealthConnectManager manager = getSystemService(HealthConnectManager.class);
            if (manager == null) return;
            ZoneId zone = ZoneId.systemDefault();
            final String queryDate = LocalDate.now(zone).toString();
            Instant start = LocalDate.parse(queryDate).atStartOfDay(zone).toInstant();
            Instant end = Instant.now();
            TimeInstantRangeFilter range = new TimeInstantRangeFilter.Builder()
                    .setStartTime(start).setEndTime(end).build();
            AggregateRecordsRequest<Long> request = new AggregateRecordsRequest.Builder<Long>(range)
                    .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL).build();
            manager.aggregate(request, getMainExecutor(),
                    new OutcomeReceiver<AggregateRecordsResponse<Long>, HealthConnectException>() {
                        @Override public void onResult(AggregateRecordsResponse<Long> response) {
                            if (!queryDate.equals(LocalDate.now(ZoneId.systemDefault()).toString())) {
                                return;
                            }
                            Long steps = response.get(StepsRecord.STEPS_COUNT_TOTAL);
                            long count = steps == null ? 0L : steps.longValue();
                            getPreferences().edit()
                                    .putString(KEY_HEALTH_COMPACT, "歩数 " + count)
                                    .putLong(KEY_HEALTH_TIME, System.currentTimeMillis())
                                    .putString(KEY_HEALTH_DATE, queryDate).apply();
                        }
                        @Override public void onError(HealthConnectException error) {
                            Log.d(TAG, "Health Connect steps unavailable: " + error.getMessage());
                        }
                    });
        } catch (Exception error) {
            Log.d(TAG, "Health Connect query failed: " + error.getMessage());
        }
    }

    private String queryGoBeBridgeHealth() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(7500);
            int replyPort = socket.getLocalPort();
            String request = "request=latest;replyPort=" + replyPort + ";source=ROKID_AI";
            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
            DatagramPacket outbound = new DatagramPacket(
                    requestBytes,
                    requestBytes.length,
                    InetAddress.getByName("127.0.0.1"),
                    45455);
            socket.send(outbound);
            byte[] buffer = new byte[2048];
            DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);
            socket.receive(inbound);
            String payload = new String(inbound.getData(), inbound.getOffset(), inbound.getLength(), StandardCharsets.UTF_8);
            return compactGoBePayload(payload);
        } catch (Exception error) {
            Log.d(TAG, "GoBe bridge health unavailable: " + error.getMessage());
            return "";
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private String compactGoBePayload(String payload) {
        if (payload == null || payload.trim().length() == 0) {
            return "";
        }
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<String, String>();
        for (String part : payload.split("[;&\\n]")) {
            int equals = part.indexOf('=');
            if (equals <= 0) continue;
            values.put(part.substring(0, equals).trim(), part.substring(equals + 1).trim());
        }
        StringBuilder result = new StringBuilder("HEALBE");
        appendHealthPart(result, "水", firstHealthValue(values, "waterStatus", "water"));
        appendHealthPart(result, "E", suffixHealth(values.get("energy"), "kcal"));
        appendHealthPart(result, "歩", values.get("steps"));
        appendHealthPart(result, "HR", suffixHealth(values.get("pulse"), "bpm"));
        appendHealthPart(result, "S", values.get("stress"));
        appendHealthPart(result, "B", suffixHealth(values.get("battery"), "%"));
        String compact = result.toString();
        return "HEALBE".equals(compact) ? "" : shortInline(compact, 90);
    }

    private String firstHealthValue(java.util.Map<String, String> values, String first, String second) {
        String value = values.get(first);
        return value != null && value.length() > 0 ? value : values.get(second);
    }

    private String suffixHealth(String value, String suffix) {
        return value == null || value.trim().length() == 0 ? "" : value.trim() + suffix;
    }

    private JSONObject buildPostHealthResult(String bodyText) throws Exception {
        String compact = compactHealthFromBody(bodyText);
        JSONObject root = new JSONObject();
        if (compact.length() == 0) {
            root.put("ok", false);
            root.put("error", "empty_health");
            return root;
        }
        long now = System.currentTimeMillis();
        getPreferences().edit()
                .putString(KEY_HEALTH_COMPACT, compact)
                .putLong(KEY_HEALTH_TIME, now)
                .apply();
        root.put("ok", true);
        root.put("compact", compact);
        root.put("time", now);
        return root;
    }

    private String compactHealthFromBody(String bodyText) {
        String body = bodyText == null ? "" : bodyText.trim();
        if (body.length() == 0) {
            return "";
        }
        try {
            if (body.startsWith("{")) {
                JSONObject json = new JSONObject(body);
                String compact = json.optString("compact", "").trim();
                if (compact.length() > 0) {
                    return shortInline(compact, 74);
                }
                StringBuilder builder = new StringBuilder("HEALBE");
                appendHealthPart(builder, "歩", json.optString("steps", ""));
                appendHealthPart(builder, "kcal", json.optString("kcal", json.optString("calories", "")));
                appendHealthPart(builder, "水", json.optString("water", json.optString("hydration", "")));
                appendHealthPart(builder, "HR", json.optString("hr", json.optString("heartRate", "")));
                String built = builder.toString().trim();
                return "HEALBE".equals(built) ? "" : shortInline(built, 74);
            }
        } catch (Exception ignored) {
        }
        return shortInline(body, 74);
    }

    private void appendHealthPart(StringBuilder builder, String label, String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return;
        }
        builder.append(' ').append(label).append(':').append(text);
    }

    private String shortInline(String value, int max) {
        String text = value == null ? "" : value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 1)) + "…";
    }

    private JSONObject buildLogResult(String request) throws Exception {
        String message = queryParam(request, "message");
        String kind = queryParam(request, "kind");
        if (message.length() > 0 && isAiLogKind(kind)) {
            addAiLog((kind.length() > 0 ? kind + ": " : "") + message);
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        return root;
    }

    private String queryParam(String request, String key) {
        try {
            int start = request.indexOf(' ');
            int end = request.indexOf(' ', start + 1);
            if (start < 0 || end < 0) return "";
            String path = request.substring(start + 1, end);
            int queryStart = path.indexOf('?');
            if (queryStart < 0) return "";
            String[] parts = path.substring(queryStart + 1).split("&");
            for (String part : parts) {
                int eq = part.indexOf('=');
                String name = eq < 0 ? part : part.substring(0, eq);
                if (key.equals(name)) {
                    String value = eq < 0 ? "" : part.substring(eq + 1);
                    return URLDecoder.decode(value, "UTF-8");
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private JSONObject buildMailJson() throws Exception {
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("timezone", TimeZone.getDefault().getID());
        root.put("date", DateFormat.format("yyyy-MM-dd", System.currentTimeMillis()).toString());
        root.put("source", "android_notifications");
        root.put("note", "Gmailなどの通知から取得した新着メール概要です。通知アクセス許可後の通知が対象です。");
        JSONArray mails = MailNotificationService.recentMailJson();
        root.put("mails", mails);
        return root;
    }

    private JSONObject buildNewsJson(String query) throws Exception {
        String safeQuery = safe(query).trim();
        String feedUrl;
        if (safeQuery.length() > 0) {
            feedUrl = "https://news.google.com/rss/search?q="
                    + URLEncoder.encode(safeQuery, "UTF-8")
                    + "&hl=ja&gl=JP&ceid=JP:ja";
        } else {
            feedUrl = "https://news.google.com/rss?hl=ja&gl=JP&ceid=JP:ja";
        }
        String xml = fetchText(feedUrl);
        JSONArray articles = parseNewsRss(xml, 8);
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("timezone", TimeZone.getDefault().getID());
        root.put("date", DateFormat.format("yyyy-MM-dd HH:mm", System.currentTimeMillis()).toString());
        root.put("source", "Google News RSS");
        root.put("query", safeQuery);
        root.put("articles", articles);
        return root;
    }

    private JSONArray parseNewsRss(String xml, int maxItems) throws Exception {
        JSONArray articles = new JSONArray();
        Matcher matcher = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL).matcher(safe(xml));
        while (matcher.find() && articles.length() < maxItems) {
            String item = matcher.group(1);
            String title = decodeXml(extractXml(item, "title"));
            String link = decodeXml(extractXml(item, "link"));
            String pubDate = decodeXml(extractXml(item, "pubDate"));
            String source = "";
            int sep = title.lastIndexOf(" - ");
            if (sep > 0 && sep < title.length() - 3) {
                source = title.substring(sep + 3).trim();
                title = title.substring(0, sep).trim();
            }
            JSONObject article = new JSONObject();
            article.put("title", shortText(title, 90));
            article.put("source", shortText(source, 30));
            article.put("published", shortText(pubDate, 40));
            article.put("link", shortText(link, 180));
            articles.put(article);
        }
        return articles;
    }

    private String extractXml(String text, String tag) {
        Matcher matcher = Pattern.compile("<" + tag + "[^>]*>(.*?)</" + tag + ">",
                Pattern.DOTALL).matcher(safe(text));
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).replaceAll("<!\\[CDATA\\[(.*?)\\]\\]>", "$1").trim();
    }

    private String decodeXml(String value) {
        return safe(value)
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");
    }

    private String fetchText(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("User-Agent", "RokidSecretary/1.0");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
        }
        reader.close();
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("news fetch failed: " + code);
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String shortText(String value, int max) {
        String text = safe(value)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }

    private int parseIntQuery(String request, String key, int fallback) {
        try {
            if (request == null) {
                return fallback;
            }
            int pathStart = request.indexOf(' ');
            int pathEnd = request.indexOf(' ', pathStart + 1);
            if (pathStart < 0 || pathEnd <= pathStart) {
                return fallback;
            }
            String path = request.substring(pathStart + 1, pathEnd);
            int queryIndex = path.indexOf('?');
            if (queryIndex < 0) {
                return fallback;
            }
            String[] pairs = path.substring(queryIndex + 1).split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String name = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                if (key.equals(name)) {
                    return Integer.parseInt(URLDecoder.decode(pair.substring(eq + 1), "UTF-8"));
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private String parseStringQuery(String request, String key, String fallback) {
        try {
            if (request == null) {
                return fallback;
            }
            int pathStart = request.indexOf(' ');
            int pathEnd = request.indexOf(' ', pathStart + 1);
            if (pathStart < 0 || pathEnd <= pathStart) {
                return fallback;
            }
            String path = request.substring(pathStart + 1, pathEnd);
            int queryIndex = path.indexOf('?');
            if (queryIndex < 0) {
                return fallback;
            }
            String[] pairs = path.substring(queryIndex + 1).split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String name = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                if (key.equals(name)) {
                    return URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private long startOfTodayMillis() {
        return startOfDayOffsetMillis(0);
    }

    private long startOfDayOffsetMillis(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long endOfTodayMillis() {
        return endOfDayOffsetMillis(0);
    }

    private long endOfDayOffsetMillis(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    private String getWifiIp() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (java.net.InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        String ip = address.getHostAddress();
                        if (ip != null
                                && !ip.startsWith("127.")
                                && !ip.startsWith("169.254.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "スマホのWi-Fi IP";
    }

    private void updateStatus(String title, String message) {
        status.setText(title);
        details.setText(message + " / MAILで通知アクセス設定");
        refreshLogs();
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    public static void addLog(String message) {
        addAiLog(message);
    }

    public static void addAiLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(new Date());
        synchronized (LOGS) {
            LOGS.add(0, time + " " + message);
            while (LOGS.size() > MAX_LOGS) {
                LOGS.remove(LOGS.size() - 1);
            }
        }
        final MainActivity activity = activeActivity;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.refreshLogs();
                }
            });
        }
    }

    private void refreshLogs() {
        if (logView == null) return;
        StringBuilder builder = new StringBuilder();
        builder.append("AI会話ログ\n");
        synchronized (LOGS) {
            if (LOGS.isEmpty()) {
                builder.append("まだログはありません。");
            } else {
                int count = LOGS.size();
                for (int i = 0; i < count; i++) {
                    builder.append(LOGS.get(i)).append('\n');
                }
            }
        }
        logView.setText(builder.toString());
    }

    private boolean isAiLogKind(String kind) {
        return "ユーザー".equals(kind)
                || "ユーザー音声".equals(kind)
                || "Gemini".equals(kind)
                || "直接回答".equals(kind)
                || "カスタム指示".equals(kind)
                || "操作".equals(kind);
    }
}
