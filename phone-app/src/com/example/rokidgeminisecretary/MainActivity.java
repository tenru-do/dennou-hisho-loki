package com.example.rokidgeminisecretary;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    private static final int MAX_LOGS = 60;
    private static final int MAX_LOG_ENTRY_CHARS = 12000;
    private static final int MAX_MEMORY_ENTRIES = 800;
    private static final int MAX_MEMORY_ENTRY_CHARS = 2400;
    private static final int MAX_MEMORY_RESPONSE_CHARS = 2600;
    private static final int MAX_ARCHIVE_QUESTION_CHARS = 420;
    private static final int MAX_ARCHIVE_ANSWER_CHARS = 620;
    private static final long MEMORY_RETENTION_MS = 90L * 24L * 60L * 60L * 1000L;
    private static final long MAX_MEMORY_FILE_BYTES = 4L * 1024L * 1024L;
    private static final String MEMORY_FILE_NAME = "conversation_memory.jsonl";
    private static final String MEMORY_ARCHIVE_FILE_NAME = "conversation_memory_archive.jsonl";
    private static final Object MEMORY_LOCK = new Object();
    // A 9-second 16 kHz mono PCM window is about 288 KB before Base64.
    // Keep a bounded 512 KB ceiling so ambient STT fits without making the
    // local bridge accept arbitrarily large request bodies.
    private static final int MAX_REQUEST_BODY_CHARS = 524288;
    private static final int MAX_COMMAND_CHARS = 3000;
    private static final int MAX_PENDING_COMMANDS = 32;
    private static final int MAX_CUSTOM_CHARS = 2400;
    private static final String PREFS = "phone_secretary";
    private static final String KEY_CUSTOM = "custom_instructions";
    private static final String KEY_CUSTOM_DIRTY = "custom_instructions_dirty";
    private static final String KEY_BRIDGE_TOKEN = "bridge_token";
    private static final String KEY_HEALTH_COMPACT = "health_compact";
    private static final String KEY_HEALTH_TIME = "health_time";
    private static final String KEY_HEALTH_DATE = "health_date";
    private static final String KEY_WEATHER_LOCATION = "weather_location";
    private static final String KEY_WEATHER_CONDITION = "weather_condition";
    private static final String KEY_WEATHER_TEMPERATURE = "weather_temperature";
    private static final String KEY_WEATHER_FEELS_LIKE = "weather_feels_like";
    private static final String KEY_WEATHER_FORECAST = "weather_forecast";
    private static final String KEY_WEATHER_TIME = "weather_time";
    private static final String KEY_PENDING_COMMAND = "pending_command";
    private static final String KEY_PENDING_COMMAND_QUEUE = "pending_command_queue";
    private static final String KEY_LAST_CODEX_NOTIFICATION = "last_codex_notification";
    private static final String KEY_LAST_CODEX_NOTIFICATION_AT = "last_codex_notification_at";
    private static final String KEY_GLASS_RUNTIME_STATE = "glass_runtime_state";
    private static final String KEY_GLASS_RUNTIME_MESSAGE = "glass_runtime_message";
    private static final String KEY_GLASS_WAIT_UNTIL = "glass_wait_until";
    private static final String KEY_GLASS_STATE_SENT_AT = "glass_state_sent_at";
    private static final String KEY_MEMORY_COMPACTED_AT = "memory_compacted_at";
    private static final String KEY_MEMORY_ARCHIVE_VERSION = "memory_archive_version";
    private static final String KEY_GMAIL_LABEL_PERMISSION_REQUESTED =
            "gmail_label_permission_requested";
    private static final String KEY_AMBIENT_RELAY_TRANSCRIPT = "ambient_relay_transcript";
    private static final String KEY_AMBIENT_RELAY_SOURCE = "ambient_relay_source";
    private static final String KEY_AMBIENT_RELAY_ID = "ambient_relay_id";
    private static final String KEY_AMBIENT_RELAY_AT = "ambient_relay_at";
    private static final String KEY_AMBIENT_SOURCE_ACTIVE = "ambient_source_active";
    private static final String KEY_AMBIENT_SOURCE_SEEN_AT = "ambient_source_seen_at";
    private static final String KEY_AMBIENT_CONSUMER_SEEN_AT = "ambient_consumer_seen_at";
    private static final String KEY_MAP_ROUTE_POINTS = "map_route_points";
    private static final String KEY_MAP_ROUTE_MANEUVERS = "map_route_maneuvers";
    private static final String KEY_MAP_ROUTE_DESTINATION = "map_route_destination";
    private static final String KEY_MAP_ROUTE_TIME = "map_route_time";
    private static final String KEY_MAP_ROUTE_ERROR = "map_route_error";
    private static final String KEY_MAP_ROUTE_MODE = "map_route_mode";
    private static final String KEY_MAP_ROUTE_DISTANCE = "map_route_distance";
    private static final String KEY_MAP_ROUTE_DURATION = "map_route_duration";
    private static final String KEY_MAP_ROUTE_DATA_VERSION = "map_route_data_version";
    private static final String KEY_MAP_CURRENT_ROAD = "map_current_road";
    private static final String KEY_MAP_CURRENT_ROAD_TIME = "map_current_road_time";
    private static final String KEY_MAP_CURRENT_ROAD_LATITUDE = "map_current_road_latitude";
    private static final String KEY_MAP_CURRENT_ROAD_LONGITUDE = "map_current_road_longitude";
    private static final String KEY_NAVIGATION_HUD_SUPPRESSED = "navigation_hud_suppressed";
    private static final long MAP_ROUTE_CACHE_MS = 12L * 60L * 60L * 1000L;
    private static final long MAP_ROAD_REFRESH_MS = 60L * 1000L;
    private static final long MAP_ROAD_CACHE_MS = 10L * 60L * 1000L;
    private static final float MAP_ROAD_CACHE_RADIUS_METERS = 180.0f;
    private static final int MAP_ROUTE_MAX_POINTS = 220;
    private static final long AMBIENT_CONSUMER_TIMEOUT_MS = 30000L;
    private static final List<String> LOGS = new ArrayList<String>();
    private static MainActivity activeActivity;
    private static String pendingCommand = "";
    private static String pendingCustomInstructions = "";
    private static boolean pendingCustomUpdate;
    private static boolean pendingCustomStateRequest;
    private static String pendingControl = "";
    private static String glassRuntimeState = "UNKNOWN";
    private static String glassRuntimeMessage = "";
    private static long glassWaitUntilMs;
    private static long glassStateSentAtMs;
    private TextView status;
    private TextView details;
    private TextView glassStateView;
    private TextView logView;
    private TextView customInfo;
    private EditText commandInput;
    private EditText navigationDestinationInput;
    private Button navigationModeButton;
    private Button navigationHudButton;
    private EditText customInput;
    private EditText bridgeTokenInput;
    private LinearLayout customPanel;
    private LinearLayout toolsPanel;
    private Button toggleCustomButton;
    private Button toolsButton;
    private Button morningCollectionButton;
    private AlertDialog customEditorDialog;
    private boolean customEditorDirty;
    private boolean customEditorApplyingRemote;
    private boolean customStateWaiting;
    private static volatile boolean running;
    private volatile long pairingUntilMs;
    private static ServerSocket serverSocket;
    private final Handler healthHandler = new Handler(Looper.getMainLooper());
    private final Handler weatherHandler = new Handler(Looper.getMainLooper());
    private final Handler glassStateHandler = new Handler(Looper.getMainLooper());
    private volatile boolean weatherRefreshInFlight;
    private volatile boolean navigationRouteFetchInFlight;
    private volatile boolean navigationRoadLookupInFlight;
    private volatile long navigationRouteRetryAfterMs;
    private volatile int navigationRouteFailureCount;
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
    private final Runnable glassStateRefresh = new Runnable() {
        @Override public void run() {
            refreshGlassStateView();
            glassStateHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        activeActivity = this;
        ensureBridgeToken();
        ensureConversationMemoryArchive();
        restoreGlassRuntimeState();
        restoreRecentConversationLogs();
        buildUi();
        startBridgeForegroundService();
        MorningBriefingManager.ensureFreshAsync(this, false);
        if (!getPreferences().getBoolean(KEY_CUSTOM_DIRTY, false)) {
            synchronized (MainActivity.class) {
                pendingCustomStateRequest = true;
            }
        }
        // The bridge must be available even while permission dialogs are still
        // pending. Individual endpoints perform their own permission checks.
        startServer();
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
        glassStateHandler.post(glassStateRefresh);
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
        refreshCustomInfo();
        updateMorningCollectionButton();
        if (hasLocationPermission()) {
            startBridgeForegroundService();
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
        healthHandler.removeCallbacks(healthRefresh);
        weatherHandler.removeCallbacksAndMessages(null);
        glassStateHandler.removeCallbacksAndMessages(null);
        // Keep the HTTP bridge owned by the foreground-service process when the
        // activity is backgrounded or removed from Recents. Closing it here
        // made glass voice input silently fall back to Gemini transcription.
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

        glassStateView = new TextView(this);
        glassStateView.setTextSize(14);
        glassStateView.setTextColor(Color.rgb(30, 90, 30));
        glassStateView.setPadding(0, 4, 0, 6);
        glassStateView.setText("グラス: 接続待ち");
        root.addView(glassStateView);

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
        commandInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_COMMAND_CHARS)});
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
                // Use the same persistent queue as background/Codex commands.
                // This makes a foreground send observable in logs and prevents
                // an activity recreation from losing the command before the
                // glasses complete their next poll.
                if (!queueBridgeCommand(MainActivity.this, text, true)) {
                    updateStatus("送信失敗", "入力を命令キューへ保存できませんでした");
                    return;
                }
                addAiLog("ユーザー: " + text);
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
                showCustomEditorDialog();
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
        customInfo.setTextIsSelectable(true);
        customPanel.addView(customInfo);

        customInput = new EditText(this);
        customInput.setSingleLine(false);
        customInput.setMinLines(2);
        customInput.setMaxLines(10);
        customInput.setHint("グラスのGeminiへ渡すカスタム指示");
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
                beginGlassPairing();
            }
        });
        customPanel.addView(pairGlass);

        Button sendCustom = new Button(this);
        sendCustom.setText("保存してグラスへ同期");
        sendCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCustomInstructionsFromPhone(customInput.getText().toString());
            }
        });
        customPanel.addView(sendCustom);
        root.addView(customPanel);

        Button clearLogs = new Button(this);
        clearLogs.setText("削除");
        clearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearConversationLogs();
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
                    persistPendingCommandsLocked(getPreferences(), new ArrayList<String>());
                }
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
                clearConversationLogs();
            }
        });
        miscRow.addView(clearTool, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolsPanel.addView(miscRow);

        LinearLayout proRow = new LinearLayout(this);
        proRow.setOrientation(LinearLayout.HORIZONTAL);

        proRow.addView(notificationAccess, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button audioSourcePair = new Button(this);
        audioSourcePair.setText("音声PAIR");
        audioSourcePair.setTextSize(12);
        audioSourcePair.setMinHeight(0);
        audioSourcePair.setPadding(4, 0, 4, 0);
        audioSourcePair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginAudioSourcePairing();
            }
        });
        proRow.addView(audioSourcePair, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        morningCollectionButton = new Button(this);
        morningCollectionButton.setTextSize(12);
        morningCollectionButton.setMinHeight(0);
        morningCollectionButton.setPadding(4, 0, 4, 0);
        morningCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = !MorningBriefingManager.isCollectionEnabled(
                        MainActivity.this);
                MorningBriefingManager.setCollectionEnabled(MainActivity.this, enabled);
                updateMorningCollectionButton();
                updateStatus(enabled ? "\u30c8\u30d4\u30c3\u30af\u66f4\u65b0 ON"
                                : "\u30c8\u30d4\u30c3\u30af\u66f4\u65b0 OFF",
                        enabled
                                ? "7\u6642\u30fb12\u6642\u30fb17\u6642\u30fb21\u6642\u306e4\u56de\u3001\u65b0\u7740\u60c5\u5831\u3092\u66f4\u65b0\u3057\u307e\u3059\u3002"
                                : "\u6b21\u56de\u4ee5\u964d\u306e\u81ea\u52d5\u53ce\u96c6\u3092\u505c\u6b62\u3057\u307e\u3057\u305f\u3002");
            }
        });
        updateMorningCollectionButton();
        proRow.addView(morningCollectionButton, new LinearLayout.LayoutParams(
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

        navigationDestinationInput = new EditText(this);
        navigationDestinationInput.setSingleLine(true);
        navigationDestinationInput.setHint("Googleマップの目的地");
        navigationDestinationInput.setTextSize(14);
        navigationDestinationInput.setMinHeight(0);
        navigationDestinationInput.setPadding(8, 2, 8, 2);
        navigationDestinationInput.setText(
                getPreferences().getString(KEY_MAP_ROUTE_DESTINATION, ""));
        toolsPanel.addView(navigationDestinationInput,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout navigationRow = new LinearLayout(this);
        navigationRow.setOrientation(LinearLayout.HORIZONTAL);

        navigationModeButton = new Button(this);
        navigationModeButton.setText(navigationModeLabel(navigationMode()));
        navigationModeButton.setTextSize(12);
        navigationModeButton.setMinHeight(0);
        navigationModeButton.setPadding(4, 0, 4, 0);
        navigationModeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                cycleNavigationMode();
            }
        });
        navigationRow.addView(navigationModeButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f));

        Button startNavigation = new Button(this);
        startNavigation.setText("MAPナビ");
        startNavigation.setTextSize(12);
        startNavigation.setMinHeight(0);
        startNavigation.setPadding(4, 0, 4, 0);
        startNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchGoogleMapsNavigation();
            }
        });
        navigationRow.addView(startNavigation, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        navigationHudButton = new Button(this);
        navigationHudButton.setTextSize(12);
        navigationHudButton.setMinHeight(0);
        navigationHudButton.setPadding(4, 0, 4, 0);
        navigationHudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean suppressed = !isNavigationHudSuppressed();
                setNavigationHudSuppressed(suppressed);
                updateStatus(suppressed ? "ナビHUD停止" : "ナビHUD再開",
                        suppressed
                                ? "Googleマップの案内は継続し、グラス表示だけを隠します"
                                : "グラスへナビ表示を再開します");
            }
        });
        updateNavigationHudButton();
        navigationRow.addView(navigationHudButton, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        toolsPanel.addView(navigationRow);

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

    private void launchGoogleMapsNavigation() {
        String destination = navigationDestinationInput == null ? ""
                : navigationDestinationInput.getText().toString().trim();
        destination = normalizeNavigationDestination(destination);
        if (destination.length() == 0) {
            updateStatus("目的地が未入力です", "目的地を入力してからMAPナビを押してください");
            if (navigationDestinationInput != null) navigationDestinationInput.requestFocus();
            return;
        }
        if (navigationDestinationInput != null) navigationDestinationInput.setText(destination);
        setNavigationHudSuppressed(false);
        final String mode = navigationMode();
        prepareNavigationRouteAsync(destination, mode);
        Intent intent;
        if (destination.length() == 0) {
            intent = getPackageManager().getLaunchIntentForPackage(
                    "com.google.android.apps.maps");
            if (intent == null) {
                intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps"));
            }
        } else {
            Uri directions = Uri.parse("https://www.google.com/maps/dir/?api=1"
                    + "&destination=" + Uri.encode(destination)
                    + "&travelmode=" + Uri.encode(mode)
                    + "&dir_action=navigate");
            intent = new Intent(Intent.ACTION_VIEW, directions);
            intent.setPackage("com.google.android.apps.maps");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            String permissionHint = hasNotificationListenerAccess()
                    ? "案内開始後、進路通知をグラスへ表示します"
                    : "通知アクセスが未許可です。操作内のMAILから許可してください";
            updateStatus(destination.length() == 0 ? "Googleマップ起動" : "MAPナビ起動",
                    permissionHint);
        } catch (Exception firstError) {
            try {
                Intent browser = new Intent(Intent.ACTION_VIEW,
                        destination.length() == 0
                                ? Uri.parse("https://www.google.com/maps")
                                : Uri.parse("https://www.google.com/maps/dir/?api=1"
                                        + "&destination=" + Uri.encode(destination)
                                        + "&travelmode=" + Uri.encode(mode)
                                        + "&dir_action=navigate"));
                startActivity(browser);
                updateStatus("Googleマップをブラウザで起動",
                        "Googleマップアプリが見つかりませんでした");
            } catch (Exception secondError) {
                updateStatus("MAP起動失敗", secondError.getMessage());
            }
        }
    }

    private String normalizeNavigationDestination(String value) {
        String destination = value == null ? "" : value.trim();
        destination = destination.replaceFirst("^(徒歩|歩き|車|自転車|電車)で", "");
        Matcher station = Pattern.compile("([^\\s、。]+?駅)").matcher(destination);
        if (station.find()) return station.group(1);
        destination = destination.replaceFirst("(まで|へ|に行く|にいく)$", "");
        return destination.trim();
    }

    private String navigationMode() {
        String mode = getPreferences().getString(KEY_MAP_ROUTE_MODE, "walking");
        if (!"walking".equals(mode) && !"driving".equals(mode)
                && !"bicycling".equals(mode)) return "walking";
        return mode;
    }

    private String navigationModeLabel(String mode) {
        if ("driving".equals(mode)) return "車";
        if ("bicycling".equals(mode)) return "自転車";
        return "徒歩";
    }

    private void cycleNavigationMode() {
        String current = navigationMode();
        String next = "walking".equals(current) ? "driving"
                : "driving".equals(current) ? "bicycling" : "walking";
        getPreferences().edit().putString(KEY_MAP_ROUTE_MODE, next).apply();
        if (navigationModeButton != null) {
            navigationModeButton.setText(navigationModeLabel(next));
        }
    }

    private void clearStoredNavigationRoute() {
        getPreferences().edit()
                .remove(KEY_MAP_ROUTE_POINTS)
                .remove(KEY_MAP_ROUTE_MANEUVERS)
                .remove(KEY_MAP_ROUTE_DESTINATION)
                .remove(KEY_MAP_ROUTE_TIME)
                .remove(KEY_MAP_ROUTE_ERROR)
                .remove(KEY_MAP_ROUTE_DISTANCE)
                .remove(KEY_MAP_ROUTE_DURATION)
                .remove(KEY_MAP_ROUTE_DATA_VERSION)
                .apply();
    }

    private boolean isNavigationHudSuppressed() {
        return getPreferences().getBoolean(KEY_NAVIGATION_HUD_SUPPRESSED, false);
    }

    private void setNavigationHudSuppressed(boolean suppressed) {
        getPreferences().edit().putBoolean(KEY_NAVIGATION_HUD_SUPPRESSED, suppressed).apply();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateNavigationHudButton();
        } else {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    updateNavigationHudButton();
                }
            });
        }
    }

    private void updateNavigationHudButton() {
        if (navigationHudButton != null) {
            navigationHudButton.setText(isNavigationHudSuppressed() ? "HUD再開" : "HUD停止");
        }
    }

    private void prepareNavigationRouteAsync(final String destination, final String mode) {
        prepareNavigationRouteAsync(destination, mode, true);
    }

    private void prepareNavigationRouteAsync(final String destination, final String mode,
                                             boolean clearExisting) {
        if (navigationRouteFetchInFlight) return;
        if (!clearExisting && System.currentTimeMillis() < navigationRouteRetryAfterMs) return;
        navigationRouteFetchInFlight = true;
        if (clearExisting) {
            String existingDestination = getPreferences().getString(
                    KEY_MAP_ROUTE_DESTINATION, "").trim();
            if (!destination.equals(existingDestination)) {
                clearStoredNavigationRoute();
                // Keep the requested destination even while route acquisition is
                // pending. Otherwise one transient OSRM/geocoder failure removes
                // the retry key and the HUD can remain without a route forever.
                getPreferences().edit()
                        .putString(KEY_MAP_ROUTE_DESTINATION, destination)
                        .apply();
            }
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Location origin = getBestAvailableLocation();
                    if (origin == null) {
                        throw new IllegalStateException("current_location_unavailable");
                    }
                    double[] target = geocodeNavigationDestination(destination);
                    if (target == null) {
                        throw new IllegalStateException("destination_not_found");
                    }
                    NavigationRouteData routeData = NavigationRouteData.EMPTY;
                    Exception lastRouteError = null;
                    for (int attempt = 0; attempt < 3; attempt++) {
                        try {
                            routeData = fetchNavigationRoute(origin.getLatitude(),
                                    origin.getLongitude(), target[0], target[1], mode);
                            if (routeData.route.length() >= 2) break;
                            lastRouteError = new IllegalStateException("route_not_found");
                        } catch (Exception routeError) {
                            lastRouteError = routeError;
                            Log.w(TAG, "navigation route attempt " + (attempt + 1)
                                    + " failed", routeError);
                        }
                        if (attempt < 2) Thread.sleep(1200L * (attempt + 1));
                    }
                    JSONArray route = routeData.route;
                    if (route.length() < 2) {
                        if (lastRouteError != null) throw lastRouteError;
                        throw new IllegalStateException("route_not_found");
                    }
                    getPreferences().edit()
                            .putString(KEY_MAP_ROUTE_POINTS, route.toString())
                            .putString(KEY_MAP_ROUTE_MANEUVERS,
                                    routeData.maneuvers.toString())
                            .putString(KEY_MAP_ROUTE_DESTINATION, destination)
                            .putLong(KEY_MAP_ROUTE_TIME, System.currentTimeMillis())
                            .putFloat(KEY_MAP_ROUTE_DISTANCE,
                                    (float) routeData.distanceMeters)
                            .putFloat(KEY_MAP_ROUTE_DURATION,
                                    (float) routeData.durationSeconds)
                            .putInt(KEY_MAP_ROUTE_DATA_VERSION, 3)
                            .remove(KEY_MAP_ROUTE_ERROR)
                            .apply();
                    navigationRouteFailureCount = 0;
                    navigationRouteRetryAfterMs = 0L;
                    Log.i(TAG, "navigation route cached points=" + route.length());
                } catch (Exception error) {
                    navigationRouteFailureCount = Math.min(5,
                            navigationRouteFailureCount + 1);
                    navigationRouteRetryAfterMs = System.currentTimeMillis()
                            + Math.min(120000L,
                            10000L * (1L << Math.max(0, navigationRouteFailureCount - 1)));
                    getPreferences().edit()
                            .putString(KEY_MAP_ROUTE_ERROR,
                                    error.getMessage() == null
                                            ? error.getClass().getSimpleName()
                                            : error.getMessage())
                            .apply();
                    Log.w(TAG, "navigation route preparation failed", error);
                } finally {
                    navigationRouteFetchInFlight = false;
                }
            }
        }, "NavigationRouteFetch").start();
    }

    private double[] geocodeNavigationDestination(String destination) throws Exception {
        try {
            if (Geocoder.isPresent()) {
                List<Address> results = new Geocoder(this, Locale.JAPAN)
                        .getFromLocationName(destination, 1);
                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    return new double[]{address.getLatitude(), address.getLongitude()};
                }
            }
        } catch (Exception error) {
            Log.w(TAG, "Android geocoder failed; using Nominatim", error);
        }
        String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1"
                + "&countrycodes=jp&q=" + URLEncoder.encode(destination, "UTF-8");
        JSONArray results = new JSONArray(fetchNavigationText(url));
        if (results.length() == 0) return null;
        JSONObject first = results.optJSONObject(0);
        if (first == null) return null;
        double latitude = Double.parseDouble(first.optString("lat", ""));
        double longitude = Double.parseDouble(first.optString("lon", ""));
        return new double[]{latitude, longitude};
    }

    private NavigationRouteData fetchNavigationRoute(double originLatitude,
                                                      double originLongitude,
                                                      double targetLatitude,
                                                      double targetLongitude,
                                                      String mode)
            throws Exception {
        String coordinates = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f",
                originLongitude, originLatitude, targetLongitude, targetLatitude);
        String backend = "walking".equals(mode) ? "routed-foot"
                : "bicycling".equals(mode) ? "routed-bike" : "routed-car";
        String url = "https://routing.openstreetmap.de/" + backend
                + "/route/v1/driving/" + coordinates
                + "?overview=full&geometries=geojson&steps=true";
        JSONObject response = new JSONObject(fetchNavigationText(url));
        JSONArray routes = response.optJSONArray("routes");
        if (routes == null || routes.length() == 0) return NavigationRouteData.EMPTY;
        JSONObject firstRoute = routes.optJSONObject(0);
        JSONObject geometry = firstRoute == null ? null : firstRoute.optJSONObject("geometry");
        JSONArray coordinatesJson = geometry == null ? null : geometry.optJSONArray("coordinates");
        if (coordinatesJson == null || coordinatesJson.length() < 2) {
            return NavigationRouteData.EMPTY;
        }
        JSONArray result = new JSONArray();
        int count = coordinatesJson.length();
        int stride = Math.max(1, (int) Math.ceil(count / (double) MAP_ROUTE_MAX_POINTS));
        for (int index = 0; index < count; index += stride) {
            JSONArray coordinate = coordinatesJson.optJSONArray(index);
            if (coordinate == null || coordinate.length() < 2) continue;
            JSONArray point = new JSONArray();
            point.put(coordinate.optDouble(1));
            point.put(coordinate.optDouble(0));
            result.put(point);
        }
        if ((count - 1) % stride != 0) {
            JSONArray last = coordinatesJson.optJSONArray(count - 1);
            if (last != null && last.length() >= 2) {
                JSONArray point = new JSONArray();
                point.put(last.optDouble(1));
                point.put(last.optDouble(0));
                result.put(point);
            }
        }
        JSONArray maneuvers = new JSONArray();
        JSONArray legs = firstRoute == null ? null : firstRoute.optJSONArray("legs");
        if (legs != null) {
            for (int legIndex = 0; legIndex < legs.length(); legIndex++) {
                JSONObject leg = legs.optJSONObject(legIndex);
                JSONArray steps = leg == null ? null : leg.optJSONArray("steps");
                if (steps == null) continue;
                for (int stepIndex = 0; stepIndex < steps.length(); stepIndex++) {
                    JSONObject step = steps.optJSONObject(stepIndex);
                    JSONObject maneuver = step == null ? null : step.optJSONObject("maneuver");
                    JSONArray point = maneuver == null ? null : maneuver.optJSONArray("location");
                    String type = maneuver == null ? "" : maneuver.optString("type", "");
                    if (point == null || point.length() < 2) continue;
                    double longitude = point.optDouble(0, Double.NaN);
                    double latitude = point.optDouble(1, Double.NaN);
                    if (Double.isNaN(latitude) || Double.isNaN(longitude)) continue;
                    int routeIndex = nearestCoordinateIndex(coordinatesJson, latitude, longitude);
                    JSONObject item = new JSONObject();
                    item.put("latitude", latitude);
                    item.put("longitude", longitude);
                    item.put("progress", coordinatesJson.length() <= 1 ? 1.0
                            : routeIndex / (double) (coordinatesJson.length() - 1));
                    item.put("type", type);
                    item.put("modifier", maneuver.optString("modifier", ""));
                    item.put("name", step.optString("name", ""));
                    item.put("distance", step.optDouble("distance", -1.0));
                    item.put("duration", step.optDouble("duration", -1.0));
                    maneuvers.put(item);
                }
            }
        }
        return new NavigationRouteData(result, maneuvers,
                firstRoute.optDouble("distance", -1.0),
                firstRoute.optDouble("duration", -1.0));
    }

    private int nearestCoordinateIndex(JSONArray coordinates, double latitude,
                                       double longitude) {
        int bestIndex = 0;
        double bestScore = Double.MAX_VALUE;
        for (int index = 0; index < coordinates.length(); index++) {
            JSONArray point = coordinates.optJSONArray(index);
            if (point == null || point.length() < 2) continue;
            double deltaLatitude = point.optDouble(1) - latitude;
            double deltaLongitude = (point.optDouble(0) - longitude)
                    * Math.cos(Math.toRadians(latitude));
            double score = deltaLatitude * deltaLatitude + deltaLongitude * deltaLongitude;
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private String fetchNavigationText(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent",
                "DennoHishoLoki/0.9 (+https://github.com/tenru-do/dennou-hisho-loki)");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0 && builder.length() < 1048576) {
            builder.append(buffer, 0, read);
        }
        reader.close();
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("route_http_" + code);
        }
        return builder.toString();
    }

    private boolean hasNotificationListenerAccess() {
        try {
            String enabled = Settings.Secure.getString(getContentResolver(),
                    "enabled_notification_listeners");
            return enabled != null && enabled.contains(getPackageName());
        } catch (Exception error) {
            return false;
        }
    }

    private void updateMorningCollectionButton() {
        if (morningCollectionButton == null) return;
        boolean enabled = MorningBriefingManager.isCollectionEnabled(this);
        morningCollectionButton.setText(enabled
                ? "TOPIC: ON" : "TOPIC: OFF");
    }

    private void showCustomEditorDialog() {
        if (customEditorDialog != null && customEditorDialog.isShowing()) {
            return;
        }
        if (customPanel != null) {
            customPanel.setVisibility(View.GONE);
        }
        customEditorDirty = false;
        customStateWaiting = true;

        LinearLayout editorLayout = new LinearLayout(this);
        editorLayout.setOrientation(LinearLayout.VERTICAL);
        editorLayout.setPadding(24, 8, 24, 8);

        customInfo = new TextView(this);
        customInfo.setTextSize(13);
        customInfo.setTextColor(Color.DKGRAY);
        customInfo.setText("グラスから現在の指示を読み込み中…");
        customInfo.setPadding(0, 0, 0, 6);
        editorLayout.addView(customInfo);

        customInput = new EditText(this);
        customInput.setSingleLine(false);
        customInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_CUSTOM_CHARS)});
        customInput.setMinLines(8);
        customInput.setMaxLines(14);
        customInput.setGravity(Gravity.TOP);
        customInput.setTextSize(16);
        customInput.setHint("グラスのGeminiへ渡すカスタム指示");
        customEditorApplyingRemote = true;
        String cached = getPreferences().getString(KEY_CUSTOM, "");
        customInput.setText(cached == null ? "" : cached);
        customInput.setSelection(customInput.getText().length());
        customEditorApplyingRemote = false;
        customInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!customEditorApplyingRemote) {
                    customEditorDirty = true;
                }
            }
            @Override public void afterTextChanged(Editable editable) {}
        });

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.addView(customInput);
        editorLayout.addView(editorScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        customEditorDialog = new AlertDialog.Builder(this)
                .setTitle("カスタム指示")
                .setView(editorLayout)
                .setPositiveButton("グラスへ反映", null)
                .setNegativeButton("キャンセル", null)
                .setNeutralButton("再ペアリング", null)
                .create();
        customEditorDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                customEditorDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                saveCustomInstructionsFromPhone(customInput.getText().toString());
                                customEditorDialog.dismiss();
                            }
                        });
                customEditorDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                beginGlassPairing();
                            }
                        });
            }
        });
        customEditorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                customEditorDialog = null;
                customStateWaiting = false;
                customEditorDirty = false;
                synchronized (MainActivity.class) {
                    pendingCustomStateRequest = false;
                }
                if (toggleCustomButton != null) {
                    toggleCustomButton.setText("指示");
                }
            }
        });
        customEditorDialog.show();
        Window dialogWindow = customEditorDialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            dialogWindow.setGravity(Gravity.TOP);
            dialogWindow.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        synchronized (MainActivity.class) {
            pendingCustomStateRequest = true;
        }
        toggleCustomButton.setText("編集中");
        updateStatus("指示を取得中", "グラスの現在値を待っています。");
    }

    private void beginGlassPairing() {
        String token = getPreferences().getString(KEY_BRIDGE_TOKEN, "").trim();
        if (token.length() < 16) {
            token = createBridgeToken();
            getPreferences().edit().putString(KEY_BRIDGE_TOKEN, token).apply();
            if (bridgeTokenInput != null) {
                bridgeTokenInput.setText(token);
            }
        }
        pairingUntilMs = System.currentTimeMillis() + 60000L;
        updateStatus("ペアリング待機中", "60秒以内にグラスのSETを押してください。");
    }

    private void beginAudioSourcePairing() {
        String token = getPreferences().getString(KEY_BRIDGE_TOKEN, "").trim();
        if (token.length() < 16) {
            token = createBridgeToken();
            getPreferences().edit().putString(KEY_BRIDGE_TOKEN, token).apply();
        }
        pairingUntilMs = System.currentTimeMillis() + 60000L;
        updateStatus("音声端末を待機中",
                "60秒以内にタブレットまたはGalaxyのロキ Audio Relayで「Galaxyを検索」を押してください。");
    }

    private void saveCustomInstructionsFromPhone(String value) {
        String text = value == null ? "" : value.trim();
        String token = getPreferences().getString(KEY_BRIDGE_TOKEN, "").trim();
        if (token.length() < 16) {
            token = createBridgeToken();
        }
        getPreferences().edit()
                .putString(KEY_CUSTOM, text)
                .putBoolean(KEY_CUSTOM_DIRTY, true)
                .putString(KEY_BRIDGE_TOKEN, token)
                .apply();
        synchronized (MainActivity.class) {
            pendingCustomInstructions = text;
            pendingCustomUpdate = true;
        }
        customEditorDirty = false;
        addAiLog("カスタム指示を更新 (" + text.length() + "文字): " + shortText(text, 1200));
        updateStatus("指示を保存", "グラスの現在値を上書きします。");
    }

    private void refreshCustomInfo() {
        if (customEditorDialog == null || !customEditorDialog.isShowing()
                || customInfo == null || customInput == null) {
            return;
        }
        customInfo.setText(customStateWaiting
                ? "グラスから現在の指示を読み込み中…"
                : "グラスから取得しました。編集後に「グラスへ反映」を押してください。");
        if (!customEditorDirty) {
            String saved = getPreferences().getString(KEY_CUSTOM, "");
            customEditorApplyingRemote = true;
            customInput.setText(saved == null ? "" : saved);
            customInput.setSelection(customInput.getText().length());
            customEditorApplyingRemote = false;
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
        if (checkSelfPermission(GmailUnreadReader.PERMISSION)
                != PackageManager.PERMISSION_GRANTED
                && !getPreferences().getBoolean(KEY_GMAIL_LABEL_PERMISSION_REQUESTED, false)) {
            getPreferences().edit().putBoolean(
                    KEY_GMAIL_LABEL_PERMISSION_REQUESTED, true).apply();
            requestPermissions(new String[]{GmailUnreadReader.PERMISSION}, 206);
            updateStatus("権限待ち", "Gmailの未読件数の読み取りを許可してください。本文は読みません。");
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
            startBridgeForegroundService();
            refreshWeatherAsync();
            return;
        }
        if (requestCode == 206) {
            startServer();
            MorningBriefingManager.ensureFreshAsync(this, true);
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
            boolean morning = request != null && request.startsWith("GET /morning");
            boolean command = request != null && request.startsWith("GET /command");
            boolean postCommand = request != null && request.startsWith("POST /command");
            boolean ackCommand = request != null && request.startsWith("GET /ack_command");
            boolean control = request != null && request.startsWith("GET /control");
            boolean memory = request != null && request.startsWith("GET /memory");
            boolean log = request != null && request.startsWith("GET /log");
            boolean postLog = request != null && request.startsWith("POST /log");
            boolean custom = request != null && request.startsWith("GET /custom");
            boolean postCustomState = request != null && request.startsWith("POST /custom_state");
            boolean postGlassState = request != null && request.startsWith("POST /state");
            boolean health = request != null && request.startsWith("GET /health");
            boolean postHealth = request != null && request.startsWith("POST /health");
            boolean weather = request != null && request.startsWith("GET /weather");
            boolean transit = request != null && request.startsWith("GET /transit");
            boolean navigationHudControl = request != null
                    && request.startsWith("GET /navigation_hud");
            boolean stt = request != null && request.startsWith("POST /stt");
            boolean ambientPlayback = request != null && request.startsWith("GET /ambient_playback");
            boolean ackAmbientPlayback = request != null && request.startsWith("GET /ack_ambient_playback");
            boolean ambientSourceState = request != null && request.startsWith("POST /ambient_source_state");
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
                    : morning ? MorningBriefingManager.readStoredForPlayback(this).toString()
                    : postCommand ? buildPostCommandResult(bodyText).toString()
                    : command ? buildCommandJson().toString()
                    : ackCommand ? buildAckCommandJson().toString()
                    : control ? buildControlJson().toString()
                    : postHealth ? buildPostHealthResult(bodyText).toString()
                    : health ? buildHealthJson().toString()
                    : weather ? buildWeatherJson(parseIntQuery(request, "offset", 0)).toString()
                    : navigationHudControl ? buildNavigationHudControlJson(
                            parseStringQuery(request, "action", "toggle")).toString()
                    : transit ? buildTransitJson().toString()
                    : ambientPlayback ? buildAmbientPlaybackJson(
                            "1".equals(parseStringQuery(request, "active", ""))).toString()
                    : ackAmbientPlayback ? buildAckAmbientPlaybackJson(
                            parseLongQuery(request, "id", 0L)).toString()
                    : ambientSourceState ? buildAmbientSourceStateJson(bodyText).toString()
                    : memory ? buildConversationMemoryJson(request).toString()
                    : postLog ? buildPostLogResult(bodyText).toString()
                    : log ? buildLogResult(request).toString()
                    : postCustomState ? buildPostCustomStateResult(bodyText).toString()
                    : postGlassState ? buildPostGlassStateResult(bodyText).toString()
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
        boolean ambientRelay = request.optBoolean("ambientRelay", false);
        String relaySource = shortText(request.optString("source", "Bluetooth"), 30);
        if (ambientRelay) {
            updateAmbientSourceState(true);
            if (!isAmbientConsumerActive()) {
                root.put("ok", true);
                root.put("transcript", "");
                root.put("inactive", true);
                return root;
            }
        }
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
            // Relay audio is already a complete PCM clip. Online recognition handles
            // streamed audio reliably on the Galaxy, while its on-device recognizer
            // currently waits until timeout for this file-descriptor input.
            byte[] recognitionPcm = ambientRelay
                    ? normalizeRelayPcmForRecognition(pcm) : pcm;
            transcript = transcribePcmWithSpeechRecognizer(recognitionPcm, sampleRate, false);
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
        if (ambientRelay) {
            if (transcript.length() > 0) {
                publishAmbientPlaybackTranscript(transcript, relaySource);
            }
        } else {
            addAiLog("音声入力: " + (transcript.length() == 0 ? "(聞き取りなし)" : transcript));
        }
        return root;
    }

    private JSONObject buildAmbientPlaybackJson(boolean activeHeartbeat) throws Exception {
        SharedPreferences preferences = getPreferences();
        long now = System.currentTimeMillis();
        if (activeHeartbeat) {
            preferences.edit().putLong(KEY_AMBIENT_CONSUMER_SEEN_AT, now).apply();
        }
        long seenAt = preferences.getLong(KEY_AMBIENT_SOURCE_SEEN_AT, 0L);
        boolean active = preferences.getBoolean(KEY_AMBIENT_SOURCE_ACTIVE, false)
                && now - seenAt < 35000L;
        long id = preferences.getLong(KEY_AMBIENT_RELAY_ID, 0L);
        long at = preferences.getLong(KEY_AMBIENT_RELAY_AT, 0L);
        String transcript = preferences.getString(KEY_AMBIENT_RELAY_TRANSCRIPT, "");
        if (now - at > 120000L) {
            transcript = "";
            id = 0L;
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("active", active);
        root.put("id", id);
        root.put("at", at);
        root.put("source", preferences.getString(KEY_AMBIENT_RELAY_SOURCE, "Bluetooth"));
        root.put("transcript", transcript == null ? "" : transcript);
        return root;
    }

    private JSONObject buildAckAmbientPlaybackJson(long id) throws Exception {
        SharedPreferences preferences = getPreferences();
        long currentId = preferences.getLong(KEY_AMBIENT_RELAY_ID, 0L);
        if (id > 0L && id == currentId) {
            preferences.edit()
                    .remove(KEY_AMBIENT_RELAY_TRANSCRIPT)
                    .remove(KEY_AMBIENT_RELAY_SOURCE)
                    .remove(KEY_AMBIENT_RELAY_ID)
                    .remove(KEY_AMBIENT_RELAY_AT)
                    .apply();
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("acked", id > 0L && id == currentId);
        return root;
    }

    private JSONObject buildAmbientSourceStateJson(String bodyText) throws Exception {
        JSONObject request = new JSONObject(bodyText == null || bodyText.trim().length() == 0
                ? "{}" : bodyText);
        boolean active = request.optBoolean("active", false);
        updateAmbientSourceState(active);
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("active", active);
        root.put("consumerActive", isAmbientConsumerActive());
        return root;
    }

    private boolean isAmbientConsumerActive() {
        long seenAt = getPreferences().getLong(KEY_AMBIENT_CONSUMER_SEEN_AT, 0L);
        return System.currentTimeMillis() - seenAt < AMBIENT_CONSUMER_TIMEOUT_MS;
    }

    private void updateAmbientSourceState(boolean active) {
        getPreferences().edit()
                .putBoolean(KEY_AMBIENT_SOURCE_ACTIVE, active)
                .putLong(KEY_AMBIENT_SOURCE_SEEN_AT, System.currentTimeMillis())
                .apply();
    }

    private void publishAmbientPlaybackTranscript(String value, String source) {
        String transcript = shortText(value, 500);
        if (transcript.length() == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        getPreferences().edit()
                .putString(KEY_AMBIENT_RELAY_TRANSCRIPT, transcript)
                .putString(KEY_AMBIENT_RELAY_SOURCE,
                        source == null || source.trim().length() == 0 ? "Bluetooth" : source.trim())
                .putLong(KEY_AMBIENT_RELAY_ID, now)
                .putLong(KEY_AMBIENT_RELAY_AT, now)
                .putBoolean(KEY_AMBIENT_SOURCE_ACTIVE, true)
                .putLong(KEY_AMBIENT_SOURCE_SEEN_AT, now)
                .apply();
        Log.i(TAG, "ambient relay transcript published chars=" + transcript.length());
    }

    private String transcribePcmWithSpeechRecognizer(final byte[] pcm, final int sampleRate,
            boolean preferOnDeviceFirst) throws Exception {
        if (preferOnDeviceFirst) {
            try {
                return transcribePcmWithSpeechRecognizerAttempt(pcm, sampleRate, true);
            } catch (Exception firstFailure) {
                if (!shouldRetrySpeechRecognition(firstFailure)) {
                    throw firstFailure;
                }
                Log.w(TAG, "phone ambient stt on-device attempt failed; retrying online: "
                        + firstFailure.getMessage());
                Thread.sleep(150L);
                return transcribePcmWithSpeechRecognizerAttempt(pcm, sampleRate, false);
            }
        }
        try {
            return transcribePcmWithSpeechRecognizerAttempt(pcm, sampleRate, false);
        } catch (Exception firstFailure) {
            if (!shouldRetrySpeechRecognition(firstFailure)) {
                throw firstFailure;
            }
            Log.w(TAG, "phone stt network failed; retrying with on-device recognition");
            Thread.sleep(250L);
            return transcribePcmWithSpeechRecognizerAttempt(pcm, sampleRate, true);
        }
    }

    private boolean shouldRetrySpeechRecognition(Exception failure) {
        if (failure == null) {
            return false;
        }
        if ("speech_timeout".equals(failure.getMessage())) {
            return true;
        }
        if (!(failure instanceof SpeechRecognitionFailure)) {
            return false;
        }
        int code = ((SpeechRecognitionFailure) failure).code;
        return code == SpeechRecognizer.ERROR_NETWORK
                || code == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                || code == SpeechRecognizer.ERROR_SERVER
                || code == SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
    }

    private String transcribePcmWithSpeechRecognizerAttempt(final byte[] pcm,
            final int sampleRate, final boolean preferOffline) throws Exception {
        Log.i(TAG, "phone stt start pcmBytes=" + (pcm == null ? 0 : pcm.length)
                + " sampleRate=" + sampleRate + " offline=" + preferOffline);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch readyLatch = new CountDownLatch(1);
        final String[] result = new String[] { "" };
        final String[] partial = new String[] { "" };
        final String[] error = new String[] { "" };
        final int[] errorCode = new int[] { 0 };
        final SpeechRecognizer[] recognizerHolder = new SpeechRecognizer[1];
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    SpeechRecognizer selectedRecognizer = null;
                    if (preferOffline && Build.VERSION.SDK_INT >= 31
                            && SpeechRecognizer.isOnDeviceRecognitionAvailable(MainActivity.this)) {
                        try {
                            selectedRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(
                                    MainActivity.this);
                        } catch (Exception onDeviceError) {
                            Log.w(TAG, "phone stt direct on-device recognizer unavailable",
                                    onDeviceError);
                        }
                    }
                    if (selectedRecognizer == null) {
                        selectedRecognizer = SpeechRecognizer.createSpeechRecognizer(
                                MainActivity.this);
                    }
                    final SpeechRecognizer recognizer = selectedRecognizer;
                    recognizerHolder[0] = recognizer;
                    final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                    final ParcelFileDescriptor readSide = pipe[0];
                    final ParcelFileDescriptor writeSide = pipe[1];
                    recognizer.setRecognitionListener(new RecognitionListener() {
                        @Override
                        public void onReadyForSpeech(Bundle params) {
                            updateStatus("音声認識中", "グラスから届いた音声を文字起こししています。");
                            readyLatch.countDown();
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
                            Log.w(TAG, "phone stt onError code=" + code + " offline="
                                    + preferOffline + " text=" + speechErrorText(code));
                            errorCode[0] = code;
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
                    intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline);
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
                                readyLatch.await(2000L, TimeUnit.MILLISECONDS);
                                writePcmAtRealtimeSpeed(out, leadingSilence, sampleRate);
                                writePcmAtRealtimeSpeed(out, pcm, sampleRate);
                                writePcmAtRealtimeSpeed(out, trailingSilence, sampleRate);
                                out.flush();
                                out.close();
                                Log.i(TAG, "phone stt audio pipe closed");
                            } catch (Exception e) {
                                String message = e.getMessage() == null ? "" : e.getMessage();
                                if (message.contains("EPIPE") || message.contains("Broken pipe")) {
                                    // SpeechRecognizer may close the read side as soon as it
                                    // has detected end-of-speech. Keep waiting for its result.
                                    Log.i(TAG, "phone stt audio pipe closed by recognizer");
                                } else {
                                    Log.w(TAG, "phone stt audio pipe error", e);
                                    error[0] = "audio_pipe_error: " + message;
                                    latch.countDown();
                                }
                            }
                        }
                    }, "PhoneSttAudioWriter").start();
                } catch (Exception e) {
                    error[0] = e.getMessage();
                    latch.countDown();
                }
            }
        });
        boolean completed = latch.await(preferOffline ? 16L : 24L, TimeUnit.SECONDS);
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
            throw new SpeechRecognitionFailure(errorCode[0], error[0]);
        }
        return result[0];
    }

    private void writePcmAtRealtimeSpeed(OutputStream out, byte[] audio,
            int sampleRate) throws Exception {
        if (out == null || audio == null || audio.length == 0) {
            return;
        }
        int bytesPerSecond = Math.max(8000, sampleRate * 2);
        int chunkBytes = Math.max(640, bytesPerSecond / 20);
        long startedAt = System.nanoTime();
        int written = 0;
        while (written < audio.length) {
            int count = Math.min(chunkBytes, audio.length - written);
            out.write(audio, written, count);
            written += count;
            long targetElapsedNs = (long) written * 1000000000L / bytesPerSecond;
            long remainingNs = targetElapsedNs - (System.nanoTime() - startedAt);
            if (remainingNs > 0L && written < audio.length) {
                long sleepMs = remainingNs / 1000000L;
                int sleepNs = (int) (remainingNs % 1000000L);
                Thread.sleep(sleepMs, sleepNs);
            }
        }
    }

    private byte[] normalizeRelayPcmForRecognition(byte[] pcm) {
        if (pcm == null || pcm.length < 2) {
            return pcm;
        }
        int sampleCount = pcm.length / 2;
        int peak = 0;
        long squareSum = 0L;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (short) ((pcm[i] & 255) | (pcm[i + 1] << 8));
            int absolute = Math.abs(sample);
            if (absolute > peak) {
                peak = absolute;
            }
            squareSum += (long) sample * (long) sample;
        }
        double rms = Math.sqrt((double) squareSum / Math.max(1, sampleCount));
        if (peak < 100 || rms < 35.0) {
            Log.i(TAG, "phone relay pcm too quiet for gain peak=" + peak
                    + " rms=" + Math.round(rms));
            return pcm;
        }
        double rmsGain = 3000.0 / rms;
        double headroomGain = 28000.0 / Math.max(1, peak);
        double gain = Math.min(4.0, Math.min(rmsGain, headroomGain));
        if (gain <= 1.10) {
            Log.i(TAG, "phone relay pcm level ok peak=" + peak
                    + " rms=" + Math.round(rms));
            return pcm;
        }
        byte[] normalized = new byte[pcm.length];
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (short) ((pcm[i] & 255) | (pcm[i + 1] << 8));
            int amplified = (int) Math.round(sample * gain);
            amplified = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, amplified));
            normalized[i] = (byte) (amplified & 255);
            normalized[i + 1] = (byte) ((amplified >> 8) & 255);
        }
        Log.i(TAG, "phone relay pcm normalized gain="
                + String.format(Locale.US, "%.2f", gain)
                + " peak=" + peak + " rms=" + Math.round(rms));
        return normalized;
    }

    private static final class SpeechRecognitionFailure extends IllegalStateException {
        final int code;

        SpeechRecognitionFailure(int code, String message) {
            super(message);
            this.code = code;
        }
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
        long endExclusive = startOfDayOffsetMillis(offsetDays + safeDays);
        long end = endExclusive - 1L;
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
                    long eventBegin = cursor.getLong(1);
                    long eventEnd = cursor.getLong(2);
                    boolean allDay = cursor.getInt(3) != 0;
                    if (!isScheduleEventInRange(eventBegin, eventEnd, allDay, start, endExclusive)) {
                        continue;
                    }
                    JSONObject event = new JSONObject();
                    event.put("title", shortText(cursor.getString(0), 60));
                    event.put("begin", eventBegin);
                    event.put("end", eventEnd);
                    event.put("allDay", allDay);
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

    private boolean isScheduleEventInRange(long eventBegin, long eventEnd, boolean allDay,
                                            long rangeStart, long rangeEndExclusive) {
        if (!allDay) {
            return eventBegin < rangeEndExclusive && eventEnd > rangeStart;
        }
        SimpleDateFormat utcDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        utcDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat localDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        localDate.setTimeZone(TimeZone.getDefault());
        String eventStartDate = utcDate.format(new Date(eventBegin));
        String eventEndDateExclusive = utcDate.format(new Date(Math.max(eventEnd, eventBegin + 86400000L)));
        String rangeStartDate = localDate.format(new Date(rangeStart));
        String rangeEndDateExclusive = localDate.format(new Date(rangeEndExclusive));
        return eventStartDate.compareTo(rangeEndDateExclusive) < 0
                && eventEndDateExclusive.compareTo(rangeStartDate) > 0;
    }

    private boolean matchesScheduleQuery(JSONObject event, String query) {
        if (query == null || query.trim().length() == 0) {
            return true;
        }
        String haystack = (event.optString("title", "") + " "
                + event.optString("location", "") + " "
                + event.optString("calendar", "")).toLowerCase(Locale.JAPAN);
        String normalized = query.toLowerCase(Locale.JAPAN).trim();
        boolean hospitalIntent = normalized.contains("病院");
        boolean motherIntent = containsScheduleWord(normalized,
                "おふくろ", "お袋", "母親", "母さん", "お母さん", "母", "ママ");
        boolean fatherIntent = containsScheduleWord(normalized,
                "親父", "おやじ", "父親", "父さん", "お父さん", "父", "パパ");
        boolean accompanyIntent = containsScheduleWord(normalized,
                "同行", "付き添", "付添", "つきそ", "一緒");
        boolean hospitalMatch = containsScheduleWord(haystack,
                "病院", "医療", "医療センター", "クリニック", "診察", "治療",
                "検査", "mri", "ct", "カンファレンス", "通院", "外来", "健診");
        boolean motherMatch = containsScheduleWord(haystack,
                "おふくろ", "お袋", "母親", "母さん", "お母さん", "母", "ママ");
        boolean fatherMatch = containsScheduleWord(haystack,
                "親父", "おやじ", "父親", "父さん", "お父さん", "父", "パパ");
        boolean accompanyMatch = containsScheduleWord(haystack,
                "同行", "付き添", "付添", "つきそ", "一緒");
        if (hospitalIntent) {
            if (!hospitalMatch) {
                return false;
            }
            if (motherIntent && !motherMatch && !accompanyMatch) {
                return false;
            }
            if (fatherIntent && !fatherMatch && !accompanyMatch) {
                return false;
            }
            if (accompanyIntent && !accompanyMatch && !motherMatch && !fatherMatch) {
                return false;
            }
            return true;
        }
        String[] words = normalized.split("[\\s　,、]+");
        for (String word : words) {
            if (word.length() > 0 && haystack.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsScheduleWord(String value, String... words) {
        if (value == null || words == null) {
            return false;
        }
        for (String word : words) {
            if (word != null && word.length() > 0
                    && value.contains(word.toLowerCase(Locale.JAPAN))) {
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
        if (contentLength > MAX_REQUEST_BODY_CHARS) {
            throw new IllegalArgumentException("request body too large");
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
            ArrayList<String> queue = readPendingCommandsLocked(getPreferences());
            command = queue.isEmpty() ? "" : queue.get(0);
            // Also migrates the former one-slot preference into the FIFO format.
            persistPendingCommandsLocked(getPreferences(), queue);
        }
        root.put("ok", true);
        root.put("command", command == null ? "" : command);
        return root;
    }

    private static ArrayList<String> readPendingCommandsLocked(SharedPreferences preferences) {
        ArrayList<String> queue = new ArrayList<String>();
        String encoded = preferences.getString(KEY_PENDING_COMMAND_QUEUE, "");
        if (encoded != null && encoded.trim().length() > 0) {
            try {
                JSONArray array = new JSONArray(encoded);
                for (int index = 0;
                        index < array.length() && queue.size() < MAX_PENDING_COMMANDS;
                        index++) {
                    String item = shortText(array.optString(index, "").trim(), MAX_COMMAND_CHARS);
                    if (item.length() > 0) queue.add(item);
                }
            } catch (Exception error) {
                Log.w(TAG, "pending command queue could not be decoded", error);
            }
        }
        if (queue.isEmpty()) {
            String legacy = pendingCommand == null ? "" : pendingCommand.trim();
            if (legacy.length() == 0) {
                legacy = preferences.getString(KEY_PENDING_COMMAND, "").trim();
            }
            legacy = shortText(legacy, MAX_COMMAND_CHARS);
            if (legacy.length() > 0) queue.add(legacy);
        }
        return queue;
    }

    private static void persistPendingCommandsLocked(SharedPreferences preferences,
                                                       ArrayList<String> queue) {
        JSONArray encoded = new JSONArray();
        if (queue != null) {
            for (String item : queue) {
                if (encoded.length() >= MAX_PENDING_COMMANDS) break;
                String command = shortText(item == null ? "" : item.trim(), MAX_COMMAND_CHARS);
                if (command.length() > 0) encoded.put(command);
            }
        }
        pendingCommand = encoded.length() == 0 ? "" : encoded.optString(0, "");
        SharedPreferences.Editor editor = preferences.edit();
        if (encoded.length() == 0) {
            editor.remove(KEY_PENDING_COMMAND_QUEUE).remove(KEY_PENDING_COMMAND);
        } else {
            editor.putString(KEY_PENDING_COMMAND_QUEUE, encoded.toString())
                    .putString(KEY_PENDING_COMMAND, pendingCommand);
        }
        editor.apply();
    }

    static boolean queueBridgeCommand(Context context, String rawCommand, boolean replacePending) {
        if (context == null) return false;
        String command = shortText(rawCommand == null ? "" : rawCommand.trim(), MAX_COMMAND_CHARS);
        if (command.length() == 0) return false;
        SharedPreferences preferences = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        synchronized (MainActivity.class) {
            ArrayList<String> queue = readPendingCommandsLocked(preferences);
            if (!replacePending && !queue.isEmpty()) return false;
            // User input and Codex notifications are independent events. Appending
            // prevents a second sender from overwriting a command before the glasses
            // complete their next three-second poll.
            if (queue.size() >= MAX_PENDING_COMMANDS) queue.remove(0);
            queue.add(command);
            persistPendingCommandsLocked(preferences, queue);
        }
        Log.i(TAG, "bridge command appended chars=" + command.length());
        return true;
    }

    private JSONObject buildPostCommandResult(String bodyText) throws Exception {
        String command = "";
        if (bodyText != null && bodyText.trim().length() > 0) {
            if (bodyText.trim().startsWith("{")) {
                command = new JSONObject(bodyText).optString("command", "").trim();
            } else {
                command = formValue(bodyText, "command").trim();
            }
        }
        command = shortText(command, MAX_COMMAND_CHARS);
        boolean duplicateCodexNotification = false;
        if (command.startsWith("__CODEX_NOTIFY__:")) {
            SharedPreferences preferences = getPreferences();
            long now = System.currentTimeMillis();
            duplicateCodexNotification = command.equals(
                    preferences.getString(KEY_LAST_CODEX_NOTIFICATION, ""))
                    && now - preferences.getLong(KEY_LAST_CODEX_NOTIFICATION_AT, 0L) < 60000L;
            if (!duplicateCodexNotification) {
                preferences.edit()
                        .putString(KEY_LAST_CODEX_NOTIFICATION, command)
                        .putLong(KEY_LAST_CODEX_NOTIFICATION_AT, now)
                        .apply();
            } else {
                Log.i(TAG, "duplicate Codex notification suppressed");
            }
        }
        if (command.length() > 0 && !duplicateCodexNotification) {
            queueBridgeCommand(this, command, true);
            Log.i(TAG, "remote command appended chars=" + command.length());
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("queued", command.length() > 0 && !duplicateCodexNotification);
        root.put("duplicate", duplicateCodexNotification);
        return root;
    }

    private JSONObject buildAckCommandJson() throws Exception {
        JSONObject root = new JSONObject();
        synchronized (MainActivity.class) {
            ArrayList<String> queue = readPendingCommandsLocked(getPreferences());
            if (!queue.isEmpty()) queue.remove(0);
            persistPendingCommandsLocked(getPreferences(), queue);
            root.put("remaining", queue.size());
        }
        if (activeActivity != null) {
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

    private JSONObject buildPostGlassStateResult(String bodyText) throws Exception {
        JSONObject body = new JSONObject(
                bodyText == null || bodyText.trim().length() == 0 ? "{}" : bodyText);
        String state = body.optString("state", "UNKNOWN").trim().toUpperCase(Locale.US);
        if (!"READY".equals(state) && !"WAIT".equals(state)
                && !"THINKING".equals(state) && !"ERROR".equals(state)) {
            state = "UNKNOWN";
        }
        String message = shortText(body.optString("message", "").trim(), 80);
        long waitMs = Math.max(0L, Math.min(600000L, body.optLong("waitMs", 0L)));
        long sentAt = body.optLong("sentAt", System.currentTimeMillis());
        long waitUntil = System.currentTimeMillis() + waitMs;
        boolean accepted = false;
        synchronized (MainActivity.class) {
            if (sentAt >= glassStateSentAtMs) {
                glassStateSentAtMs = sentAt;
                glassRuntimeState = state;
                glassRuntimeMessage = message;
                glassWaitUntilMs = waitUntil;
                accepted = true;
            }
        }
        if (accepted) {
            getPreferences().edit()
                    .putString(KEY_GLASS_RUNTIME_STATE, state)
                    .putString(KEY_GLASS_RUNTIME_MESSAGE, message)
                    .putLong(KEY_GLASS_WAIT_UNTIL, waitUntil)
                    .putLong(KEY_GLASS_STATE_SENT_AT, sentAt)
                    .apply();
            Log.i(TAG, "glass state received=" + state + " waitMs=" + waitMs);
        }
        if (activeActivity != null) {
            activeActivity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    activeActivity.refreshGlassStateView();
                }
            });
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        return root;
    }

    private void restoreGlassRuntimeState() {
        SharedPreferences preferences = getPreferences();
        synchronized (MainActivity.class) {
            glassRuntimeState = preferences.getString(
                    KEY_GLASS_RUNTIME_STATE, glassRuntimeState);
            glassRuntimeMessage = preferences.getString(
                    KEY_GLASS_RUNTIME_MESSAGE, glassRuntimeMessage);
            glassWaitUntilMs = preferences.getLong(
                    KEY_GLASS_WAIT_UNTIL, glassWaitUntilMs);
            glassStateSentAtMs = preferences.getLong(
                    KEY_GLASS_STATE_SENT_AT, glassStateSentAtMs);
        }
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
                if (isConversationMemoryKind(kind) && !isFailedAnswer(message)) {
                    appendConversationMemorySafely(kind, message);
                }
            }
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        return root;
    }

    private boolean isConversationMemoryKind(String kind) {
        return "ユーザー".equals(kind)
                || "ユーザー音声".equals(kind)
                || "Gemini".equals(kind)
                || "直接回答".equals(kind);
    }

    private boolean isFailedAnswer(String message) {
        String value = safe(message).trim();
        return value.startsWith("Geminiエラー")
                || value.startsWith("通信エラー")
                || value.startsWith("スマホの実データを取得できませんでした")
                || value.startsWith("ニュースを取得できませんでした")
                || value.startsWith("天気を取得できませんでした");
    }

    private File conversationMemoryFile() {
        return new File(getFilesDir(), MEMORY_FILE_NAME);
    }

    private File conversationMemoryArchiveFile() {
        return new File(getFilesDir(), MEMORY_ARCHIVE_FILE_NAME);
    }

    private void appendConversationMemorySafely(String kind, String message) {
        try {
            appendConversationMemory(kind, message);
        } catch (Exception error) {
            Log.w(TAG, "conversation memory append failed", error);
        }
    }

    private void ensureConversationMemoryArchive() {
        if (getPreferences().getInt(KEY_MEMORY_ARCHIVE_VERSION, 0) >= 1) {
            return;
        }
        synchronized (MEMORY_LOCK) {
            compactConversationMemoryLocked();
        }
        getPreferences().edit().putInt(KEY_MEMORY_ARCHIVE_VERSION, 1)
                .putLong(KEY_MEMORY_COMPACTED_AT, System.currentTimeMillis()).apply();
    }

    private void appendConversationMemory(String kind, String message) throws Exception {
        JSONObject entry = new JSONObject();
        long now = System.currentTimeMillis();
        entry.put("time", now);
        entry.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(new Date(now)));
        entry.put("kind", shortText(kind, 20));
        entry.put("message", shortText(message, MAX_MEMORY_ENTRY_CHARS));
        synchronized (MEMORY_LOCK) {
            File file = conversationMemoryFile();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true), StandardCharsets.UTF_8));
            try {
                writer.write(entry.toString());
                writer.newLine();
            } finally {
                writer.close();
            }
            long lastCompactedAt = getPreferences().getLong(KEY_MEMORY_COMPACTED_AT, 0L);
            if (file.length() > MAX_MEMORY_FILE_BYTES
                    || now - lastCompactedAt >= 24L * 60L * 60L * 1000L) {
                compactConversationMemoryLocked();
                getPreferences().edit().putLong(KEY_MEMORY_COMPACTED_AT, now).apply();
            }
        }
    }

    static void rememberMorningBriefing(android.content.Context context, String summary) {
        String value = summary == null ? "" : summary.trim();
        if (value.length() == 0) return;
        MainActivity active = activeActivity;
        if (active != null) {
            active.appendConversationMemorySafely("トピック要約", value);
            return;
        }
        synchronized (MEMORY_LOCK) {
            try {
                long now = System.currentTimeMillis();
                JSONObject entry = new JSONObject();
                entry.put("time", now);
                entry.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
                        .format(new Date(now)));
                entry.put("kind", "トピック要約");
                entry.put("message", shortStaticText(value, MAX_MEMORY_ENTRY_CHARS));
                File file = new File(context.getFilesDir(), MEMORY_FILE_NAME);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file, true), StandardCharsets.UTF_8));
                try {
                    writer.write(entry.toString());
                    writer.newLine();
                } finally {
                    writer.close();
                }
            } catch (Exception error) {
                Log.w(TAG, "morning memory append failed", error);
            }
        }
    }

    private static String shortStaticText(String value, int max) {
        String text = value == null ? "" : value.replace('\n', ' ')
                .replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private ArrayList<JSONObject> readConversationMemory() {
        synchronized (MEMORY_LOCK) {
            return readConversationMemoryLocked();
        }
    }

    private ArrayList<JSONObject> readConversationMemoryLocked() {
        ArrayList<JSONObject> entries = new ArrayList<JSONObject>();
        ArrayList<JSONObject> all = readAllConversationMemoryLocked();
        long cutoff = System.currentTimeMillis() - MEMORY_RETENTION_MS;
        for (JSONObject entry : all) {
            if (entry.optLong("time", 0L) >= cutoff) {
                entries.add(entry);
                if (entries.size() > MAX_MEMORY_ENTRIES) {
                    entries.remove(0);
                }
            }
        }
        return entries;
    }

    private ArrayList<JSONObject> readAllConversationMemoryLocked() {
        ArrayList<JSONObject> entries = new ArrayList<JSONObject>();
        File file = conversationMemoryFile();
        if (!file.isFile()) {
            return entries;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        JSONObject entry = new JSONObject(line);
                        if (entry.optLong("time", 0L) > 0L
                                && isConversationMemoryKind(entry.optString("kind", ""))) {
                            entries.add(entry);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Exception error) {
            Log.w(TAG, "conversation memory read failed", error);
        }
        return entries;
    }

    private void compactConversationMemoryLocked() {
        ArrayList<JSONObject> all = readAllConversationMemoryLocked();
        ArrayList<JSONObject> recent = new ArrayList<JSONObject>();
        ArrayList<JSONObject> archiveCandidates = new ArrayList<JSONObject>();
        long cutoff = System.currentTimeMillis() - MEMORY_RETENTION_MS;
        for (JSONObject entry : all) {
            if (entry.optLong("time", 0L) < cutoff) {
                archiveCandidates.add(entry);
            } else {
                recent.add(entry);
            }
        }
        while (recent.size() > MAX_MEMORY_ENTRIES) {
            archiveCandidates.add(recent.remove(0));
        }
        if (!archiveConversationMemoryLocked(archiveCandidates)) {
            // Never discard raw history if the archive could not be written.
            recent = all;
        }
        File file = conversationMemoryFile();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, false), StandardCharsets.UTF_8));
            try {
                for (JSONObject entry : recent) {
                    writer.write(entry.toString());
                    writer.newLine();
                }
            } finally {
                writer.close();
            }
        } catch (Exception error) {
            Log.w(TAG, "conversation memory compact failed", error);
        }
    }

    private boolean archiveConversationMemoryLocked(ArrayList<JSONObject> rawEntries) {
        if (rawEntries == null || rawEntries.isEmpty()) {
            return true;
        }
        try {
            ArrayList<JSONObject> archive = readConversationArchiveLocked();
            java.util.HashSet<String> ids = new java.util.HashSet<String>();
            for (JSONObject entry : archive) {
                ids.add(entry.optString("id", ""));
            }
            ArrayList<JSONObject> compressed = buildCompressedArchiveEntries(rawEntries);
            for (JSONObject entry : compressed) {
                if (ids.add(entry.optString("id", ""))) {
                    archive.add(entry);
                }
            }
            Collections.sort(archive, new java.util.Comparator<JSONObject>() {
                @Override public int compare(JSONObject left, JSONObject right) {
                    return Long.compare(left.optLong("time", 0L), right.optLong("time", 0L));
                }
            });
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(conversationMemoryArchiveFile(), false), StandardCharsets.UTF_8));
            try {
                for (JSONObject entry : archive) {
                    writer.write(entry.toString());
                    writer.newLine();
                }
            } finally {
                writer.close();
            }
            return true;
        } catch (Exception error) {
            Log.w(TAG, "conversation memory archive failed", error);
            return false;
        }
    }

    private ArrayList<JSONObject> readConversationArchive() {
        synchronized (MEMORY_LOCK) {
            return readConversationArchiveLocked();
        }
    }

    private ArrayList<JSONObject> readConversationArchiveLocked() {
        ArrayList<JSONObject> entries = new ArrayList<JSONObject>();
        File file = conversationMemoryArchiveFile();
        if (!file.isFile()) {
            return entries;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        JSONObject entry = new JSONObject(line);
                        if (entry.optLong("time", 0L) > 0L
                                && entry.optString("message", "").length() > 0) {
                            entries.add(entry);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Exception error) {
            Log.w(TAG, "conversation memory archive read failed", error);
        }
        return entries;
    }

    private ArrayList<JSONObject> buildCompressedArchiveEntries(ArrayList<JSONObject> rawEntries)
            throws Exception {
        ArrayList<JSONObject> result = new ArrayList<JSONObject>();
        JSONObject pendingUser = null;
        for (JSONObject entry : rawEntries) {
            String kind = entry.optString("kind", "");
            if (kind.startsWith("ユーザー")) {
                if (pendingUser != null) {
                    result.add(buildCompressedArchiveEntry(pendingUser, null));
                }
                pendingUser = entry;
            } else if (pendingUser != null) {
                result.add(buildCompressedArchiveEntry(pendingUser, entry));
                pendingUser = null;
            } else {
                result.add(buildCompressedArchiveEntry(null, entry));
            }
        }
        if (pendingUser != null) {
            result.add(buildCompressedArchiveEntry(pendingUser, null));
        }
        return result;
    }

    private JSONObject buildCompressedArchiveEntry(JSONObject user, JSONObject answer)
            throws Exception {
        long time = user != null ? user.optLong("time", 0L) : answer.optLong("time", 0L);
        String question = user == null ? ""
                : compressMemoryExcerpt(user.optString("message", ""), MAX_ARCHIVE_QUESTION_CHARS);
        String response = answer == null ? ""
                : compressMemoryExcerpt(answer.optString("message", ""), MAX_ARCHIVE_ANSWER_CHARS);
        StringBuilder summary = new StringBuilder();
        if (question.length() > 0) {
            summary.append("質問: ").append(question);
        }
        if (response.length() > 0) {
            if (summary.length() > 0) summary.append('\n');
            summary.append("回答: ").append(response);
        }
        String month = new SimpleDateFormat("yyyy-MM", Locale.JAPAN).format(new Date(time));
        String idSource = time + "|" + summary.toString();
        JSONObject compact = new JSONObject();
        compact.put("id", month + "-" + time + "-" + Integer.toHexString(idSource.hashCode()));
        compact.put("time", time);
        compact.put("month", month);
        compact.put("kind", "長期記憶");
        compact.put("message", summary.toString());
        compact.put("sourceCount", user != null && answer != null ? 2 : 1);
        return compact;
    }

    private String compressMemoryExcerpt(String message, int maxChars) {
        String value = safe(message).replace('\r', ' ').replace('\n', ' ')
                .replaceAll("\\s+", " ").trim();
        if (value.length() <= maxChars) {
            return value;
        }
        String[] sentences = value.split("(?<=[。！？!?])\\s*");
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        StringBuilder summary = new StringBuilder();
        for (String sentence : sentences) {
            String part = sentence.trim();
            String normalized = normalizeMemoryText(part);
            if (part.length() == 0 || normalized.length() == 0 || !seen.add(normalized)) {
                continue;
            }
            if (summary.length() > 0 && summary.length() + part.length() + 1 > maxChars) {
                continue;
            }
            if (summary.length() > 0) summary.append(' ');
            summary.append(part);
            if (summary.length() >= maxChars * 3 / 4) {
                break;
            }
        }
        if (summary.length() == 0) {
            return shortText(value, maxChars);
        }
        return shortText(summary.toString(), maxChars);
    }

    private JSONObject buildConversationMemoryJson(String request) throws Exception {
        int offset = Math.max(-36500, Math.min(1, parseIntQuery(request, "offset", 0)));
        int days = Math.max(1, Math.min(36501, parseIntQuery(request, "days", 1)));
        String query = shortText(parseStringQuery(request, "q", ""), 100).trim();
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.DAY_OF_MONTH, offset);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, days);

        ArrayList<JSONObject> all = readConversationMemory();
        all.addAll(readConversationArchive());
        Collections.sort(all, new java.util.Comparator<JSONObject>() {
            @Override public int compare(JSONObject left, JSONObject right) {
                return Long.compare(left.optLong("time", 0L), right.optLong("time", 0L));
            }
        });
        ArrayList<JSONObject> inRange = new ArrayList<JSONObject>();
        for (JSONObject entry : all) {
            long time = entry.optLong("time", 0L);
            if (time >= start.getTimeInMillis() && time < end.getTimeInMillis()) {
                inRange.add(entry);
            }
        }

        boolean[] selected = new boolean[inRange.size()];
        if (query.length() == 0) {
            boolean includesArchivePeriod = start.getTimeInMillis()
                    < System.currentTimeMillis() - MEMORY_RETENTION_MS;
            if (includesArchivePeriod) {
                int recentCount = 0;
                int archiveCount = 0;
                for (int i = inRange.size() - 1; i >= 0; i--) {
                    boolean archived = "長期記憶".equals(inRange.get(i).optString("kind", ""));
                    if (archived && archiveCount < 6) {
                        selected[i] = true;
                        archiveCount++;
                    } else if (!archived && recentCount < 6) {
                        selected[i] = true;
                        recentCount++;
                    }
                }
            } else {
                int from = Math.max(0, inRange.size() - 12);
                for (int i = from; i < inRange.size(); i++) {
                    selected[i] = true;
                }
            }
        } else {
            for (int i = 0; i < inRange.size(); i++) {
                JSONObject entry = inRange.get(i);
                if (memoryMatches(entry.optString("message", ""), query)) {
                    selected[i] = true;
                    if (!"長期記憶".equals(entry.optString("kind", ""))) {
                        if (i > 0 && isNearbyMemoryTurn(entry, inRange.get(i - 1))) {
                            selected[i - 1] = true;
                        }
                        if (i + 1 < inRange.size()
                                && isNearbyMemoryTurn(entry, inRange.get(i + 1))) {
                            selected[i + 1] = true;
                        }
                    }
                }
            }
        }

        ArrayList<JSONObject> chosenNewestFirst = new ArrayList<JSONObject>();
        int usedChars = 0;
        for (int i = inRange.size() - 1; i >= 0 && chosenNewestFirst.size() < 12; i--) {
            if (!selected[i]) continue;
            JSONObject source = inRange.get(i);
            String message = shortText(source.optString("message", ""), 900);
            if (message.length() == 0) continue;
            if (!chosenNewestFirst.isEmpty()
                    && usedChars + message.length() > MAX_MEMORY_RESPONSE_CHARS) {
                continue;
            }
            JSONObject compact = new JSONObject();
            compact.put("time", source.optLong("time", 0L));
            compact.put("kind", source.optString("kind", ""));
            compact.put("message", message);
            chosenNewestFirst.add(compact);
            usedChars += message.length();
        }

        JSONArray entries = new JSONArray();
        for (int i = chosenNewestFirst.size() - 1; i >= 0; i--) {
            entries.put(chosenNewestFirst.get(i));
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("from", new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(start.getTime()));
        root.put("to", new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(
                new Date(end.getTimeInMillis() - 1L)));
        root.put("query", query);
        root.put("count", entries.length());
        root.put("entries", entries);
        return root;
    }

    private boolean memoryMatches(String message, String query) {
        String haystack = normalizeMemoryText(message);
        String needle = normalizeMemoryText(query);
        if (needle.length() == 0 || haystack.contains(needle)) {
            return true;
        }
        String[] tokens = query.toLowerCase(Locale.JAPAN).split("[\\s、。・,./]+");
        for (String token : tokens) {
            String normalized = normalizeMemoryText(token);
            if (normalized.length() >= 2 && haystack.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearbyMemoryTurn(JSONObject left, JSONObject right) {
        long leftTime = left == null ? 0L : left.optLong("time", 0L);
        long rightTime = right == null ? 0L : right.optLong("time", 0L);
        return leftTime > 0L && rightTime > 0L
                && Math.abs(leftTime - rightTime) <= 3L * 60L * 1000L;
    }

    private String normalizeMemoryText(String value) {
        return safe(value).toLowerCase(Locale.JAPAN)
                .replaceAll("[\\s\\p{Punct}、。！？・「」『』（）()]+", "");
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
        boolean hasUpdate;
        boolean requestState;
        synchronized (MainActivity.class) {
            custom = pendingCustomInstructions;
            hasUpdate = pendingCustomUpdate;
            requestState = pendingCustomStateRequest;
            pendingCustomInstructions = "";
            pendingCustomUpdate = false;
        }
        if (!hasUpdate && getPreferences().getBoolean(KEY_CUSTOM_DIRTY, false)) {
            custom = getPreferences().getString(KEY_CUSTOM, "");
            hasUpdate = true;
        }
        if (hasUpdate) {
            getPreferences().edit().remove(KEY_CUSTOM_DIRTY).apply();
        }
        root.put("ok", true);
        root.put("custom", custom == null ? "" : custom);
        root.put("hasUpdate", hasUpdate);
        root.put("requestState", requestState);
        root.put("current", getPreferences().getString(KEY_CUSTOM, ""));
        return root;
    }

    private JSONObject buildPostCustomStateResult(String bodyText) throws Exception {
        String custom = "";
        if (bodyText != null && bodyText.trim().length() > 0) {
            if (bodyText.trim().startsWith("{")) {
                custom = new JSONObject(bodyText).optString("custom", "");
            } else {
                custom = formValue(bodyText, "custom");
            }
        }
        final String syncedCustom = custom == null ? "" : custom;
        boolean localEditPending = getPreferences().getBoolean(KEY_CUSTOM_DIRTY, false);
        String previousCustom = getPreferences().getString(KEY_CUSTOM, "");
        boolean customChanged = !previousCustom.equals(syncedCustom);
        if (!localEditPending) {
            getPreferences().edit().putString(KEY_CUSTOM, syncedCustom).apply();
            synchronized (MainActivity.class) {
                pendingCustomStateRequest = false;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    customStateWaiting = false;
                    refreshCustomInfo();
                }
            });
            Log.i(TAG, "custom state synchronized chars=" + syncedCustom.length()
                    + " changed=" + customChanged);
            if (customChanged) {
                MorningBriefingManager.ensureFreshAsync(this, true);
            }
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("saved", !localEditPending);
        root.put("localEditPending", localEditPending);
        return root;
    }

    private void ensureLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 205);
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private JSONObject buildTransitJson() throws Exception {
        JSONObject maps = MailNotificationService.recentTransitJson();
        JSONObject estimatedTransit = TransitLocationTracker.recentTransitJson(this);
        JSONObject result;
        if (maps.optBoolean("ok", false)) {
            maps.put("estimated", false);
            result = maps;
        } else {
            result = estimatedTransit;
            if (!result.optBoolean("ok", false) && !hasLocationPermission()) {
                result.put("error", "location_permission_missing");
            }
        }
        boolean navigationActive = result.optBoolean("navigationActive", false);
        // Ordinary rail estimation can stay in a low-power GPS mode. An active
        // Maps navigation session needs fresh samples so the remaining distance
        // and duration do not stay frozen at the route's initial values.
        TransitLocationTracker.setNavigationActive(navigationActive);
        result.put("topCompact", estimatedTransit.optString("compact", ""));
        result.put("topTime", estimatedTransit.optLong("time", 0L));
        Location location = getBestAvailableLocation();
        if (location != null) {
            result.put("latitude", location.getLatitude());
            result.put("longitude", location.getLongitude());
            result.put("locationTime", location.getTime());
            result.put("accuracy", location.hasAccuracy() ? location.getAccuracy() : -1.0f);
            result.put("bearing", location.hasBearing() ? location.getBearing() : -1.0f);
            result.put("speed", location.hasSpeed() ? location.getSpeed() : -1.0f);
        }
        if (navigationActive && location != null) {
            String currentRoad = cachedCurrentRoad(location);
            if (currentRoad.length() > 0) {
                result.put("currentRoad", currentRoad);
            }
            refreshCurrentRoadAsync(location);
        }
        SharedPreferences preferences = getPreferences();
        long routeTime = preferences.getLong(KEY_MAP_ROUTE_TIME, 0L);
        String routeText = preferences.getString(KEY_MAP_ROUTE_POINTS, "");
        String maneuverText = preferences.getString(KEY_MAP_ROUTE_MANEUVERS, "");
        String routeDestination = preferences.getString(KEY_MAP_ROUTE_DESTINATION, "");
        float routeDistanceMeters = preferences.getFloat(KEY_MAP_ROUTE_DISTANCE, -1.0f);
        float routeDurationSeconds = preferences.getFloat(KEY_MAP_ROUTE_DURATION, -1.0f);
        int routeDataVersion = preferences.getInt(KEY_MAP_ROUTE_DATA_VERSION, 0);
        boolean routeFresh = routeTime > 0L
                && System.currentTimeMillis() - routeTime <= MAP_ROUTE_CACHE_MS
                && routeText.length() > 2;
        result.put("routeReady", routeFresh);
        result.put("routeTime", routeFresh ? routeTime : 0L);
        result.put("routeDestination", routeDestination);
        result.put("route", routeFresh ? new JSONArray(routeText) : new JSONArray());
        result.put("routeError", preferences.getString(KEY_MAP_ROUTE_ERROR, ""));
        if (navigationActive && routeFresh
                && location != null && maneuverText.length() > 2) {
            JSONObject routeMetrics = buildRouteHudMetrics(location, routeText, maneuverText,
                    routeDistanceMeters, routeDurationSeconds);
            if (result.optString("nextDistance", "").length() == 0) {
                String routeDistance = routeMetrics.optString("nextDistance", "");
                if (routeDistance.length() > 0) result.put("nextDistance", routeDistance);
            }
            String[] metricNames = {"afterNextInstruction", "afterNextDistance",
                    "afterNextDuration", "totalRemainingDistance",
                    "totalRemainingDuration", "routeArrival", "currentRoad"};
            for (String metricName : metricNames) {
                String metricValue = routeMetrics.optString(metricName, "");
                if (metricValue.length() > 0) result.put(metricName, metricValue);
            }
            if (result.optString("arrival", "").length() == 0) {
                String routeArrival = routeMetrics.optString("routeArrival", "");
                if (routeArrival.length() > 0) result.put("arrival", routeArrival);
            }
        }
        // Google Maps' advertised arrival clock is more authoritative than the
        // static OSRM duration. Recalculate it on every HUD poll so the initial
        // value counts down even while route geometry or GPS briefly stalls.
        if (navigationActive) {
            String liveDuration = remainingDurationFromNavigationArrival(
                    result.optString("arrival", ""), System.currentTimeMillis());
            if (liveDuration.length() > 0) {
                result.put("totalRemainingDuration", liveDuration);
            }
        }
        if (navigationActive
                && (maneuverText.length() <= 2 || routeDataVersion < 3
                || routeDistanceMeters <= 0.0f || routeDurationSeconds <= 0.0f)
                && routeDestination.length() > 0
                && !navigationRouteFetchInFlight
                && System.currentTimeMillis() >= navigationRouteRetryAfterMs) {
            prepareNavigationRouteAsync(routeDestination, navigationMode(), false);
        }
        boolean suppressed = isNavigationHudSuppressed();
        result.put("suppressed", suppressed);
        if (suppressed) {
            result.put("navigationActive", false);
            result.put("instruction", "");
            result.put("detail", "");
            result.put("nextDistance", "");
            result.put("arrival", "");
            result.put("afterNextInstruction", "");
            result.put("afterNextDistance", "");
            result.put("afterNextDuration", "");
            result.put("totalRemainingDistance", "");
            result.put("totalRemainingDuration", "");
            result.put("routeArrival", "");
            result.put("currentRoad", "");
        }
        return result;
    }

    private String remainingDurationFromNavigationArrival(String value, long now) {
        String text = safe(value).trim();
        if (text.length() == 0 || (!text.contains("着") && !text.contains("到着"))) {
            return "";
        }
        Matcher clock = Pattern.compile(
                "(?<![0-9])([01]?[0-9]|2[0-3])\\s*[:：]\\s*([0-5][0-9])\\s*(?:着|到着)")
                .matcher(text);
        if (!clock.find()) {
            clock = Pattern.compile(
                    "(?:着|到着)[^0-9]{0,4}([01]?[0-9]|2[0-3])\\s*[:：]\\s*([0-5][0-9])")
                    .matcher(text);
            if (!clock.find()) return "";
        }
        try {
            int hour = Integer.parseInt(clock.group(1));
            int minute = Integer.parseInt(clock.group(2));
            Calendar arrival = Calendar.getInstance();
            arrival.setTimeInMillis(now);
            arrival.set(Calendar.HOUR_OF_DAY, hour);
            arrival.set(Calendar.MINUTE, minute);
            arrival.set(Calendar.SECOND, 0);
            arrival.set(Calendar.MILLISECOND, 0);
            // Around midnight, a destination clock can belong to tomorrow.
            if (arrival.getTimeInMillis() < now - 2L * 60L * 1000L) {
                long pastBy = now - arrival.getTimeInMillis();
                // A clock shortly before now is a stale notification, not an
                // arrival tomorrow. A large negative offset is the normal
                // midnight rollover (for example 23:50 -> 00:03).
                if (pastBy < 6L * 60L * 60L * 1000L) return "";
                arrival.add(Calendar.DAY_OF_MONTH, 1);
            }
            long remainingMs = arrival.getTimeInMillis() - now;
            if (remainingMs <= 0L || remainingMs > 36L * 60L * 60L * 1000L) {
                return "";
            }
            double roundedUpSeconds = Math.ceil(remainingMs / 60000.0) * 60.0;
            return formatRouteDuration(roundedUpSeconds);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String cachedCurrentRoad(Location current) {
        SharedPreferences preferences = getPreferences();
        String road = preferences.getString(KEY_MAP_CURRENT_ROAD, "").trim();
        long time = preferences.getLong(KEY_MAP_CURRENT_ROAD_TIME, 0L);
        if (!isUsefulRoadName(road) || time <= 0L
                || System.currentTimeMillis() - time > MAP_ROAD_CACHE_MS) {
            return "";
        }
        if (!preferences.contains(KEY_MAP_CURRENT_ROAD_LATITUDE)
                || !preferences.contains(KEY_MAP_CURRENT_ROAD_LONGITUDE)) {
            return "";
        }
        Location cached = new Location("road-cache");
        cached.setLatitude(Double.longBitsToDouble(preferences.getLong(
                KEY_MAP_CURRENT_ROAD_LATITUDE, Double.doubleToRawLongBits(Double.NaN))));
        cached.setLongitude(Double.longBitsToDouble(preferences.getLong(
                KEY_MAP_CURRENT_ROAD_LONGITUDE, Double.doubleToRawLongBits(Double.NaN))));
        if (Double.isNaN(cached.getLatitude()) || Double.isNaN(cached.getLongitude())
                || current.distanceTo(cached) > MAP_ROAD_CACHE_RADIUS_METERS) {
            return "";
        }
        return road;
    }

    private void refreshCurrentRoadAsync(Location current) {
        if (navigationRoadLookupInFlight || current == null) return;
        SharedPreferences preferences = getPreferences();
        long cachedAt = preferences.getLong(KEY_MAP_CURRENT_ROAD_TIME, 0L);
        if (cachedAt > 0L && System.currentTimeMillis() - cachedAt < MAP_ROAD_REFRESH_MS
                && cachedCurrentRoad(current).length() > 0) {
            return;
        }
        navigationRoadLookupInFlight = true;
        final Location snapshot = new Location(current);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    String road = reverseGeocodeRoadName(snapshot);
                    if (road.length() > 0) {
                        getPreferences().edit()
                                .putString(KEY_MAP_CURRENT_ROAD, shortInline(road, 36))
                                .putLong(KEY_MAP_CURRENT_ROAD_TIME, System.currentTimeMillis())
                                .putLong(KEY_MAP_CURRENT_ROAD_LATITUDE,
                                        Double.doubleToRawLongBits(snapshot.getLatitude()))
                                .putLong(KEY_MAP_CURRENT_ROAD_LONGITUDE,
                                        Double.doubleToRawLongBits(snapshot.getLongitude()))
                                .apply();
                    }
                } catch (Exception error) {
                    Log.d(TAG, "current road lookup unavailable: " + error.getMessage());
                } finally {
                    navigationRoadLookupInFlight = false;
                }
            }
        }, "NavigationRoadLookup").start();
    }

    private String reverseGeocodeRoadName(Location location) throws Exception {
        if (Geocoder.isPresent()) {
            try {
                List<Address> addresses = new Geocoder(this, Locale.JAPAN)
                        .getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String road = safe(addresses.get(0).getThoroughfare()).trim();
                    if (isUsefulRoadName(road)) return road;
                }
            } catch (Exception error) {
                Log.d(TAG, "Android road geocoder unavailable: " + error.getMessage());
            }
        }
        String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2"
                + "&zoom=18&addressdetails=1&lat="
                + String.format(Locale.US, "%.6f", location.getLatitude())
                + "&lon=" + String.format(Locale.US, "%.6f", location.getLongitude());
        JSONObject response = new JSONObject(fetchNavigationText(url));
        JSONObject address = response.optJSONObject("address");
        if (address == null) return "";
        String[] keys = {"road", "pedestrian", "footway", "path", "residential",
                "living_street", "cycleway"};
        for (String key : keys) {
            String road = address.optString(key, "").trim();
            if (isUsefulRoadName(road)) return road;
        }
        return "";
    }

    private boolean isUsefulRoadName(String value) {
        String road = safe(value).trim();
        if (road.length() == 0) return false;
        String compact = road.replaceAll("[\\s縲]", "");
        if (compact.matches("[0-9０-９一二三四五六七八九十]+丁目(?:[0-9０-９一二三四五六七八九十-]*番地?)?")) {
            return false;
        }
        return !compact.matches("[0-9０-９一二三四五六七八九十-]+(番地?|号)?");
    }

    private JSONObject buildRouteHudMetrics(Location current, String routeText,
                                             String maneuverText,
                                             float routeTotalDistance,
                                             float routeTotalDuration) {
        JSONObject result = new JSONObject();
        try {
            JSONArray route = new JSONArray(routeText);
            JSONArray maneuvers = new JSONArray(maneuverText);
            if (route.length() < 2 || maneuvers.length() == 0) return result;
            int nearestIndex = 0;
            float nearestDistance = Float.MAX_VALUE;
            for (int index = 0; index < route.length(); index++) {
                JSONArray point = route.optJSONArray(index);
                if (point == null || point.length() < 2) continue;
                float distance = coordinateDistance(current.getLatitude(),
                        current.getLongitude(), point.optDouble(0), point.optDouble(1));
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = index;
                }
            }
            if (nearestDistance > 500.0f) return result;
            double currentProgress = nearestIndex / (double) (route.length() - 1);
            ArrayList<JSONObject> future = new ArrayList<JSONObject>();
            String currentRoad = "";
            for (int index = 0; index < maneuvers.length(); index++) {
                JSONObject maneuver = maneuvers.optJSONObject(index);
                if (maneuver == null) continue;
                double progress = maneuver.optDouble("progress", -1.0);
                String roadName = maneuver.optString("name", "").trim();
                if (progress <= currentProgress + 0.002 && roadName.length() > 0) {
                    currentRoad = roadName;
                }
                if (progress <= currentProgress + 0.0005) continue;
                int targetIndex = routeIndexForProgress(route, progress);
                float fromCurrent = nearestDistance
                        + routeDistanceBetween(route, nearestIndex, targetIndex);
                if (fromCurrent < 8.0f) continue;
                future.add(maneuver);
                if (future.size() >= 2) break;
            }
            if (currentRoad.length() == 0 && maneuvers.length() > 0) {
                JSONObject firstManeuver = maneuvers.optJSONObject(0);
                if (firstManeuver != null) {
                    currentRoad = firstManeuver.optString("name", "").trim();
                }
            }
            if (currentRoad.length() > 0) result.put("currentRoad", currentRoad);
            if (!future.isEmpty()) {
                JSONObject next = future.get(0);
                int nextIndex = routeIndexForProgress(route,
                        next.optDouble("progress", currentProgress));
                float distance = nearestDistance
                        + routeDistanceBetween(route, nearestIndex, nextIndex);
                if (distance >= 8.0f) result.put("nextDistance", formatRouteDistance(distance));
            }
            if (future.size() >= 2) {
                JSONObject next = future.get(0);
                JSONObject afterNext = future.get(1);
                String instruction = routeManeuverInstruction(afterNext);
                if (instruction.length() > 0) result.put("afterNextInstruction", instruction);
                int nextIndex = routeIndexForProgress(route,
                        next.optDouble("progress", currentProgress));
                int afterIndex = routeIndexForProgress(route,
                        afterNext.optDouble("progress", currentProgress));
                float afterDistance = routeDistanceBetween(route, nextIndex, afterIndex);
                if (afterDistance <= 0.0f) {
                    afterDistance = (float) next.optDouble("distance", -1.0);
                }
                if (afterDistance > 0.0f) {
                    result.put("afterNextDistance", formatRouteDistance(afterDistance));
                }
                double afterDuration = next.optDouble("duration", -1.0);
                if (afterDuration <= 0.0 && routeTotalDistance > 0.0f
                        && routeTotalDuration > 0.0f && afterDistance > 0.0f) {
                    afterDuration = routeTotalDuration
                            * (afterDistance / routeTotalDistance);
                }
                if (afterDuration > 0.0) {
                    result.put("afterNextDuration", formatRouteDuration(afterDuration));
                }
            }
            float remainingDistance = nearestDistance
                    + routeDistanceBetween(route, nearestIndex, route.length() - 1);
            if (remainingDistance > 0.0f) {
                result.put("totalRemainingDistance", formatRouteDistance(remainingDistance));
            }
            double remainingDuration = -1.0;
            if (routeTotalDistance > 0.0f && routeTotalDuration > 0.0f
                    && remainingDistance > 0.0f) {
                remainingDuration = routeTotalDuration
                        * Math.min(1.0, remainingDistance / routeTotalDistance);
                result.put("totalRemainingDuration", formatRouteDuration(remainingDuration));
                long arrivalAt = System.currentTimeMillis()
                        + Math.max(60000L, Math.round(remainingDuration * 1000.0));
                result.put("routeArrival", new SimpleDateFormat("HH:mm'着'", Locale.JAPAN)
                        .format(new Date(arrivalAt)));
            }
        } catch (Exception error) {
            Log.w(TAG, "route HUD metrics failed", error);
        }
        return result;
    }

    private int routeIndexForProgress(JSONArray route, double progress) {
        if (route == null || route.length() <= 1) return 0;
        return Math.max(0, Math.min(route.length() - 1,
                (int) Math.ceil(Math.max(0.0, Math.min(1.0, progress))
                        * (route.length() - 1))));
    }

    private float routeDistanceBetween(JSONArray route, int fromIndex, int toIndex) {
        if (route == null || route.length() < 2) return 0.0f;
        int start = Math.max(0, Math.min(route.length() - 1, fromIndex));
        int end = Math.max(start, Math.min(route.length() - 1, toIndex));
        float distance = 0.0f;
        for (int index = start; index < end; index++) {
            JSONArray from = route.optJSONArray(index);
            JSONArray to = route.optJSONArray(index + 1);
            if (from == null || to == null) continue;
            distance += coordinateDistance(from.optDouble(0), from.optDouble(1),
                    to.optDouble(0), to.optDouble(1));
        }
        return distance;
    }

    private String routeManeuverInstruction(JSONObject maneuver) {
        if (maneuver == null) return "";
        String type = maneuver.optString("type", "").toLowerCase(Locale.US);
        String modifier = maneuver.optString("modifier", "").toLowerCase(Locale.US);
        String name = maneuver.optString("name", "").trim();
        if ("arrive".equals(type)) return "目的地に到着";
        String action;
        if (modifier.contains("uturn")) action = "Uターン";
        else if (modifier.contains("sharp right")) action = "大きく右へ";
        else if (modifier.contains("sharp left")) action = "大きく左へ";
        else if (modifier.contains("slight right")) action = "斜め右へ";
        else if (modifier.contains("slight left")) action = "斜め左へ";
        else if (modifier.contains("right")) action = "右折";
        else if (modifier.contains("left")) action = "左折";
        else if (modifier.contains("straight")) action = "直進";
        else if (type.contains("roundabout")) action = "ロータリーへ";
        else if (type.contains("merge")) action = "合流";
        else action = "進む";
        if (name.length() == 0) return action;
        if ("右折".equals(action) || "左折".equals(action) || "直進".equals(action)) {
            return name + "を" + action;
        }
        return action + " " + name;
    }

    private String formatRouteDuration(double seconds) {
        long minutes = Math.max(1L, Math.round(seconds / 60.0));
        if (minutes < 60L) return minutes + "分";
        long hours = minutes / 60L;
        long rest = minutes % 60L;
        return rest == 0L ? hours + "時間" : hours + "時間" + rest + "分";
    }

    private String routeNextActionDistance(Location current, String routeText,
                                           String maneuverText) {
        try {
            JSONArray route = new JSONArray(routeText);
            JSONArray maneuvers = new JSONArray(maneuverText);
            if (route.length() < 2 || maneuvers.length() == 0) return "";
            int nearestIndex = 0;
            float nearestDistance = Float.MAX_VALUE;
            for (int index = 0; index < route.length(); index++) {
                JSONArray point = route.optJSONArray(index);
                if (point == null || point.length() < 2) continue;
                float distance = coordinateDistance(current.getLatitude(),
                        current.getLongitude(), point.optDouble(0), point.optDouble(1));
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = index;
                }
            }
            if (nearestDistance > 500.0f) return "";
            double currentProgress = nearestIndex / (double) (route.length() - 1);
            for (int index = 0; index < maneuvers.length(); index++) {
                JSONObject maneuver = maneuvers.optJSONObject(index);
                if (maneuver == null) continue;
                double progress = maneuver.optDouble("progress", -1.0);
                if (progress <= currentProgress + 0.0005) continue;
                int targetIndex = Math.max(nearestIndex + 1, Math.min(route.length() - 1,
                        (int) Math.ceil(progress * (route.length() - 1))));
                float distance = nearestDistance;
                for (int routeIndex = nearestIndex; routeIndex < targetIndex; routeIndex++) {
                    JSONArray from = route.optJSONArray(routeIndex);
                    JSONArray to = route.optJSONArray(routeIndex + 1);
                    if (from == null || to == null) continue;
                    distance += coordinateDistance(from.optDouble(0), from.optDouble(1),
                            to.optDouble(0), to.optDouble(1));
                }
                if (distance < 8.0f) continue;
                return formatRouteDistance(distance);
            }
        } catch (Exception error) {
            Log.w(TAG, "next route action distance failed", error);
        }
        return "";
    }

    private float coordinateDistance(double fromLatitude, double fromLongitude,
                                     double toLatitude, double toLongitude) {
        float[] result = new float[1];
        Location.distanceBetween(fromLatitude, fromLongitude,
                toLatitude, toLongitude, result);
        return result[0];
    }

    private String formatRouteDistance(float metres) {
        if (metres >= 1000.0f) {
            float kilometres = metres / 1000.0f;
            return String.format(Locale.JAPAN,
                    kilometres >= 10.0f ? "%.0f km" : "%.1f km", kilometres);
        }
        int rounded = metres >= 100.0f
                ? Math.max(10, Math.round(metres / 10.0f) * 10)
                : Math.max(1, Math.round(metres));
        return rounded + " m";
    }

    private JSONObject buildNavigationHudControlJson(String action) throws Exception {
        String command = action == null ? "toggle" : action.trim().toLowerCase(Locale.US);
        boolean suppressed = isNavigationHudSuppressed();
        if ("hide".equals(command) || "off".equals(command)) {
            suppressed = true;
        } else if ("show".equals(command) || "on".equals(command)) {
            suppressed = false;
        } else {
            suppressed = !suppressed;
        }
        final boolean resultSuppressed = suppressed;
        setNavigationHudSuppressed(resultSuppressed);
        runOnUiThread(new Runnable() {
            @Override public void run() {
                updateStatus(resultSuppressed ? "ナビHUD停止" : "ナビHUD再開",
                        resultSuppressed
                                ? "グラスからナビ表示を隠しました"
                                : "グラスからナビ表示を再開しました");
            }
        });
        JSONObject result = new JSONObject();
        result.put("ok", true);
        result.put("suppressed", resultSuppressed);
        return result;
    }

    private JSONObject buildWeatherJson(int dayOffset) throws Exception {
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
        JSONArray forecast = new JSONArray(preferences.getString(KEY_WEATHER_FORECAST, "[]"));
        if (dayOffset >= 0 && dayOffset < forecast.length()) {
            JSONObject day = forecast.optJSONObject(dayOffset);
            if (day != null) {
                root.put("forecast", day);
            }
        }
        root.put("dayOffset", dayOffset);
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
                + "&current=temperature_2m,apparent_temperature,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&forecast_days=3&timezone=auto";
        JSONObject response = new JSONObject(fetchText(url));
        JSONObject current = response.optJSONObject("current");
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
        JSONObject daily = response.optJSONObject("daily");
        JSONArray forecast = new JSONArray();
        if (daily != null) {
            JSONArray dates = daily.optJSONArray("time");
            JSONArray codes = daily.optJSONArray("weather_code");
            JSONArray maximums = daily.optJSONArray("temperature_2m_max");
            JSONArray minimums = daily.optJSONArray("temperature_2m_min");
            JSONArray rain = daily.optJSONArray("precipitation_probability_max");
            int count = dates == null ? 0 : Math.min(3, dates.length());
            for (int index = 0; index < count; index++) {
                JSONObject day = new JSONObject();
                int code = codes == null ? -1 : codes.optInt(index, -1);
                day.put("date", dates.optString(index, ""));
                day.put("condition", weatherCondition(code));
                if (maximums != null && !maximums.isNull(index)) {
                    day.put("max", String.format(Locale.JAPAN, "%.1f", maximums.optDouble(index)));
                }
                if (minimums != null && !minimums.isNull(index)) {
                    day.put("min", String.format(Locale.JAPAN, "%.1f", minimums.optDouble(index)));
                }
                if (rain != null && !rain.isNull(index)) {
                    day.put("rain", rain.optInt(index, -1));
                }
                forecast.put(day);
            }
        }
        editor.putString(KEY_WEATHER_FORECAST, forecast.toString());
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
        Location bestWithSpeed = null;
        for (String provider : manager.getProviders(true)) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
                if (candidate != null && candidate.hasSpeed()
                        && (bestWithSpeed == null
                        || candidate.getTime() > bestWithSpeed.getTime())) {
                    bestWithSpeed = candidate;
                }
            } catch (Exception ignored) {
            }
        }
        long now = System.currentTimeMillis();
        if (bestWithSpeed != null && now - bestWithSpeed.getTime() <= 120000L
                && (best == null || best.getTime() - bestWithSpeed.getTime() <= 60000L)) {
            return bestWithSpeed;
        }
        if (best != null && now - best.getTime() <= 900000L) {
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
            if (isConversationMemoryKind(kind) && !isFailedAnswer(message)) {
                appendConversationMemorySafely(kind, message);
            }
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
        root.put("note", "GmailなどのAndroid通知から取得したメール候補です。Gmail APIの未読状態とは一致しません。");
        JSONArray mails = MailNotificationService.recentMailJson();
        root.put("mails", mails);
        root.put("gmailUnread", GmailUnreadReader.read(this));
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String shortText(String value, int max) {
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

    private long parseLongQuery(String request, String key, long fallback) {
        try {
            String value = parseStringQuery(request, key, "");
            return value.length() == 0 ? fallback : Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
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

    private void refreshGlassStateView() {
        if (glassStateView == null) {
            return;
        }
        String state;
        String message;
        long waitUntil;
        synchronized (MainActivity.class) {
            state = glassRuntimeState;
            message = glassRuntimeMessage;
            waitUntil = glassWaitUntilMs;
        }
        long remainingSeconds = Math.max(0L,
                (Math.max(0L, waitUntil - System.currentTimeMillis()) + 999L) / 1000L);
        if ("WAIT".equals(state) && waitUntil > 0L && remainingSeconds == 0L) {
            glassStateView.setText("グラス: READY");
            glassStateView.setTextColor(Color.rgb(30, 130, 30));
        } else if ("WAIT".equals(state)) {
            glassStateView.setText("グラス: WAIT " + remainingSeconds + "秒"
                    + (message.length() == 0 ? "" : " / " + message));
            glassStateView.setTextColor(Color.rgb(180, 110, 0));
        } else if ("THINKING".equals(state)) {
            glassStateView.setText("グラス: 回答生成中"
                    + (message.length() == 0 ? "" : " / " + message));
            glassStateView.setTextColor(Color.rgb(30, 90, 160));
        } else if ("ERROR".equals(state)) {
            glassStateView.setText("グラス: エラー"
                    + (message.length() == 0 ? "" : " / " + message));
            glassStateView.setTextColor(Color.rgb(180, 30, 30));
        } else if ("READY".equals(state)) {
            glassStateView.setText("グラス: READY");
            glassStateView.setTextColor(Color.rgb(30, 130, 30));
        } else {
            glassStateView.setText("グラス: 接続待ち");
            glassStateView.setTextColor(Color.DKGRAY);
        }
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    public static void addLog(String message) {
        addAiLog(message);
    }

    public static void addAiLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(new Date());
        String safeMessage = message == null ? "" : message;
        if (safeMessage.length() > MAX_LOG_ENTRY_CHARS) {
            safeMessage = safeMessage.substring(0, MAX_LOG_ENTRY_CHARS) + "\n…";
        }
        synchronized (LOGS) {
            LOGS.add(0, time + " " + safeMessage);
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

    private void restoreRecentConversationLogs() {
        synchronized (LOGS) {
            if (!LOGS.isEmpty()) {
                return;
            }
            ArrayList<JSONObject> entries = readConversationMemory();
            SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPAN);
            for (int i = entries.size() - 1; i >= 0 && LOGS.size() < MAX_LOGS; i--) {
                JSONObject entry = entries.get(i);
                long time = entry.optLong("time", 0L);
                String prefix = time > 0L ? timeFormat.format(new Date(time)) : "--/-- --:--:--";
                LOGS.add(prefix + " " + entry.optString("kind", "") + ": "
                        + entry.optString("message", ""));
            }
        }
    }

    private void clearConversationLogs() {
        synchronized (LOGS) {
            LOGS.clear();
        }
        synchronized (MEMORY_LOCK) {
            File file = conversationMemoryFile();
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "conversation memory file could not be deleted");
            }
            File archive = conversationMemoryArchiveFile();
            if (archive.exists() && !archive.delete()) {
                Log.w(TAG, "conversation memory archive could not be deleted");
            }
        }
        refreshLogs();
    }

    private void refreshLogs() {
        if (logView == null) return;
        StringBuilder builder = new StringBuilder();
        builder.append("AI会話ログ（端末内・直近90日＋圧縮長期記憶）\n");
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

    private static final class NavigationRouteData {
        static final NavigationRouteData EMPTY = new NavigationRouteData(
                new JSONArray(), new JSONArray(), -1.0, -1.0);
        final JSONArray route;
        final JSONArray maneuvers;
        final double distanceMeters;
        final double durationSeconds;

        NavigationRouteData(JSONArray route, JSONArray maneuvers,
                            double distanceMeters, double durationSeconds) {
            this.route = route == null ? new JSONArray() : route;
            this.maneuvers = maneuvers == null ? new JSONArray() : maneuvers;
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
        }
    }
}
