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
    // A 16-second 16 kHz mono PCM turn is about 512 KB before Base64 and
    // roughly 683 KB inside JSON. Keep a bounded 1 MB ceiling so longer VOICE
    // turns fit without allowing arbitrarily large local bridge requests.
    private static final int MAX_REQUEST_BODY_CHARS = 1048576;
    private static final int MAX_COMMAND_CHARS = 3000;
    private static final int MAX_PENDING_COMMANDS = 32;
    private static final int MAX_CUSTOM_CHARS = 12000;
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
    private static final String KEY_AMBIENT_RELAY_QUEUE = "ambient_relay_queue";
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
    private static final long MAP_ROUTE_PERIODIC_REFRESH_MS = 8L * 60L * 1000L;
    private static final long MAP_ROUTE_REROUTE_COOLDOWN_MS = 60000L;
    private static final long MAP_ROUTE_FAST_REROUTE_COOLDOWN_MS = 120000L;
    private static final long MAP_ROUTE_DEVIATION_CONFIRM_MS = 10000L;
    private static final long MAP_ROUTE_FAST_DEVIATION_CONFIRM_MS = 20000L;
    private static final long MAP_ROAD_REFRESH_MS = 60L * 1000L;
    private static final long MAP_ROAD_CACHE_MS = 10L * 60L * 1000L;
    private static final float MAP_ROAD_CACHE_RADIUS_METERS = 180.0f;
    private static final int MAP_ROUTE_MAX_POINTS = 4096;
    private static final long AMBIENT_CONSUMER_TIMEOUT_MS = 30000L;
    private static final long AMBIENT_RELAY_RETENTION_MS = 180000L;
    private static final int AMBIENT_RELAY_QUEUE_LIMIT = 8;
    private static final int AMBIENT_RELAY_BATCH_CHARS = 2800;
    private static final Object AMBIENT_RELAY_LOCK = new Object();
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
    private String customEditorBase = "";
    private boolean customEditorApplyingRemote;
    private boolean customStateWaiting;
    private static volatile boolean running;
    // The bridge server outlives Activity recreation; its pairing windows must too.
    private static volatile long pairingUntilMs;
    private static volatile long pcPairingUntilMs;
    private static ServerSocket serverSocket;
    private final Handler healthHandler = new Handler(Looper.getMainLooper());
    private final Handler weatherHandler = new Handler(Looper.getMainLooper());
    private final Handler glassStateHandler = new Handler(Looper.getMainLooper());
    private volatile boolean weatherRefreshInFlight;
    private volatile boolean navigationRouteFetchInFlight;
    private volatile GoogleRouteCache googleRouteCache;
    private volatile TransitAccess transitAccess;
    private volatile long googleRouteGeneration;
    private volatile long navigationCompletedUntil;
    private volatile String navigationCompletedDestination = "";
    private volatile long arrivalCandidateSince;
    private volatile String transitJourneyTarget = "";
    private volatile long transitJourneyTargetUntil;
    private static volatile String confirmedSharedDestination = "";
    private static volatile long confirmedSharedUntil;
    private static volatile boolean confirmedSharedNavigationSeen;
    private static final class GoogleRouteCache {
        final NavigationRouteData data;
        final String destination, mode;
        final String effectiveMode;
        final long generation;
        String scheduledDeparture = "";
        final TransitJourney journey;
        final JourneyProgress progress;
        final long time = System.currentTimeMillis();
        GoogleRouteCache(NavigationRouteData data, String destination, String mode, String effectiveMode,
                         TransitJourney journey, long generation) {
            this.data = data; this.destination = destination; this.mode = mode;
            this.effectiveMode = effectiveMode;
            this.journey = journey;
            this.generation = generation;
            this.progress = journey == null ? null : new JourneyProgress(journey);
        }
    }
    private volatile boolean navigationRoadLookupInFlight;
    private volatile long navigationRouteRetryAfterMs;
    private volatile int navigationRouteFailureCount;
    private volatile long navigationRouteDeviationSinceMs;
    private volatile long navigationRouteLastRerouteAtMs;
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
        // One-time recheck of the user's existing setup after fixing diagnostic
        // classification. Never runs for a device without a prior manual check.
        final SharedPreferences mapsCheck = getSharedPreferences("loki_maps_secure", MODE_PRIVATE);
        if (mapsCheck.getLong("last_check", 0L) > 0L
                && mapsCheck.getInt("diagnostic_version", 0) < 4) {
            mapsCheck.edit().putInt("diagnostic_version", 4).apply();
            new Thread(new Runnable() {
                @Override public void run() {
                    String result;
                    try { result = GoogleMapsConnectionCheck.verify(MainActivity.this); }
                    catch (Exception ignored) { result = "地図APIの接続確認で通信エラー。キーは保持しています。"; }
                    final String safeResult = result;
                    runOnUiThread(new Runnable() {
                        @Override public void run() { updateStatus("地図API確認結果", safeResult); }
                    });
                }
            }, "MapsSetupRecheck").start();
        }
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
        handleSharedDestination(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSharedDestination(intent);
    }

    private void handleSharedDestination(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        intent.setAction(null); // Do not replay a consumed share after rotation.
        if (text == null || text.length() > 8000) return;
        String candidate = text.replaceAll("https?://\\S+", "").trim();
        final EditText target = new EditText(this);
        target.setSingleLine(true);
        target.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
        target.setHint("施設名または住所（リンクのみの場合は入力）");
        target.setText(candidate.length() <= 200 ? candidate : "");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(24, 12, 24, 12);
        TextView explanation = new TextView(this);
        explanation.setText("共有内容から目的地を確認してください。短縮リンクだけでは場所を確定できません。ロキが別途計算する経路で、Googleマップの選択経路と異なる場合があります。マップ側で目的地を変更したら再共有してください。");
        layout.addView(explanation); layout.addView(target);
        final android.widget.Spinner mode = new android.widget.Spinner(this);
        mode.setAdapter(new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"車", "徒歩", "自転車", "公共交通"}));
        layout.addView(mode);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("共有された目的地")
                .setView(layout).setPositiveButton("この目的地でナビ", null).setNegativeButton("キャンセル", null).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { public void onShow(DialogInterface d) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                String destination = target.getText().toString().trim();
                if (destination.isEmpty() || destination.contains("://")) { target.setError("施設名または住所を確認してください"); return; }
                getPreferences().edit().putString(KEY_MAP_ROUTE_MODE,
                        new String[]{"driving", "walking", "bicycling", "transit"}[mode.getSelectedItemPosition()]).apply();
                navigationDestinationInput.setText(destination);
                if (mode.getSelectedItemPosition() == 0) {
                    try {
                        Class.forName("com.example.rokidgeminisecretary.SdkPhoneBridge")
                                .getMethod("show", android.app.Activity.class, String.class)
                                .invoke(null, MainActivity.this, destination);
                        dialog.dismiss();
                        return;
                    } catch (ClassNotFoundException sdkFreeBuild) {
                        // Preserve the existing Maps flow in SDK-free builds.
                    } catch (Exception failure) {
                        target.setError("車ナビを開けませんでした。もう一度お試しください");
                        return;
                    }
                }
                launchGoogleMapsNavigation();
                confirmedSharedDestination = destination;
                confirmedSharedNavigationSeen = false;
                confirmedSharedUntil = System.currentTimeMillis() + 2L * 60L * 60L * 1000L;
                dialog.dismiss();
            }});
        }});
        dialog.show();
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
        Button geminiSettings = new Button(this);
        geminiSettings.setText("Gemini APIキー設定");
        geminiSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showGeminiKeySettings(); }
        });
        root.addView(geminiSettings);

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
        navigationDestinationInput.setSelectAllOnFocus(true);
        LinearLayout destinationRow = new LinearLayout(this);
        destinationRow.setOrientation(LinearLayout.HORIZONTAL);
        destinationRow.addView(navigationDestinationInput,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button clearDestination = new Button(this);
        clearDestination.setText("×");
        clearDestination.setContentDescription("目的地を全消去");
        clearDestination.setMinWidth(0); clearDestination.setMinHeight(0);
        clearDestination.setOnClickListener(view -> {
            navigationDestinationInput.setText("");
            navigationDestinationInput.requestFocus();
        });
        destinationRow.addView(clearDestination, new LinearLayout.LayoutParams(
                (int)(48 * getResources().getDisplayMetrics().density), LinearLayout.LayoutParams.WRAP_CONTENT));
        toolsPanel.addView(destinationRow);
        LinearLayout presetRow = new LinearLayout(this);
        for (String kind : new String[]{"home", "work"}) {
            Button preset = new Button(this);
            preset.setText("home".equals(kind) ? "自宅" : "勤務先");
            preset.setOnClickListener(view -> useDestinationPreset(kind));
            preset.setOnLongClickListener(view -> { showDestinationPresetRegistration(kind); return true; });
            presetRow.addView(preset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        Button registerPreset = new Button(this);
        registerPreset.setText("地点を登録");
        registerPreset.setOnClickListener(view -> new AlertDialog.Builder(this).setTitle("登録先")
                .setItems(new String[]{"自宅", "勤務先"}, (dialog, which) ->
                        showDestinationPresetRegistration(which == 0 ? "home" : "work")).show());
        presetRow.addView(registerPreset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        toolsPanel.addView(presetRow);

        LinearLayout navigationRow = new LinearLayout(this);
        navigationRow.setOrientation(LinearLayout.HORIZONTAL);

        android.widget.RadioGroup modes = new android.widget.RadioGroup(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        for (String mode : new String[]{"driving", "walking", "bicycling", "transit"}) {
            android.widget.RadioButton choice = new android.widget.RadioButton(this);
            choice.setId(View.generateViewId());
            choice.setText(navigationModeLabel(mode));
            choice.setTag(mode);
            choice.setTextSize(12);
            modes.addView(choice, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            choice.setChecked(mode.equals(navigationMode()));
        }
        modes.setOnCheckedChangeListener((group, id) -> {
            View selected = group.findViewById(id);
            if (selected == null) return;
            String mode = (String) selected.getTag();
            getPreferences().edit().putString(KEY_MAP_ROUTE_MODE, mode).apply();
            Log.d("LokiGoogleNav", "mode_selected=" + mode);
        });
        toolsPanel.addView(modes);

        Button startNavigation = new Button(this);
        startNavigation.setText("MAPナビ開始");
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
        try {
            Class.forName("com.example.rokidgeminisecretary.SdkPhoneBridge");
            Button sdkNavigation = new Button(this);
            sdkNavigation.setText("車ナビ・車線案内 / 状態・停止");
            sdkNavigation.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    try {
                        Class.forName("com.example.rokidgeminisecretary.SdkPhoneBridge")
                                .getMethod("show", android.app.Activity.class, String.class)
                                .invoke(null, MainActivity.this, navigationDestinationInput.getText().toString());
                    } catch (Exception failure) { updateStatus("SDKナビ", "操作画面を開けませんでした"); }
                }
            });
            toolsPanel.addView(sdkNavigation);
        } catch (ClassNotFoundException manualBuild) { /* Existing SDK-free build remains supported. */ }
        TextView mapHint = new TextView(this);
        mapHint.setText("地図はグラスで描画します。車線案内は『車ナビ・車線案内』でSDK案内中、対象地点の車線データがある場合に表示します。MAPナビのGoogle計算経路はマップアプリの選択経路と異なる場合があります。");
        mapHint.setTextSize(12);
        toolsPanel.addView(mapHint);
        Button mapsSetup = new Button(this);
        mapsSetup.setText("Google地図API設定・接続確認");
        mapsSetup.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showMapsApiSetup(); }
        });
        toolsPanel.addView(mapsSetup);
        Button pcPair = new Button(this);
        pcPair.setText("PC接続（60秒間）");
        pcPair.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                ensureBridgeToken();
                pcPairingUntilMs = System.currentTimeMillis() + 60000L;
                updateStatus("PC接続待ち", "PCのロキでUSB接続を押してください。");
            }
        });
        toolsPanel.addView(pcPair);
        Button importMemory = new Button(this);
        importMemory.setText("Gemini会話を取り込む・記憶を確認");
        importMemory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showImportedMemoryMenu(); }
        });
        toolsPanel.addView(importMemory);

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

    private void showMapsApiSetup() {
        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24, 8, 24, 8);
        TextView steps = new TextView(this);
        steps.setText("① 下のボタンで、このスマホのブラウザを開く\n② Loki Maps Android - Routes and Tiles の「鍵を表示」→「鍵をコピー」\n③ 最近使ったアプリからロキへ戻り、下の欄に貼り付け\n④「保存して接続確認」\n※ Gemini用キーではありません。手入力・キーの新規作成は不要です。");
        panel.addView(steps);
        Button openCloud = new Button(this);
        openCloud.setText("① このスマホで地図用キーを開く");
        openCloud.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent cloud = new Intent(Intent.ACTION_VIEW, Uri.parse("https://console.cloud.google.com/apis/credentials?project=loki-maps"));
                cloud.addCategory(Intent.CATEGORY_BROWSABLE);
                try {
                    cloud.setPackage("com.android.chrome");
                    startActivity(cloud);
                } catch (android.content.ActivityNotFoundException noChrome) {
                    cloud.setPackage(null);
                    try { startActivity(cloud); }
                    catch (android.content.ActivityNotFoundException noBrowser) {
                        android.widget.Toast.makeText(MainActivity.this, "ブラウザをインストールしてからお試しください", android.widget.Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        panel.addView(openCloud);
        final TextView feedback = new TextView(this);
        feedback.setTextSize(15);
        try {
            String saved = MapsCredentialStore.read(this);
            feedback.setText(saved.length() == 0 ? "キーは未保存です。"
                    : MapsKeyInput.canSend(MapsKeyInput.normalize(saved))
                    ? "キーは暗号化保存済みです。空欄のまま再確認できます。"
                    : "保存された値は形式確認が必要です（" + saved.length() + "文字）。キー自体は再表示しません。");
        } catch (Exception ignored) { feedback.setText("保存済みキーを読み出せません。再設定が必要です。"); }
        String lastResult = getSharedPreferences("loki_maps_secure", MODE_PRIVATE).getString("last_result", "");
        if (lastResult.length() > 0) feedback.append("\n前回: " + lastResult);
        panel.addView(feedback);
        final EditText keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setHint("Maps専用キー（Geminiキーではありません）");
        panel.addView(keyInput);
        android.widget.ScrollView setupScroll = new android.widget.ScrollView(this);
        setupScroll.addView(panel);
        final android.app.AlertDialog setup = new android.app.AlertDialog.Builder(this).setTitle("Google地図APIの接続準備")
                .setMessage("キーはこのスマホ内で暗号化保存します。接続確認には公開地点を使用します。確認成功後、ナビ使用時は現在地・目的地をGoogleへ送り、Google地図とAPI計算経路を表示します（マップアプリの選択経路と異なる場合あり）。空欄なら保存済みキーを再確認します。")
                .setView(setupScroll).setNegativeButton("閉じる", null)
                .setPositiveButton("保存して接続確認", null).create();
        setup.show();
        setup.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setup.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View button) {
                        final String value = MapsKeyInput.normalize(keyInput.getText().toString());
                        if (value.length() > 0 && !MapsKeyInput.canSend(value)) {
                            keyInput.setError("途中に空白・改行・全角文字等があるか、512文字を超えています（" + value.length() + "文字）。保存済みの値は変更していません。");
                            feedback.setText("入力はまだ保存していません。設定画面は閉じずに確認できます。");
                            return;
                        }
                        setup.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                        keyInput.setEnabled(false);
                        feedback.setText("保存状態と接続を確認中…");
                        updateStatus("地図API確認中", "アプリ制限と接続を検証しています");
                        new Thread(new Runnable() {
                            @Override public void run() {
                                String message;
                                try {
                                    if (value.length() > 0) {
                                        MapsCredentialStore.save(MainActivity.this, value);
                                        if (!value.equals(MapsCredentialStore.read(MainActivity.this)))
                                            throw new IllegalStateException("save verification failed");
                                    }
                                    message = GoogleMapsConnectionCheck.verify(MainActivity.this);
                                } catch (Exception error) {
                                    // Never log an exception that might contain the API-key URL.
                                    message = "接続確認に失敗しました。通信・キー設定を確認してください。";
                                }
                                final String result = message;
                                getSharedPreferences("loki_maps_secure", MODE_PRIVATE).edit()
                                        .putString("last_result", result).apply();
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        updateStatus("地図API確認結果", result);
                                        if (!setup.isShowing()) return;
                                        keyInput.setText("");
                                        keyInput.setEnabled(true);
                                        feedback.setText("確認結果:\n" + result + "\n保存済みキーは安全のため入力欄に再表示しません。");
                                        setup.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                                    }
                                });
                            }
                        }, "MapsConnectionCheck").start();
                    }
                });
    }

    private void useDestinationPreset(String kind) {
        DestinationPresets.Place place = DestinationPresets.read(this, kind);
        if (place == null) { showDestinationPresetRegistration(kind); return; }
        GoogleNavigationClient.rememberDestination(place.label, place.address());
        navigationDestinationInput.setText(place.label);
        launchSelectedGoogleMapsNavigation();
    }

    private void showDestinationPresetRegistration(String kind) {
        String title = "home".equals(kind) ? "自宅に登録" : "勤務先に登録";
        String label = navigationDestinationInput.getText().toString().trim();
        String coordinate = GoogleNavigationClient.selectedDestinationCoordinates(label);
        DestinationPresets.Place selected = null;
        if (!coordinate.isEmpty()) {
            String[] parts = coordinate.split(",", 2);
            selected = new DestinationPresets.Place(label, Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]), GoogleNavigationClient.selectedDestinationPlaceId(label));
        }
        DestinationPresets.Place here = null;
        try {
            Location location = getBestAvailableLocation();
            if (location != null && System.currentTimeMillis() - location.getTime() <= 180000)
                here = new DestinationPresets.Place("home".equals(kind) ? "自宅（登録地点）" : "勤務先（登録地点）",
                        location.getLatitude(), location.getLongitude(), "");
        } catch (Exception ignored) { }
        final DestinationPresets.Place selectedPlace = selected, currentPlace = here;
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.List<DestinationPresets.Place> options = new java.util.ArrayList<>();
        if (selectedPlace != null) { names.add("選択済みの検索結果：" + selectedPlace.label); options.add(selectedPlace); }
        if (currentPlace != null) { names.add("現在地を登録"); options.add(currentPlace); }
        if (options.isEmpty()) {
            new AlertDialog.Builder(this).setTitle(title)
                    .setMessage("先に目的地を検索して候補を選ぶか、GPSを取得してください。")
                    .setPositiveButton("OK", null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle(title)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    DestinationPresets.Place place = options.get(which);
                    if (DestinationPresets.save(this, kind, place)) {
                        GoogleNavigationClient.rememberDestination(place.label, place.address());
                        navigationDestinationInput.setText(place.label);
                        updateStatus(title, "保存しました。自宅・勤務先ボタンから案内を開始できます");
                    } else updateStatus(title, "保存できませんでした");
                }).setNegativeButton("キャンセル", null).show();
    }

    private void launchGoogleMapsNavigation() {
        Log.d("LokiGoogleNav", "map_start_clicked mode=" + navigationMode());
        final String query = normalizeNavigationDestination(navigationDestinationInput == null ? ""
                : navigationDestinationInput.getText().toString());
        if (query.isEmpty() || !GoogleNavigationClient.enabled(this)
                || !GoogleNavigationClient.selectedDestinationCoordinates(query).isEmpty()) {
            launchSelectedGoogleMapsNavigation();
            return;
        }
        updateStatus("目的地検索", "候補の住所・座標を確認しています");
        new Thread(() -> {
            try {
                final List<Address> choices = GoogleNavigationClient.searchPlaces(this, query);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (choices.isEmpty()) {
                        new AlertDialog.Builder(this).setTitle("目的地検索")
                                .setMessage("候補がありません。地域名を追加して再検索してください。")
                                .setPositiveButton("OK", null).show();
                        return;
                    }
                    String[] labels = new String[choices.size()];
                    for (int i = 0; i < labels.length; i++) labels[i] = choices.get(i).getAddressLine(0).replace('\n', ' ');
                    new android.app.AlertDialog.Builder(this).setTitle("目的地を選択")
                            .setItems(labels, (dialog, which) -> {
                                GoogleNavigationClient.rememberDestination(labels[which], choices.get(which));
                                navigationDestinationInput.setText(labels[which]);
                                launchSelectedGoogleMapsNavigation();
                            }).setNegativeButton("キャンセル", null).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    String reason = "google_places_local_limit".equals(error.getMessage())
                            ? "施設検索のアプリ内上限に達しました。時間をおいて再試行してください。"
                            : "候補取得に失敗しました。通信・API設定を確認して再試行してください。";
                    new AlertDialog.Builder(this).setTitle("目的地検索を開始できません")
                            .setMessage(reason).setPositiveButton("OK", null).show();
                });
            }
        }, "MapDestinationLookup").start();
    }

    private void launchSelectedGoogleMapsNavigation() {
        if ("transit".equals(navigationMode())) {
            new AlertDialog.Builder(this).setTitle("最寄り駅候補までの移動")
                    .setItems(new String[]{"徒歩", "自転車（経路なし時は徒歩）"}, (dialog, which) -> {
                        getPreferences().edit().putString("transit_access_mode", which == 1 ? "bicycling" : "walking").apply();
                        startSelectedGoogleMapsNavigation();
                    }).setNegativeButton("キャンセル", null).show();
            return;
        }
        startSelectedGoogleMapsNavigation();
    }

    private void startSelectedGoogleMapsNavigation() {
        navigationCompletedUntil = 0;
        navigationCompletedDestination = "";
        arrivalCandidateSince = 0;
        clearStoredNavigationRoute();
        noRouteTarget = ""; noRouteMode = "";
        navigationRouteRetryAfterMs = 0;
        try {
            Class.forName("com.example.rokidgeminisecretary.SdkPhoneBridge").getMethod("stopAndRelease").invoke(null);
        } catch (ClassNotFoundException manualBuild) { }
        catch (Exception failure) { updateStatus("ナビ切替", "SDKナビを停止できませんでした"); return; }
        String destination = navigationDestinationInput == null ? ""
                : navigationDestinationInput.getText().toString().trim();
        destination = normalizeNavigationDestination(destination);
        if (destination.length() == 0) {
            confirmedSharedDestination = "";
            confirmedSharedUntil = 0L;
            confirmedSharedNavigationSeen = false;
            clearStoredNavigationRoute();
            // Let Google Maps own destination and mode selection. Its
            // navigation notification still feeds the glasses HUD, while an
            // old value from this optional field can no longer be reused.
            setNavigationHudSuppressed(false);
            Intent maps = getPackageManager().getLaunchIntentForPackage(
                    "com.google.android.apps.maps");
            if (maps == null) {
                maps = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps"));
            }
            maps.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(maps);
            updateStatus("Googleマップ起動", "Googleマップで目的地と移動手段を選んでください");
            return;
        }
        if (navigationDestinationInput != null) navigationDestinationInput.setText(destination);
        // Explicit MAP input is as authoritative as a confirmed share. Maps
        // notifications frequently omit the destination, including bicycle trips.
        confirmedSharedDestination = destination;
        confirmedSharedUntil = System.currentTimeMillis() + 2L * 60L * 60L * 1000L;
        confirmedSharedNavigationSeen = false;
        setNavigationHudSuppressed(false);
        final String mode = navigationMode();
        prepareNavigationRouteAsync(destination, mode);
        // Preparing a different destination clears the old cache and its shared identity.
        // Restore this explicit UI selection AFTER that reset, not before it.
        confirmedSharedDestination = destination;
        confirmedSharedUntil = System.currentTimeMillis() + 2L * 60L * 60L * 1000L;
        confirmedSharedNavigationSeen = false;
        Log.d("LokiGoogleNav", "map_start mode=" + mode);
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
                    + "&destination=" + Uri.encode(GoogleNavigationClient.selectedDestinationCoordinates(destination).isEmpty()
                            ? destination : GoogleNavigationClient.selectedDestinationCoordinates(destination))
                    + (GoogleNavigationClient.selectedDestinationPlaceId(destination).isEmpty() ? ""
                            : "&destination_place_id=" + Uri.encode(GoogleNavigationClient.selectedDestinationPlaceId(destination)))
                    + "&travelmode=" + Uri.encode(mode)
                    );
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
                                        ));
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
        destination = destination.replaceFirst("(まで|へ|に行く|にいく)$", "");
        return destination.trim();
    }

    private String navigationMode() {
        String mode = getPreferences().getString(KEY_MAP_ROUTE_MODE, "walking");
        if (!"walking".equals(mode) && !"driving".equals(mode) && !"transit".equals(mode)
                && !"bicycling".equals(mode)) return "walking";
        return mode;
    }

    private String navigationModeLabel(String mode) {
        if ("transit".equals(mode)) return "公共交通";
        if ("driving".equals(mode)) return "車";
        if ("bicycling".equals(mode)) return "自転車";
        return "徒歩";
    }


    private synchronized void clearStoredNavigationRoute() {
        googleRouteGeneration++;
        transitAccess = null;
        confirmedSharedDestination = "";
        confirmedSharedUntil = 0L;
        confirmedSharedNavigationSeen = false;
        googleRouteCache = null;
        transitJourneyTarget = "";
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
        if (suppressed && transitAccess != null) clearStoredNavigationRoute();
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
        noRouteUntil = 0; navigationRouteRetryAfterMs = 0; // Explicit user request may retry immediately.
        prepareNavigationRouteAsync(destination, mode, true);
    }

    private void prepareNavigationRouteAsync(final String destination, final String mode,
                                             boolean clearExisting) {
        if (GoogleNavigationClient.enabled(this)) {
            prepareGoogleNavigationRoute(destination, mode, clearExisting);
            return;
        }
        if ("transit".equals(mode)) return;
        if (navigationRouteFetchInFlight) {
            if (clearExisting && !destination.equals(getPreferences().getString(KEY_MAP_ROUTE_DESTINATION, ""))) {
                clearStoredNavigationRoute();
                getPreferences().edit().putString(KEY_MAP_ROUTE_DESTINATION, destination).apply();
            }
            return;
        }
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
                    final Location origin = getBestAvailableLocation();
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
                    if (!destination.equals(getPreferences().getString(KEY_MAP_ROUTE_DESTINATION, ""))) return;
                    if (!mode.equals(navigationMode())) return;
                    getPreferences().edit()
                            .putString(KEY_MAP_ROUTE_POINTS, route.toString())
                            .putString("map_cached_mode", mode)
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

    private volatile String noRouteTarget = "", noRouteMode = "";
    private volatile long noRouteUntil;
    private volatile String desiredGoogleRouteMode = "";


    private synchronized void prepareGoogleNavigationRoute(final String destination, final String mode, boolean clear) {
        desiredGoogleRouteMode = mode;
        if (destination.trim().length() == 0) return;
        if (destination.equals(noRouteTarget) && mode.equals(noRouteMode)
                && System.currentTimeMillis() < noRouteUntil) return;
        if (clear && !destination.equals(getPreferences().getString(KEY_MAP_ROUTE_DESTINATION, ""))) {
            clearStoredNavigationRoute();
            getPreferences().edit().putString(KEY_MAP_ROUTE_DESTINATION, destination).apply();
        }
        if ("transit".equals(mode)) {
            transitJourneyTarget = destination;
            transitJourneyTargetUntil = System.currentTimeMillis() + MAP_ROUTE_CACHE_MS;
        }
        else if (clear) transitJourneyTarget = "";
        if ("transit".equals(mode) && transitAccess == null) {
            transitAccess = new TransitAccess(destination, GoogleNavigationClient.selectedDestinationCoordinates(destination),
                    getPreferences().getString("transit_access_mode", "walking"));
        }
        final TransitAccess access = "transit".equals(mode) ? transitAccess : null;
        final long generation = googleRouteGeneration;
        if (navigationRouteFetchInFlight) {
            // A new selection waits for the old worker, whose generation can no longer publish.
            healthHandler.postDelayed(() -> {
                if (generation == googleRouteGeneration && !isNavigationHudSuppressed())
                    prepareGoogleNavigationRoute(destination, mode, false);
            }, 500);
            return;
        }
        if (System.currentTimeMillis() < navigationRouteRetryAfterMs) return;
        // A recalculation starts a new route identity. The old geometry and its
        // maneuvers may not be combined with the replacement response.
        if (!clear && googleRouteCache != null) {
            googleRouteGeneration++;
            googleRouteCache = null;
        }
        final long requestGeneration = googleRouteGeneration;
        navigationRouteFetchInFlight = true;
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Location origin = getBestAvailableLocation();
                    if (origin == null || System.currentTimeMillis() - origin.getTime() > 180000)
                        throw new Exception("google_current_location_unavailable");
                    String legDestination = destination;
                    if (access != null) {
                        if (access.stationCoordinates.isEmpty()) {
                            android.location.Address station = GoogleNavigationClient.nearestStation(MainActivity.this,
                                    origin.getLatitude(), origin.getLongitude());
                            access.station = station.getFeatureName();
                            access.stationCoordinates = station.getLatitude() + "," + station.getLongitude();
                            GoogleNavigationClient.rememberDestination(access.stationCoordinates, station);
                            Log.i("LokiGoogleNav", "transit_station_resolved");
                        }
                        legDestination = access.stationCoordinates;
                    }
                    if (requestGeneration != googleRouteGeneration) return;
                    final String routeTarget = legDestination;
                    WalkingFallback.Result<JSONObject> fetched = WalkingFallback.fetch(access == null ? mode : access.mode,
                            new WalkingFallback.Request<JSONObject>() {
                                @Override public JSONObject fetch(String requestedMode) throws Exception {
                                    Log.d("LokiGoogleNav", "route_request mode=" + requestedMode);
                                    return GoogleNavigationClient.route(MainActivity.this,
                                            origin.getLatitude(), origin.getLongitude(), routeTarget, requestedMode);
                                }
                            });
                    JSONObject route = fetched.value;
                    TransitJourney journey = null;
                    if (access == null && !"driving".equals(mode)) {
                        try { journey = TransitJourney.parse(destination, route, System.currentTimeMillis()); }
                        catch (Exception stepError) {
                            Log.w("LokiGoogleNav", "journey_steps_unavailable_use_whole_route");
                        }
                    }
                    double[][] points = GooglePolyline.decode(route.getJSONObject("polyline").getString("encodedPolyline"));
                    if (points.length < 2) throw new Exception("google_route_parse_error");
                    JSONArray retained = new JSONArray();
                    for (int index : RouteGeometry.indices(points, MAP_ROUTE_MAX_POINTS))
                        retained.put(new JSONArray().put(points[index][0]).put(points[index][1]));
                    JSONArray maneuvers = new JSONArray();
                    JSONArray legs = route.optJSONArray("legs");
                    if (legs != null) for (int i = 0; i < legs.length(); i++) {
                        JSONObject routeLeg = legs.optJSONObject(i);
                        JSONArray steps = routeLeg == null ? null : routeLeg.optJSONArray("steps");
                        if (steps == null) continue;
                        for (int j = 0; j < steps.length(); j++) {
                            JSONObject step = steps.optJSONObject(j);
                            JSONObject start = step == null ? null : step.optJSONObject("startLocation");
                            JSONObject point = start == null ? null : start.optJSONObject("latLng");
                            if (point == null) continue;
                            double lat = point.optDouble("latitude", Double.NaN), lng = point.optDouble("longitude", Double.NaN);
                            if (!Double.isFinite(lat) || !Double.isFinite(lng)) continue;
                            int nearest = 0; double best = Double.MAX_VALUE;
                            for (int k = 0; k < retained.length(); k++) {
                                JSONArray p = retained.getJSONArray(k);
                                double distance = coordinateDistance(lat, lng, p.getDouble(0), p.getDouble(1));
                                if (distance < best) { best = distance; nearest = k; }
                            }
                            JSONObject instruction = step.optJSONObject("navigationInstruction");
                            maneuvers.put(new JSONObject().put("latitude", lat).put("longitude", lng)
                                    .put("progress", nearest / (double)(retained.length() - 1))
                                    .put("type", "google").put("modifier", "")
                                    .put("name", "").put("instruction", instruction == null ? "" : instruction.optString("instructions", ""))
                                    .put("distance", step.optDouble("distanceMeters", 0))
                                    .put("duration", googleDuration(step.optString("staticDuration", "0s"))));
                        }
                    }
                    if (requestGeneration != googleRouteGeneration || !destination.equals(getPreferences().getString(KEY_MAP_ROUTE_DESTINATION, "")) || !mode.equals(desiredGoogleRouteMode)) return;
                    GoogleRouteCache readyCache = new GoogleRouteCache(new NavigationRouteData(retained, maneuvers,
                            route.optDouble("distanceMeters", -1), googleDuration(route.optString("duration", ""))), destination, mode, fetched.mode, journey, requestGeneration);
                    readyCache.scheduledDeparture = route.optString("lokiScheduledDeparture", "");
                    synchronized (MainActivity.this) {
                        if (requestGeneration != googleRouteGeneration) return;
                        googleRouteCache = readyCache;
                        if (access != null) { access.error = ""; access.fetching = false; }
                    }
                    noRouteUntil = 0;
                    getPreferences().edit().remove(KEY_MAP_ROUTE_ERROR).apply();
                    navigationRouteRetryAfterMs = 0;
                    Log.i("LokiGoogleNav", "route ready points=" + retained.length() + " mode=" + mode + " effective=" + fetched.mode);
                } catch (Exception error) {
                    if (requestGeneration != googleRouteGeneration || !destination.equals(getPreferences().getString(KEY_MAP_ROUTE_DESTINATION, ""))
                            || !mode.equals(desiredGoogleRouteMode)) return;
                    navigationRouteRetryAfterMs = System.currentTimeMillis() + 60000;
                    String reason = error.getMessage();
                    if (reason == null || !reason.matches("google_[a-z_0-9]+")) reason = "google_route_parse_error";
                    if ("google_route_not_found".equals(reason)) {
                        noRouteTarget = destination; noRouteMode = mode;
                        noRouteUntil = System.currentTimeMillis() + 5L * 60L * 1000L;
                    }
                    if (access != null) { access.error = reason; access.fetching = false; }
                    getPreferences().edit().putString(KEY_MAP_ROUTE_ERROR, reason).apply();
                    Log.w("LokiGoogleNav", reason);
                } finally { navigationRouteFetchInFlight = false; }
            }
        }, "GoogleNavigationRoute").start();
    }

    private static double googleDuration(String value) {
        try { return Double.parseDouble(value.replace("s", "")); }
        catch (Exception ignored) { return -1; }
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
        double[][] geometryPoints = new double[count][2];
        for (int i = 0; i < count; i++) {
            geometryPoints[i][0] = coordinatesJson.getJSONArray(i).getDouble(1);
            geometryPoints[i][1] = coordinatesJson.getJSONArray(i).getDouble(0);
        }
        for (int index : RouteGeometry.indices(geometryPoints, MAP_ROUTE_MAX_POINTS)) {
            JSONArray coordinate = coordinatesJson.optJSONArray(index);
            if (coordinate == null || coordinate.length() < 2) continue;
            JSONArray point = new JSONArray();
            point.put(coordinate.optDouble(1));
            point.put(coordinate.optDouble(0));
            result.put(point);
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
                    int routeIndex = 0;
                    double nearest = Double.MAX_VALUE;
                    for (int pointIndex = 0; pointIndex < result.length(); pointIndex++) {
                        JSONArray retained = result.getJSONArray(pointIndex);
                        double d = coordinateDistance(latitude, longitude,
                                retained.getDouble(0), retained.getDouble(1));
                        if (d < nearest) { nearest = d; routeIndex = pointIndex; }
                    }
                    JSONObject item = new JSONObject();
                    item.put("latitude", latitude);
                    item.put("longitude", longitude);
                    item.put("progress", result.length() <= 1 ? 1.0
                            : routeIndex / (double) (result.length() - 1));
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

    private void showImportedMemoryMenu() {
        new AlertDialog.Builder(this).setTitle("Geminiの参考記憶（自動同期ではありません）")
                .setItems(new String[]{"会話を貼り付けて取り込む", "保存した記憶を確認・削除"},
                        new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) showImportedMemoryInput();
                            else showImportedMemoryList();
                        }}).setNegativeButton("閉じる", null).show();
    }

    private void showImportedMemoryInput() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 8, 20, 8);
        TextView help = new TextView(this);
        help.setText("1往復ずつ貼り付けてください。原文は保存・外部送信せず、短い抜粋を確認して保存します。AIによる自動要約ではありません。同じ会話の再登録は防ぎます。");
        layout.addView(help);
        final EditText user = new EditText(this), assistant = new EditText(this);
        user.setHint("あなたの発言（必須）");
        assistant.setHint("Geminiの回答（任意）");
        for (EditText input : new EditText[]{user, assistant}) {
            input.setMinLines(3); input.setMaxLines(5); input.setGravity(Gravity.TOP);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12000)});
            layout.addView(input);
        }
        ScrollView scroll = new ScrollView(this); scroll.addView(layout);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("会話を取り込む")
                .setView(scroll).setPositiveButton("抜粋を確認", null).setNegativeButton("キャンセル", null).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { public void onShow(DialogInterface d) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            String u = user.getText().toString().trim(), a = assistant.getText().toString().trim();
            if (u.isEmpty()) { user.setError("本人の発言を入力してください"); return; }
            try {
                String id = ImportedMemory.identity(u, a);
                if (ImportedMemory.contains(MainActivity.this, id)) {
                    android.widget.Toast.makeText(MainActivity.this, "この会話は取り込み済みです", 1).show(); return;
                }
                showImportedMemoryPreview(id, ImportedMemory.excerpt(u, a));
                dialog.dismiss();
            } catch (Exception error) { updateStatus("取り込み失敗", "保存データを確認できませんでした"); }
        }}); }});
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void showImportedMemoryPreview(final String id, String excerpt) {
        showImportedMemoryPreview(id, excerpt, false, false);
    }

    private void showImportedMemoryPreview(final String id, final String excerpt, boolean wasImportant, final boolean editing) {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 8, 20, 8);
        TextView help = new TextView(this);
        help.setText("残したい事実・前提だけに編集してください（600文字以内）。提案や選択肢を決定事項にしないでください。回答に使う際はGemini APIへ送信されます。");
        layout.addView(help);
        final EditText summary = new EditText(this);
        summary.setMinLines(5); summary.setMaxLines(10); summary.setGravity(Gravity.TOP);
        summary.setFilters(new InputFilter[]{new InputFilter.LengthFilter(600)}); summary.setText(excerpt);
        layout.addView(summary);
        final android.widget.CheckBox important = new android.widget.CheckBox(this);
        important.setChecked(wasImportant);
        important.setText("重要な会話（関連する質問で優先。常時送信はしません）"); layout.addView(important);
        ScrollView scroll = new ScrollView(this); scroll.addView(layout);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("保存する記憶を確認")
                .setView(scroll).setPositiveButton("確認して保存", null).setNegativeButton("キャンセル", null).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { public void onShow(DialogInterface d) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            try {
                boolean saved;
                if (editing) {
                    ImportedMemory.update(MainActivity.this, id, excerpt, summary.getText().toString(), important.isChecked());
                    saved = true;
                } else {
                    saved = ImportedMemory.save(MainActivity.this, id, summary.getText().toString(), important.isChecked());
                }
                updateStatus(saved ? "参考記憶を保存しました" : "取り込み済みです", "カスタム指示とは分けて保存しています。");
                android.widget.Toast.makeText(MainActivity.this, saved ? "参考記憶を保存しました" : "取り込み済みです", 1).show();
                dialog.dismiss();
            } catch (Exception error) { summary.setError(error.getMessage()); }
        }}); }});
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void showImportedMemoryList() {
        try {
            final JSONArray all = ImportedMemory.read(this);
            String[] labels = new String[all.length()];
            for (int i = 0; i < all.length(); i++) labels[i] =
                    (all.getJSONObject(i).optBoolean("important") ? "★ " : "")
                    + shortText(all.getJSONObject(i).optString("summary"), 60);
            if (labels.length == 0) { android.widget.Toast.makeText(this, "保存した参考記憶はありません", 1).show(); return; }
            new AlertDialog.Builder(this).setTitle("保存した参考記憶")
                    .setItems(labels, new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int which) {
                        final JSONObject entry = all.optJSONObject(which);
                        new AlertDialog.Builder(MainActivity.this).setTitle("参考記憶")
                                .setMessage(entry.optString("summary"))
                                .setPositiveButton("閉じる", null)
                                .setNeutralButton("編集・重要指定", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dd, int w) {
                                    showImportedMemoryPreview(entry.optString("id"), entry.optString("summary"), entry.optBoolean("important"), true);
                                }})
                                .setNegativeButton("削除", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dd, int w) {
                                    try { ImportedMemory.remove(MainActivity.this, entry.optString("id")); showImportedMemoryList(); }
                                    catch (Exception e) { updateStatus("削除失敗", "記憶を削除できませんでした"); }
                                }}).show();
                    }}).setNegativeButton("閉じる", null).show();
        } catch (Exception error) { updateStatus("読み込み失敗", "参考記憶を読み込めませんでした"); }
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
        customEditorBase = cached == null ? "" : cached;
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
                                if (!customEditorBase.equals(getPreferences().getString(KEY_CUSTOM, ""))) {
                                    customInfo.setText("編集中に指示が同期されました。入力をコピーしてから開き直し、統合してください。未保存の入力は残しています。");
                                    return;
                                }
                                saveCustomInstructionsFromPhone(customInput.getText().toString());
                                refreshCustomInfo();
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
        addAiLog("カスタム指示を更新 (" + text.length() + "文字)");
        updateStatus("指示を保存・同期待ち", "スマホには保存済みです。グラスからの受領確認を待っています。");
    }

    private void refreshCustomInfo() {
        if (customEditorDialog == null || !customEditorDialog.isShowing()
                || customInfo == null || customInput == null) {
            return;
        }
        customInfo.setText(getPreferences().getBoolean(KEY_CUSTOM_DIRTY, false)
                ? "スマホに保存済み・グラスへの同期待ち…（閉じても同期は続きます）"
                : customStateWaiting
                ? "グラスから現在の指示を読み込み中…"
                : "グラスと同期済み。編集後に「グラスへ反映」を押してください。");
        if (!customEditorDirty) {
            String saved = getPreferences().getString(KEY_CUSTOM, "");
            customEditorBase = saved == null ? "" : saved;
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
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
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
            boolean pcPair = request != null && request.startsWith("GET /pc_pair ");
            boolean pair = pcPair || (request != null && request.startsWith("GET /pair "));
            RequestPayload payload = readRequestPayload(reader);
            String bodyText = payload.body;
            if (pair) {
                if (System.currentTimeMillis() > (pcPair ? pcPairingUntilMs : pairingUntilMs)) {
                    writeJsonResponse(socket, 403, "{\"ok\":false,\"error\":\"pairing_closed\"}");
                    return;
                }
                if (pcPair) pcPairingUntilMs = 0L;
                else pairingUntilMs = 0L;
                JSONObject paired = new JSONObject();
                paired.put("ok", true);
                paired.put("token", getPreferences().getString(KEY_BRIDGE_TOKEN, ""));
                writeJsonResponse(socket, 200, paired.toString());
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        updateStatus(pcPair ? "PC接続できました" : "ペアリング完了",
                                pcPair ? "PCの認証が完了しました。指示の同期完了とは別の状態です。"
                                        : "グラスへ認証情報を安全に転送しました。");
                        android.widget.Toast.makeText(MainActivity.this,
                                pcPair ? "PC接続できました" : "グラスとペアリングしました",
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }
            if (!constantTimeEquals(getPreferences().getString(KEY_BRIDGE_TOKEN, ""), payload.token)) {
                writeJsonResponse(socket, 401, "{\"ok\":false,\"error\":\"unauthorized\"}");
                return;
            }
            if (request.startsWith("GET /google_tile?")) {
                try {
                    JSONObject tile = GoogleNavigationClient.tile(this,
                            Integer.parseInt(queryParam(request, "z")), Integer.parseInt(queryParam(request, "x")),
                            Integer.parseInt(queryParam(request, "y")));
                    writeJsonResponse(socket, 200, tile.toString());
                } catch (Exception error) {
                    // Application error, not a dead LAN endpoint: prevent retries through every host alias.
                    writeJsonResponse(socket, 200, "{\"ok\":false,\"error\":\"google_tile_unavailable\"}");
                    String reason = error.getMessage();
                    Log.w("LokiGoogleNav", reason != null && reason.matches("google_[a-z_0-9]+") ? reason : "google_tile_unavailable");
                }
                return;
            }
            if (request.startsWith("GET /custom_snapshot")) {
                JSONObject snapshot = new JSONObject();
                snapshot.put("custom", getPreferences().getString(KEY_CUSTOM, ""));
                snapshot.put("pending", getPreferences().getBoolean(KEY_CUSTOM_DIRTY, false));
                snapshot.put("glassAudit", new JSONObject(getPreferences().getString("custom_glass_audit", "{}")));
                synchronized (MainActivity.class) { pendingCustomStateRequest = true; }
                writeJsonResponse(socket, 200, snapshot.toString());
                return;
            }
            if (request.startsWith("GET /imported_memory?")) {
                writeJsonResponse(socket, 200, ImportedMemory.search(this,
                        shortText(parseStringQuery(request, "q", ""), 300)).toString());
                return;
            }
            if (request.startsWith("GET /navigation_action")) {
                writeJsonResponse(socket, 200, performNavigationAction(request).toString());
                return;
            }
            if (request.startsWith("GET /desktop_state")) {
                ArrayList<JSONObject> history = readConversationMemory();
                JSONArray recent = new JSONArray();
                for (int index = Math.max(0, history.size() - 20); index < history.size(); index++) {
                    recent.put(history.get(index));
                }
                JSONObject desktop = new JSONObject().put("ok", true).put("entries", recent)
                        .put("state", glassRuntimeState).put("message", glassRuntimeMessage)
                        .put("waitMs", Math.max(0L, glassWaitUntilMs - System.currentTimeMillis()));
                writeJsonResponse(socket, 200, desktop.toString());
                return;
            }
            if (request.startsWith("POST /custom_edit")) {
                JSONObject edit = new JSONObject(bodyText);
                String replacement = edit.getString("custom");
                synchronized (MainActivity.class) {
                    if (!edit.optString("base", "").equals(getPreferences().getString(KEY_CUSTOM, ""))) {
                        writeJsonResponse(socket, 409, "{\"error\":\"指示が更新されています。再読込してください\"}");
                        return;
                    }
                    if (replacement.length() > MAX_CUSTOM_CHARS) {
                        writeJsonResponse(socket, 400, "{\"error\":\"指示は12000文字までです\"}");
                        return;
                    }
                    getPreferences().edit().putString(KEY_CUSTOM, replacement)
                            .putBoolean(KEY_CUSTOM_DIRTY, true).apply();
                    pendingCustomInstructions = replacement;
                    pendingCustomUpdate = true;
                }
                writeJsonResponse(socket, 200, "{\"ok\":true,\"pending\":true}");
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
                    : weather ? buildRequestedWeatherJson(parseIntQuery(request, "offset", 0),
                            parseStringQuery(request, "q", "")).toString()
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
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("active", active);
        synchronized (AMBIENT_RELAY_LOCK) {
            String storedQueue = preferences.getString(KEY_AMBIENT_RELAY_QUEUE, "");
            int storedCount = 0;
            if (storedQueue != null && storedQueue.trim().length() > 0) {
                try {
                    storedCount = new JSONArray(storedQueue).length();
                } catch (Exception ignored) {
                    storedCount = -1;
                }
            }
            ArrayList<JSONObject> queue = loadAmbientRelayQueue(preferences, now);
            if (storedCount != queue.size()) {
                // This is also the expiry cleanup. Recognized speech remains a
                // short-lived delivery queue, not a permanent transcript log.
                saveAmbientRelayQueue(preferences, queue);
            }
            long id = 0L;
            long at = 0L;
            String source = "Bluetooth";
            for (int i = 0; i < queue.size(); i++) {
                JSONObject entry = queue.get(i);
                id = Math.max(id, entry.optLong("id", 0L));
                if (entry.optLong("at", 0L) >= at) {
                    at = entry.optLong("at", 0L);
                    source = entry.optString("source", "Bluetooth");
                }
            }
            String transcript = mergeAmbientRelayTranscripts(queue);
            root.put("id", id);
            root.put("at", at);
            root.put("source", source.length() == 0 ? "Bluetooth" : source);
            root.put("transcript", transcript);
            root.put("count", queue.size());
        }
        return root;
    }

    private JSONObject buildAckAmbientPlaybackJson(long id) throws Exception {
        SharedPreferences preferences = getPreferences();
        boolean acked = false;
        synchronized (AMBIENT_RELAY_LOCK) {
            ArrayList<JSONObject> queue = loadAmbientRelayQueue(
                    preferences, System.currentTimeMillis());
            ArrayList<JSONObject> remaining = new ArrayList<JSONObject>();
            for (int i = 0; i < queue.size(); i++) {
                JSONObject entry = queue.get(i);
                long entryId = entry.optLong("id", 0L);
                if (id > 0L && entryId > 0L && entryId <= id) {
                    acked = true;
                } else {
                    remaining.add(entry);
                }
            }
            saveAmbientRelayQueue(preferences, remaining);
            long currentId = preferences.getLong(KEY_AMBIENT_RELAY_ID, 0L);
            if (id > 0L && currentId > 0L && currentId <= id) {
                preferences.edit()
                        .remove(KEY_AMBIENT_RELAY_TRANSCRIPT)
                        .remove(KEY_AMBIENT_RELAY_SOURCE)
                        .remove(KEY_AMBIENT_RELAY_ID)
                        .remove(KEY_AMBIENT_RELAY_AT)
                        .apply();
                acked = true;
            }
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("acked", acked);
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
        String relaySource = source == null || source.trim().length() == 0
                ? "Bluetooth" : source.trim();
        int pendingCount;
        synchronized (AMBIENT_RELAY_LOCK) {
            SharedPreferences preferences = getPreferences();
            ArrayList<JSONObject> queue = loadAmbientRelayQueue(preferences, now);
            long latestId = preferences.getLong(KEY_AMBIENT_RELAY_ID, 0L);
            for (int i = 0; i < queue.size(); i++) {
                latestId = Math.max(latestId, queue.get(i).optLong("id", 0L));
            }
            long id = Math.max(now, latestId + 1L);
            JSONObject entry = new JSONObject();
            try {
                entry.put("id", id);
                entry.put("at", now);
                entry.put("source", relaySource);
                entry.put("transcript", transcript);
            } catch (Exception error) {
                Log.w(TAG, "cannot queue ambient relay transcript", error);
                return;
            }
            queue.add(entry);
            while (queue.size() > AMBIENT_RELAY_QUEUE_LIMIT) {
                queue.remove(0);
            }
            saveAmbientRelayQueue(preferences, queue);
            // Keep the legacy single-item fields for compatibility with an
            // older glasses build. The queue is authoritative for this build.
            preferences.edit()
                    .putString(KEY_AMBIENT_RELAY_TRANSCRIPT, transcript)
                    .putString(KEY_AMBIENT_RELAY_SOURCE, relaySource)
                    .putLong(KEY_AMBIENT_RELAY_ID, id)
                    .putLong(KEY_AMBIENT_RELAY_AT, now)
                    .putBoolean(KEY_AMBIENT_SOURCE_ACTIVE, true)
                    .putLong(KEY_AMBIENT_SOURCE_SEEN_AT, now)
                    .apply();
            pendingCount = queue.size();
        }
        Log.i(TAG, "ambient relay transcript queued chars=" + transcript.length()
                + " pending=" + pendingCount);
    }

    private ArrayList<JSONObject> loadAmbientRelayQueue(
            SharedPreferences preferences, long now) {
        ArrayList<JSONObject> result = new ArrayList<JSONObject>();
        String stored = preferences.getString(KEY_AMBIENT_RELAY_QUEUE, "");
        if (stored != null && stored.trim().length() > 0) {
            try {
                JSONArray array = new JSONArray(stored);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject entry = array.optJSONObject(i);
                    if (entry == null) continue;
                    long id = entry.optLong("id", 0L);
                    long at = entry.optLong("at", 0L);
                    String transcript = entry.optString("transcript", "").trim();
                    if (id > 0L && transcript.length() > 0
                            && now - at <= AMBIENT_RELAY_RETENTION_MS) {
                        result.add(entry);
                    }
                }
            } catch (Exception error) {
                Log.w(TAG, "ambient relay queue was invalid; rebuilding", error);
            }
        }
        long legacyId = preferences.getLong(KEY_AMBIENT_RELAY_ID, 0L);
        long legacyAt = preferences.getLong(KEY_AMBIENT_RELAY_AT, 0L);
        String legacyTranscript = preferences.getString(
                KEY_AMBIENT_RELAY_TRANSCRIPT, "");
        boolean alreadyQueued = false;
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).optLong("id", 0L) == legacyId) {
                alreadyQueued = true;
                break;
            }
        }
        if (!alreadyQueued && legacyId > 0L && legacyTranscript != null
                && legacyTranscript.trim().length() > 0
                && now - legacyAt <= AMBIENT_RELAY_RETENTION_MS) {
            JSONObject legacy = new JSONObject();
            try {
                legacy.put("id", legacyId);
                legacy.put("at", legacyAt);
                legacy.put("source", preferences.getString(
                        KEY_AMBIENT_RELAY_SOURCE, "Bluetooth"));
                legacy.put("transcript", legacyTranscript.trim());
                result.add(legacy);
            } catch (Exception error) {
                Log.w(TAG, "cannot migrate legacy ambient relay item", error);
            }
        } else if (legacyId > 0L
                && now - legacyAt > AMBIENT_RELAY_RETENTION_MS) {
            preferences.edit()
                    .remove(KEY_AMBIENT_RELAY_TRANSCRIPT)
                    .remove(KEY_AMBIENT_RELAY_SOURCE)
                    .remove(KEY_AMBIENT_RELAY_ID)
                    .remove(KEY_AMBIENT_RELAY_AT)
                    .apply();
        }
        while (result.size() > AMBIENT_RELAY_QUEUE_LIMIT) {
            result.remove(0);
        }
        return result;
    }

    private void saveAmbientRelayQueue(SharedPreferences preferences,
            ArrayList<JSONObject> queue) {
        JSONArray array = new JSONArray();
        if (queue != null) {
            for (int i = 0; i < queue.size(); i++) {
                array.put(queue.get(i));
            }
        }
        if (array.length() == 0) {
            preferences.edit().remove(KEY_AMBIENT_RELAY_QUEUE).apply();
        } else {
            preferences.edit().putString(
                    KEY_AMBIENT_RELAY_QUEUE, array.toString()).apply();
        }
    }

    private String mergeAmbientRelayTranscripts(ArrayList<JSONObject> queue) {
        ArrayList<String> segments = new ArrayList<String>();
        if (queue != null) {
            for (int i = 0; i < queue.size(); i++) {
                String candidate = queue.get(i).optString(
                        "transcript", "").trim();
                if (candidate.length() == 0) continue;
                if (segments.isEmpty()) {
                    segments.add(candidate);
                    continue;
                }
                int lastIndex = segments.size() - 1;
                String previous = segments.get(lastIndex);
                String previousKey = normalizeAmbientRelayMergeKey(previous);
                String candidateKey = normalizeAmbientRelayMergeKey(candidate);
                if (candidateKey.length() == 0 || candidateKey.equals(previousKey)
                        || previousKey.contains(candidateKey)) {
                    continue;
                }
                if (candidateKey.contains(previousKey)) {
                    segments.set(lastIndex, candidate);
                } else {
                    segments.add(candidate);
                }
            }
        }
        while (segments.size() > 1
                && ambientRelaySegmentChars(segments) > AMBIENT_RELAY_BATCH_CHARS) {
            segments.remove(0);
        }
        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            if (merged.length() > 0 && !endsWithRelaySentenceMark(merged)) {
                merged.append('。');
            }
            merged.append(segment);
        }
        if (merged.length() > AMBIENT_RELAY_BATCH_CHARS) {
            return merged.substring(merged.length() - AMBIENT_RELAY_BATCH_CHARS);
        }
        return merged.toString();
    }

    private int ambientRelaySegmentChars(ArrayList<String> segments) {
        int chars = 0;
        for (int i = 0; i < segments.size(); i++) {
            chars += segments.get(i).length() + 1;
        }
        return chars;
    }

    private String normalizeAmbientRelayMergeKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.JAPAN)
                .replaceAll("[\\s　、。,.!！?？…〜～]+", "");
    }

    private boolean endsWithRelaySentenceMark(CharSequence value) {
        if (value == null || value.length() == 0) return true;
        char last = value.charAt(value.length() - 1);
        return last == '。' || last == '！' || last == '？'
                || last == '!' || last == '?' || last == '、' || last == ',';
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
        if (offset != contentLength) throw new IllegalArgumentException("incomplete request body");
        return new RequestPayload(new String(new String(chars, 0, offset)
                .getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8), token);
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
            if (queue.size() >= MAX_PENDING_COMMANDS) return false;
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
        boolean queued = false;
        if (command.length() > 0 && !duplicateCodexNotification) {
            queued = queueBridgeCommand(this, command, true);
            Log.i(TAG, "remote command appended chars=" + command.length());
        }
        JSONObject root = new JSONObject();
        root.put("ok", true);
        root.put("queued", queued);
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

    private void showGeminiKeySettings() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("APIキーを貼り付け（保存済みの値は表示しません）");
        input.setSaveEnabled(false);
        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Gemini APIキー設定")
                .setMessage("スマホに保存し、接続中のグラスへ自動同期します。空欄では変更しません。")
                .setView(input).setNegativeButton("閉じる", null)
                .setPositiveButton("保存して同期", null).create();
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
          public void onShow(android.content.DialogInterface ignored) {
           dialog.getButton(-1).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            String key = MapsKeyInput.normalize(input.getText().toString());
            if (!MapsKeyInput.canSend(key)) {
                input.setError("キーのみを貼り付けてください（空欄・途中の空白は不可）");
                return;
            }
            try {
                GeminiCredentialStore.save(MainActivity.this, key);
                input.setText("");
                updateStatus("Geminiキー保存済み", "グラスへの同期は接続後に自動実行されます");
                dialog.dismiss();
            } catch (Exception error) {
                input.setError("保存できませんでした。もう一度お試しください。");
            }
            }
           });
          }
        });
        dialog.show();
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
        // Delivery is acknowledged by POST /custom_state with matching content,
        // not by serving a GET which may never arrive at the glasses.
        root.put("ok", true);
        root.put("custom", custom == null ? "" : custom);
        root.put("hasUpdate", hasUpdate);
        root.put("requestState", requestState);
        root.put("current", getPreferences().getString(KEY_CUSTOM, ""));
        // This response is behind the existing paired bridge-token check.
        try {
            String key = GeminiCredentialStore.read(this);
            if (!key.isEmpty()) root.put("geminiApiKey", key);
        } catch (Exception unavailable) {
            // A keystore failure must not disable the rest of the bridge.
        }
        return root;
    }

    private JSONObject buildPostCustomStateResult(String bodyText) throws Exception {
        String auditText = formValue(bodyText == null ? "" : bodyText, "audit");
        if (auditText != null && auditText.length() > 0 && auditText.length() < 4096) {
            JSONObject audit = new JSONObject(auditText);
            // Diagnostics contain only counts, never instruction text or credentials.
            JSONObject safe = new JSONObject();
            safe.put("receivedAt", System.currentTimeMillis());
            safe.put("currentChars", audit.optInt("currentChars", -1));
            for (String source : new String[]{"glass", "phone"}) {
                safe.put(source + "BackupPresent", audit.optBoolean(source + "BackupPresent"));
                safe.put(source + "OriginalLines", audit.optInt(source + "OriginalLines", -1));
                safe.put(source + "MissingExactLines", audit.optInt(source + "MissingExactLines", -1));
            }
            getPreferences().edit().putString("custom_glass_audit", safe.toString()).apply();
        }
        String custom = "";
        if (bodyText != null && bodyText.trim().length() > 0) {
            if (bodyText.trim().startsWith("{")) {
                custom = new JSONObject(bodyText).optString("custom", "");
            } else {
                custom = formValue(bodyText, "custom");
            }
        }
        final String syncedCustom = custom == null ? "" : custom;
        if ("true".equals(formValue(bodyText == null ? "" : bodyText, "migration"))
                && !getPreferences().getBoolean("custom_merge_v2", false)) {
            getPreferences().edit()
                    .putString("custom_backup_phone_v2", getPreferences().getString(KEY_CUSTOM, ""))
                    .putBoolean("custom_merge_v2", true).remove(KEY_CUSTOM_DIRTY).apply();
            synchronized (MainActivity.class) {
                pendingCustomUpdate = false;
                pendingCustomInstructions = "";
            }
        }
        boolean localEditPending = getPreferences().getBoolean(KEY_CUSTOM_DIRTY, false);
        String previousCustom = getPreferences().getString(KEY_CUSTOM, "");
        final boolean acknowledgedEdit = localEditPending && previousCustom.equals(syncedCustom);
        if (localEditPending && previousCustom.equals(syncedCustom)) {
            getPreferences().edit().remove(KEY_CUSTOM_DIRTY).apply();
            localEditPending = false;
        }
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
                    if (acknowledgedEdit) {
                        updateStatus("指示の同期完了", "グラスとスマホの指示が一致しました。");
                        android.widget.Toast.makeText(MainActivity.this,
                                "指示をグラスへ反映しました", android.widget.Toast.LENGTH_LONG).show();
                    }
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
        try {
            JSONObject sdk = (JSONObject) Class.forName("com.example.rokidgeminisecretary.SdkPhoneBridge")
                    .getMethod("snapshot").invoke(null);
            if (sdk != null) {
                JSONObject top = TransitLocationTracker.recentTransitJson(this);
                sdk.put("topCompact", top.optString("compact", ""));
                sdk.put("topTime", top.optLong("time", 0L));
                boolean suppressed = isNavigationHudSuppressed();
                sdk.put("suppressed", suppressed);
                boolean active = sdk.optBoolean("navigationActive", false) && !suppressed;
                sdk.put("navigationActive", active);
                if (!active) sdk.put("sdkLaneText", "");
                TransitLocationTracker.setNavigationActive(active);
                Location location = getBestAvailableLocation();
                if (location != null) {
                    sdk.put("latitude", location.getLatitude()).put("longitude", location.getLongitude())
                            .put("locationTime", location.getTime()).put("accuracy", location.hasAccuracy() ? location.getAccuracy() : -1)
                            .put("bearing", location.hasBearing() ? location.getBearing() : -1)
                            .put("speed", location.hasSpeed() ? location.getSpeed() : -1);
                    if (active) sdk.put("trafficSignals", SignalLocations.snapshot(location.getLatitude(), location.getLongitude()));
                }
                sdk.put("mapShare", new JSONObject().put("active", false));
                Log.d("LokiLaneTx", "bridge seq=" + sdk.optLong("sdkLaneSequence", 0)
                        + " active=" + active + " suppressed=" + suppressed
                        + " chars=" + sdk.optString("sdkLaneText").length()
                        + " ttl=" + sdk.optLong("sdkLaneTtlMs", 0));
                return sdk;
            }
        } catch (ClassNotFoundException manualBuild) { }
        catch (Exception failure) {
            return new JSONObject().put("ok", true).put("source", "navigation_sdk")
                    .put("navigationActive", false).put("time", System.currentTimeMillis());
        }
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
        String reportedDestination = result.optString("destination", "").trim();
        if (!navigationCompletedDestination.isEmpty()
                && (reportedDestination.isEmpty() || navigationCompletedDestination.equals(reportedDestination))) {
            result.put("navigationActive", false).put("routeReady", false).put("route", new JSONArray());
            if (System.currentTimeMillis() < navigationCompletedUntil)
                result.put("navigationComplete", true).put("completionId", "maps-" + googleRouteGeneration);
            return result;
        }
        // An explicit in-app start must not wait for Maps to publish its first notification.
        boolean localStart = !navigationActive && !confirmedSharedNavigationSeen
                && confirmedSharedDestination.length() > 0 && System.currentTimeMillis() < confirmedSharedUntil
                && !isNavigationHudSuppressed();
        if (localStart) {
            navigationActive = true;
            result.put("navigationActive", true).put("ok", true).put("time", System.currentTimeMillis());
            result.put("destination", confirmedSharedDestination).put("instruction", "経路を確認中");
        }
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
            result.put("trafficSignals", SignalLocations.snapshot(location.getLatitude(), location.getLongitude()));
            String currentRoad = cachedCurrentRoad(location);
            if (currentRoad.length() > 0) {
                result.put("currentRoad", currentRoad);
            }
            refreshCurrentRoadAsync(location);
        }
        SharedPreferences preferences = getPreferences();
        boolean googleMap = GoogleNavigationClient.enabled(this);
        GoogleRouteCache googleCached = googleRouteCache;
        if (googleCached != null && googleCached.generation != googleRouteGeneration) googleCached = null;
        TransitAccess access = transitAccess;
        if (access != null && !isNavigationHudSuppressed()) {
            boolean ready = googleCached != null && googleCached.destination.equals(access.destination)
                    && "transit".equals(googleCached.mode);
            access.apply(result, ready ? googleCached.data.route : null,
                    ready ? googleCached.effectiveMode : access.mode, ready ? googleCached.data.durationSeconds : -1);
            TransitLocationTracker.setNavigationActive(true);
            result.put("suppressed", false);
            return result;
        }
        long routeTime = preferences.getLong(KEY_MAP_ROUTE_TIME, 0L);
        String routeText = preferences.getString(KEY_MAP_ROUTE_POINTS, "");
        String maneuverText = preferences.getString(KEY_MAP_ROUTE_MANEUVERS, "");
        String routeDestination = cleanNavigationLabel(
                preferences.getString(KEY_MAP_ROUTE_DESTINATION, ""));
        String mapsDestination = result.optString("destination", "").trim();
        // The explicitly selected final destination owns the Transit journey.
        // Access-leg notifications may name a station or report cycling; neither
        // may replace the full TRANSIT request (whose access/egress is walking).
        boolean explicitTransit = "transit".equals(navigationMode())
                && !confirmedSharedDestination.isEmpty() && System.currentTimeMillis() < confirmedSharedUntil;
        if (explicitTransit && navigationActive) {
            mapsDestination = confirmedSharedDestination;
            result.put("destination", mapsDestination).put("accessMode", "walking");
        }
        if (System.currentTimeMillis() > confirmedSharedUntil
                || (!navigationActive && confirmedSharedNavigationSeen)
                || (mapsDestination.length() > 0 && !mapsDestination.equals(confirmedSharedDestination))) {
            confirmedSharedDestination = "";
            confirmedSharedUntil = 0L;
            confirmedSharedNavigationSeen = false;
        }
        if (navigationActive && confirmedSharedDestination.length() > 0) {
            if (!localStart) confirmedSharedNavigationSeen = true;
            if (mapsDestination.length() == 0) {
                mapsDestination = confirmedSharedDestination;
                result.put("destination", mapsDestination);
                result.put("destinationSource", "user_confirmed_share");
            }
        }
        // Resolve explicit/shared identity before considering cached data. A current walking
        // notification must not turn the overall Transit plan into a walk to the first station.
        if (googleMap && mapsDestination.length() > 0
                && ("transit".equals(navigationMode()) || "transit".equals(result.optString("travelMode", "")))) {
            if (!mapsDestination.equals(transitJourneyTarget)) {
                transitJourneyTarget = mapsDestination;
                transitJourneyTargetUntil = System.currentTimeMillis() + MAP_ROUTE_CACHE_MS;
            }
        }
        boolean keepTransitJourney = transitJourneyTarget.length() > 0
                && System.currentTimeMillis() < transitJourneyTargetUntil
                && (mapsDestination.length() == 0 || mapsDestination.equals(transitJourneyTarget));
        if (keepTransitJourney) mapsDestination = transitJourneyTarget;
        else if (mapsDestination.length() == 0 && navigationActive && googleCached != null
                && System.currentTimeMillis() - googleCached.time <= MAP_ROUTE_CACHE_MS) {
            mapsDestination = googleCached.destination;
        }
        result.put("routeTargetKind", "destination");
        String reportedMode = result.optString("travelMode", "");
        final String routeMode = keepTransitJourney ? "transit"
                : mapsDestination.equals(confirmedSharedDestination) && System.currentTimeMillis() < confirmedSharedUntil
                    && ("walking".equals(navigationMode()) || "bicycling".equals(navigationMode()) || "transit".equals(navigationMode())) ? navigationMode()
                : reportedMode.length() > 0 ? reportedMode
                : googleCached != null && mapsDestination.equals(googleCached.destination) ? googleCached.mode : navigationMode();
        desiredGoogleRouteMode = routeMode;
        if (reportedMode.length() > 0 && !reportedMode.equals(navigationMode())) {
            routeText = "";
            routeTime = 0L;
        }
        if (googleMap) {
            routeTime = googleCached == null ? 0 : googleCached.time;
            routeText = googleCached == null ? "" : googleCached.data.route.toString();
            maneuverText = googleCached == null ? "" : googleCached.data.maneuvers.toString();
            if (googleCached != null) routeDestination = googleCached.destination;
        }
        if (navigationActive && mapsDestination.length() > 0
                && !mapsDestination.equals(routeDestination)) {
            routeDestination = mapsDestination;
            routeText = "";
            maneuverText = "";
            routeTime = 0L;
            prepareNavigationRouteAsync(mapsDestination, routeMode, true);
        }
        result.put("routeMode", routeMode);
        result.put("mapProvider", googleMap ? "google" : "osm");
        result.put("routeSource", googleMap ? "Google API計算経路（マップアプリの選択経路と異なる場合あり）" : "OSM推定経路（Googleマップと異なる場合あり）");
        float routeDistanceMeters = preferences.getFloat(KEY_MAP_ROUTE_DISTANCE, -1.0f);
        float routeDurationSeconds = preferences.getFloat(KEY_MAP_ROUTE_DURATION, -1.0f);
        int routeDataVersion = preferences.getInt(KEY_MAP_ROUTE_DATA_VERSION, 0);
        if (googleMap) {
            routeDistanceMeters = googleCached == null ? -1 : (float)googleCached.data.distanceMeters;
            routeDurationSeconds = googleCached == null ? -1 : (float)googleCached.data.durationSeconds;
            routeDataVersion = 3;
        }
        boolean routeFresh = mapsDestination.length() > 0 && mapsDestination.equals(routeDestination)
                && (googleMap || !"transit".equals(routeMode))
                && (!googleMap || (googleCached != null && googleCached.generation == googleRouteGeneration))
                && routeMode.equals(googleMap ? (googleCached == null ? "" : googleCached.mode) : preferences.getString("map_cached_mode", "")) && routeTime > 0L
                && System.currentTimeMillis() - routeTime <= MAP_ROUTE_CACHE_MS
                && routeText.length() > 2;
        if (navigationActive && routeFresh && googleMap && !"transit".equals(routeMode)
                && googleCached != null && location != null) {
            String finalPoint = GoogleNavigationClient.selectedDestinationCoordinates(mapsDestination);
            boolean near = false;
            if (!finalPoint.isEmpty()) {
                try {
                    String[] pair = finalPoint.split(",", 2);
                    Location target = new Location("route-final");
                    target.setLatitude(Double.parseDouble(pair[0]));
                    target.setLongitude(Double.parseDouble(pair[1]));
                    near = NavigationArrivalPolicy.nearFinalPoint(System.currentTimeMillis(), location.getTime(),
                            location.hasAccuracy() ? location.getAccuracy() : -1,
                            location.hasSpeed() ? location.getSpeed() : -1,
                            googleCached.time, location.distanceTo(target));
                } catch (Exception ignored) { }
            }
            if (!near) arrivalCandidateSince = 0;
            else if (arrivalCandidateSince == 0) arrivalCandidateSince = System.currentTimeMillis();
            if (near && System.currentTimeMillis() - arrivalCandidateSince >= 6000) {
                clearStoredNavigationRoute();
                navigationCompletedDestination = mapsDestination;
                navigationCompletedUntil = System.currentTimeMillis() + 8000;
                result.put("navigationActive", false).put("navigationComplete", true)
                        .put("completionId", "maps-" + googleRouteGeneration)
                        .put("routeReady", false).put("route", new JSONArray());
                Log.i("LokiGoogleNav", "destination_arrived");
                return result;
            }
        } else arrivalCandidateSince = 0;
        result.put("routeReady", routeFresh);
        result.put("routeGeneration", routeFresh && googleCached != null ? googleCached.generation : 0L);
        if (routeFresh && googleCached != null && !googleCached.scheduledDeparture.isEmpty()) {
            String when = java.time.Instant.parse(googleCached.scheduledDeparture).atZone(java.time.ZoneId.of("Asia/Tokyo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm"));
            result.put("route", new JSONArray(routeText)).put("routeStatus", "ready").put("routeMode", "transit")
                    .put("instruction", when + "出発の参考経路（現在の案内ではありません）")
                    .put("detail", "現在時刻の経路は取得できませんでした。Googleマップ側の出発日時も確認してください。")
                    .put("routeDestination", googleCached.destination);
            for (String key : new String[]{"nextDistance", "arrival", "routeArrival", "totalRemainingDuration", "totalRemainingDistance", "afterNextInstruction"}) result.put(key, "");
            return result;
        }
        if (routeFresh && googleCached != null && googleCached.progress != null) {
            JSONObject journeyPayload = googleCached.progress.snapshot(
                    location == null ? Double.NaN : location.getLatitude(),
                    location == null ? Double.NaN : location.getLongitude(),
                    location == null || !location.hasAccuracy() ? -1 : location.getAccuracy(),
                    location == null ? 0 : location.getTime(), System.currentTimeMillis(), googleCached.data.route);
            journeyPayload.put("requestedMode", googleCached.mode).put("effectiveMode", googleCached.effectiveMode)
                    .put("walkingFallback", !googleCached.mode.equals(googleCached.effectiveMode));
            result.put("journey", journeyPayload);
        }
        result.put("routeTime", routeFresh ? routeTime : 0L);
        // Cached route identity is not evidence of Google Maps' current target.
        // Never present a previous trip's name when the notification omits it.
        result.put("routeDestination", cleanNavigationLabel(mapsDestination));
        result.put("route", routeFresh ? new JSONArray(routeText) : new JSONArray());
        result.put("routeError", preferences.getString(KEY_MAP_ROUTE_ERROR, ""));
        result.put("routeStatus", routeFresh ? "ready"
                : mapsDestination.length() == 0 ? "destination_missing"
                : navigationRouteFetchInFlight ? "fetching"
                : "google_routes_local_limit".equals(preferences.getString(KEY_MAP_ROUTE_ERROR, "")) ? "local_limit"
                : "google_route_not_found".equals(preferences.getString(KEY_MAP_ROUTE_ERROR, "")) ? "no_route"
                : preferences.getString(KEY_MAP_ROUTE_ERROR, "").length() > 0 ? "request_failed" : "pending");
        if (mapsDestination.length() > 0) maybeRefreshNavigationRoute(navigationActive, location, routeText,
                routeDestination, routeTime, result, routeMode);
        result.put("routeRefreshing", navigationRouteFetchInFlight);
        if (navigationActive && routeFresh && !"transit".equals(routeMode)
                && location != null && maneuverText.length() > 2) {
            JSONObject routeMetrics = buildRouteHudMetrics(location, routeText, maneuverText,
                    routeDistanceMeters, routeDurationSeconds);
            if (googleMap && googleCached != null) {
                // API geometry, step guidance and duration come from one response.
                result.put("instruction", routeMetrics.optString("instruction", "API計算経路を確認中"));
                result.put("detail", "Google API計算経路（マップアプリの案内と異なる場合あり）");
                for (String key : new String[]{"nextDistance", "afterNextInstruction", "afterNextDistance",
                        "afterNextDuration", "totalRemainingDistance", "totalRemainingDuration", "routeArrival", "arrival", "currentRoad"})
                    result.put(key, "");
            }
            if (result.optString("nextDistance", "").length() == 0) {
                String routeDistance = routeMetrics.optString("nextDistance", "");
                if (routeDistance.length() > 0) result.put("nextDistance", routeDistance);
            }
            String[] metricNames = {"afterNextInstruction", "afterNextDistance",
                    "afterNextDuration", "totalRemainingDistance",
                    "totalRemainingDuration", "routeArrival", "currentRoad"};
            for (String metricName : metricNames) {
                String metricValue = routeMetrics.optString(metricName, "");
                if (metricValue.length() > 0 && result.optString(metricName, "").length() == 0) {
                    result.put(metricName, metricValue);
                }
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
        if (navigationActive && mapsDestination.length() > 0
                && (!routeFresh || (!googleMap && maneuverText.length() <= 2) || routeDataVersion < 3
                || routeDistanceMeters <= 0.0f || routeDurationSeconds <= 0.0f)
                && routeDestination.length() > 0
                && !navigationRouteFetchInFlight
                && System.currentTimeMillis() >= navigationRouteRetryAfterMs) {
            prepareNavigationRouteAsync(routeDestination, routeMode, false);
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
        // Compatibility with older glasses: explicitly retire image sharing.
        if (googleMap && googleCached != null && googleCached.generation != googleRouteGeneration) {
            result.put("routeReady", false).put("route", new JSONArray())
                    .put("routeStatus", "fetching").put("routeGeneration", 0L)
                    .put("instruction", "経路更新中");
            for (String key : new String[]{"nextDistance", "afterNextInstruction", "afterNextDistance",
                    "afterNextDuration", "totalRemainingDistance", "totalRemainingDuration", "routeArrival", "arrival"})
                result.put(key, "");
        }
        result.put("mapShare", new JSONObject().put("active", false));
        return result;
    }

    /** Prevent malformed or control-character destination text from reaching the HUD. */
    private String cleanNavigationLabel(String value) {
        String text = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ')
                .replaceAll("[\\p{Cntrl}]", "").replaceAll("\\s+", " ").trim();
        if (text.indexOf('\uFFFD') >= 0 || text.contains("??")) return "";
        return shortText(text, 80);
    }

    private void maybeRefreshNavigationRoute(boolean navigationActive, Location current,
                                             String routeText, String destination,
                                             long routeTime, JSONObject navigation, String routeMode) {
        if (!navigationActive || current == null || destination == null
                || destination.trim().length() == 0) {
            navigationRouteDeviationSinceMs = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        String combined = safe(navigation.optString("instruction", "")) + " "
                + safe(navigation.optString("detail", "")) + " "
                + safe(navigation.optString("compact", ""));
        boolean explicitReroute = containsRouteRecalculationSignal(combined);
        float nearestRouteDistance = nearestDistanceToNavigationRoute(current, routeText);
        boolean fastTravel = current.hasSpeed() && current.getSpeed() >= 12.0f;
        float baseThreshold = "walking".equals(routeMode) ? 90.0f
                : "bicycling".equals(routeMode) ? 140.0f : 220.0f;
        if (current.hasAccuracy() && current.getAccuracy() > 0.0f) {
            baseThreshold = Math.max(baseThreshold, current.getAccuracy() * 3.0f);
        }
        if (fastTravel) baseThreshold = Math.max(baseThreshold, 800.0f);
        boolean outsideRoute = nearestRouteDistance < Float.MAX_VALUE
                && nearestRouteDistance > baseThreshold;
        if (outsideRoute) {
            if (navigationRouteDeviationSinceMs <= 0L) {
                navigationRouteDeviationSinceMs = now;
            }
        } else {
            navigationRouteDeviationSinceMs = 0L;
        }
        long confirmation = fastTravel ? MAP_ROUTE_FAST_DEVIATION_CONFIRM_MS
                : MAP_ROUTE_DEVIATION_CONFIRM_MS;
        boolean confirmedDeviation = navigationRouteDeviationSinceMs > 0L
                && now - navigationRouteDeviationSinceMs >= confirmation;
        boolean periodicRefresh = routeTime <= 0L
                || (!GoogleNavigationClient.enabled(this)
                && now - routeTime >= MAP_ROUTE_PERIODIC_REFRESH_MS);
        if (!explicitReroute && !confirmedDeviation && !periodicRefresh) return;
        if (navigationRouteFetchInFlight || now < navigationRouteRetryAfterMs) return;
        long cooldown = fastTravel ? MAP_ROUTE_FAST_REROUTE_COOLDOWN_MS
                : MAP_ROUTE_REROUTE_COOLDOWN_MS;
        if (navigationRouteLastRerouteAtMs > 0L
                && now - navigationRouteLastRerouteAtMs < cooldown) return;
        navigationRouteLastRerouteAtMs = now;
        navigationRouteDeviationSinceMs = 0L;
        Log.i(TAG, "navigation reroute requested reason="
                + (explicitReroute ? "maps" : confirmedDeviation ? "deviation" : "periodic")
                + " deviation="
                + (nearestRouteDistance == Float.MAX_VALUE
                        ? "unknown" : Math.round(nearestRouteDistance)));
        // Do not clear the stored geometry here. The glasses keep drawing the
        // last complete route while the replacement is fetched, and the new
        // route is swapped in only after a successful response.
        prepareNavigationRouteAsync(destination.trim(), routeMode, false);
    }

    private boolean containsRouteRecalculationSignal(String value) {
        String text = safe(value).toLowerCase(Locale.US)
                .replaceAll("[\\s　]", "");
        return text.contains("再検索") || text.contains("再探索")
                || text.contains("経路を検索") || text.contains("ルートを検索")
                || text.contains("rerouting") || text.contains("recalculating");
    }

    private float nearestDistanceToNavigationRoute(Location current, String routeText) {
        if (current == null || routeText == null || routeText.length() <= 2) {
            return Float.MAX_VALUE;
        }
        try {
            JSONArray route = new JSONArray(routeText);
            float nearest = Float.MAX_VALUE;
            double[] previous = null;
            for (int index = 0; index < route.length(); index++) {
                JSONArray point = route.optJSONArray(index);
                if (point == null || point.length() < 2) continue;
                double latitude = point.optDouble(0, Double.NaN);
                double longitude = point.optDouble(1, Double.NaN);
                if (Double.isNaN(latitude) || Double.isNaN(longitude)) continue;
                nearest = Math.min(nearest, coordinateDistance(
                        current.getLatitude(), current.getLongitude(), latitude, longitude));
                if (previous != null) {
                    nearest = Math.min(nearest, distanceToRouteSegmentMeters(
                            current.getLatitude(), current.getLongitude(),
                            previous[0], previous[1], latitude, longitude));
                }
                previous = new double[]{latitude, longitude};
            }
            return nearest;
        } catch (Exception error) {
            Log.w(TAG, "route deviation check failed", error);
            return Float.MAX_VALUE;
        }
    }

    private float distanceToRouteSegmentMeters(double latitude, double longitude,
                                               double fromLatitude, double fromLongitude,
                                               double toLatitude, double toLongitude) {
        double longitudeScale = 111320.0d * Math.cos(Math.toRadians(latitude));
        double fromX = (fromLongitude - longitude) * longitudeScale;
        double fromY = (fromLatitude - latitude) * 110540.0d;
        double toX = (toLongitude - longitude) * longitudeScale;
        double toY = (toLatitude - latitude) * 110540.0d;
        double deltaX = toX - fromX;
        double deltaY = toY - fromY;
        double lengthSquared = deltaX * deltaX + deltaY * deltaY;
        if (lengthSquared <= 0.001d) {
            return (float) Math.sqrt(fromX * fromX + fromY * fromY);
        }
        double progress = -(fromX * deltaX + fromY * deltaY) / lengthSquared;
        progress = Math.max(0.0d, Math.min(1.0d, progress));
        double nearestX = fromX + progress * deltaX;
        double nearestY = fromY + progress * deltaY;
        return (float) Math.sqrt(nearestX * nearestX + nearestY * nearestY);
    }

    private String remainingDurationFromNavigationArrival(String value, long now) {
        long remaining = NavigationTime.remainingMillis(value, now, TimeZone.getDefault());
        return remaining <= 0L ? "" : formatRouteDuration(Math.ceil(remaining / 60000.0) * 60.0);
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
                String instruction = routeManeuverInstruction(next);
                if (!instruction.isEmpty()) result.put("instruction", instruction);
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
        if ("google".equals(type)) return maneuver.optString("instruction", "");
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

    private JSONObject performNavigationAction(String request) throws Exception {
        final String action = parseStringQuery(request, "action", "open");
        if ("stop".equals(action)) {
            boolean stopped = MailNotificationService.stopMapsNavigation();
            setNavigationHudSuppressed(true);
            return new JSONObject().put("ok", true).put("message", stopped
                    ? "ナビを終了しました。" : "グラスのナビ表示を停止しました。Googleマップの終了はスマホで確認してください。");
        }
        if ("resume".equals(action)) {
            setNavigationHudSuppressed(false);
            return new JSONObject().put("ok", true).put("message", "グラスのナビ表示を再開しました。");
        }
        final String destination = cleanNavigationLabel(parseStringQuery(request, "destination",
                getPreferences().getString(KEY_MAP_ROUTE_DESTINATION, "")));
        final String mode = parseStringQuery(request, "mode", "");
        final boolean avoidTolls = "avoid_tolls".equals(action);
        final CountDownLatch done = new CountDownLatch(1);
        final String[] failure = {""};
        runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    Intent intent;
                    if (destination.length() == 0) {
                        intent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
                        if (intent == null) throw new IllegalStateException("Googleマップがありません");
                    } else {
                        String uri = "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination);
                        if (mode.matches("walking|driving|bicycling|transit")) uri += "&travelmode=" + mode;
                        if (avoidTolls) uri = "google.navigation:q=" + Uri.encode(destination) + "&mode=d&avoid=th";
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        if (mode.matches("walking|driving|bicycling|transit")) {
                            getPreferences().edit().putString(KEY_MAP_ROUTE_MODE, mode).apply();
                        }
                        clearStoredNavigationRoute();
                        getPreferences().edit().putString(KEY_MAP_ROUTE_DESTINATION, destination).apply();
                    }
                    setNavigationHudSuppressed(false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception error) { failure[0] = error.getMessage(); }
                finally { done.countDown(); }
            }
        });
        if (!done.await(3, TimeUnit.SECONDS)) return new JSONObject().put("ok", false).put("message", "スマホの操作待ちです。");
        return new JSONObject().put("ok", failure[0].length() == 0).put("message", failure[0].length() > 0
                ? failure[0] : avoidTolls && destination.length() > 0
                ? "有料道路・高速道路を避ける条件をGoogleマップへ送りました。スマホで経路を確認してください。"
                : "Googleマップを開きました。スマホで目的地と移動手段を確認してください。");
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

    private JSONObject buildRequestedWeatherJson(int dayOffset, String query) throws Exception {
        String place = WeatherPlaceQuery.extract(query);
        if (place.length() == 0) return buildWeatherJson(dayOffset);
        // Named destinations must never fall back to the current-location cache.
        double[] coordinates = geocodeNavigationDestination(place);
        if (coordinates == null) throw new IllegalStateException("指定された場所を確認できません: " + place);
        String url = "https://api.open-meteo.com/v1/forecast?latitude="
                + String.format(Locale.US, "%.5f", coordinates[0]) + "&longitude="
                + String.format(Locale.US, "%.5f", coordinates[1])
                + "&current=temperature_2m,apparent_temperature,weather_code"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                + "&forecast_days=3&timezone=auto";
        JSONObject response = new JSONObject(fetchText(url));
        JSONObject current = response.getJSONObject("current");
        JSONObject daily = response.getJSONObject("daily");
        int index = Math.max(0, Math.min(2, dayOffset));
        JSONObject forecast = new JSONObject();
        forecast.put("date", daily.getJSONArray("time").getString(index));
        forecast.put("condition", weatherCondition(daily.getJSONArray("weather_code").getInt(index)));
        forecast.put("max", daily.getJSONArray("temperature_2m_max").getDouble(index));
        forecast.put("min", daily.getJSONArray("temperature_2m_min").getDouble(index));
        if (!daily.getJSONArray("precipitation_probability_max").isNull(index)) {
            forecast.put("rain", daily.getJSONArray("precipitation_probability_max").getInt(index));
        }
        JSONObject result = new JSONObject();
        result.put("ok", true);
        result.put("location", place);
        result.put("condition", weatherCondition(current.getInt("weather_code")));
        result.put("temperature", current.getDouble("temperature_2m"));
        result.put("forecast", forecast);
        result.put("dayOffset", index);
        result.put("time", System.currentTimeMillis());
        result.put("source", "Open-Meteo");
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
