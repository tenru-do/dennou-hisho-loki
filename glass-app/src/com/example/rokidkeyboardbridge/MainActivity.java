package com.example.rokidkeyboardbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class MainActivity extends Activity implements SensorEventListener {
    private static final String ASSIST_DESCRIPTOR = "com.rokid.os.sprite.assist.server.IAssistServer";
    private static final String ASSIST_PACKAGE = "com.rokid.os.sprite.assistserver";
    private static final String ASSIST_SERVICE = "com.rokid.os.sprite.assist.MasterAssistService";
    private static final long GEMINI_LOCAL_PACING_MS = 45000;
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BRIDGE_TOKEN = "bridge_token";
    private static final String KEY_CUSTOM_INSTRUCTIONS = "custom_instructions";
    private static final String KEY_GEMINI_COOLDOWN_UNTIL = "gemini_cooldown_until";
    private static final String KEY_LAST_PHONE_HOST = "last_phone_host";
    private static final String KEY_VOICE_AUDIO_SOURCE_INDEX = "voice_audio_source_index";
    private static final int MAX_CONTEXT_CHARS = 2600;
    private static final int MAX_CUSTOM_CHARS = 900;
    private static final int MAX_MAIL_SUMMARY_CHARS = 140;
    private static final int MAX_USER_PROMPT_CHARS = 900;
    private static final String PREFS = "gemini_settings";
    private static final String TAG = "RokidKeyboardAI";
    private static final boolean PREFER_GLASS_SYSTEM_SPEECH = false;
    private volatile HttpURLConnection activeGeminiConnection;
    private TextView answer;
    private ScrollView answerScroll;
    private LinearLayout.LayoutParams answerScrollParams;
    private IBinder assistBinder;
    private LinearLayout buttonPanel;
    private volatile boolean conversationActive;
    private PowerManager.WakeLock conversationWakeLock;
    private PowerManager.WakeLock notificationWakeLock;
    private volatile long geminiCooldownUntil;
    private volatile boolean geminiRequestActive;
    private Button imeButton;
    private TextView info;
    private EditText input;
    private volatile String healthCompactLine = "";
    private volatile long healthUpdatedAt;
    private volatile boolean healthPollInFlight;
    private volatile String codexStatusLine = "CODEX --";
    private volatile String codexEventKey = "";
    private volatile boolean codexPollInFlight;
    private volatile long hudHoldUntil;
    private volatile long lastNavigationAt;
    private volatile int lastNavigationKeyCode;
    private volatile long lastGeminiVoiceFallbackAt;
    private volatile long lastProactiveRequestAt;
    private volatile long lastWifiRepairAt;
    private volatile int mascotMode;
    private MascotView mascotView;
    private volatile boolean proactiveMode;
    private Button proactiveOffButton;
    private Button proactiveOnButton;
    private Thread proactiveThread;
    private LinearLayout readButtonPanel;
    private volatile int requestGeneration;
    private Button scrollDownButton;
    private Button scrollUpButton;
    private Button sendButton;
    private Button settingsButton;
    private TextView status;
    private View topSpacer;
    private FrameLayout hudRoot;
    private SensorManager sensorManager;
    private Sensor headRotationSensor;
    private float neutralPitch;
    private boolean neutralPitchReady;
    private int neutralPitchSamples;
    private boolean glanceHudVisible = true;
    private boolean headGlanceWake;
    private boolean activityForeground;
    private long lastUpwardGlanceAt;
    private int normalScreenTimeoutMs = 6000;
    private volatile int ttsGeneration;
    private Button voiceButton;
    private volatile boolean voiceLoopMode;
    private volatile boolean voiceRecording;
    private Thread voiceThread;
    private SpeechRecognizer speechRecognizer;
    private volatile boolean speechRecognizerActive;
    private Runnable speechRecognizerTimeoutRunnable;
    private Button wifiButton;
    private Button zoomButton;
    private static final String[] PHONE_TODAY_URLS = {"http://127.0.0.1:8765/today", "http://192.168.43.1:8765/today", "http://192.168.239.1:8765/today"};
    private static final String[] PHONE_MAIL_URLS = {"http://127.0.0.1:8765/mail", "http://192.168.43.1:8765/mail", "http://192.168.239.1:8765/mail"};
    private static final String[] PHONE_NEWS_URLS = {"http://127.0.0.1:8765/news", "http://192.168.43.1:8765/news", "http://192.168.239.1:8765/news"};
    private static final String[] MODELS = {"gemini-2.5-flash", "gemini-2.5-flash-lite"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.1
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.hideControls();
        }
    };
    private final Runnable hideInputRunnable = new Runnable() {
        @Override
        public void run() {
            MainActivity.this.hideInputIfIdle();
        }
    };
    private final Runnable dimConversationRunnable = new Runnable() {
        @Override
        public void run() {
            if (MainActivity.this.conversationActive
                    && !MainActivity.this.geminiRequestActive
                    && !MainActivity.this.voiceRecording
                    && MainActivity.this.mascotMode != 2) {
                MainActivity.this.setScreenBrightness(MainActivity.this.glanceHudVisible ? 0.12f : 0.0f);
            }
        }
    };
    private final Runnable commandPoller = new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.2
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.pollPhoneCommand();
            MainActivity.this.handler.postDelayed(this, 3000L);
        }
    };
    private final Runnable infoUpdater = new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.3
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.updateInfoLine();
            MainActivity.this.handler.postDelayed(this, MainActivity.this.isGeminiCoolingDown() ? 1000L : 30000L);
        }
    };
    private final Runnable healthUpdater = new Runnable() {
        @Override
        public void run() {
            MainActivity.this.pollPhoneHealthAsync();
            MainActivity.this.handler.postDelayed(this, 20000L);
        }
    };
    private final Runnable codexUpdater = new Runnable() {
        @Override
        public void run() {
            MainActivity.this.pollCodexStatusAsync();
            MainActivity.this.handler.postDelayed(this, 5000L);
        }
    };
    private final Runnable hideGlanceHudRunnable = new Runnable() {
        @Override
        public void run() {
            long remaining = MainActivity.this.hudHoldUntil - System.currentTimeMillis();
            if (remaining > 0L) {
                MainActivity.this.handler.postDelayed(this, remaining + 50L);
                return;
            }
            if (System.currentTimeMillis() - MainActivity.this.lastUpwardGlanceAt >= 2800L) {
                MainActivity.this.setGlanceHudVisible(false);
            }
        }
    };
    private final ServiceConnection connection = new ServiceConnection() { // from class: com.example.rokidkeyboardbridge.MainActivity.4
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MainActivity.this.assistBinder = iBinder;
            MainActivity.this.setStatus("Rokid音声：準備完了", Color.rgb(90, 220, 120));
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            MainActivity.this.assistBinder = null;
            MainActivity.this.setStatus("Rokid音声との接続が切れました", -256);
        }
    };

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setSoftInputMode(19);
        initConversationWakeLock();
        rememberNormalScreenTimeout();
        this.geminiCooldownUntil = getPreferences().getLong(KEY_GEMINI_COOLDOWN_UNTIL, 0L);
        buildUi();
        initHeadPoseSensor();
        requestWifiOnForStartup();
        bindAssistService();
        this.handler.postDelayed(this.commandPoller, 2500L);
        this.handler.post(this.infoUpdater);
        this.handler.post(this.healthUpdater);
        this.handler.post(this.codexUpdater);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        this.activityForeground = true;
        Log.i(TAG, "onResume");
        requestWifiOnForStartup();
        registerHeadPoseSensor();
        this.handler.removeCallbacks(this.commandPoller);
        this.handler.postDelayed(this.commandPoller, 800L);
        if (this.conversationActive || this.geminiRequestActive || this.voiceRecording || this.voiceLoopMode) {
            setConversationActive(true);
        } else {
            setScreenBrightness(-1.0f);
        }
        try {
            getWindow().getDecorView().requestFocus();
        } catch (Exception e) {
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.activityForeground = false;
        if (!this.conversationActive) {
            try {
                getWindow().clearFlags(128);
            } catch (Exception e) {
            }
            releaseConversationWakeLock();
        }
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        emergencyStop("destroy", false, false);
        try {
            unbindService(this.connection);
        } catch (Exception e) {
        }
        this.handler.removeCallbacks(this.commandPoller);
        this.handler.removeCallbacks(this.infoUpdater);
        this.handler.removeCallbacks(this.healthUpdater);
        this.handler.removeCallbacks(this.codexUpdater);
        this.handler.removeCallbacks(this.hideInputRunnable);
        this.handler.removeCallbacks(this.dimConversationRunnable);
        this.handler.removeCallbacks(this.hideGlanceHudRunnable);
        unregisterHeadPoseSensor();
        releaseConversationWakeLock();
        super.onDestroy();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == 0) {
            showControlsTemporarily();
            int keyCode = keyEvent.getKeyCode();
            if (isNavigationKey(keyCode)) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                if (jCurrentTimeMillis - this.lastNavigationAt < 380) {
                    return true;
                }
                this.lastNavigationKeyCode = keyCode;
                this.lastNavigationAt = jCurrentTimeMillis;
            }
            View currentFocus = getCurrentFocus();
            if (currentFocus != this.input && isTextInputKey(keyEvent)) {
                revealInputForEditing();
                if (this.input != null) {
                    this.input.requestFocus();
                    this.input.append(String.valueOf((char) keyEvent.getUnicodeChar()));
                    this.input.setSelection(this.input.getText().length());
                    return true;
                }
            }
            if (currentFocus == this.input) {
                if (keyCode == 20 || keyCode == 22 || keyCode == 61) {
                    hideKeyboard();
                    if (this.voiceButton != null) {
                        this.voiceButton.requestFocus();
                    }
                    return true;
                }
                if (keyCode == 21) {
                    hideKeyboard();
                    if (this.sendButton != null) {
                        this.sendButton.requestFocus();
                    }
                    return true;
                }
            }
            if ((keyEvent.isCtrlPressed() && keyCode == 66) || keyCode == 139) {
                sendCurrentText();
                return true;
            }
            if (keyCode == 111 || keyCode == 4 || keyCode == MAX_MAIL_SUMMARY_CHARS || keyCode == 97) {
                emergencyStop("App closed", true, true);
                return true;
            }
            if (handleFocusNavigation(currentFocus, keyCode)) {
                return true;
            }
            if (keyCode == 135 || keyCode == 84) {
                Log.i(TAG, "voice key received code=" + keyCode);
                toggleVoiceRecording();
                return true;
            }
            if ((keyCode == 109 || keyCode == 23) && !(currentFocus instanceof Button)) {
                Log.i(TAG, "voice select key received code=" + keyCode);
                toggleVoiceRecording();
                return true;
            }
            if (keyCode == 134) {
                setStatus("PRO AI is disabled", -256);
                return true;
            }
            if (keyCode == 138) {
                speakWithRokidChunked("読み上げテストです。Rokidの女性音声で聞こえていますか。");
                return true;
            }
            if (keyCode == 136) {
                speakWithPhoneTts("読み上げテストです。スマホ側のRokid音声で聞こえていますか。");
                return true;
            }
            if (keyCode == 137) {
                playBeepTest();
                return true;
            }
            if (currentFocus != this.input && this.answerScroll != null && (keyCode == 93 || keyCode == 62)) {
                this.answerScroll.smoothScrollBy(0, 220);
                return true;
            }
            if (currentFocus != this.input && this.answerScroll != null && keyCode == 92) {
                this.answerScroll.smoothScrollBy(0, -220);
                return true;
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            showControlsTemporarily();
            if (this.zoomButton != null && this.zoomButton.getVisibility() == 0) {
                this.zoomButton.requestFocus();
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    private boolean handleFocusNavigation(View view, int i) {
        if (i == 22 || i == 20) {
            return view == this.sendButton ? requestFocusSafely(this.voiceButton)
                    : view == this.voiceButton ? requestFocusSafely(this.wifiButton)
                    : view == this.wifiButton ? requestFocusSafely(this.zoomButton)
                    : requestFocusSafely(this.sendButton);
        }
        if (i == 21 || i == 19) {
            return view == this.sendButton ? requestFocusSafely(this.zoomButton)
                    : view == this.voiceButton ? requestFocusSafely(this.sendButton)
                    : view == this.wifiButton ? requestFocusSafely(this.voiceButton)
                    : view == this.zoomButton ? requestFocusSafely(this.wifiButton)
                    : requestFocusSafely(this.sendButton);
        }
        return false;
    }

    private boolean requestFocusSafely(View view) {
        if (view == null || view.getVisibility() != 0) {
            return false;
        }
        showControlsTemporarily();
        view.requestFocus();
        return true;
    }

    private boolean isNavigationKey(int i) {
        return i == 21 || i == 22 || i == 19 || i == 20;
    }

    private boolean isTextInputKey(KeyEvent event) {
        if (event == null || event.isCtrlPressed() || event.isAltPressed() || event.isMetaPressed()) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (isNavigationKey(keyCode) || keyCode == 4 || keyCode == 23 || keyCode == 66
                || keyCode == 61 || keyCode == 62 || keyCode == 92 || keyCode == 93
                || keyCode == 111 || keyCode == 135 || keyCode == 136 || keyCode == 137
                || keyCode == 138 || keyCode == 139) {
            return false;
        }
        int unicode = event.getUnicodeChar();
        return unicode >= 32;
    }

    private void revealInputForEditing() {
        if (this.input == null) {
            return;
        }
        this.input.setVisibility(0);
        this.input.setAlpha(1.0f);
        this.handler.removeCallbacks(this.hideInputRunnable);
        this.handler.postDelayed(this.hideInputRunnable, 6500L);
    }

    private void revealInputForText(String text) {
        if (this.input == null) {
            return;
        }
        String value = text == null ? "" : text.trim();
        if (value.length() == 0) {
            hideInputIfIdle();
            return;
        }
        this.input.setVisibility(0);
        this.input.setAlpha(1.0f);
        this.handler.removeCallbacks(this.hideInputRunnable);
        this.handler.postDelayed(this.hideInputRunnable, 9000L);
    }

    private void hideInputIfIdle() {
        if (this.input == null) {
            return;
        }
        String value = this.input.getText() == null ? "" : this.input.getText().toString().trim();
        View currentFocus = getCurrentFocus();
        if (value.length() == 0 && currentFocus != this.input) {
            this.input.setVisibility(8);
        } else if (!this.geminiRequestActive && !this.voiceRecording && currentFocus != this.input) {
            this.input.setVisibility(8);
        }
    }

    private void setInputTextVisible(String text) {
        if (this.input == null) {
            return;
        }
        this.input.setText(text == null ? "" : text);
        this.input.setSelection(this.input.getText().length());
        revealInputForText(text);
    }

    private void buildUi() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(6, dp(100), 6, 2);
        linearLayout.setBackgroundColor(-16777216);
        TextView textView = new TextView(this);
        textView.setText("Gemini for Rokid");
        textView.setTextColor(-1);
        textView.setTextSize(13.0f);
        textView.setVisibility(8);
        linearLayout.addView(textView);
        this.input = new EditText(this);
        this.input.setHint("日本語で質問を入力");
        this.input.setHintTextColor(-7829368);
        this.input.setTextColor(-1);
        this.input.setTextSize(10.0f);
        this.input.setMinHeight(0);
        this.input.setPadding(4, 0, 4, 0);
        this.input.setBackgroundColor(Color.TRANSPARENT);
        this.input.setSingleLine(true);
        this.input.setImeOptions(4);
        this.input.setInputType(16385);
        this.input.setShowSoftInputOnFocus(false);
        this.input.setVisibility(8);
        linearLayout.addView(this.input, new LinearLayout.LayoutParams(-1, -2));
        this.buttonPanel = new LinearLayout(this);
        this.buttonPanel.setOrientation(0);
        this.sendButton = new Button(this);
        this.sendButton.setText("Geminiへ送信");
        this.sendButton.setText("SEND");
        this.sendButton.setTextSize(11.0f);
        this.sendButton.setMinHeight(0);
        this.sendButton.setMinWidth(0);
        this.sendButton.setPadding(2, 0, 2, 0);
        this.sendButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                MainActivity.this.sendCurrentText();
            }
        });
        this.sendButton.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.6
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                Log.i(MainActivity.TAG, "send long click starts voice");
                MainActivity.this.toggleVoiceRecording();
                return true;
            }
        });
        focusLabel(this.sendButton, "SEND");
        this.buttonPanel.addView(this.sendButton, new LinearLayout.LayoutParams(0, dp(38), 1.0f));
        this.voiceButton = new Button(this);
        this.voiceButton.setText("音声");
        this.voiceButton.setText("VOICE");
        this.voiceButton.setTextSize(11.0f);
        this.voiceButton.setMinHeight(0);
        this.voiceButton.setMinWidth(0);
        this.voiceButton.setPadding(2, 0, 2, 0);
        this.voiceButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.7
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                Log.i(MainActivity.TAG, "voice button clicked");
                MainActivity.this.toggleVoiceRecording();
            }
        });
        focusLabel(this.voiceButton, "VOICE");
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
        layoutParams.leftMargin = 4;
        this.buttonPanel.addView(this.voiceButton, layoutParams);
        this.wifiButton = new Button(this);
        this.wifiButton.setText("WiFi");
        this.wifiButton.setTextSize(11.0f);
        this.wifiButton.setMinHeight(0);
        this.wifiButton.setMinWidth(0);
        this.wifiButton.setPadding(2, 0, 2, 0);
        this.wifiButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.8
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                MainActivity.this.maintainWifiConnection(true);
                MainActivity.this.answer.setText(MainActivity.this.describeWifiState());
                if (MainActivity.this.isWifiConnected()) {
                    MainActivity.this.setStatus("WiFi reconnect requested", Color.rgb(90, 220, 120));
                } else {
                    MainActivity.this.setStatus("WiFi not connected. Opening WiFi settings.", -256);
                    MainActivity.this.openWifiSettingsFallback();
                }
                MainActivity.this.updateInfoLine();
            }
        });
        focusLabel(this.wifiButton, "WiFi");
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
        layoutParams2.leftMargin = 4;
        this.buttonPanel.addView(this.wifiButton, layoutParams2);
        this.zoomButton = new Button(this);
        this.zoomButton.setText("ZOOM");
        this.zoomButton.setTextSize(10.0f);
        this.zoomButton.setMinHeight(0);
        this.zoomButton.setMinWidth(0);
        this.zoomButton.setPadding(1, 0, 1, 0);
        this.zoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                try {
                    Intent launch = MainActivity.this.getPackageManager()
                            .getLaunchIntentForPackage("com.example.rokidzoomcamera");
                    if (launch == null) {
                        launch = new Intent();
                        launch.setComponent(new ComponentName(
                                "com.example.rokidzoomcamera",
                                "com.example.rokidzoomcamera.MainActivity"));
                    }
                    MainActivity.this.startActivity(launch);
                } catch (Exception e) {
                    MainActivity.this.setStatus("Zoom camera could not start", Color.YELLOW);
                    Log.w(MainActivity.TAG, "Zoom camera launch failed", e);
                }
            }
        });
        focusLabel(this.zoomButton, "ZOOM");
        LinearLayout.LayoutParams zoomLayout = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
        zoomLayout.leftMargin = 4;
        this.buttonPanel.addView(this.zoomButton, zoomLayout);
        this.buttonPanel.removeView(this.zoomButton);
        zoomLayout.leftMargin = 0;
        this.buttonPanel.addView(this.zoomButton, 0, zoomLayout);
        this.settingsButton = new Button(this);
        this.settingsButton.setText("APIキー設定");
        this.settingsButton.setText("API");
        this.settingsButton.setTextSize(11.0f);
        this.settingsButton.setMinHeight(0);
        this.settingsButton.setMinWidth(0);
        this.settingsButton.setPadding(2, 0, 2, 0);
        this.settingsButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.9
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                MainActivity.this.showApiKeyDialog();
            }
        });
        focusLabel(this.settingsButton, "API");
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(0, dp(38), 0.8f);
        layoutParams3.leftMargin = 4;
        this.settingsButton.setVisibility(8);
        this.imeButton = new Button(this);
        this.imeButton.setText("日本語入力を切替");
        this.imeButton.setText("IME");
        this.imeButton.setTextSize(7.0f);
        this.imeButton.setMinHeight(0);
        this.imeButton.setMinWidth(0);
        this.imeButton.setPadding(2, 0, 2, 0);
        this.imeButton.setFocusable(false);
        this.imeButton.setVisibility(8);
        this.imeButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.10
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                MainActivity.this.showInputMethodPicker();
            }
        });
        focusLabel(this.imeButton, "IME");
        linearLayout.addView(this.imeButton, new LinearLayout.LayoutParams(-1, dp(14)));
        this.readButtonPanel = new LinearLayout(this);
        this.readButtonPanel.setOrientation(0);
        this.scrollUpButton = new Button(this);
        this.scrollUpButton.setText("上へ");
        this.scrollUpButton.setText("UP");
        this.scrollUpButton.setTextSize(7.0f);
        this.scrollUpButton.setMinHeight(0);
        this.scrollUpButton.setMinWidth(0);
        this.scrollUpButton.setPadding(2, 0, 2, 0);
        this.scrollUpButton.setFocusable(false);
        this.scrollUpButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.11
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                if (MainActivity.this.answerScroll != null) {
                    MainActivity.this.answerScroll.smoothScrollBy(0, -220);
                }
            }
        });
        focusLabel(this.scrollUpButton, "UP");
        this.readButtonPanel.addView(this.scrollUpButton, new LinearLayout.LayoutParams(0, dp(14), 1.0f));
        this.scrollDownButton = new Button(this);
        this.scrollDownButton.setText("下へ");
        this.scrollDownButton.setText("DOWN");
        this.scrollDownButton.setTextSize(7.0f);
        this.scrollDownButton.setMinHeight(0);
        this.scrollDownButton.setMinWidth(0);
        this.scrollDownButton.setPadding(2, 0, 2, 0);
        this.scrollDownButton.setFocusable(false);
        this.scrollDownButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.12
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                if (MainActivity.this.answerScroll != null) {
                    MainActivity.this.answerScroll.smoothScrollBy(0, 220);
                }
            }
        });
        focusLabel(this.scrollDownButton, "DOWN");
        this.readButtonPanel.addView(this.scrollDownButton, new LinearLayout.LayoutParams(0, dp(14), 1.0f));
        this.proactiveOnButton = new Button(this);
        this.proactiveOnButton.setVisibility(8);
        this.proactiveOnButton.setText("PRO ON");
        this.proactiveOnButton.setTextSize(7.0f);
        this.proactiveOnButton.setMinHeight(0);
        this.proactiveOnButton.setMinWidth(0);
        this.proactiveOnButton.setPadding(2, 0, 2, 0);
        this.proactiveOnButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.13
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                MainActivity.this.setProactiveMode(true);
            }
        });
        focusLabel(this.proactiveOnButton, "PRO ON");
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(0, dp(14), 1.0f);
        layoutParams4.leftMargin = 4;
        this.readButtonPanel.addView(this.proactiveOnButton, layoutParams4);
        this.proactiveOffButton = new Button(this);
        this.proactiveOffButton.setVisibility(8);
        this.proactiveOffButton.setText("PRO OFF");
        this.proactiveOffButton.setTextSize(7.0f);
        this.proactiveOffButton.setMinHeight(0);
        this.proactiveOffButton.setMinWidth(0);
        this.proactiveOffButton.setPadding(2, 0, 2, 0);
        this.proactiveOffButton.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.14
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
                MainActivity.this.setProactiveMode(false);
            }
        });
        focusLabel(this.proactiveOffButton, "PRO OFF");
        LinearLayout.LayoutParams layoutParams5 = new LinearLayout.LayoutParams(0, dp(14), 1.0f);
        layoutParams5.leftMargin = 4;
        this.readButtonPanel.addView(this.proactiveOffButton, layoutParams5);
        Button button = new Button(this);
        button.setVisibility(8);
        button.setText("音声テスト");
        button.setTextSize(15.0f);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.15
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.speakWithRokidChunked("読み上げテストです。Rokidの女性音声で聞こえていますか。");
            }
        });
        this.readButtonPanel.addView(button, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button2 = new Button(this);
        button2.setVisibility(8);
        button2.setText("Beep");
        button2.setTextSize(15.0f);
        button2.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.16
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.playBeepTest();
            }
        });
        this.readButtonPanel.addView(button2, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button3 = new Button(this);
        button3.setVisibility(8);
        button3.setText("PhoneTTS");
        button3.setTextSize(15.0f);
        button3.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.17
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.speakWithPhoneTts("読み上げテストです。スマホ側のRokid音声で聞こえていますか。");
            }
        });
        this.readButtonPanel.addView(button3, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.readButtonPanel.setVisibility(8);
        linearLayout.addView(this.readButtonPanel);
        TextView textView2 = new TextView(this);
        textView2.setText("日本語変換中はEnterで確定、送信は「Geminiへ送信」ボタンを押してください。");
        textView2.setTextColor(-3355444);
        textView2.setTextSize(9.0f);
        textView2.setVisibility(8);
        linearLayout.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText("送信: Ctrl+Enter/F9  読み上げ: F8  スクロール: PageUp/PageDown");
        textView3.setTextColor(-3355444);
        textView3.setTextSize(9.0f);
        textView3.setVisibility(8);
        linearLayout.addView(textView3);
        this.topSpacer = new View(this);
        this.topSpacer.setVisibility(8);
        linearLayout.addView(this.topSpacer, new LinearLayout.LayoutParams(-1, 0, 0.0f));
        this.answerScroll = new ScrollView(this);
        this.answerScroll.setFillViewport(true);
        this.answerScroll.setBackgroundColor(Color.TRANSPARENT);
        this.answerScroll.setVerticalScrollBarEnabled(false);
        this.answerScroll.setVerticalFadingEdgeEnabled(false);
        this.answer = new TextView(this);
        this.answer.setText("");
        this.answer.setTextColor(-1);
        this.answer.setTextSize(11.0f);
        this.answer.setPadding(3, 2, 3, 4);
        this.answer.setTextIsSelectable(true);
        this.answer.setMovementMethod(new ScrollingMovementMethod());
        this.answer.setFocusable(false);
        this.answer.setFocusableInTouchMode(false);
        this.answer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (MainActivity.this.answerScroll != null) {
                    boolean hasText = s != null && s.toString().trim().length() > 0;
                    MainActivity.this.answerScroll.setVisibility(hasText ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        this.answer.setOnKeyListener(new View.OnKeyListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.18
            @Override // android.view.View.OnKeyListener
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() != 0 || MainActivity.this.answerScroll == null) {
                    return false;
                }
                if (i == 93 || i == 20 || i == 62) {
                    MainActivity.this.answerScroll.smoothScrollBy(0, 220);
                    return true;
                }
                if (i != 92 && i != 19) {
                    return false;
                }
                MainActivity.this.answerScroll.smoothScrollBy(0, -220);
                return true;
            }
        });
        this.answerScroll.addView(this.answer);
        this.answerScrollParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        linearLayout.addView(this.answerScroll, this.answerScrollParams);
        this.status = new TextView(this);
        this.status.setTextColor(-3355444);
        this.status.setTextSize(7.0f);
        linearLayout.addView(this.status);
        this.info = new TextView(this);
        this.info.setTextColor(-3355444);
        this.info.setTextSize(11.5f);
        this.info.setGravity(51);
        this.info.setPadding(2, 0, 2, 0);
        this.info.setLineSpacing(0.0f, 0.84f);
        this.input.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.19
            @Override // android.widget.TextView.OnEditorActionListener
            public boolean onEditorAction(TextView textView4, int i, KeyEvent keyEvent) {
                if (i == 4 || i == 6) {
                    MainActivity.this.sendCurrentText();
                    return true;
                }
                return false;
            }
        });
        FrameLayout frameLayout = new FrameLayout(this);
        this.hudRoot = frameLayout;
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -1));
        this.mascotView = new MascotView(this);
        FrameLayout.LayoutParams layoutParams6 = new FrameLayout.LayoutParams(dp(96), dp(96), 51);
        layoutParams6.leftMargin = dp(0);
        layoutParams6.topMargin = dp(34);
        frameLayout.addView(this.mascotView, layoutParams6);
        FrameLayout.LayoutParams layoutParams7 = new FrameLayout.LayoutParams(dp(312), dp(34), 51);
        layoutParams7.leftMargin = dp(0);
        layoutParams7.topMargin = dp(0);
        frameLayout.addView(this.buttonPanel, layoutParams7);
        FrameLayout.LayoutParams layoutParams8 = new FrameLayout.LayoutParams(dp(216), dp(96), 51);
        layoutParams8.leftMargin = dp(96);
        layoutParams8.topMargin = dp(34);
        frameLayout.addView(this.info, layoutParams8);
        setContentView(frameLayout);
        setMascotMode(0);
        updateInfoLine();
        showControlsTemporarily();
        hideKeyboard();
        if (this.zoomButton != null) {
            this.zoomButton.requestFocus();
        }
        this.handler.postDelayed(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.20
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.hideKeyboard();
                if (MainActivity.this.zoomButton != null) {
                    MainActivity.this.zoomButton.requestFocusFromTouch();
                }
            }
        }, 250L);
        this.input.setOnClickListener(new View.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.21
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showControlsTemporarily();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showApiKeyDialog() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(16, 4, 16, 4);
        final EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setHint("Google AI StudioのGemini APIキー");
        editText.setText(getPreferences().getString(KEY_API_KEY, ""));
        editText.setInputType(129);
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editText.setPadding(24, 8, 24, 8);
        linearLayout.addView(editText);
        final EditText editText2 = new EditText(this);
        editText2.setSingleLine(false);
        editText2.setMinLines(3);
        editText2.setMaxLines(6);
        editText2.setHint("カスタム指示 例: 私専用の秘書として、短く、予定とメールを優先して答える");
        editText2.setText(getPreferences().getString(KEY_CUSTOM_INSTRUCTIONS, "あなたはRokidグラス上の私専用の日本語秘書です。回答は必要なことを先に言い、そのあと理由や補足も含めて十分に詳しく答えてください。"));
        editText2.setInputType(147457);
        editText2.setPadding(24, 8, 24, 8);
        linearLayout.addView(editText2);
        final EditText bridgeToken = new EditText(this);
        bridgeToken.setSingleLine(true);
        bridgeToken.setHint("スマホ連携トークン");
        bridgeToken.setText(getPreferences().getString(KEY_BRIDGE_TOKEN, ""));
        bridgeToken.setInputType(129);
        bridgeToken.setTransformationMethod(PasswordTransformationMethod.getInstance());
        bridgeToken.setPadding(24, 8, 24, 8);
        linearLayout.addView(bridgeToken);
        new AlertDialog.Builder(this).setTitle("Gemini設定").setMessage("APIキーとカスタム指示を保存します。GeminiアプリのGem本体ではなく、このグラスアプリ用の指示です。").setView(linearLayout).setPositiveButton("保存", new DialogInterface.OnClickListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.22
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.getPreferences().edit().putString(MainActivity.KEY_API_KEY, editText.getText().toString().trim()).putString(MainActivity.KEY_CUSTOM_INSTRUCTIONS, editText2.getText().toString().trim()).putString(MainActivity.KEY_BRIDGE_TOKEN, bridgeToken.getText().toString().trim()).apply();
                MainActivity.this.setStatus("Gemini設定を保存しました", Color.rgb(90, 220, 120));
            }
        }).setNegativeButton("キャンセル", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, 0);
    }

    private int dp(int i) {
        return (int) ((i * getResources().getDisplayMetrics().density) + 0.5f);
    }

    private void initHeadPoseSensor() {
        try {
            this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (this.sensorManager != null) {
                this.headRotationSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR, true);
                if (this.headRotationSensor == null) {
                    this.headRotationSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                }
                if (this.headRotationSensor == null) {
                    this.headRotationSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "head pose sensor init failed", e);
        }
    }

    private void registerHeadPoseSensor() {
        if (this.sensorManager == null || this.headRotationSensor == null) {
            return;
        }
        this.sensorManager.registerListener(this, this.headRotationSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterHeadPoseSensor() {
        if (this.sensorManager != null) {
            try {
                this.sensorManager.unregisterListener(this);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null || event.values == null) {
            return;
        }
        float[] rotation = new float[9];
        float[] orientation = new float[3];
        try {
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            SensorManager.getOrientation(rotation, orientation);
        } catch (Exception e) {
            return;
        }
        float pitch = orientation[1];
        if (!this.neutralPitchReady) {
            if (this.neutralPitchSamples == 0) {
                this.neutralPitch = pitch;
            } else {
                this.neutralPitch = (this.neutralPitch * this.neutralPitchSamples + pitch) / (this.neutralPitchSamples + 1);
            }
            this.neutralPitchSamples++;
            if (this.neutralPitchSamples >= 4) {
                this.neutralPitchReady = true;
                this.lastUpwardGlanceAt = System.currentTimeMillis();
                this.handler.removeCallbacks(this.hideGlanceHudRunnable);
                this.handler.postDelayed(this.hideGlanceHudRunnable, 3000L);
                Log.i(TAG, "head pose calibrated pitch=" + this.neutralPitch);
            }
            return;
        }
        float delta = wrapAngle(pitch - this.neutralPitch);
        // Sensor mounting differs between Rokid revisions. Detect a deliberate
        // vertical head tilt in either pitch direction, with hysteresis.
        float tilt = Math.abs(delta);
        if (tilt >= 0.045f) {
            this.lastUpwardGlanceAt = System.currentTimeMillis();
            this.handler.removeCallbacks(this.hideGlanceHudRunnable);
            showHeadGlanceHud();
        } else if (tilt <= 0.022f) {
            this.handler.removeCallbacks(this.hideGlanceHudRunnable);
            this.handler.postDelayed(this.hideGlanceHudRunnable, 3000L);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private float wrapAngle(float value) {
        while (value > Math.PI) {
            value -= (float) (Math.PI * 2.0);
        }
        while (value < -Math.PI) {
            value += (float) (Math.PI * 2.0);
        }
        return value;
    }

    private void setGlanceHudVisible(boolean visible) {
        if (this.hudRoot == null) {
            return;
        }
        if (this.glanceHudVisible == visible) {
            setScreenBrightness(visible ? (this.headGlanceWake ? 0.28f : -1.0f) : 0.0f);
            return;
        }
        this.glanceHudVisible = visible;
        this.hudRoot.animate().cancel();
        if (visible) {
            bringTaskForwardForGlance();
            restoreNormalScreenTimeout();
            wakeDisplayForGlance();
            setScreenBrightness(this.headGlanceWake ? 0.28f : -1.0f);
            this.hudRoot.setVisibility(View.VISIBLE);
            this.hudRoot.animate().alpha(1.0f).setDuration(100L).start();
        } else {
            requestFastDisplaySleep();
            setScreenBrightness(0.01f);
            this.hudRoot.animate().alpha(0.0f).setDuration(140L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (!MainActivity.this.glanceHudVisible && MainActivity.this.hudRoot != null) {
                        MainActivity.this.hudRoot.setVisibility(View.INVISIBLE);
                    }
                }
            }).start();
        }
    }

    private void showHeadGlanceHud() {
        this.headGlanceWake = true;
        this.hudHoldUntil = Math.max(this.hudHoldUntil, System.currentTimeMillis() + 3000L);
        if (this.input != null) {
            String draft = this.input.getText() == null ? "" : this.input.getText().toString().trim();
            if (draft.length() == 0) {
                this.input.clearFocus();
                this.input.setVisibility(View.GONE);
            }
        }
        if (this.answerScroll != null && this.answer != null) {
            String response = this.answer.getText() == null ? "" : this.answer.getText().toString().trim();
            this.answerScroll.setVisibility(response.length() == 0 ? View.GONE : View.VISIBLE);
        }
        if (this.status != null && !this.conversationActive && !this.geminiRequestActive && !this.voiceRecording) {
            this.status.setVisibility(View.GONE);
        }
        setGlanceHudVisible(true);
    }

    private void bringTaskForwardForGlance() {
        if (this.activityForeground) {
            return;
        }
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "could not bring glance HUD forward", e);
        }
    }

    private void rememberNormalScreenTimeout() {
        try {
            int current = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 6000);
            if (current >= 2000) this.normalScreenTimeoutMs = current;
        } catch (Exception e) {
            Log.w(TAG, "screen timeout read failed", e);
        }
    }

    private void requestFastDisplaySleep() {
        try {
            getWindow().clearFlags(128);
            if (Settings.System.canWrite(this)) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1000);
            }
        } catch (Exception e) {
            Log.w(TAG, "fast display sleep failed", e);
        }
    }

    private void restoreNormalScreenTimeout() {
        try {
            if (Settings.System.canWrite(this)) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, this.normalScreenTimeoutMs);
            }
        } catch (Exception e) {
            Log.w(TAG, "screen timeout restore failed", e);
        }
    }

    private void wakeDisplayForGlance() {
        try {
            PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (manager != null && !manager.isInteractive()) {
                PowerManager.WakeLock wake = manager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        TAG + ":glance");
                wake.acquire(1800L);
            }
            if (android.os.Build.VERSION.SDK_INT >= 27) {
                setTurnScreenOn(true);
            }
        } catch (Exception e) {
            Log.w(TAG, "display wake failed", e);
        }
    }

    private void holdDisplayForNotification() {
        try {
            PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (manager != null) {
                if (this.notificationWakeLock == null) {
                    this.notificationWakeLock = manager.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            TAG + ":codexNotification");
                    this.notificationWakeLock.setReferenceCounted(false);
                }
                this.notificationWakeLock.acquire(6500L);
            }
            getWindow().addFlags(128);
            this.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!MainActivity.this.conversationActive) {
                        try {
                            MainActivity.this.getWindow().clearFlags(128);
                        } catch (Exception ignored) {
                        }
                    }
                    if (MainActivity.this.notificationWakeLock != null
                            && MainActivity.this.notificationWakeLock.isHeld()) {
                        try {
                            MainActivity.this.notificationWakeLock.release();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }, 6500L);
        } catch (Exception e) {
            Log.w(TAG, "notification display hold failed", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMascotMode(int i) {
        this.mascotMode = i;
        if (this.mascotView != null) {
            this.mascotView.setMode(i);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMascotExpression(int i) {
        if (this.mascotView != null) {
            this.mascotView.setExpression(i);
        }
    }

    private void requestWifiOnForStartup() {
        maintainWifiConnection(true);
        this.handler.postDelayed(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.4
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.maintainWifiConnection(true);
                MainActivity.this.updateInfoLine();
            }
        }, 1500L);
    }

    private boolean containsAny(String str, String... keywords) {
        if (str == null || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && keyword.length() > 0 && str.contains(keyword.toLowerCase(Locale.JAPAN))) {
                return true;
            }
        }
        return false;
    }

    private int chooseMascotExpressionForText(String str, String str2) {
        String str3 = ((str == null ? "" : str) + "\n" + (str2 == null ? "" : str2)).toLowerCase(Locale.JAPAN);
        if (containsAny(str3, "キス", "kiss", "セクシ", "色っぽ", "艶", "色気", "えっち", "甘えて", "誘惑", "口説", "抱いて", "好き", "ドキドキ")) {
            return 12;
        }
        if (containsAny(str3, "感じて", "感じる", "気持ちいい", "うっとり", "とろけ", "悩ましい", "恍惚", "官能", "濡れ", "火照", "喘", "sensual", "sexy")) {
            return 13;
        }
        if (containsAny(str3, "恥ずかし", "照れ", "照れる", "照れて", "赤面", "はずかし", "かわいい", "可愛い")) {
            return 8;
        }
        if (str3.contains("えっち") || str3.contains("エッチ") || str3.contains("色っぽ") || str3.contains("セクシ") || str3.contains("キス") || str3.contains("kiss") || str3.contains("おっぱい") || str3.contains("胸") || str3.contains("下着")) {
            return 12;
        }
        if (str3.contains("恥") || str3.contains("照") || str3.contains("照れ") || str3.contains("好き") || str3.contains("かわいい")) {
            return 8;
        }
        if (str3.contains("ありがとう") || str3.contains("嬉") || str3.contains("楽しい") || str3.contains("成功") || str3.contains("よかった") || str3.contains("いい感じ")) {
            return 1;
        }
        if (str3.contains("驚") || str3.contains("びっくり") || str3.contains("意外") || str3.contains("まさか")) {
            return 11;
        }
        if (str3.contains("悲") || str3.contains("残念") || str3.contains("泣") || str3.contains("つら") || str3.contains("疲れ")) {
            return 10;
        }
        if (str3.contains("怒") || str3.contains("だめ") || str3.contains("失敗") || str3.contains("ポンコツ") || str3.contains("違う")) {
            return 15;
        }
        if (str3.contains("エラー") || str3.contains("通信") || str3.contains("wi-fi") || str3.contains("wifi") || str3.contains("できません")) {
            return 6;
        }
        if (str3.contains("ニュース") || str3.contains("会見") || str3.contains("ファクト") || str3.contains("調べ") || str3.contains("検索")) {
            return 2;
        }
        if (str3.contains("予定") || str3.contains("カレンダー") || str3.contains("明日") || str3.contains("今日") || str3.contains("来週") || str3.contains("来月")) {
            return 7;
        }
        if (str3.contains("メール") || str3.contains("通知")) {
            return 2;
        }
        if (str3.contains("?") || str3.contains("？") || str3.contains("なぜ") || str3.contains("どう")) {
            return 14;
        }
        return 4;
    }

    private void updateMascotForStatus(String str, int i) {
        if (this.mascotView != null) {
            if (this.mascotMode == 2) {
                return;
            }
            if (str == null) {
                str = "";
            }
            if (i == -65536 || str.contains("エラー") || str.contains("できません")) {
                setMascotExpression((str.contains("通信") || str.contains("Wi-Fi")) ? 6 : 6);
                return;
            }
            if (i == -256 || str.contains("待機") || str.contains("確認")) {
                setMascotExpression(2);
                return;
            }
            if (str.contains("ニュース")) {
                setMascotExpression(2);
                return;
            }
            if (str.contains("メール")) {
                setMascotExpression(1);
                return;
            }
            if (str.contains("予定")) {
                setMascotExpression(7);
                return;
            }
            if (str.contains("音声") || str.contains("聞いて")) {
                setMascotExpression(14);
            } else if (i == Color.rgb(90, 220, 120)) {
                setMascotExpression(1);
            } else {
                setMascotExpression(0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showControlsTemporarily() {
        this.headGlanceWake = false;
        setGlanceHudVisible(true);
        this.lastUpwardGlanceAt = System.currentTimeMillis();
        if (this.conversationActive) {
            setScreenBrightness(-1.0f);
            scheduleConversationDim();
        }
        if (this.proactiveMode) {
            applyProactiveLayout(true);
            return;
        }
        hideInputIfIdle();
        if (this.answerScroll != null && this.answer != null) {
            String response = this.answer.getText() == null ? "" : this.answer.getText().toString().trim();
            this.answerScroll.setVisibility(response.length() == 0 ? View.GONE : View.VISIBLE);
        }
        if (this.status != null) {
            this.status.setVisibility((this.conversationActive || this.geminiRequestActive || this.voiceRecording) ? View.VISIBLE : View.GONE);
        }
        if (this.buttonPanel != null) {
            this.buttonPanel.setVisibility(0);
        }
        if (this.imeButton != null) {
            this.imeButton.setVisibility(8);
        }
        if (this.readButtonPanel != null) {
            this.readButtonPanel.setVisibility(8);
        }
        applyProactiveLayout(false);
        setControlAlpha(1.0f);
        this.handler.removeCallbacks(this.hideControlsRunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideControls() {
        this.proactiveMode = false;
        hideInputIfIdle();
        if (this.buttonPanel != null) {
            this.buttonPanel.setVisibility(0);
        }
        if (this.imeButton != null) {
            this.imeButton.setVisibility(8);
        }
        if (this.readButtonPanel != null) {
            this.readButtonPanel.setVisibility(8);
        }
        applyProactiveLayout(false);
        setControlAlpha(1.0f);
        if (!this.conversationActive) {
            try {
                getWindow().clearFlags(128);
            } catch (Exception e) {
            }
        }
    }

    private void setControlAlpha(float f) {
        if (this.input != null) {
            this.input.setAlpha(f);
        }
        if (this.buttonPanel != null) {
            this.buttonPanel.setAlpha(f);
        }
        if (this.imeButton != null) {
            this.imeButton.setAlpha(f);
        }
        if (this.readButtonPanel != null) {
            this.readButtonPanel.setAlpha(f);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyProactiveLayout(boolean z) {
        if (this.input != null) {
            this.input.setVisibility(z ? 8 : this.input.getVisibility());
        }
        if (this.buttonPanel != null) {
            this.buttonPanel.setVisibility(z ? 8 : this.buttonPanel.getVisibility());
        }
        if (this.imeButton != null) {
            this.imeButton.setVisibility(z ? 8 : this.imeButton.getVisibility());
        }
        if (this.readButtonPanel != null) {
            this.readButtonPanel.setVisibility(z ? 8 : this.readButtonPanel.getVisibility());
        }
        if (this.status != null) {
            this.status.setVisibility(z ? 8 : 0);
        }
        if (this.topSpacer != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.topSpacer.getLayoutParams();
            layoutParams.height = 0;
            layoutParams.weight = z ? 2.0f : 0.0f;
            this.topSpacer.setLayoutParams(layoutParams);
            this.topSpacer.setVisibility(z ? 0 : 8);
        }
        if (this.answerScroll != null && this.answerScrollParams != null) {
            this.answerScrollParams.height = 0;
            this.answerScrollParams.weight = 1.0f;
            this.answerScroll.setLayoutParams(this.answerScrollParams);
        }
        if (this.answer != null) {
            this.answer.setTextSize(z ? 10.0f : 11.0f);
            this.answer.setGravity(z ? 80 : 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setConversationActive(boolean z) {
        this.conversationActive = z;
        if (z) {
            getWindow().addFlags(128);
            acquireConversationWakeLock();
            setScreenBrightness(-1.0f);
            scheduleConversationDim();
            showControlsTemporarily();
            if (this.mascotMode != 2) {
                setMascotMode(1);
                return;
            }
            return;
        }
        getWindow().clearFlags(128);
        this.handler.removeCallbacks(this.dimConversationRunnable);
        releaseConversationWakeLock();
        setScreenBrightness(-1.0f);
        this.handler.removeCallbacks(this.hideControlsRunnable);
        setMascotMode(0);
    }

    private void initConversationWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService("power");
            if (powerManager != null) {
                this.conversationWakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                        TAG + ":conversation");
                this.conversationWakeLock.setReferenceCounted(false);
            }
        } catch (Exception e) {
            Log.w(TAG, "initConversationWakeLock failed", e);
        }
    }

    private void acquireConversationWakeLock() {
        try {
            if (this.conversationWakeLock != null) {
                if (this.conversationWakeLock.isHeld()) {
                    this.conversationWakeLock.release();
                }
                this.conversationWakeLock.acquire(600000L);
            }
        } catch (Exception e) {
            Log.w(TAG, "acquireConversationWakeLock failed", e);
        }
    }

    private void releaseConversationWakeLock() {
        try {
            if (this.conversationWakeLock != null && this.conversationWakeLock.isHeld()) {
                this.conversationWakeLock.release();
            }
        } catch (Exception e) {
            Log.w(TAG, "releaseConversationWakeLock failed", e);
        }
    }

    private void scheduleConversationDim() {
        this.handler.removeCallbacks(this.dimConversationRunnable);
        // Keep full visibility while an answer is generated or spoken.
    }

    private void setScreenBrightness(float brightness) {
        try {
            android.view.WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.screenBrightness = brightness;
            getWindow().setAttributes(attributes);
        } catch (Exception e) {
            Log.w(TAG, "setScreenBrightness failed", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void keepScreenAwakeFor(long j) {
        setConversationActive(true);
        final int i = this.requestGeneration;
        final int i2 = this.ttsGeneration;
        this.handler.postDelayed(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.23
            @Override // java.lang.Runnable
            public void run() {
                if (i == MainActivity.this.requestGeneration && i2 == MainActivity.this.ttsGeneration && !MainActivity.this.geminiRequestActive && !MainActivity.this.voiceRecording && !MainActivity.this.voiceLoopMode && !MainActivity.this.proactiveMode) {
                    MainActivity.this.setConversationActive(false);
                }
            }
        }, j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopCurrentActivity(String str) {
        if (str == null) {
            str = "Stopped";
        }
        emergencyStop(str, false, true);
    }

    private void emergencyStop(String str, boolean z, boolean z2) {
        this.requestGeneration++;
        this.ttsGeneration++;
        this.proactiveMode = false;
        this.voiceLoopMode = false;
        this.voiceRecording = false;
        releaseSpeechRecognizer();
        this.geminiRequestActive = false;
        disconnectActiveGemini();
        try {
            if (this.voiceThread != null) {
                this.voiceThread.interrupt();
            }
        } catch (Exception e) {
        }
        try {
            if (this.proactiveThread != null) {
                this.proactiveThread.interrupt();
            }
        } catch (Exception e2) {
        }
        try {
            getWindow().clearFlags(128);
        } catch (Exception e3) {
        }
        this.handler.removeCallbacks(this.dimConversationRunnable);
        releaseConversationWakeLock();
        setScreenBrightness(-1.0f);
        this.conversationActive = false;
        setMascotMode(0);
        this.handler.removeCallbacks(this.hideControlsRunnable);
        if (this.voiceButton != null) {
            this.voiceButton.setText("VOICE");
        }
        applyProactiveLayout(false);
        if (this.input != null) {
            this.input.setVisibility(8);
        }
        setStatus(str == null ? "Stopped" : str, -256);
        if (z2) {
            if (str == null) {
                str = "Stopped";
            }
            logToPhoneAsync("Operation", str);
        }
        this.handler.postDelayed(this.hideControlsRunnable, 1200L);
        if (z) {
            finish();
        }
    }

    private void disconnectActiveGemini() {
        HttpURLConnection httpURLConnection = this.activeGeminiConnection;
        this.activeGeminiConnection = null;
        if (httpURLConnection != null) {
            try {
                httpURLConnection.disconnect();
                Log.i(TAG, "active Gemini connection disconnected");
            } catch (Exception e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateInfoLine() {
        String str;
        if (this.info == null) {
            return;
        }
        maintainWifiConnection(false);
        Date date = new Date();
        String str2 = new SimpleDateFormat("yyyy/M/d", Locale.JAPAN).format(date);
        String str3 = new SimpleDateFormat("HH:mm", Locale.JAPAN).format(date);
        String[] strArr = {"日", "月", "火", "水", "木", "金", "土"};
        Calendar calendar = Calendar.getInstance(Locale.JAPAN);
        String str4 = strArr[Math.max(0, Math.min(6, calendar.get(7) - 1))];
        String batteryLabel = readBatteryLabel();
        String str5 = wifiShortState();
        String str6 = isNetworkReady() ? "NET OK" : "NET NG";
        if (!isGeminiCoolingDown()) {
            str = "";
        } else {
            str = "  WAIT " + Math.max(1L, ((this.geminiCooldownUntil - System.currentTimeMillis()) + 999) / 1000) + "s";
        }
        String str7 = str2 + "(" + str4 + ") " + str3;
        String healthLine = compactHealthInfoLine();
        String codexLine = this.codexStatusLine == null ? "CODEX --" : this.codexStatusLine;
        String str8 = str7 + "\n" + batteryLabel + "  " + str5 + "/" + str6 + str + "\n" + healthLine + "\n" + codexLine;
        SpannableString spannableString = new SpannableString(str8);
        spannableString.setSpan(new RelativeSizeSpan(1.70f), 0, str7.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int healthStart = str8.indexOf(healthLine);
        if (healthStart >= 0) {
            spannableString.setSpan(new RelativeSizeSpan(1.05f), healthStart, healthStart + healthLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        this.info.setText(spannableString);
    }

    private void pollCodexStatusAsync() {
        if (this.codexPollInFlight) return;
        this.codexPollInFlight = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    JSONObject latest = null;
                    try {
                        JSONObject phone = new JSONObject(MainActivity.this.fetchPhoneEndpointJson("codex"));
                        if (phone.optBoolean("ok", false)) latest = phone;
                    } catch (Exception phoneError) {
                        Log.w(TAG, "phone Codex status unavailable", phoneError);
                    }
                    if (latest != null) {
                        final String state = latest.optString("state", "").toLowerCase(Locale.US);
                        final long updated = latest.optLong("updatedAt", latest.optLong("time", 0L));
                        final String key = state + ":" + updated;
                        String label = "CODEX --";
                        if ("working".equals(state)) label = "CODEX ● 作業中";
                        else if ("waiting".equals(state)) label = "CODEX ! 許可待ち";
                        else if ("done".equals(state)) label = "CODEX ✓ 回答完了";
                        else if ("failed".equals(state)) label = "CODEX × エラー";
                        final String display = label;
                        MainActivity.this.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                boolean changed = !key.equals(MainActivity.this.codexEventKey);
                                MainActivity.this.codexEventKey = key;
                                MainActivity.this.codexStatusLine = display;
                                MainActivity.this.updateInfoLine();
                                if (changed && ("waiting".equals(state) || "done".equals(state) || "failed".equals(state))) {
                                    MainActivity.this.hudHoldUntil = System.currentTimeMillis() + 6000L;
                                    MainActivity.this.handler.removeCallbacks(MainActivity.this.hideGlanceHudRunnable);
                                    MainActivity.this.setGlanceHudVisible(true);
                                    MainActivity.this.holdDisplayForNotification();
                                    MainActivity.this.handler.postDelayed(MainActivity.this.hideGlanceHudRunnable, 6050L);
                                    if (updated > 0L && Math.abs(System.currentTimeMillis() - updated) < 60000L) {
                                        String spoken = "done".equals(state)
                                                ? "お知らせします。回答が届きました。"
                                                : "waiting".equals(state)
                                                ? "確認をお願いします。作業の許可が必要です。"
                                                : "お知らせします。作業中にエラーが発生しました。";
                                        // The local Rokid engine only synthesizes a small static phrase map.
                                        // Route notification prose through the phone TTS path so arbitrary
                                        // Japanese is spoken through the glasses.
                                        MainActivity.this.speakWithPhoneTts(spoken);
                                    }
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Codex status poll failed", e);
                } finally {
                    if (connection != null) connection.disconnect();
                    MainActivity.this.codexPollInFlight = false;
                }
            }
        }, "CodexStatusPoll").start();
    }

    private String compactHealthInfoLine() {
        String value = this.healthCompactLine == null ? "" : this.healthCompactLine.trim();
        if (value.length() == 0) {
            return "歩数 --";
        }
        value = prioritizeHealthLine(value);
        if (value.length() > 60) {
            value = value.substring(0, 33) + "…";
        }
        long age = this.healthUpdatedAt <= 0L ? 0L : System.currentTimeMillis() - this.healthUpdatedAt;
        if (age > 7200000L) {
            return value + " old";
        }
        return value;
    }

    private String prioritizeHealthLine(String value) {
        String steps = healthMetric(value, "歩数 ", "歩:", "steps:");
        StringBuilder result = new StringBuilder("歩数 ");
        result.append(steps.length() == 0 ? "--" : steps);
        return result.toString();
    }

    private String healthMetric(String text, String... labels) {
        if (text == null) return "";
        for (String label : labels) {
            int start = text.indexOf(label);
            if (start < 0) continue;
            start += label.length();
            int end = start;
            while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
            return text.substring(start, end).trim();
        }
        return "";
    }

    private void pollPhoneHealthAsync() {
        if (this.healthPollInFlight) {
            return;
        }
        this.healthPollInFlight = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json = new JSONObject(MainActivity.this.fetchPhoneEndpointJson("health"));
                    final String compact = json.optString("compact", "").trim();
                    final long time = json.optLong("time", 0L);
                    MainActivity.this.healthCompactLine = compact;
                    MainActivity.this.healthUpdatedAt = time;
                    MainActivity.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateInfoLine();
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "pollPhoneHealth failed", e);
                } finally {
                    MainActivity.this.healthPollInFlight = false;
                }
            }
        }, "PhoneHealthPoll").start();
    }

    private String wifiShortState() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi");
            if (wifiManager == null) {
                return "WiFi --";
            }
            if (!wifiManager.isWifiEnabled()) {
                return "WiFi OFF";
            }
            android.net.wifi.WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo == null || connectionInfo.getNetworkId() < 0) {
                return "WiFi NC";
            }
            String ssid = connectionInfo.getSSID();
            if (ssid == null || ssid.length() == 0 || "<unknown ssid>".equalsIgnoreCase(ssid)) {
                return "WiFi LINK";
            }
            ssid = ssid.replace("\"", "");
            if (ssid.length() > 8) {
                ssid = ssid.substring(0, 8);
            }
            return "WiFi " + ssid;
        } catch (Exception e) {
            return isWifiEnabled() ? "WiFi ON" : "WiFi OFF";
        }
    }

    private int readBatteryPercent() {
        try {
            Intent intentRegisterReceiver = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (intentRegisterReceiver == null) {
                return -1;
            }
            int intExtra = intentRegisterReceiver.getIntExtra("level", -1);
            int intExtra2 = intentRegisterReceiver.getIntExtra("scale", -1);
            if (intExtra >= 0 && intExtra2 > 0) {
                return Math.round((intExtra * 100.0f) / intExtra2);
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String readBatteryLabel() {
        try {
            Intent intentRegisterReceiver = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (intentRegisterReceiver == null) {
                return "--%";
            }
            int intExtra = intentRegisterReceiver.getIntExtra("level", -1);
            int intExtra2 = intentRegisterReceiver.getIntExtra("scale", -1);
            int intExtra3 = intentRegisterReceiver.getIntExtra("status", -1);
            int intExtra4 = intentRegisterReceiver.getIntExtra("plugged", 0);
            String str = (intExtra < 0 || intExtra2 <= 0) ? "--%" : Math.round((intExtra * 100.0f) / intExtra2) + "%";
            if (intExtra3 == 5) {
                return str + " 満充電";
            }
            if (intExtra3 == 2 || intExtra4 > 0) {
                return str + " 充電中";
            }
            return str;
        } catch (Exception e) {
            return "--%";
        }
    }

    private void focusLabel(final Button button, final String str) {
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setTextColor(Color.rgb(90, 255, 130));
        try {
            button.setStateListAnimator(null);
        } catch (Exception e) {
        }
        button.setText("  " + str + "  ");
        button.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.example.rokidkeyboardbridge.MainActivity.24
            @Override // android.view.View.OnFocusChangeListener
            public void onFocusChange(View view, boolean z) {
                String str2;
                Button button2 = button;
                button2.setBackgroundColor(Color.TRANSPARENT);
                if (z) {
                    str2 = ">>>> " + str + " <<<<";
                } else {
                    str2 = "  " + str + "  ";
                }
                button2.setText(str2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendCurrentText() {
        final String strTrim = this.input.getText().toString().trim();
        if (strTrim.isEmpty()) {
            setStatus("質問を入力してください", -256);
            hideKeyboard();
            if (this.sendButton != null) {
                this.sendButton.requestFocus();
                return;
            }
            return;
        }
        if (handleLocalCommand(strTrim) || handleDirectDataQuestion(strTrim) || handleUnsupportedNewsQuestion(strTrim) || handleSmallTalkQuestion(strTrim)) {
            return;
        }
        final String strTrim2 = getPreferences().getString(KEY_API_KEY, "").trim();
        if (strTrim2.isEmpty()) {
            setStatus("先にAPIキーを設定してください", -256);
            showApiKeyDialog();
            return;
        }
        if (handleLocalCommand(strTrim)) {
            return;
        }
        String strApplyCustomInstructionFromText = applyCustomInstructionFromText(strTrim);
        if (strApplyCustomInstructionFromText != null) {
            this.answer.setText(strApplyCustomInstructionFromText);
            setStatus("カスタム指示を更新しました", Color.rgb(90, 220, 120));
            logToPhoneAsync("カスタム指示", strApplyCustomInstructionFromText);
            return;
        }
        if (isGeminiCoolingDown()) {
            showGeminiCooldown();
            return;
        }
        if (!isNetworkReady()) {
            setStatus("Wi-Fiが未接続です。グラスのWi-Fiを確認してください。", -65536);
            this.answer.setText("通信できません。\nグラス本体のWi-Fiがオフ、またはインターネット未接続です。");
            return;
        }
        this.sendButton.setEnabled(false);
        this.ttsGeneration++;
        this.voiceLoopMode = false;
        this.voiceRecording = false;
        hideKeyboard();
        this.answer.setText("考えています…");
        setStatus("Geminiへ接続中", -3355444);
        this.handler.removeCallbacks(this.hideInputRunnable);
        this.handler.postDelayed(this.hideInputRunnable, 3500L);
        setMascotExpression(isGreetingPrompt(strTrim) ? 1 : 14);
        setConversationActive(true);
        final int i = this.requestGeneration + 1;
        this.requestGeneration = i;
        this.geminiRequestActive = true;
        logToPhoneAsync("ユーザー", strTrim);
        new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.25
            @Override // java.lang.Runnable
            public void run() {
                try {
                    final String strRequestGeminiWithRetry = MainActivity.this.requestGeminiWithRetry(strTrim2, MainActivity.this.buildGeminiPromptCompact(strTrim));
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.25.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (i == MainActivity.this.requestGeneration) {
                                MainActivity.this.geminiRequestActive = false;
                                MainActivity.this.sendButton.setEnabled(true);
                                MainActivity.this.answer.setText(strRequestGeminiWithRetry);
                                MainActivity.this.setMascotExpression(MainActivity.this.chooseMascotExpressionForText(strTrim, strRequestGeminiWithRetry));
                                MainActivity.this.scrollAnswerToTop();
                                MainActivity.this.setStatus("回答を受信しました", Color.rgb(90, 220, 120));
                                MainActivity.this.logToPhoneAsync("Gemini", strRequestGeminiWithRetry);
                                MainActivity.this.speakWithPhoneTtsChunked(strRequestGeminiWithRetry);
                            }
                        }
                    });
                } catch (Exception e) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.25.2
                        @Override // java.lang.Runnable
                        public void run() {
                            if (i != MainActivity.this.requestGeneration) {
                                return;
                            }
                            if ((e instanceof GeminiHttpException) && ((GeminiHttpException) e).isRetryable()) {
                                MainActivity.this.beginGeminiCooldown(((GeminiHttpException) e).cooldownMs());
                            }
                            MainActivity.this.geminiRequestActive = false;
                            MainActivity.this.sendButton.setEnabled(true);
                            MainActivity.this.answer.setText("通信エラー\n" + e.getMessage());
                            MainActivity.this.setMascotExpression(11);
                            MainActivity.this.setStatus("Wi-Fi・APIキー・利用枠を確認してください", -65536);
                            MainActivity.this.setConversationActive(false);
                        }
                    });
                }
            }
        }, "GeminiRequest").start();
    }

    private boolean handleDirectDataQuestion(final String str) {
        if (!isScheduleQuestion(str) && !isMailQuestion(str)) {
            return false;
        }
        this.sendButton.setEnabled(false);
        this.ttsGeneration++;
        this.voiceLoopMode = false;
        this.voiceRecording = false;
        hideKeyboard();
        this.answer.setText("スマホの実データを確認中…");
        setStatus("Geminiなしで確認中", -3355444);
        setMascotExpression(isMailQuestion(str) ? 1 : 7);
        setConversationActive(true);
        final int i = this.requestGeneration + 1;
        this.requestGeneration = i;
        logToPhoneAsync("ユーザー", str);
        new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.26
            @Override // java.lang.Runnable
            public void run() {
                final String strBuildDirectScheduleText;
                try {
                    if (MainActivity.this.isMailQuestion(str)) {
                        strBuildDirectScheduleText = MainActivity.this.buildDirectMailText(MainActivity.this.fetchRecentMailJson());
                    } else {
                        ScheduleRange scheduleRangeDetectScheduleRange = MainActivity.this.detectScheduleRange(str);
                        strBuildDirectScheduleText = MainActivity.this.buildDirectScheduleText(MainActivity.this.fetchScheduleJson(scheduleRangeDetectScheduleRange), scheduleRangeDetectScheduleRange);
                    }
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.26.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (i == MainActivity.this.requestGeneration) {
                                MainActivity.this.sendButton.setEnabled(true);
                                MainActivity.this.answer.setText(strBuildDirectScheduleText);
                                MainActivity.this.setMascotExpression(MainActivity.this.chooseMascotExpressionForText(str, strBuildDirectScheduleText));
                                MainActivity.this.scrollAnswerToTop();
                                MainActivity.this.setStatus("実データで回答しました", Color.rgb(90, 220, 120));
                                MainActivity.this.logToPhoneAsync("直接回答", strBuildDirectScheduleText);
                                MainActivity.this.speakWithPhoneTtsChunked(strBuildDirectScheduleText);
                            }
                        }
                    });
                } catch (Exception e) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.26.2
                        @Override // java.lang.Runnable
                        public void run() {
                            if (i == MainActivity.this.requestGeneration) {
                                MainActivity.this.sendButton.setEnabled(true);
                                MainActivity.this.answer.setText("スマホの実データを取得できませんでした。\n" + e.getMessage());
                                MainActivity.this.setMascotExpression(2);
                                MainActivity.this.setStatus("スマホ実データ取得エラー", -65536);
                                MainActivity.this.setConversationActive(false);
                            }
                        }
                    });
                }
            }
        }, "DirectDataRequest").start();
        return true;
    }

    private boolean handleUnsupportedNewsQuestion(String str) {
        if (!isNewsQuestion(str)) {
            return false;
        }
        this.sendButton.setEnabled(false);
        this.ttsGeneration++;
        this.voiceLoopMode = false;
        this.voiceRecording = false;
        hideKeyboard();
        this.answer.setText("ニュースを取得しています…");
        setStatus("スマホからニュース取得中", -3355444);
        setMascotExpression(15);
        setConversationActive(true);
        final int i = this.requestGeneration + 1;
        this.requestGeneration = i;
        final String strExtractNewsQuery = extractNewsQuery(str);
        final boolean zIsReputationNewsQuery = isReputationNewsQuery(str);
        logToPhoneAsync("ユーザー", str);
        new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.27
            @Override // java.lang.Runnable
            public void run() {
                try {
                    final String strBuildDirectNewsText = MainActivity.this.buildDirectNewsText(MainActivity.this.fetchNewsJson(strExtractNewsQuery), strExtractNewsQuery, zIsReputationNewsQuery);
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.27.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (i != MainActivity.this.requestGeneration) {
                                return;
                            }
                            MainActivity.this.sendButton.setEnabled(true);
                            MainActivity.this.answer.setText(strBuildDirectNewsText);
                            MainActivity.this.setMascotExpression(MainActivity.this.chooseMascotExpressionForText(strExtractNewsQuery, strBuildDirectNewsText));
                            MainActivity.this.scrollAnswerToTop();
                            MainActivity.this.setStatus("ニュースを取得しました", Color.rgb(90, 220, 120));
                            MainActivity.this.logToPhoneAsync("直接回答", strBuildDirectNewsText);
                            MainActivity.this.speakWithPhoneTtsChunked(strBuildDirectNewsText);
                        }
                    });
                } catch (Exception e) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.27.2
                        @Override // java.lang.Runnable
                        public void run() {
                            if (i != MainActivity.this.requestGeneration) {
                                return;
                            }
                            MainActivity.this.sendButton.setEnabled(true);
                            String str2 = "ニュースを取得できませんでした。\nスマホ側アプリを開き、スマホの通信状態を確認してください。\n" + e.getMessage();
                            MainActivity.this.answer.setText(str2);
                            MainActivity.this.setMascotExpression(6);
                            MainActivity.this.setStatus("ニュース取得エラー", -256);
                            MainActivity.this.logToPhoneAsync("直接回答", str2);
                            MainActivity.this.setConversationActive(false);
                        }
                    });
                }
            }
        }, "DirectNewsRequest").start();
        return true;
    }

    private boolean isGreetingPrompt(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.trim().toLowerCase(Locale.JAPAN);
        if (lowerCase.length() > 40) {
            return false;
        }
        return lowerCase.contains("おはよう")
                || lowerCase.contains("こんにちは")
                || lowerCase.contains("こんばんは")
                || lowerCase.contains("やあ")
                || lowerCase.equals("hi")
                || lowerCase.startsWith("hi ")
                || lowerCase.contains("hello");
    }

    private boolean handleSmallTalkQuestion(String str) {
        if (str == null) {
            return false;
        }
        String value = str.trim();
        String lowerCase = value.toLowerCase(Locale.JAPAN);
        String response = null;
        int expression = 1;
        if (isGreetingPrompt(value)) {
            response = "おはようございます。今日もそばで手伝います。予定、メール、ニュース、どれから見ますか。";
            expression = 1;
        } else if (value.contains("キス") || lowerCase.contains("kiss")) {
            response = "ふふ、気持ちは受け取りました。私は秘書として近くにいます。今は用件を一つください。すぐ動きます。";
            expression = 12;
        } else if (value.contains("ありがとう") || lowerCase.contains("thanks") || lowerCase.contains("thank you")) {
            response = "どういたしまして。必要な時に短く、すぐ返します。";
            expression = 7;
        } else if (value.contains("疲れた") || value.contains("つかれた")) {
            response = "少し休みましょう。今は大事なものだけ拾います。予定かメールを確認しますか。";
            expression = 6;
        }
        if (response == null) {
            return false;
        }
        this.ttsGeneration++;
        this.voiceLoopMode = false;
        this.voiceRecording = false;
        hideKeyboard();
        this.handler.removeCallbacks(this.hideInputRunnable);
        this.handler.postDelayed(this.hideInputRunnable, 3500L);
        this.answer.setText(response);
        setMascotExpression(expression);
        scrollAnswerToTop();
        setStatus("Local reply", Color.rgb(90, 220, 120));
        logToPhoneAsync("User", value);
        logToPhoneAsync("Assistant", response);
        speakWithPhoneTtsChunked(response);
        return true;
    }

    private boolean isNewsQuestion(String str) {
        String strTrim = str == null ? "" : str.trim();
        return strTrim.contains("ニュース") || strTrim.toLowerCase(Locale.JAPAN).contains("news") || strTrim.contains("記者会見") || strTrim.contains("会見") || strTrim.contains("発言") || strTrim.contains("国会") || strTrim.contains("選挙") || strTrim.contains("政府") || strTrim.contains("首相") || strTrim.contains("大臣") || strTrim.contains("政権") || strTrim.contains("高市") || strTrim.contains("トランプ") || strTrim.contains("イラン") || strTrim.contains("イスラエル") || strTrim.contains("円安") || strTrim.contains("株価");
    }

    private boolean handleLocalCommand(String str) {
        String lowerCase = str == null ? "" : str.trim().toLowerCase(Locale.JAPAN);
        if (lowerCase.contains("プロアクティブオン") || lowerCase.contains("proactive on") || lowerCase.contains("watch on")) {
            setProactiveMode(true);
            setInputTextVisible("");
            return true;
        }
        if (!lowerCase.contains("プロアクティブオフ") && !lowerCase.contains("proactive off") && !lowerCase.contains("watch off")) {
            return false;
        }
        setProactiveMode(false);
        setInputTextVisible("");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleVoiceRecording() {
        Log.i(TAG, "toggleVoiceRecording current=" + this.voiceRecording);
        if (!this.voiceRecording) {
            setProactiveMode(false);
            this.voiceLoopMode = false;
        }
        if (this.voiceRecording) {
            if (this.speechRecognizerActive && this.speechRecognizer != null) {
                this.voiceRecording = false;
                if (this.voiceButton != null) {
                    this.voiceButton.setText("VOICE");
                }
                if (this.answer != null) {
                    this.answer.setText("音声を文字にしています…");
                }
                setStatus("音声認識中", -3355444);
                try {
                    this.speechRecognizer.stopListening();
                } catch (Exception e) {
                    releaseSpeechRecognizer();
                }
                showControlsTemporarily();
                return;
            }
            this.voiceRecording = false;
            setMascotMode(1);
            if (this.answer != null) {
                this.answer.setText("音声を送信中…");
            }
            setStatus("音声を送信中", -3355444);
            if (this.voiceButton != null) {
                this.voiceButton.setText("VOICE");
            }
            showControlsTemporarily();
            return;
        }
        if (this.answer != null) {
            this.answer.setText("音声入力を開始します…");
        }
        if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 20);
            if (this.answer != null) {
                this.answer.setText("マイク権限を許可してください。");
            }
            setStatus("マイク権限を許可してください", -256);
            return;
        }
        final String strTrim = getPreferences().getString(KEY_API_KEY, "").trim();
        if (strTrim.isEmpty()) {
            if (this.answer != null) {
                this.answer.setText("先にAPIキーを設定してください。");
            }
            setStatus("先にAPIキーを設定してください", -256);
            showApiKeyDialog();
            return;
        }
        if (!isNetworkReady()) {
            if (this.answer != null) {
                this.answer.setText("Wi-Fiまたはインターネット未接続です。");
            }
            setStatus("Wi-Fiまたはインターネット未接続です", -65536);
            return;
        }
        this.voiceRecording = true;
        this.ttsGeneration++;
        setConversationActive(true);
        setMascotMode(1);
        final int i = this.requestGeneration + 1;
        this.requestGeneration = i;
        if (this.voiceButton != null) {
            this.voiceButton.setText("STOP");
        }
        showControlsTemporarily();
        this.answer.setText("聞いています… F5または音声ボタンでもう一度押すと送信します。");
        setStatus("音声入力中", Color.rgb(90, 220, 120));
        this.answer.setText("聞いています… 話し終わると自動で送信します。");
        Log.i(TAG, "voice recording start requested");
        if (PREFER_GLASS_SYSTEM_SPEECH && startSystemSpeechRecognition(strTrim, i)) {
            return;
        }
        this.voiceThread = new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.28
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.recordVoiceAndSend(strTrim, i);
            }
        }, "VoiceRecordGemini");
        this.voiceThread.start();
    }

    private boolean startSystemSpeechRecognition(final String apiKey, final int requestId) {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Log.i(TAG, "system SpeechRecognizer is not available");
                return false;
            }
            releaseSpeechRecognizer();
            final SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            if (recognizer == null) {
                Log.i(TAG, "SpeechRecognizer.createSpeechRecognizer returned null");
                return false;
            }
            this.speechRecognizer = recognizer;
            this.speechRecognizerActive = true;
            this.speechRecognizerTimeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    if (MainActivity.this.speechRecognizerActive && requestId == MainActivity.this.requestGeneration) {
                        try {
                            recognizer.stopListening();
                            MainActivity.this.setStatus("音声を処理中", -3355444);
                        } catch (Exception e) {
                            MainActivity.this.releaseSpeechRecognizer();
                        }
                    }
                }
            };
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (requestId == MainActivity.this.requestGeneration) {
                        MainActivity.this.setStatus("聞いています", Color.rgb(90, 220, 120));
                        if (MainActivity.this.answer != null) {
                            MainActivity.this.answer.setText("話してください。終わると文字にします。");
                        }
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                    if (requestId == MainActivity.this.requestGeneration) {
                        MainActivity.this.setMascotMode(1);
                        MainActivity.this.setStatus("聞き取り中", Color.rgb(90, 220, 120));
                    }
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    if (requestId == MainActivity.this.requestGeneration) {
                        MainActivity.this.setStatus("音声を文字にしています", -3355444);
                        if (MainActivity.this.answer != null) {
                            MainActivity.this.answer.setText("音声を文字にしています…");
                        }
                    }
                }

                @Override
                public void onError(int error) {
                    if (requestId != MainActivity.this.requestGeneration) {
                        MainActivity.this.releaseSpeechRecognizer();
                        return;
                    }
                    MainActivity.this.releaseSpeechRecognizer();
                    MainActivity.this.voiceRecording = false;
                    if (MainActivity.this.voiceButton != null) {
                        MainActivity.this.voiceButton.setText("VOICE");
                    }
                    MainActivity.this.setConversationActive(false);
                    String message = MainActivity.this.speechErrorMessage(error);
                    if (MainActivity.this.answer != null) {
                        MainActivity.this.answer.setText("音声認識できませんでした。\n" + message + "\n\n録音をGeminiへ直接送る方式は429が出やすいため、今回は自動送信しません。もう一度VOICEを押してください。");
                    }
                    MainActivity.this.setStatus("音声認識エラー", Color.YELLOW);
                    MainActivity.this.setMascotExpression(11);
                }

                @Override
                public void onResults(Bundle results) {
                    if (requestId != MainActivity.this.requestGeneration) {
                        MainActivity.this.releaseSpeechRecognizer();
                        return;
                    }
                    String text = MainActivity.this.bestSpeechText(results);
                    MainActivity.this.releaseSpeechRecognizer();
                    MainActivity.this.voiceRecording = false;
                    if (MainActivity.this.voiceButton != null) {
                        MainActivity.this.voiceButton.setText("VOICE");
                    }
                    if (text.length() == 0) {
                        if (MainActivity.this.answer != null) {
                            MainActivity.this.answer.setText("音声を文字にできませんでした。もう一度VOICEを押してください。");
                        }
                        MainActivity.this.setStatus("聞き取り失敗", Color.YELLOW);
                        MainActivity.this.setConversationActive(false);
                        return;
                    }
                    if (MainActivity.this.input != null) {
                        MainActivity.this.setInputTextVisible(text);
                    }
                    if (MainActivity.this.answer != null) {
                        MainActivity.this.answer.setText("聞き取り: " + text + "\n\n処理します…");
                    }
                    MainActivity.this.setStatus("音声を文字入力しました", Color.rgb(90, 220, 120));
                    MainActivity.this.sendCurrentText();
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    if (requestId != MainActivity.this.requestGeneration) {
                        return;
                    }
                    String text = MainActivity.this.bestSpeechText(partialResults);
                    if (text.length() > 0 && MainActivity.this.input != null) {
                        MainActivity.this.setInputTextVisible(text);
                        MainActivity.this.setStatus("聞き取り中: " + MainActivity.this.limitText(text, 18), Color.rgb(90, 220, 120));
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 700L);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 850L);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 650L);
            recognizer.startListening(intent);
            this.handler.postDelayed(this.speechRecognizerTimeoutRunnable, 9000L);
            Log.i(TAG, "system SpeechRecognizer started");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "startSystemSpeechRecognition failed", e);
            releaseSpeechRecognizer();
            return false;
        }
    }

    private void releaseSpeechRecognizer() {
        try {
            if (this.speechRecognizerTimeoutRunnable != null) {
                this.handler.removeCallbacks(this.speechRecognizerTimeoutRunnable);
            }
        } catch (Exception e) {
        }
        this.speechRecognizerTimeoutRunnable = null;
        SpeechRecognizer recognizer = this.speechRecognizer;
        this.speechRecognizer = null;
        this.speechRecognizerActive = false;
        if (recognizer != null) {
            try {
                recognizer.destroy();
            } catch (Exception e2) {
            }
        }
    }

    private String bestSpeechText(Bundle bundle) {
        if (bundle == null) {
            return "";
        }
        ArrayList<String> list = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return "";
        }
        return list.get(0).trim();
    }

    private String speechErrorMessage(int error) {
        if (error == SpeechRecognizer.ERROR_AUDIO) {
            return "マイク入力エラーです。";
        }
        if (error == SpeechRecognizer.ERROR_CLIENT) {
            return "音声認識サービスを開始できませんでした。";
        }
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            return "マイク権限がありません。";
        }
        if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
            return "音声認識サービスがネットワークに接続できません。";
        }
        if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            return "聞き取れる音声が見つかりませんでした。";
        }
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            return "音声認識サービスが使用中です。";
        }
        if (error == SpeechRecognizer.ERROR_SERVER) {
            return "音声認識サービス側のエラーです。";
        }
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            return "話し始めを検出できませんでした。";
        }
        return "エラーコード: " + error;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find top splitter block for handler:B:56:0x0118
        	at jadx.core.utils.BlockUtils.getTopSplitterForHandler(BlockUtils.java:1182)
        	at jadx.core.dex.visitors.regions.maker.ExcHandlersRegionMaker.collectHandlerRegions(ExcHandlersRegionMaker.java:53)
        	at jadx.core.dex.visitors.regions.maker.ExcHandlersRegionMaker.process(ExcHandlersRegionMaker.java:38)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:27)
        */
    public void recordVoiceAndSend(String apiKey, final int requestId) {
        AudioRecord recorder = null;
        try {
            int sampleRate = 16000;
            int minBuffer = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = Math.max(minBuffer, sampleRate);
            int[] audioSources = voiceAudioSources();
            int audioSourceIndex = getVoiceAudioSourceIndex();
            int audioSource = audioSources[audioSourceIndex];
            for (int sourceTry = 0; sourceTry < audioSources.length; sourceTry++) {
                int candidateIndex = (audioSourceIndex + sourceTry) % audioSources.length;
                int candidateSource = audioSources[candidateIndex];
                try {
                    recorder = new AudioRecord(candidateSource,
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize);
                    if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                        audioSource = candidateSource;
                        if (candidateIndex != audioSourceIndex) {
                            getPreferences().edit().putInt(KEY_VOICE_AUDIO_SOURCE_INDEX, candidateIndex).apply();
                        }
                        break;
                    }
                    try {
                        recorder.release();
                    } catch (Exception ignored) {
                    }
                    recorder = null;
                } catch (Exception sourceError) {
                    Log.w(TAG, "AudioRecord source failed " + audioSourceLabel(candidateSource), sourceError);
                    try {
                        if (recorder != null) {
                            recorder.release();
                        }
                    } catch (Exception ignored) {
                    }
                    recorder = null;
                }
            }
            if (recorder == null) {
                throw new IllegalStateException("使えるマイク入力経路が見つかりません");
            }
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            byte[] buffer = new byte[Math.max(2048, minBuffer)];
            recorder.startRecording();
            long started = System.currentTimeMillis();
            long lastVoiceAt = started;
            int maxVoiceLevel = 0;
            int voiceHitCount = 0;
            while (this.voiceRecording && requestId == this.requestGeneration) {
                int read = recorder.read(buffer, 0, buffer.length);
                if (read > 0) {
                    pcm.write(buffer, 0, read);
                    int level = averageAbs16(buffer, read);
                    if (level > maxVoiceLevel) {
                        maxVoiceLevel = level;
                    }
                    long now = System.currentTimeMillis();
                    if (level > 2) {
                        lastVoiceAt = now;
                        voiceHitCount++;
                    }
                    if (now - started > 4500L && now - lastVoiceAt > 1800L) {
                        break;
                    }
                    if (now - started > 10000L) {
                        break;
                    }
                }
            }
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
            if (requestId != this.requestGeneration) {
                return;
            }
            final byte[] rawPcmBytes = pcm.toByteArray();
            final byte[] pcmBytes = normalizePcm16(rawPcmBytes, maxVoiceLevel);
            final int recordedMaxVoiceLevel = maxVoiceLevel;
            final int recordedVoiceHitCount = voiceHitCount;
            final int recordedGainPercent = voiceGainPercent(maxVoiceLevel);
            final int recordedAudioSource = audioSource;
            final String recordedAudioSourceLabel = audioSourceLabel(recordedAudioSource);
            if (recordedMaxVoiceLevel <= 2) {
                advanceVoiceAudioSource("silent level " + recordedMaxVoiceLevel + " source=" + recordedAudioSourceLabel);
            }
            Log.i(TAG, "voice recorded source=" + recordedAudioSourceLabel + " pcmBytes=" + rawPcmBytes.length + " maxLevel=" + recordedMaxVoiceLevel + " hits=" + recordedVoiceHitCount + " gainPercent=" + recordedGainPercent);
            final byte[] wav = pcmToWavSafe(pcmBytes, sampleRate);
            if (wav.length < 12000) {
                this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecording = false;
                        if (voiceButton != null) voiceButton.setText("VOICE");
                        setStatus("Voice was too short", Color.YELLOW);
                        setConversationActive(false);
                    }
                });
                return;
            }
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    setStatus("音声受領 " + (pcmBytes.length / 1024) + "KB Lv" + recordedMaxVoiceLevel + " G" + (recordedGainPercent / 100.0f), Color.rgb(90, 220, 120));
                    if (answer != null) {
                        answer.setText("音声を受け取りました。\nスマホで文字起こししています…\n" + recordedAudioSourceLabel + " / Lv " + recordedMaxVoiceLevel + " / " + (pcmBytes.length / 1024) + "KB / G" + (recordedGainPercent / 100.0f));
                    }
                }
            });
            try {
                final String phoneTranscript = requestPhoneSpeechText(pcmBytes, sampleRate).trim();
                if (phoneTranscript.length() > 0) {
                    this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (requestId != requestGeneration) {
                                return;
                            }
                            voiceRecording = false;
                            if (voiceButton != null) voiceButton.setText("VOICE");
                            if (input != null) {
                                setInputTextVisible(phoneTranscript);
                            }
                            answer.setText("聞き取り: " + phoneTranscript + "\n\n処理します…");
                            setStatus("スマホ音声認識 OK", Color.rgb(90, 220, 120));
                            sendCurrentText();
                        }
                    });
                    return;
                }
            } catch (final Exception phoneSttError) {
                Log.w(TAG, "phone STT failed", phoneSttError);
                Log.i(TAG, "phone STT failed; limited Gemini voice fallback may run");
                if (recordedMaxVoiceLevel >= 700 && recordedVoiceHitCount >= 3 && !isGeminiCoolingDown()
                        && System.currentTimeMillis() - lastGeminiVoiceFallbackAt > 90000L) {
                    try {
                        lastGeminiVoiceFallbackAt = System.currentTimeMillis();
                        this.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("Geminiで音声文字起こし中", -3355444);
                                if (answer != null) {
                                    answer.setText("スマホ音声認識が使えませんでした。\n" + phoneSttError.getMessage() + "\n\n代わりにGeminiで音声を文字起こししています…");
                                }
                            }
                        });
                        this.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("Gemini音声文字起こし中", -3355444);
                                if (answer != null) {
                                    answer.setText("スマホ音声認識がタイムアウトしました。\n" + phoneSttError.getMessage() + "\n\n429防止のため、Gemini音声文字起こしは90秒に1回だけ実行します。\nいまGeminiで文字起こししています…");
                                }
                            }
                        });
                        final VoiceResult voiceResult = requestGeminiAudioWithRetry(apiKey, wav);
                        this.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (requestId != requestGeneration) {
                                    return;
                                }
                                voiceRecording = false;
                                if (voiceButton != null) voiceButton.setText("VOICE");
                                if (input != null) {
                                    setInputTextVisible(voiceResult.transcript);
                                }
                                logToPhoneAsync("User", voiceResult.transcript);
                                if (voiceResult.transcript.length() > 0 && (handleDirectDataQuestion(voiceResult.transcript) || handleUnsupportedNewsQuestion(voiceResult.transcript) || handleSmallTalkQuestion(voiceResult.transcript))) {
                                    return;
                                }
                                answer.setText(voiceResult.answer);
                                scrollAnswerToTop();
                                setMascotExpression(chooseMascotExpressionForText(voiceResult.transcript, voiceResult.answer));
                                setStatus("Gemini音声文字起こし OK", Color.rgb(90, 220, 120));
                                setStatus("Gemini音声文字起こし OK", Color.rgb(90, 220, 120));
                                logToPhoneAsync("Gemini", voiceResult.answer);
                                speakWithPhoneTtsChunked(voiceResult.answer);
                            }
                        });
                        return;
                    } catch (final Exception geminiVoiceError) {
                        Log.w(TAG, "Gemini voice fallback failed", geminiVoiceError);
                        if ((geminiVoiceError instanceof GeminiHttpException) && ((GeminiHttpException) geminiVoiceError).isRetryable()) {
                            beginGeminiCooldown(((GeminiHttpException) geminiVoiceError).cooldownMs());
                        }
                        this.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                voiceRecording = false;
                                if (voiceButton != null) voiceButton.setText("VOICE");
                                answer.setText("スマホ音声認識とGemini音声文字起こしの両方に失敗しました。\n\nスマホ: " + phoneSttError.getMessage() + "\nGemini: " + geminiVoiceError.getMessage());
                                setStatus("音声文字起こしエラー", Color.YELLOW);
                                setConversationActive(false);
                            }
                        });
                        return;
                    }
                }
                this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecording = false;
                        if (voiceButton != null) voiceButton.setText("VOICE");
                        advanceVoiceAudioSource("STT failure level " + recordedMaxVoiceLevel + " source=" + recordedAudioSourceLabel);
                        answer.setText("スマホ音声認識に失敗しました。\n" + phoneSttError.getMessage() + "\n\nスマホ側の録音権限は確認済みです。原因は権限ではなく、音声認識サービスがグラス録音を文字化できなかった可能性が高いです。\n429防止のためGemini音声認識へは自動送信しません。もう一度、少し長めにはっきり話してください。");
                        setStatus("スマホ音声認識エラー", Color.YELLOW);
                        setConversationActive(false);
                    }
                });
                return;
            }
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    voiceRecording = false;
                    if (voiceButton != null) voiceButton.setText("VOICE");
                    answer.setText("音声を文字にできませんでした。もう一度VOICEを押してください。");
                    setStatus("聞き取りなし", Color.YELLOW);
                    setConversationActive(false);
                }
            });
            return;
        } catch (final Exception error) {
            Log.e(TAG, "recordVoiceAndSend failed", error);
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    voiceRecording = false;
                    if (voiceButton != null) voiceButton.setText("VOICE");
                    answer.setText("音声入力エラー\n" + error.getMessage() + "\n\n録音はできています。429の場合はGeminiの利用枠/混雑なので、少し待ってから再試行してください。");
                    setMascotExpression(11);
                    setStatus("Voice error", Color.YELLOW);
                    setConversationActive(false);
                }
            });
        } finally {
            if (recorder != null) {
                try {
                    recorder.release();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private byte[] pcmToWavSafe(byte[] pcm, int sampleRate) throws Exception {
        int dataLength = pcm == null ? 0 : pcm.length;
        int totalLength = dataLength + 36;
        ByteArrayOutputStream out = new ByteArrayOutputStream(dataLength + 44);
        writeAsciiSafe(out, "RIFF");
        writeLittleEndianIntSafe(out, totalLength);
        writeAsciiSafe(out, "WAVE");
        writeAsciiSafe(out, "fmt ");
        writeLittleEndianIntSafe(out, 16);
        writeLittleEndianShortSafe(out, 1);
        writeLittleEndianShortSafe(out, 1);
        writeLittleEndianIntSafe(out, sampleRate);
        writeLittleEndianIntSafe(out, sampleRate * 2);
        writeLittleEndianShortSafe(out, 2);
        writeLittleEndianShortSafe(out, 16);
        writeAsciiSafe(out, "data");
        writeLittleEndianIntSafe(out, dataLength);
        if (pcm != null) {
            out.write(pcm);
        }
        return out.toByteArray();
    }

    private int voiceGainPercent(int maxLevel) {
        if (maxLevel <= 0) {
            return 100;
        }
        if (maxLevel >= 6000) {
            return 100;
        }
        double gain = Math.min(128.0d, 6000.0d / Math.max(1.0d, (double) maxLevel));
        return (int) Math.round(gain * 100.0d);
    }

    private byte[] normalizePcm16(byte[] pcm, int maxLevel) {
        if (pcm == null || pcm.length < 2 || maxLevel <= 0) {
            return pcm;
        }
        int gainPercent = voiceGainPercent(maxLevel);
        if (gainPercent <= 115) {
            return pcm;
        }
        double gain = gainPercent / 100.0d;
        byte[] out = new byte[pcm.length];
        int i = 0;
        while (i + 1 < pcm.length) {
            int sample = (short) ((pcm[i] & 255) | (pcm[i + 1] << 8));
            int scaled = (int) Math.round(sample * gain);
            if (scaled > 32767) {
                scaled = 32767;
            } else if (scaled < -32768) {
                scaled = -32768;
            }
            out[i] = (byte) (scaled & 255);
            out[i + 1] = (byte) ((scaled >> 8) & 255);
            i += 2;
        }
        if (i < pcm.length) {
            out[i] = pcm[i];
        }
        return out;
    }

    private int[] voiceAudioSources() {
        return new int[]{
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.CAMCORDER,
                MediaRecorder.AudioSource.DEFAULT,
                MediaRecorder.AudioSource.UNPROCESSED,
                MediaRecorder.AudioSource.VOICE_PERFORMANCE
        };
    }

    private int getVoiceAudioSourceIndex() {
        int[] sources = voiceAudioSources();
        int index = getPreferences().getInt(KEY_VOICE_AUDIO_SOURCE_INDEX, 0);
        if (index < 0) {
            index = 0;
        }
        return index % sources.length;
    }

    private void advanceVoiceAudioSource(String reason) {
        try {
            int[] sources = voiceAudioSources();
            int current = getVoiceAudioSourceIndex();
            int next = (current + 1) % sources.length;
            getPreferences().edit().putInt(KEY_VOICE_AUDIO_SOURCE_INDEX, next).apply();
            Log.i(TAG, "advance voice audio source " + current + " -> " + next + " reason=" + reason + " next=" + audioSourceLabel(sources[next]));
        } catch (Exception e) {
            Log.w(TAG, "advanceVoiceAudioSource failed", e);
        }
    }

    private String audioSourceLabel(int source) {
        if (source == MediaRecorder.AudioSource.MIC) {
            return "MIC";
        }
        if (source == MediaRecorder.AudioSource.CAMCORDER) {
            return "CAMCORDER";
        }
        if (source == MediaRecorder.AudioSource.VOICE_COMMUNICATION) {
            return "VOICE_COMM";
        }
        if (source == MediaRecorder.AudioSource.VOICE_RECOGNITION) {
            return "VOICE_RECOG";
        }
        if (source == MediaRecorder.AudioSource.DEFAULT) {
            return "DEFAULT";
        }
        if (source == MediaRecorder.AudioSource.UNPROCESSED) {
            return "UNPROCESSED";
        }
        if (source == MediaRecorder.AudioSource.VOICE_PERFORMANCE) {
            return "VOICE_PERF";
        }
        return "SRC" + source;
    }

    private void writeAsciiSafe(ByteArrayOutputStream out, String text) throws Exception {
        out.write(text.getBytes("US-ASCII"));
    }

    private void writeLittleEndianIntSafe(ByteArrayOutputStream out, int value) {
        out.write(value & 255);
        out.write((value >> 8) & 255);
        out.write((value >> 16) & 255);
        out.write((value >> 24) & 255);
    }

    private void writeLittleEndianShortSafe(ByteArrayOutputStream out, int value) {
        out.write(value & 255);
        out.write((value >> 8) & 255);
    }

    private int averageAbs16(byte[] bArr, int i) {
        long jAbs = 0;
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int i4 = i2 + 1;
            if (i4 >= i) {
                break;
            }
            jAbs += (long) Math.abs((bArr[i4] << 8) | (bArr[i2] & 255));
            i3++;
            i2 += 2;
        }
        if (i3 == 0) {
            return 0;
        }
        return (int) (jAbs / ((long) i3));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isGeminiCoolingDown() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.geminiCooldownUntil - jCurrentTimeMillis > 300000L) {
            this.geminiCooldownUntil = jCurrentTimeMillis + 240000L;
            getPreferences().edit().putLong(KEY_GEMINI_COOLDOWN_UNTIL, this.geminiCooldownUntil).apply();
        }
        boolean z = jCurrentTimeMillis < this.geminiCooldownUntil;
        if (!z && this.geminiCooldownUntil != 0) {
            this.geminiCooldownUntil = 0L;
            getPreferences().edit().remove(KEY_GEMINI_COOLDOWN_UNTIL).apply();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void beginGeminiCooldown(long j) {
        long jCurrentTimeMillis = System.currentTimeMillis() + Math.max(j, 15000L);
        if (jCurrentTimeMillis > this.geminiCooldownUntil) {
            this.geminiCooldownUntil = jCurrentTimeMillis;
            getPreferences().edit().putLong(KEY_GEMINI_COOLDOWN_UNTIL, this.geminiCooldownUntil).apply();
        }
        this.handler.removeCallbacks(this.infoUpdater);
        this.handler.post(this.infoUpdater);
    }

    private void markGeminiRequestStarted() {
        long jCurrentTimeMillis = System.currentTimeMillis() + GEMINI_LOCAL_PACING_MS;
        if (jCurrentTimeMillis > this.geminiCooldownUntil) {
            this.geminiCooldownUntil = jCurrentTimeMillis;
            getPreferences().edit().putLong(KEY_GEMINI_COOLDOWN_UNTIL, this.geminiCooldownUntil).apply();
        }
        this.handler.removeCallbacks(this.infoUpdater);
        this.handler.post(this.infoUpdater);
    }

    private void markGeminiRequestSucceeded() {
        long nextAllowedAt = System.currentTimeMillis() + GEMINI_LOCAL_PACING_MS;
        this.geminiCooldownUntil = nextAllowedAt;
        getPreferences().edit().putLong(KEY_GEMINI_COOLDOWN_UNTIL, this.geminiCooldownUntil).apply();
        this.handler.removeCallbacks(this.infoUpdater);
        this.handler.post(this.infoUpdater);
        Log.i(TAG, "Gemini success pacing until=" + this.geminiCooldownUntil);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showGeminiCooldown() {
        long jMax = Math.max(1L, (Math.max(1L, this.geminiCooldownUntil - System.currentTimeMillis()) + 999) / 1000);
        Log.i(TAG, "Gemini cooldown active seconds=" + jMax);
        this.voiceRecording = false;
        this.voiceLoopMode = false;
        if (this.voiceButton != null) {
            this.voiceButton.setText("VOICE");
        }
        if (this.answer != null) {
            this.answer.setText("Gemini待機中です。\nあと約" + jMax + "秒、音声送信を止めています。\n429/503の連続発生を防ぐためです。少し待ってからもう一度VOICEを押してください。");
        }
        setStatus("Gemini WAIT " + jMax + "s", -256);
        setConversationActive(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restartVoiceLoopAfterDelay(long j) {
        this.handler.postDelayed(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.33
            @Override // java.lang.Runnable
            public void run() {
                if (MainActivity.this.voiceLoopMode && !MainActivity.this.voiceRecording) {
                    MainActivity.this.toggleVoiceRecording();
                }
            }
        }, j);
    }

    private void toggleProactiveMode() {
        setProactiveMode(!this.proactiveMode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setProactiveMode(boolean z) {
        if (z) {
            emergencyStop("PRO AI disabled", false, true);
            if (this.answer != null) {
                this.answer.setText("PRO AI is temporarily disabled for stability.");
            }
            setStatus("PRO AI disabled", -256);
            return;
        }
        emergencyStop("PRO AI OFF", false, true);
        if (this.answer != null) {
            this.answer.setText("PRO AI OFF");
        }
    }

    private void runProactiveLoop() throws Throwable {
        String strTrim = getPreferences().getString(KEY_API_KEY, "").trim();
        if (strTrim.isEmpty()) {
            this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.34
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.proactiveMode = false;
                    MainActivity.this.setConversationActive(false);
                    MainActivity.this.applyProactiveLayout(false);
                    MainActivity.this.setStatus("APIキーを設定してください", -256);
                }
            });
            return;
        }
        if (this.proactiveMode) {
            long j = GEMINI_LOCAL_PACING_MS;
            try {
                if (!isNetworkReady()) {
                    postProactiveStatus("PRO AI: ネット未接続。待機します。", -256);
                } else {
                    postProactiveStatus("PRO AI: 待機中", -3355444);
                    long jCurrentTimeMillis = GEMINI_LOCAL_PACING_MS - (System.currentTimeMillis() - this.lastProactiveRequestAt);
                    if (jCurrentTimeMillis > 0) {
                        Thread.sleep(jCurrentTimeMillis);
                    }
                    postProactiveStatus("PRO AI: 聞き取り中", -3355444);
                    byte[] bArrRecordProactiveWav = recordProactiveWav();
                    if (!this.proactiveMode) {
                        return;
                    }
                    if (bArrRecordProactiveWav == null || bArrRecordProactiveWav.length < 12000) {
                        postProactiveStatus("PRO AI: speech not detected", -256);
                    } else {
                        this.lastProactiveRequestAt = System.currentTimeMillis();
                        postProactiveStatus("PRO AI: 解析中", -3355444);
                        final String strRequestProactiveAudioWithRetry = requestProactiveAudioWithRetry(strTrim, bArrRecordProactiveWav);
                        this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.35
                            @Override // java.lang.Runnable
                            public void run() {
                                if (MainActivity.this.proactiveMode) {
                                    MainActivity.this.answer.setText(strRequestProactiveAudioWithRetry);
                                    MainActivity.this.scrollAnswerToTop();
                                    MainActivity.this.setStatus("PRO AI", Color.rgb(90, 220, 120));
                                    MainActivity.this.logToPhoneAsync("Gemini", "[PRO AI] " + strRequestProactiveAudioWithRetry);
                                }
                            }
                        });
                    }
                }
            } catch (GeminiHttpException e) {
                Log.e(TAG, "proactive Gemini failed", e);
                postProactiveStatus(e.getMessage(), -256);
                try {
                    if (!e.isRetryable()) {
                        j = 15000;
                    }
                    Thread.sleep(j);
                } catch (InterruptedException e2) {
                }
            } catch (Exception e3) {
                Log.e(TAG, "proactive loop failed", e3);
                postProactiveStatus("PRO AI error: " + e3.getMessage(), -256);
                try {
                    Thread.sleep(15000L);
                } catch (InterruptedException e4) {
                }
            }
        }
        this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.36
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.proactiveMode = false;
                MainActivity.this.setConversationActive(false);
                MainActivity.this.applyProactiveLayout(false);
                MainActivity.this.setStatus("PRO AI OFF", -256);
            }
        });
    }

    private void postProactiveStatus(final String str, final int i) {
        this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.37
            @Override // java.lang.Runnable
            public void run() {
                if (MainActivity.this.proactiveMode) {
                    MainActivity.this.setStatus(str, i);
                }
            }
        });
    }

    /* JADX WARN: Removed duplicated region for block: B:108:? A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:92:0x00d2 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private byte[] recordProactiveWav() throws java.lang.Throwable {
        /*
            Method dump skipped, instruction units count: 223
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.example.rokidkeyboardbridge.MainActivity.recordProactiveWav():byte[]");
    }

    private byte[] makeWav(byte[] bArr, int i) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int length = bArr.length;
        writeAscii(byteArrayOutputStream, "RIFF");
        writeLeInt(byteArrayOutputStream, length + 36);
        writeAscii(byteArrayOutputStream, "WAVE");
        writeAscii(byteArrayOutputStream, "fmt ");
        writeLeInt(byteArrayOutputStream, 16);
        writeLeShort(byteArrayOutputStream, 1);
        writeLeShort(byteArrayOutputStream, 1);
        writeLeInt(byteArrayOutputStream, i);
        writeLeInt(byteArrayOutputStream, i * 2);
        writeLeShort(byteArrayOutputStream, 2);
        writeLeShort(byteArrayOutputStream, 16);
        writeAscii(byteArrayOutputStream, "data");
        writeLeInt(byteArrayOutputStream, length);
        byteArrayOutputStream.write(bArr);
        return byteArrayOutputStream.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream byteArrayOutputStream, String str) throws Exception {
        byteArrayOutputStream.write(str.getBytes(StandardCharsets.US_ASCII));
    }

    private String limitText(String str, int i) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        if (strTrim.length() <= i) {
            return strTrim;
        }
        return strTrim.substring(0, Math.max(0, i)) + "\n…";
    }

    private void writeLeInt(ByteArrayOutputStream byteArrayOutputStream, int i) {
        byteArrayOutputStream.write(i & 255);
        byteArrayOutputStream.write((i >> 8) & 255);
        byteArrayOutputStream.write((i >> 16) & 255);
        byteArrayOutputStream.write((i >> 24) & 255);
    }

    private void writeLeShort(ByteArrayOutputStream byteArrayOutputStream, int i) {
        byteArrayOutputStream.write(i & 255);
        byteArrayOutputStream.write((i >> 8) & 255);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildGeminiPromptCompact(String str) throws Exception {
        boolean z = str != null && str.length() > 700;
        boolean z2 = str != null && str.length() > 1800;
        String strLimitText = limitText(getCustomInstructions(), z2 ? 250 : z ? 450 : 900);
        String strLimitText2 = limitText(str, z2 ? 3600 : z ? 1800 : 900);
        int i = z2 ? 600 : z ? 1200 : MAX_CONTEXT_CHARS;
        StringBuilder sb = new StringBuilder();
        sb.append(strLimitText);
        sb.append("\n\n回答方針: 日本語で、まず結論を短く。その後、必要な補足だけを続ける。");
        sb.append("\n重要: 予定やメールについて聞かれた場合、下に添付された実データだけを根拠にする。実データにない予定・メールは絶対に作らない。データがない場合は「確認できる予定はありません」または「スマホ側から取得できません」と答える。");
        if (isMailQuestion(str)) {
            String strLimitText3 = limitText(buildRecentMailText(fetchRecentMailJson()), i);
            sb.append("\n\n最近のメール概要。本文ではなく通知情報だけを根拠に答える:\n");
            sb.append(strLimitText3);
        } else if (isScheduleQuestion(str)) {
            String strLimitText4 = limitText(buildTodayScheduleText(fetchScheduleJson(detectScheduleRange(str))), i);
            sb.append("\n\n今日の予定:\n");
            sb.append(strLimitText4);
        }
        sb.append("\n\nユーザーの質問: ");
        sb.append(strLimitText2);
        return limitText(sb.toString(), z2 ? 5600 : z ? 4300 : 5100);
    }

    private String buildGeminiPrompt(String str) throws Exception {
        String strLimitText = limitText(getCustomInstructions(), 900);
        limitText(str, 900);
        if (isMailQuestion(str)) {
            return strLimitText + "\n\n以下はスマホの通知から取得した最近のメール概要です。このメール情報だけを根拠に答えてください。\n本文全文ではなく通知に出た範囲だけです。回答は要点を先に、そのあと必要な補足を含めて詳しくまとめてください。Markdown記号は使わないでください。\n\n" + limitText(buildRecentMailText(fetchRecentMailJson()), MAX_CONTEXT_CHARS) + "\n\nユーザーの質問: " + str;
        }
        if (isTodayScheduleQuestion(str)) {
            return strLimitText + "\n\n以下はスマホから取得した今日のカレンダー予定です。この予定情報だけを根拠に答えてください。\n回答は要点を先に、そのあと重要な予定、次の予定、注意点を十分に詳しくまとめてください。Markdown記号は使わないでください。\n\n" + limitText(buildTodayScheduleText(fetchTodayScheduleJson()), MAX_CONTEXT_CHARS) + "\n\nユーザーの質問: " + str;
        }
        return strLimitText + "\n\nユーザーの質問: " + str;
    }

    private String getCustomInstructions() {
        String strTrim = getPreferences().getString(KEY_CUSTOM_INSTRUCTIONS, "").trim();
        if (strTrim.isEmpty()) {
            strTrim = "あなたはRokidグラス上の私専用の日本語秘書です。回答は必要なことを先に言い、そのあと理由や補足も含めて十分に詳しく答えてください。短すぎて情報が欠けないようにしてください。";
        }
        return "カスタム指示:\n" + strTrim;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String applyCustomInstructionFromText(String str) {
        String strTrim = str == null ? "" : str.trim();
        if (strTrim.length() == 0) {
            return null;
        }
        if (!(strTrim.contains("カスタム指示") || strTrim.contains("指示に追加") || strTrim.contains("覚えて") || strTrim.contains("記憶して"))) {
            return null;
        }
        if (strTrim.contains("削除") || strTrim.contains("消して") || strTrim.contains("クリア")) {
            getPreferences().edit().putString(KEY_CUSTOM_INSTRUCTIONS, "").apply();
            return "カスタム指示を削除しました。";
        }
        if (!strTrim.contains("追加") && !strTrim.contains("覚えて") && !strTrim.contains("記憶して")) {
            return null;
        }
        String strTrim2 = getPreferences().getString(KEY_CUSTOM_INSTRUCTIONS, "").trim();
        String strTrim3 = strTrim.replace("カスタム指示に", "").replace("カスタム指示へ", "").replace("追加して", "").replace("追加", "").replace("覚えて", "").replace("記憶して", "").trim();
        if (strTrim3.length() == 0) {
            return null;
        }
        getPreferences().edit().putString(KEY_CUSTOM_INSTRUCTIONS, strTrim2.length() == 0 ? strTrim3 : strTrim2 + "\n" + strTrim3).apply();
        return "カスタム指示に追加しました。\n" + strTrim3;
    }

    private boolean isTodayScheduleQuestion(String str) {
        if (str == null) {
            str = "";
        }
        String strTrim = str.trim();
        boolean z = strTrim.contains("予定") || strTrim.contains("スケジュール") || strTrim.contains("カレンダー") || strTrim.contains("calendar") || strTrim.contains("Calendar");
        boolean z2 = strTrim.contains("今日") || strTrim.contains("本日") || strTrim.contains("きょう") || strTrim.contains("このあと") || strTrim.contains("次の予定") || strTrim.contains("次は") || strTrim.contains("なにがある") || strTrim.contains("何がある");
        if (z) {
            return z2 || strTrim.length() <= 24;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isMailQuestion(String str) {
        if (str == null) {
            str = "";
        }
        String strTrim = str.trim();
        return strTrim.contains("メール") || strTrim.contains("メイル") || strTrim.contains("Gmail") || strTrim.contains("gmail") || strTrim.contains("受信") || strTrim.contains("新着");
    }

    private boolean isScheduleQuestion(String str) {
        String strTrim = str == null ? "" : str.trim();
        return strTrim.contains("予定") || strTrim.contains("スケジュール") || strTrim.contains("カレンダー") || strTrim.contains("calendar") || strTrim.contains("Calendar") || strTrim.contains("いつだっけ") || strTrim.contains("いつだった") || (strTrim.contains("いつ") && strTrim.contains("だっけ")) || ((strTrim.contains("いつ") && strTrim.contains("行く")) || ((strTrim.contains("いつ") && strTrim.contains("いく")) || strTrim.contains("いつある") || strTrim.contains("何日") || strTrim.contains("何時") || (isBareDateScheduleQuestion(strTrim) && !isNewsQuestion(strTrim))));
    }

    private boolean isBareDateScheduleQuestion(String str) {
        String strTrim = str == null ? "" : str.trim();
        return strTrim.length() <= 16 && (strTrim.contains("昨日") || strTrim.contains("明日") || strTrim.contains("明後日") || strTrim.contains("あした") || strTrim.contains("あさって"));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ScheduleRange detectScheduleRange(String str) {
        String strTrim = str == null ? "" : str.trim();
        Calendar calendar = Calendar.getInstance();
        String strExtractScheduleSearchQuery = extractScheduleSearchQuery(strTrim);
        if (strExtractScheduleSearchQuery.length() > 0) {
            if (strTrim.contains("明後日") || strTrim.contains("あさって")) {
                return new ScheduleRange(2, 1, "明後日", strExtractScheduleSearchQuery);
            }
            if (strTrim.contains("明日") || strTrim.contains("あした")) {
                return new ScheduleRange(1, 1, "明日", strExtractScheduleSearchQuery);
            }
            if (strTrim.contains("昨日")) {
                return new ScheduleRange(-1, 1, "昨日", strExtractScheduleSearchQuery);
            }
            if (strTrim.contains("今日") || strTrim.contains("本日") || strTrim.contains("きょう")) {
                int i = 0;
                return new ScheduleRange(i, 1, "今日", strExtractScheduleSearchQuery);
            }
            if (!isExplicitPastScheduleSearch(strTrim)) {
                if (isPastScheduleSearch(strTrim)) {
                    return new ScheduleRange(-365, 730, "キーワード予定検索", strExtractScheduleSearchQuery);
                }
                return new ScheduleRange(0, 365, "今後の予定検索", strExtractScheduleSearchQuery);
            }
            return new ScheduleRange(-365, 365, "過去の予定検索", strExtractScheduleSearchQuery);
        }
        Calendar explicitDate = parseExplicitDate(strTrim, calendar);
        if (explicitDate != null) {
            return new ScheduleRange(daysBetween(calendar, explicitDate), 1, "指定日");
        }
        if (strTrim.contains("明後日") || strTrim.contains("あさって")) {
            return new ScheduleRange(2, 1, "明後日");
        }
        if (strTrim.contains("明日") || strTrim.contains("あした")) {
            return new ScheduleRange(1, 1, "明日");
        }
        if (strTrim.contains("昨日")) {
            return new ScheduleRange(-1, 1, "昨日");
        }
        if (strTrim.contains("来週")) {
            return new ScheduleRange(daysUntilNextMonday(calendar), 7, "来週");
        }
        if (strTrim.contains("今週")) {
            return new ScheduleRange(0, Math.max(1, daysUntilThisSunday(calendar)), "今週");
        }
        if (strTrim.contains("来月")) {
            Calendar calendar2 = (Calendar) calendar.clone();
            calendar2.add(2, 1);
            calendar2.set(5, 1);
            return new ScheduleRange(daysBetween(calendar, calendar2), calendar2.getActualMaximum(5), "来月");
        }
        if (strTrim.contains("今月")) {
            Calendar calendar3 = (Calendar) calendar.clone();
            return new ScheduleRange(0, Math.max(1, (calendar3.getActualMaximum(5) - calendar3.get(5)) + 1), "今月");
        }
        if (strTrim.contains("先月")) {
            Calendar calendar4 = (Calendar) calendar.clone();
            calendar4.add(2, -1);
            calendar4.set(5, 1);
            return new ScheduleRange(daysBetween(calendar, calendar4), calendar4.getActualMaximum(5), "先月");
        }
        if (strTrim.contains("過去") || strTrim.contains("さかのぼ") || strTrim.contains("いつだっけ") || strTrim.contains("いつだった")) {
            return new ScheduleRange(-365, 730, "過去1年から今後1年");
        }
        String strExtractScheduleSearchQuery2 = extractScheduleSearchQuery(strTrim);
        if (strExtractScheduleSearchQuery2.length() > 0) {
            if (isPastScheduleSearch(strTrim)) {
                return new ScheduleRange(-365, 365, "過去の予定検索", strExtractScheduleSearchQuery2);
            }
            return new ScheduleRange(0, 365, "今後の予定検索", strExtractScheduleSearchQuery2);
        }
        return new ScheduleRange(0, 1, "今日", "");
    }

    private boolean isExplicitPastScheduleSearch(String str) {
        String strTrim = str == null ? "" : str.trim();
        return strTrim.contains("過去") || strTrim.contains("前の") || strTrim.contains("以前") || strTrim.contains("前回") || strTrim.contains("この前") || strTrim.contains("最後") || strTrim.contains("さかのぼ");
    }

    private boolean isPastScheduleSearch(String str) {
        String strTrim = str == null ? "" : str.trim();
        return isExplicitPastScheduleSearch(strTrim) || strTrim.contains("いつだった") || strTrim.contains("いつだっけ") || (strTrim.contains("いつ") && strTrim.contains("だっけ")) || ((strTrim.contains("いつ") && strTrim.contains("行く")) || (strTrim.contains("いつ") && strTrim.contains("いく")));
    }

    private String extractScheduleSearchQuery(String str) {
        String strTrim = str == null ? "" : str.trim();
        if (strTrim.contains("病院")) {
            return "病院 医療 センター クリニック 診察 治療 面談 カンファレンス 多摩 多摩総合";
        }
        String strTrim2 = strTrim.replace("の予定", " ").replace("予定", " ").replace("スケジュール", " ").replace("カレンダー", " ").replace("明後日", " ").replace("あさって", " ").replace("明日", " ").replace("あした", " ").replace("昨日", " ").replace("今日", " ").replace("本日", " ").replace("きょう", " ").replace("いつだっけ", " ").replace("いつだった", " ").replace("んだっけ", " ").replace("だっけ", " ").replace("いつ", " ").replace("に行く", " ").replace("にいく", " ").replace("行く", " ").replace("いく", " ").replace("ある", " ").replace("の", " ").replace("は", " ").replace("？", " ").replace("?", " ").trim();
        if (strTrim2.length() <= 1 || strTrim2.contains("今日") || strTrim2.contains("明日") || strTrim2.contains("明後日") || strTrim2.contains("昨日") || strTrim2.contains("来週") || strTrim2.contains("来月") || strTrim2.contains("今週") || strTrim2.contains("今月")) {
            return "";
        }
        return limitText(strTrim2, 80);
    }

    private Calendar parseExplicitDate(String str, Calendar calendar) {
        Matcher matcher = Pattern.compile("(20\\d{2})[-/年](\\d{1,2})[-/月](\\d{1,2})").matcher(str);
        if (matcher.find()) {
            return calendarFor(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        Matcher matcher2 = Pattern.compile("(\\d{1,2})月(\\d{1,2})日").matcher(str);
        if (matcher2.find()) {
            return calendarFor(calendar.get(1), Integer.parseInt(matcher2.group(1)), Integer.parseInt(matcher2.group(2)));
        }
        Matcher matcher3 = Pattern.compile("(^|[^0-9])(\\d{1,2})/(\\d{1,2})([^0-9]|$)").matcher(str);
        if (matcher3.find()) {
            return calendarFor(calendar.get(1), Integer.parseInt(matcher3.group(2)), Integer.parseInt(matcher3.group(3)));
        }
        return null;
    }

    private Calendar calendarFor(int i, int i2, int i3) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1, i);
        calendar.set(2, i2 - 1);
        calendar.set(5, i3);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar;
    }

    private int daysBetween(Calendar calendar, Calendar calendar2) {
        return (int) ((calendarFor(calendar2.get(1), calendar2.get(2) + 1, calendar2.get(5)).getTimeInMillis() - calendarFor(calendar.get(1), calendar.get(2) + 1, calendar.get(5)).getTimeInMillis()) / 86400000);
    }

    private int daysUntilNextMonday(Calendar calendar) {
        int i = ((2 - calendar.get(7)) + 7) % 7;
        if (i == 0) {
            return 7;
        }
        return i;
    }

    private int daysUntilThisSunday(Calendar calendar) {
        return (((1 - calendar.get(7)) + 7) % 7) + 1;
    }

    private static final class ScheduleRange {
        final int days;
        final String label;
        final int offsetDays;
        final String query;

        ScheduleRange(int i, int i2, String str) {
            this(i, i2, str, "");
        }

        ScheduleRange(int i, int i2, String str, String str2) {
            this.offsetDays = i;
            this.days = i2;
            this.label = str;
            this.query = str2 == null ? "" : str2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String fetchScheduleJson(ScheduleRange scheduleRange) throws Exception {
        String[] strArrBuildPhoneScheduleUrls = buildPhoneScheduleUrls(scheduleRange);
        Exception e = null;
        for (int i = 0; i < strArrBuildPhoneScheduleUrls.length; i++) {
            try {
                return fetchUrl(strArrBuildPhoneScheduleUrls[i]);
            } catch (Exception e2) {
                e = e2;
            }
        }
        maintainWifiConnection(true);
        throw new IllegalStateException("スマホの予定サーバーに接続できません。スマホ側アプリを開き、グラスと同じ通信経路に接続してください。" + (e == null ? "" : "\n" + e.getMessage()));
    }

    private String fetchTodayScheduleJson() throws Exception {
        String[] strArrBuildPhoneTodayUrls = buildPhoneTodayUrls();
        Exception e = null;
        for (int i = 0; i < strArrBuildPhoneTodayUrls.length; i++) {
            try {
                return fetchUrl(strArrBuildPhoneTodayUrls[i]);
            } catch (Exception e2) {
                e = e2;
            }
        }
        throw new IllegalStateException("スマホの予定サーバーに接続できません。スマホ側アプリを開き、グラスと同じ通信経路に接続してください。" + (e == null ? "" : "\n" + e.getMessage()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String fetchRecentMailJson() throws Exception {
        String[] strArrBuildPhoneMailUrls = buildPhoneMailUrls();
        Exception e = null;
        for (int i = 0; i < strArrBuildPhoneMailUrls.length; i++) {
            try {
                return fetchUrl(strArrBuildPhoneMailUrls[i]);
            } catch (Exception e2) {
                e = e2;
            }
        }
        throw new IllegalStateException("スマホのメールサーバーに接続できません。スマホ側アプリを開き、通知アクセスを許可してください。" + (e == null ? "" : "\n" + e.getMessage()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String fetchNewsJson(String str) throws Exception {
        String[] strArrBuildPhoneNewsUrls = buildPhoneNewsUrls(str);
        Exception e = null;
        for (int i = 0; i < strArrBuildPhoneNewsUrls.length; i++) {
            try {
                return fetchUrl(strArrBuildPhoneNewsUrls[i]);
            } catch (Exception e2) {
                e = e2;
            }
        }
        throw new IllegalStateException("スマホのニュースサーバーに接続できません。" + (e == null ? "" : "\n" + e.getMessage()));
    }

    /* JADX INFO: renamed from: com.example.rokidkeyboardbridge.MainActivity$38, reason: invalid class name */
    class AnonymousClass38 implements Runnable {
        AnonymousClass38() {
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                Log.i(MainActivity.TAG, "pollPhoneCommand start");
                String strTrim = new JSONObject(MainActivity.this.fetchPhoneEndpointJson("custom")).optString("custom", "").trim();
                if (strTrim.length() > 0) {
                    MainActivity.this.getPreferences().edit().putString(MainActivity.KEY_CUSTOM_INSTRUCTIONS, strTrim).apply();
                    MainActivity.this.logToPhoneAsync("カスタム指示", "保存しました");
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.1
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.setStatus("スマホからカスタム指示を保存", Color.rgb(90, 220, 120));
                        }
                    });
                }
                String strTrim2 = new JSONObject(MainActivity.this.fetchPhoneEndpointJson("control")).optString("control", "").trim();
                if ("stop".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.2
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.stopCurrentActivity("スマホから停止しました");
                        }
                    });
                    return;
                }
                if ("pro_off".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.3
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.setStatus("PRO AI is disabled", -256);
                        }
                    });
                    return;
                }
                if ("pro_on".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.4
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.setStatus("PRO AI is disabled", -256);
                        }
                    });
                    return;
                }
                if ("wifi_on".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.5
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.maintainWifiConnection(true);
                            String strDescribeWifiState = MainActivity.this.describeWifiState();
                            MainActivity.this.answer.setText(strDescribeWifiState);
                            MainActivity.this.setStatus("WiFi ON requested", Color.rgb(90, 220, 120));
                            MainActivity.this.logToPhoneAsync("操作", strDescribeWifiState);
                        }
                    });
                    return;
                }
                if ("wifi_reconnect".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.6
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.maintainWifiConnection(true);
                            String strDescribeWifiState = MainActivity.this.describeWifiState();
                            MainActivity.this.answer.setText(strDescribeWifiState);
                            MainActivity.this.setStatus("WiFi reconnect requested", Color.rgb(90, 220, 120));
                            MainActivity.this.logToPhoneAsync("操作", strDescribeWifiState);
                        }
                    });
                    return;
                }
                if ("wifi_status".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.7
                        @Override // java.lang.Runnable
                        public void run() {
                            String strDescribeWifiState = MainActivity.this.describeWifiState();
                            MainActivity.this.answer.setText(strDescribeWifiState);
                            MainActivity.this.setStatus(strDescribeWifiState, -3355444);
                            MainActivity.this.logToPhoneAsync("操作", strDescribeWifiState);
                        }
                    });
                    return;
                }
                if ("open_ai".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.8
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.bringAiToFront();
                        }
                    });
                    return;
                }
                if ("open_manager".equals(strTrim2)) {
                    MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.9
                        @Override // java.lang.Runnable
                        public void run() {
                            MainActivity.this.openRokidManager();
                        }
                    });
                    return;
                }
                if (MainActivity.this.voiceRecording) {
                    return;
                }
                final String strTrim3 = new JSONObject(MainActivity.this.fetchPhoneEndpointJson("command")).optString("command", "").trim();
                Log.i(MainActivity.TAG, "pollPhoneCommand command length=" + strTrim3.length());
                if (strTrim3.length() != 0) {
                    if (MainActivity.this.geminiRequestActive) {
                        MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.10
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.stopCurrentActivity("前の回答を停止して次の指示へ");
                            }
                        });
                        try {
                            Thread.sleep(300L);
                        } catch (InterruptedException e) {
                        }
                    }
                    if (MainActivity.this.isGeminiCoolingDown()) {
                        MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.11
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.setInputTextVisible(strTrim3);
                                MainActivity.this.showGeminiCooldown();
                            }
                        });
                    } else {
                        try {
                            MainActivity.this.fetchPhoneEndpointJson("ack_command");
                        } catch (Exception e2) {
                        }
                        MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.12
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.setInputTextVisible(strTrim3);
                                MainActivity.this.showControlsTemporarily();
                                MainActivity.this.answer.setText("PHONE OK\n" + strTrim3 + "\n\nGeminiへ送ります…");
                                MainActivity.this.setStatus("PHONE OK", Color.rgb(90, 220, 120));
                                MainActivity.this.handler.postDelayed(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.38.12.1
                                    @Override // java.lang.Runnable
                                    public void run() {
                                        MainActivity.this.sendCurrentText();
                                    }
                                }, 700L);
                            }
                        });
                    }
                }
            } catch (Exception e3) {
                Log.e(MainActivity.TAG, "pollPhoneCommand failed", e3);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pollPhoneCommand() {
        new Thread(new AnonymousClass38(), "PhoneCommandPoll").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logToPhoneAsync(final String str, final String str2) {
        if (str2 == null || str2.length() == 0) {
            return;
        }
        new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.39
            @Override // java.lang.Runnable
            public void run() {
                try {
                    MainActivity.this.postPhoneLog(str, str2);
                } catch (Exception e) {
                }
            }
        }, "PhoneLog").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postPhoneLog(String str, String str2) throws Exception {
        int responseCode = -1;
        StringBuilder sbAppend = new StringBuilder().append("kind=");
        if (str == null) {
            str = "";
        }
        StringBuilder sbAppend2 = sbAppend.append(URLEncoder.encode(str, "UTF-8")).append("&message=");
        if (str2 == null) {
            str2 = "";
        }
        byte[] bytes = sbAppend2.append(URLEncoder.encode(str2, "UTF-8")).toString().getBytes(StandardCharsets.UTF_8);
        Exception e = null;
        for (String str3 : buildPhoneEndpointUrls("log")) {
            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str3).openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setConnectTimeout(1800);
                httpURLConnection.setReadTimeout(2500);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                addBridgeAuthorization(httpURLConnection);
                httpURLConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(bytes);
                outputStream.close();
                responseCode = httpURLConnection.getResponseCode();
                readAll((responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
                httpURLConnection.disconnect();
            } catch (Exception e2) {
                e = e2;
            }
            if (responseCode >= 200 && responseCode < 300) {
                rememberPhoneHostFromUrl(str3);
                return;
            }
            e = new IllegalStateException("phone log " + responseCode);
        }
        if (e != null) {
            throw e;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String fetchPhoneEndpointJson(String str) throws Exception {
        String[] strArrBuildPhoneEndpointUrls = buildPhoneEndpointUrls(str);
        Exception e = null;
        for (int i = 0; i < strArrBuildPhoneEndpointUrls.length; i++) {
            try {
                return fetchUrl(strArrBuildPhoneEndpointUrls[i]);
            } catch (Exception e2) {
                e = e2;
            }
        }
        throw new IllegalStateException("スマホ側アプリに接続できません。" + (e == null ? "" : "\n" + e.getMessage()));
    }

    private String requestPhoneSpeechText(byte[] pcm, int sampleRate) throws Exception {
        JSONObject body = new JSONObject();
        body.put("sampleRate", sampleRate);
        body.put("pcm", Base64.encodeToString(pcm, Base64.NO_WRAP));
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        String[] urls = buildPhoneEndpointUrls("stt");
        Exception last = null;
        for (int i = 0; i < urls.length; i++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(urls[i]).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(2200);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                addBridgeAuthorization(connection);
                connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                OutputStream out = connection.getOutputStream();
                out.write(bytes);
                out.close();
                int responseCode = connection.getResponseCode();
                String response = readAll((responseCode < 200 || responseCode >= 300) ? connection.getErrorStream() : connection.getInputStream());
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IllegalStateException("phone stt http " + responseCode + ": " + response);
                }
                JSONObject json = new JSONObject(response);
                if (!json.optBoolean("ok", false)) {
                    throw new PhoneSttResponseException("スマホ音声認識: " + json.optString("error", "failed"));
                }
                rememberPhoneHostFromUrl(urls[i]);
                return json.optString("transcript", "").trim();
            } catch (Exception e) {
                if (e instanceof PhoneSttResponseException) {
                    throw e;
                }
                last = e;
                Log.w(TAG, "phone stt failed url=" + urls[i], e);
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (last != null) {
            maintainWifiConnection(true);
            throw last;
        }
        maintainWifiConnection(true);
        throw new IllegalStateException("phone stt unavailable");
    }

    private String[] buildPhoneTodayUrls() {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        addPhoneHostCandidates(linkedHashSet, "today");
        for (String str : PHONE_TODAY_URLS) {
            linkedHashSet.add(str);
        }
        return (String[]) linkedHashSet.toArray(new String[linkedHashSet.size()]);
    }

    private String[] buildPhoneScheduleUrls(ScheduleRange scheduleRange) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        String str = "schedule?offset=" + scheduleRange.offsetDays + "&days=" + scheduleRange.days;
        try {
            if (scheduleRange.query != null && scheduleRange.query.trim().length() > 0) {
                str = str + "&q=" + URLEncoder.encode(scheduleRange.query.trim(), "UTF-8");
            }
        } catch (Exception e) {
        }
        addPhoneHostCandidates(linkedHashSet, str);
        linkedHashSet.add("http://127.0.0.1:8765/" + str);
        linkedHashSet.add("http://192.168.43.1:8765/" + str);
        linkedHashSet.add("http://192.168.239.1:8765/" + str);
        return (String[]) linkedHashSet.toArray(new String[linkedHashSet.size()]);
    }

    private String[] buildPhoneMailUrls() {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        addPhoneHostCandidates(linkedHashSet, "mail");
        for (String str : PHONE_MAIL_URLS) {
            linkedHashSet.add(str);
        }
        return (String[]) linkedHashSet.toArray(new String[linkedHashSet.size()]);
    }

    private String[] buildPhoneNewsUrls(String str) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        String str2 = "";
        if (str != null) {
            try {
                if (str.trim().length() > 0) {
                    str2 = "?q=" + URLEncoder.encode(str.trim(), "UTF-8");
                }
            } catch (Exception e) {
            }
        }
        addPhoneHostCandidates(linkedHashSet, "news" + str2);
        for (String str3 : PHONE_NEWS_URLS) {
            linkedHashSet.add(str3 + str2);
        }
        return (String[]) linkedHashSet.toArray(new String[linkedHashSet.size()]);
    }

    private String[] buildPhoneEndpointUrls(String str) {
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        addPhoneHostCandidates(linkedHashSet, str);
        linkedHashSet.add("http://127.0.0.1:8765/" + str);
        String wifiGatewayIp = getWifiGatewayIp();
        if (wifiGatewayIp != null && wifiGatewayIp.length() > 0) {
            linkedHashSet.add("http://" + wifiGatewayIp + ":8765/" + str);
        }
        linkedHashSet.add("http://192.168.43.1:8765/" + str);
        linkedHashSet.add("http://192.168.239.1:8765/" + str);
        return (String[]) linkedHashSet.toArray(new String[linkedHashSet.size()]);
    }

    private void addPhoneHostCandidates(LinkedHashSet linkedHashSet, String path) {
        addPhoneHostCandidate(linkedHashSet, getPreferences().getString(KEY_LAST_PHONE_HOST, ""), path);
        addPhoneHostCandidate(linkedHashSet, getWifiGatewayIp(), path);
        addSameSubnetPhoneCandidates(linkedHashSet, getWifiLocalIp(), path);
        addSameSubnetPhoneCandidates(linkedHashSet, getWifiGatewayIp(), path);
        addPhoneHostCandidate(linkedHashSet, "192.168.43.1", path);
        addPhoneHostCandidate(linkedHashSet, "192.168.239.1", path);
    }

    private void addPhoneHostCandidate(LinkedHashSet linkedHashSet, String host, String path) {
        if (host == null) {
            return;
        }
        String trim = host.trim();
        if (trim.length() == 0) {
            return;
        }
        if (trim.startsWith("http://") || trim.startsWith("https://")) {
            linkedHashSet.add(trim.endsWith("/") ? trim + path : trim + "/" + path);
            return;
        }
        linkedHashSet.add("http://" + trim + ":8765/" + path);
    }

    private void addSameSubnetPhoneCandidates(LinkedHashSet linkedHashSet, String ip, String path) {
        if (ip == null) {
            return;
        }
        String trim = ip.trim();
        int dot = trim.lastIndexOf('.');
        if (dot <= 0) {
            return;
        }
        String prefix = trim.substring(0, dot + 1);
        int[] commonHosts = {20, 14, 16, 9, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 15, 17, 18, 19, 21, 22, 23, 24, 25, 30, 50, 100, 101};
        for (int i = 0; i < commonHosts.length; i++) {
            addPhoneHostCandidate(linkedHashSet, prefix + commonHosts[i], path);
        }
    }

    private String getWifiGatewayIp() {
        DhcpInfo dhcpInfo;
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi");
            if (wifiManager != null && (dhcpInfo = wifiManager.getDhcpInfo()) != null && dhcpInfo.gateway != 0) {
                return intToIp(dhcpInfo.gateway);
            }
            return "";
        } catch (Exception e) {
            Log.w(TAG, "getWifiGatewayIp failed", e);
            return "";
        }
    }

    private String getWifiLocalIp() {
        DhcpInfo dhcpInfo;
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi");
            if (wifiManager != null && (dhcpInfo = wifiManager.getDhcpInfo()) != null && dhcpInfo.ipAddress != 0) {
                return intToIp(dhcpInfo.ipAddress);
            }
            return "";
        } catch (Exception e) {
            Log.w(TAG, "getWifiLocalIp failed", e);
            return "";
        }
    }

    private String intToIp(int i) {
        return (i & 255) + "." + ((i >> 8) & 255) + "." + ((i >> 16) & 255) + "." + ((i >> 24) & 255);
    }

    private void rememberPhoneHostFromUrl(String str) {
        try {
            URL url = new URL(str);
            if (url.getPort() == 8765) {
                String host = url.getHost();
                if (host != null && host.length() > 0 && !"127.0.0.1".equals(host)) {
                    getPreferences().edit().putString(KEY_LAST_PHONE_HOST, host).apply();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String fetchUrl(String str) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setRequestMethod("GET");
        addBridgeAuthorization(httpURLConnection);
        httpURLConnection.setConnectTimeout(1800);
        httpURLConnection.setReadTimeout(2500);
        int responseCode = httpURLConnection.getResponseCode();
        String all = readAll((responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
        httpURLConnection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("スマホ予定サーバー " + responseCode + ": " + all);
        }
        rememberPhoneHostFromUrl(str);
        return all;
    }

    private void addBridgeAuthorization(HttpURLConnection connection) {
        String token = getPreferences().getString(KEY_BRIDGE_TOKEN, "").trim();
        if (token.length() > 0) {
            connection.setRequestProperty("X-Roki-Token", token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildDirectScheduleText(String str, ScheduleRange scheduleRange) throws Exception {
        JSONObject jSONObject = new JSONObject(str);
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("events");
        String strOptString = jSONObject.optString("startDate", jSONObject.optString("date", ""));
        String strOptString2 = jSONObject.optString("endDate", strOptString);
        String strOptString3 = jSONObject.optString("query", "");
        boolean z = false;
        int length = jSONArrayOptJSONArray == null ? 0 : jSONArrayOptJSONArray.length();
        StringBuilder sb = new StringBuilder();
        sb.append("予定確認");
        if (scheduleRange != null && scheduleRange.label != null && scheduleRange.label.length() > 0) {
            sb.append("（").append(scheduleRange.label).append("）");
        }
        sb.append('\n');
        sb.append(strOptString);
        if (!strOptString2.equals(strOptString)) {
            sb.append("〜").append(strOptString2);
        }
        if (strOptString3.length() > 0) {
            sb.append("\n検索語: ").append(strOptString3);
        }
        sb.append('\n');
        if (length == 0) {
            sb.append("該当する予定は見つかりませんでした。");
            return sb.toString();
        }
        sb.append("該当予定: ").append(length).append("件\n");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d HH:mm", Locale.JAPAN);
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm", Locale.JAPAN);
        int iMin = Math.min(length, 6);
        int i = 0;
        while (i < iMin) {
            JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i);
            boolean zOptBoolean = jSONObject2.optBoolean("allDay", z);
            long jOptLong = jSONObject2.optLong("begin");
            long jOptLong2 = jSONObject2.optLong("end");
            String strOptString4 = jSONObject2.optString("title", "無題");
            String strOptString5 = jSONObject2.optString("location", "");
            sb.append("- ").append(zOptBoolean ? simpleDateFormat.format(new Date(jOptLong)) + " 終日" : simpleDateFormat.format(new Date(jOptLong)) + "-" + simpleDateFormat2.format(new Date(jOptLong2))).append(" ").append(strOptString4);
            if (strOptString5.length() > 0) {
                sb.append(" / ").append(strOptString5);
            }
            sb.append('\n');
            i++;
            z = false;
        }
        if (length > iMin) {
            sb.append("ほか ").append(length - iMin).append(" 件あります。条件を絞ると見やすくなります。");
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildDirectMailText(String str) throws Exception {
        JSONArray jSONArrayOptJSONArray = new JSONObject(str).optJSONArray("mails");
        int length = jSONArrayOptJSONArray == null ? 0 : jSONArrayOptJSONArray.length();
        StringBuilder sb = new StringBuilder();
        sb.append("最近のメール通知\n");
        if (length == 0) {
            sb.append("新着メール通知はありません。");
            return sb.toString();
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d HH:mm", Locale.JAPAN);
        int iMin = Math.min(length, 8);
        for (int i = 0; i < iMin; i++) {
            JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i);
            String str2 = simpleDateFormat.format(new Date(jSONObject.optLong("time")));
            String strOptString = jSONObject.optString("from_or_title", "不明");
            String strLimitText = limitText(jSONObject.optString("summary", ""), MAX_MAIL_SUMMARY_CHARS);
            sb.append("- ").append(str2).append(" ").append(strOptString);
            if (strLimitText.length() > 0) {
                sb.append(": ").append(strLimitText);
            }
            sb.append('\n');
        }
        if (length > iMin) {
            sb.append("ほか ").append(length - iMin).append(" 件あります。");
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildDirectNewsText(String str, String str2, boolean z) throws Exception {
        String str3;
        JSONObject jSONObject = new JSONObject(str);
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("articles");
        int length = jSONArrayOptJSONArray == null ? 0 : jSONArrayOptJSONArray.length();
        StringBuilder sb = new StringBuilder();
        if (str2 != null && str2.trim().length() > 0) {
            str3 = "ニュース検索: " + str2.trim();
        } else {
            str3 = "今日のニュース";
        }
        sb.append(str3);
        sb.append('\n');
        sb.append(jSONObject.optString("date", "")).append(" / ").append(jSONObject.optString("source", "news")).append('\n');
        if (z) {
            sb.append("人物評価は断定せず、ニュース見出しで確認できる範囲だけ表示します。\n");
        }
        if (length == 0) {
            sb.append("ニュースが見つかりませんでした。");
            return sb.toString();
        }
        int iMin = Math.min(length, 6);
        for (int i = 0; i < iMin; i++) {
            JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i);
            sb.append("- ").append(jSONObject2.optString("title", "無題"));
            String strOptString = jSONObject2.optString("source", "");
            if (strOptString.length() > 0) {
                sb.append(" / ").append(strOptString);
            }
            sb.append('\n');
        }
        if (length > iMin) {
            sb.append("ほか ").append(length - iMin).append(" 件あります。");
        }
        return sb.toString();
    }

    private String extractNewsQuery(String str) {
        String strTrim = (str == null ? "" : str.trim()).replace("今日のニュース", " ").replace("今日ニュース", " ").replace("ニュース", " ").replace("news", " ").replace("News", " ").replace("今日", " ").replace("本日", " ").replace("最新", " ").replace("昨日", " ").replace("機能", " ").replace("さん", " ").replace("記者会見", " 記者会見 ").replace("よっぱらっていたよね", " ").replace("酔っぱらっていたよね", " ").replace("酔っていたよね", " ").replace("よっぱらっていた", " ").replace("酔っぱらっていた", " ").replace("酔っていた", " ").replace("だったよね", " ").replace("だよね", " ").replace("教えて", " ").replace("とは", " ").replace("では", " ").replace("が", " ").replace("の", " ").replace("で", " ").replace("は", " ").replace("？", " ").replace("?", " ").trim();
        if (strTrim.length() <= 1) {
            return "";
        }
        if (strTrim.contains("高市") && !strTrim.contains("高市早苗")) {
            strTrim = strTrim.replace("高市", "高市早苗");
        }
        return limitText(strTrim, 80);
    }

    private boolean isReputationNewsQuery(String str) {
        String strTrim = str == null ? "" : str.trim();
        return strTrim.contains("よっぱら") || strTrim.contains("酔っぱら") || strTrim.contains("酔って") || strTrim.contains("泥酔") || strTrim.contains("変だった");
    }

    private String buildTodayScheduleText(String str) throws Exception {
        JSONObject jSONObject = new JSONObject(str);
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("events");
        String strOptString = jSONObject.optString("date", "");
        StringBuilder sb = new StringBuilder();
        sb.append("日付: ").append(strOptString).append('\n');
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            sb.append("予定: 今日は登録された予定がありません。");
            return sb.toString();
        }
        sb.append("予定:\n");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.JAPAN);
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i);
            boolean zOptBoolean = jSONObject2.optBoolean("allDay", false);
            String strOptString2 = jSONObject2.optString("title", "無題");
            String strOptString3 = jSONObject2.optString("location", "");
            sb.append("- ").append(zOptBoolean ? "終日" : simpleDateFormat.format(new Date(jSONObject2.optLong("begin"))) + "-" + simpleDateFormat.format(new Date(jSONObject2.optLong("end")))).append(" ").append(strOptString2);
            if (!strOptString3.isEmpty()) {
                sb.append("（").append(strOptString3).append("）");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String buildRecentMailText(String str) throws Exception {
        JSONObject jSONObject = new JSONObject(str);
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("mails");
        StringBuilder sb = new StringBuilder();
        sb.append("メール取得元: ").append(jSONObject.optString("source", "android_notifications")).append('\n');
        sb.append("注意: ").append(jSONObject.optString("note", "通知に出た範囲だけです。")).append('\n');
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            sb.append("メール: 新着メール通知はありません。");
            return sb.toString();
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.JAPAN);
        sb.append("最近のメール通知:\n");
        int iMin = Math.min(jSONArrayOptJSONArray.length(), 5);
        for (int i = 0; i < iMin; i++) {
            JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i);
            String str2 = simpleDateFormat.format(new Date(jSONObject2.optLong("time")));
            String strOptString = jSONObject2.optString("from_or_title", "不明");
            String strLimitText = limitText(jSONObject2.optString("summary", ""), MAX_MAIL_SUMMARY_CHARS);
            sb.append("- ").append(str2).append(" ").append(strOptString);
            if (!strLimitText.isEmpty()) {
                sb.append(": ").append(strLimitText);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String requestGeminiWithRetry(String str, String str2) throws Exception {
        GeminiHttpException e = null;
        String[] strArr = (str2 != null && str2.length() > 3000) ? new String[]{"gemini-2.5-flash-lite", "gemini-2.5-flash"} : MODELS;
        for (int i = 0; i < strArr.length; i++) {
            String str3 = strArr[i];
            for (int i2 = 1; i2 <= 2; i2++) {
                try {
                    return requestGemini(str, str2, str3);
                } catch (GeminiHttpException e2) {
                    e = e2;
                    if (e.isQuotaLimited()) {
                        throw e;
                    }
                    if (!e.isRetryable()) {
                        throw e;
                    }
                    if (i2 != 2) {
                        Thread.sleep(e.isServiceUnavailable() ? 3500L : 1200L);
                    }
                }
            }
        }
        throw e;
    }

    private VoiceResult requestGeminiAudioWithRetry(String str, byte[] bArr) throws Exception {
        GeminiHttpException e = null;
        String[] strArr = {"gemini-2.5-flash-lite", "gemini-2.5-flash"};
        for (int i = 0; i < strArr.length; i++) {
            String str2 = strArr[i];
            for (int i2 = 1; i2 <= 2; i2++) {
                try {
                    return requestGeminiAudio(str, bArr, str2);
                } catch (GeminiHttpException e2) {
                    e = e2;
                    if (e.isQuotaLimited()) {
                        throw e;
                    }
                    if (!e.isRetryable()) {
                        throw e;
                    }
                    if (i2 != 2) {
                        Thread.sleep(e.isServiceUnavailable() ? 3500L : 1200L);
                    }
                }
            }
        }
        throw e;
    }

    private String requestProactiveAudioWithRetry(String str, byte[] bArr) throws Exception {
        GeminiHttpException e = null;
        for (int i = 0; i < MODELS.length; i++) {
            try {
                return requestProactiveAudio(str, bArr, MODELS[i]);
            } catch (GeminiHttpException e2) {
                e = e2;
                if (!e.isRetryable()) {
                    throw e;
                }
                Thread.sleep(1200L);
            }
        }
        throw e;
    }

    private String requestGemini(String str, String str2, String str3) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + str3 + ":generateContent").openConnection();
        this.activeGeminiConnection = httpURLConnection;
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setConnectTimeout(8000);
        httpURLConnection.setReadTimeout(50000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpURLConnection.setRequestProperty("x-goog-api-key", str);
        markGeminiRequestStarted();
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("text", str2);
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(jSONObject);
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("role", "user");
        jSONObject2.put("parts", jSONArray);
        JSONArray jSONArray2 = new JSONArray();
        jSONArray2.put(jSONObject2);
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("contents", jSONArray2);
        JSONObject jSONObject4 = new JSONObject();
        jSONObject4.put("maxOutputTokens", (str2 != null && str2.length() > 5000) ? 1100 : (str2 != null && str2.length() > 3000) ? 1500 : 2200);
        jSONObject4.put("temperature", 0.4d);
        jSONObject3.put("generationConfig", jSONObject4);
        byte[] bytes = jSONObject3.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream outputStream = httpURLConnection.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
        int responseCode = httpURLConnection.getResponseCode();
        String all = readAll((responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
        httpURLConnection.disconnect();
        if (this.activeGeminiConnection == httpURLConnection) {
            this.activeGeminiConnection = null;
        }
        if (responseCode < 200 || responseCode >= 300) {
            throw new GeminiHttpException(responseCode, str3, extractError(all));
        }
        JSONArray jSONArrayOptJSONArray = new JSONObject(all).optJSONArray("candidates");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            throw new IllegalStateException("回答が生成されませんでした");
        }
        JSONArray jSONArray3 = jSONArrayOptJSONArray.getJSONObject(0).getJSONObject("content").getJSONArray("parts");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jSONArray3.length(); i++) {
            String strOptString = jSONArray3.getJSONObject(i).optString("text", "");
            if (!strOptString.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(strOptString);
            }
        }
        if (sb.length() == 0) {
            throw new IllegalStateException("テキスト回答がありません");
        }
        markGeminiRequestSucceeded();
        return sb.toString();
    }

    private String requestProactiveAudio(String str, byte[] bArr, String str2) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + str2 + ":generateContent").openConnection();
        this.activeGeminiConnection = httpURLConnection;
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setConnectTimeout(8000);
        httpURLConnection.setReadTimeout(30000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpURLConnection.setRequestProperty("x-goog-api-key", str);
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("text", getCustomInstructions() + "\n\nあなたはRokidグラスのプロアクティブ表示AIです。添付音声は周囲の会話や再生音の一部です。ユーザーの会話を邪魔しないため、音声読み上げではなく画面表示だけに使います。以下の条件で日本語で短く返してください。1. 重要な用語、固有名詞、数字、確認すべき主張があれば説明する。2. 不確かな場合は断定せず「確認候補」とする。3. 個人的・機密的な内容をむやみに詳述しない。4. 雑音や意味の薄い会話なら「表示する補足はありません」とだけ返す。5. 画面下部に出すため、最大3行、Markdownなし。");
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("mime_type", "audio/wav");
        jSONObject2.put("data", Base64.encodeToString(bArr, 2));
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("inline_data", jSONObject2);
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(jSONObject);
        jSONArray.put(jSONObject3);
        JSONObject jSONObject4 = new JSONObject();
        jSONObject4.put("role", "user");
        jSONObject4.put("parts", jSONArray);
        JSONArray jSONArray2 = new JSONArray();
        jSONArray2.put(jSONObject4);
        JSONObject jSONObject5 = new JSONObject();
        jSONObject5.put("contents", jSONArray2);
        JSONObject jSONObject6 = new JSONObject();
        jSONObject6.put("maxOutputTokens", 260);
        jSONObject6.put("temperature", 0.2d);
        jSONObject5.put("generationConfig", jSONObject6);
        byte[] bytes = jSONObject5.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream outputStream = httpURLConnection.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
        int responseCode = httpURLConnection.getResponseCode();
        String all = readAll((responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
        httpURLConnection.disconnect();
        if (this.activeGeminiConnection == httpURLConnection) {
            this.activeGeminiConnection = null;
        }
        if (responseCode < 200 || responseCode >= 300) {
            throw new GeminiHttpException(responseCode, str2, extractError(all));
        }
        JSONArray jSONArrayOptJSONArray = new JSONObject(all).optJSONArray("candidates");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            throw new IllegalStateException("回答が生成されませんでした");
        }
        JSONArray jSONArray3 = jSONArrayOptJSONArray.getJSONObject(0).getJSONObject("content").getJSONArray("parts");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jSONArray3.length(); i++) {
            String strOptString = jSONArray3.getJSONObject(i).optString("text", "");
            if (!strOptString.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(strOptString);
            }
        }
        String strTrim = sb.toString().trim();
        if (strTrim.length() == 0) {
            return "表示する補足はありません";
        }
        return strTrim;
    }

    private VoiceResult requestGeminiAudio(String str, byte[] bArr, String str2) throws Exception {
        String strBuildTodayScheduleText;
        String strBuildRecentMailText;
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + str2 + ":generateContent").openConnection();
        this.activeGeminiConnection = httpURLConnection;
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setConnectTimeout(8000);
        httpURLConnection.setReadTimeout(45000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpURLConnection.setRequestProperty("x-goog-api-key", str);
        markGeminiRequestStarted();
        strBuildTodayScheduleText = "";
        strBuildRecentMailText = "";
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("text", limitText(getCustomInstructions(), 350) + "\n\nこの音声を日本語で文字起こしし、短く答えてください。必ず次のJSONだけを返してください。{\"transcript\":\"音声を文字起こしした内容\",\"answer\":\"短い日本語回答\"} 予定、カレンダー、メール、ニュースの質問でも、ここでは実データを推測しないでください。その場合のanswerは「確認します」にしてください。Markdownなし。");
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("mime_type", "audio/wav");
        jSONObject2.put("data", Base64.encodeToString(bArr, 2));
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("inline_data", jSONObject2);
        JSONArray jSONArray = new JSONArray();
        jSONArray.put(jSONObject);
        jSONArray.put(jSONObject3);
        JSONObject jSONObject4 = new JSONObject();
        jSONObject4.put("role", "user");
        jSONObject4.put("parts", jSONArray);
        JSONArray jSONArray2 = new JSONArray();
        jSONArray2.put(jSONObject4);
        JSONObject jSONObject5 = new JSONObject();
        jSONObject5.put("contents", jSONArray2);
        JSONObject jSONObject6 = new JSONObject();
        jSONObject6.put("maxOutputTokens", 360);
        jSONObject6.put("temperature", 0.2d);
        jSONObject5.put("generationConfig", jSONObject6);
        byte[] bytes = jSONObject5.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream outputStream = httpURLConnection.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
        int responseCode = httpURLConnection.getResponseCode();
        String all = readAll((responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
        httpURLConnection.disconnect();
        if (this.activeGeminiConnection == httpURLConnection) {
            this.activeGeminiConnection = null;
        }
        if (responseCode < 200 || responseCode >= 300) {
            throw new GeminiHttpException(responseCode, str2, extractError(all));
        }
        JSONArray jSONArrayOptJSONArray = new JSONObject(all).optJSONArray("candidates");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            throw new IllegalStateException("回答が生成されませんでした");
        }
        JSONArray jSONArray3 = jSONArrayOptJSONArray.getJSONObject(0).getJSONObject("content").getJSONArray("parts");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jSONArray3.length(); i++) {
            String strOptString = jSONArray3.getJSONObject(i).optString("text", "");
            if (!strOptString.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(strOptString);
            }
        }
        if (sb.length() == 0) {
            throw new IllegalStateException("テキスト回答がありません");
        }
        markGeminiRequestSucceeded();
        return parseVoiceResult(sb.toString());
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x002c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private com.example.rokidkeyboardbridge.MainActivity.VoiceResult parseVoiceResult(java.lang.String r5) {
        if (System.currentTimeMillis() >= 0) {
            String raw = r5 == null ? "" : r5.trim();
            String json = raw;
            if (json.startsWith("```")) {
                int firstBreak = json.indexOf('\n');
                int lastFence = json.lastIndexOf("```");
                if (firstBreak >= 0 && lastFence > firstBreak) {
                    json = json.substring(firstBreak + 1, lastFence).trim();
                }
            }
            try {
                JSONObject object = new JSONObject(json);
                String transcript = object.optString("transcript", "").trim();
                String answerText = object.optString("answer", "").trim();
                if (answerText.length() == 0) {
                    answerText = raw;
                }
                return new VoiceResult(transcript, answerText);
            } catch (Exception ignored) {
                return new VoiceResult("", raw);
            }
        }
        /*
            r4 = this;
            java.lang.String r0 = ""
            if (r5 != 0) goto L6
            r5 = r0
            goto La
        L6:
            java.lang.String r5 = r5.trim()
        La:
            java.lang.String r1 = "```"
            boolean r2 = r5.startsWith(r1)
            if (r2 == 0) goto L2c
            r2 = 10
            int r2 = r5.indexOf(r2)
            int r1 = r5.lastIndexOf(r1)
            if (r2 < 0) goto L2c
            if (r1 <= r2) goto L2c
            int r2 = r2 + 1
            java.lang.String r1 = r5.substring(r2, r1)
            java.lang.String r1 = r1.trim()
            goto L2d
        L2c:
            r1 = r5
        L2d:
            org.json.JSONObject r2 = new org.json.JSONObject     // Catch: java.lang.Exception -> L53
            r2.<init>(r1)     // Catch: java.lang.Exception -> L53
            java.lang.String r1 = "transcript"
            java.lang.String r1 = r2.optString(r1, r0)     // Catch: java.lang.Exception -> L53
            java.lang.String r1 = r1.trim()     // Catch: java.lang.Exception -> L53
            java.lang.String r3 = "answer"
            java.lang.String r2 = r2.optString(r3, r0)     // Catch: java.lang.Exception -> L53
            java.lang.String r2 = r2.trim()     // Catch: java.lang.Exception -> L53
            int r3 = r2.length()     // Catch: java.lang.Exception -> L53
            if (r3 != 0) goto L4d
            r2 = r5
        L4d:
            com.example.rokidkeyboardbridge.MainActivity$VoiceResult r3 = new com.example.rokidkeyboardbridge.MainActivity$VoiceResult     // Catch: java.lang.Exception -> L53
            r3.<init>(r1, r2)     // Catch: java.lang.Exception -> L53
            return r3
        L53:
            r1 = move-exception
            java.lang.String r1 = "回答:"
            int r2 = r5.indexOf(r1)
            if (r2 < 0) goto L80
            int r1 = r1.length()
            int r1 = r1 + r2
            java.lang.String r1 = r5.substring(r1)
            java.lang.String r1 = r1.trim()
            r3 = 0
            java.lang.String r5 = r5.substring(r3, r2)
            java.lang.String r5 = r5.trim()
            java.lang.String r2 = "認識:"
            java.lang.String r5 = r5.replace(r2, r0)
            java.lang.String r0 = r5.trim()
            r5 = r1
        L80:
            com.example.rokidkeyboardbridge.MainActivity$VoiceResult r1 = new com.example.rokidkeyboardbridge.MainActivity$VoiceResult
            r1.<init>(r0, r5)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.example.rokidkeyboardbridge.MainActivity.parseVoiceResult(java.lang.String):com.example.rokidkeyboardbridge.MainActivity$VoiceResult");
    }

    private String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = bufferedReader.readLine();
            if (line != null) {
                sb.append(line);
            } else {
                bufferedReader.close();
                return sb.toString();
            }
        }
    }

    private String extractError(String str) {
        try {
            return new JSONObject(str).getJSONObject("error").optString("message", str);
        } catch (Exception e) {
            return str;
        }
    }

    private boolean isNetworkReady() {
        Network activeNetwork;
        NetworkCapabilities networkCapabilities;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService("connectivity");
        if (connectivityManager == null || (activeNetwork = connectivityManager.getActiveNetwork()) == null || (networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)) == null) {
            return false;
        }
        return networkCapabilities.hasCapability(16) || networkCapabilities.hasTransport(1) || networkCapabilities.hasTransport(0) || networkCapabilities.hasTransport(3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWifiEnabled() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi");
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    public boolean isWifiConnected() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi");
            if (wifiManager == null || !wifiManager.isWifiEnabled()) {
                return false;
            }
            android.net.wifi.WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo == null || connectionInfo.getNetworkId() < 0) {
                return false;
            }
            String ssid = connectionInfo.getSSID();
            return ssid != null && ssid.length() > 0 && !"<unknown ssid>".equalsIgnoreCase(ssid);
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maintainWifiConnection(boolean z) {
        WifiManager wifiManager;
        long jCurrentTimeMillis = System.currentTimeMillis();
        if ((!z && jCurrentTimeMillis - this.lastWifiRepairAt < 60000) || (wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi")) == null) {
            return;
        }
        try {
            if (!wifiManager.isWifiEnabled()) {
                this.lastWifiRepairAt = jCurrentTimeMillis;
                Log.i(TAG, "wifi enable requested accepted=" + wifiManager.setWifiEnabled(true));
            } else if (z || !hasActiveNetwork()) {
                this.lastWifiRepairAt = jCurrentTimeMillis;
                try {
                    wifiManager.reassociate();
                } catch (Exception e) {
                }
                try {
                    wifiManager.reconnect();
                } catch (Exception e2) {
                }
                Log.i(TAG, "wifi reconnect requested");
            }
        } catch (Exception e3) {
            Log.e(TAG, "maintainWifiConnection failed", e3);
        }
    }

    private boolean hasActiveNetwork() {
        Network activeNetwork;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService("connectivity");
        if (connectivityManager == null || (activeNetwork = connectivityManager.getActiveNetwork()) == null) {
            return false;
        }
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return networkCapabilities == null || networkCapabilities.hasTransport(1) || networkCapabilities.hasTransport(3) || networkCapabilities.hasCapability(12);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String describeWifiState() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService("wifi");
        if (wifiManager == null) {
            return "WiFi: unavailable";
        }
        return "WiFi: " + (wifiManager.isWifiEnabled() ? "ON" : "OFF") + "\n接続: " + wifiShortState() + "\nネット: " + (isNetworkReady() ? "OK" : "NG") + "\n外ではスマホのテザリング等に接続してください。";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openWifiSettingsFallback() {
        try {
            Intent intent = new Intent("android.settings.WIFI_SETTINGS");
            intent.addFlags(268435456);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openWifiSettingsFallback failed", e);
            openRokidManager();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bringAiToFront() {
        try {
            Intent intent = new Intent(this, (Class<?>) MainActivity.class);
            intent.addFlags(872415232);
            startActivity(intent);
            showControlsTemporarily();
            setStatus("RokidKeyboardAIを表示", Color.rgb(90, 220, 120));
            logToPhoneAsync("操作", "RokidKeyboardAIを表示");
        } catch (Exception e) {
            setStatus("AI起動エラー: " + e.getMessage(), -256);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openRokidManager() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.example.advancedsettingsmanager", "com.example.advancedsettingsmanager.MainActivity"));
            intent.addFlags(268435456);
            startActivity(intent);
            setStatus("RokidManagerを開きました", Color.rgb(90, 220, 120));
            logToPhoneAsync("操作", "RokidManagerを開きました");
        } catch (Exception e) {
            try {
                Intent intent2 = new Intent();
                intent2.setComponent(new ComponentName("com.example.rokidmanagerlauncher", "com.example.rokidmanagerlauncher.MainActivity"));
                intent2.addFlags(268435456);
                startActivity(intent2);
                setStatus("RokidManagerを開きました", Color.rgb(90, 220, 120));
                logToPhoneAsync("操作", "RokidManagerを開きました");
            } catch (Exception e2) {
                setStatus("Manager起動エラー: " + e2.getMessage(), -256);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showInputMethodPicker() {
        this.input.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        if (inputMethodManager != null) {
            inputMethodManager.showInputMethodPicker();
            setStatus("Fcitx5を選ぶと日本語入力できます。", -3355444);
        }
    }

    private void bindAssistService() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(ASSIST_PACKAGE, ASSIST_SERVICE));
        boolean zBindService = bindService(intent, this.connection, 1);
        setStatus(zBindService ? "Rokid音声へ接続中…" : "Rokid音声を開始できません", zBindService ? -3355444 : -65536);
    }

    private void showKeyboard() {
        this.input.requestFocus();
        hideKeyboard();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideKeyboard() {
        ((InputMethodManager) getSystemService("input_method")).hideSoftInputFromWindow(this.input.getWindowToken(), 0);
        this.input.clearFocus();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void speakWithRokidChunked(final String str) {
        boolean z = false;
        StringBuilder sbAppend = new StringBuilder().append("speakWithRokidChunked length=").append(str == null ? 0 : str.length()).append(" binder=").append(this.assistBinder != null).append(" alive=");
        if (this.assistBinder != null && this.assistBinder.isBinderAlive()) {
            z = true;
        }
        Log.i(TAG, sbAppend.append(z).toString());
        if (this.assistBinder == null || !this.assistBinder.isBinderAlive()) {
            setStatus("Rokid音声へ再接続中", -256);
            bindAssistService();
            this.handler.postDelayed(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.40
                @Override // java.lang.Runnable
                public void run() {
                    if (MainActivity.this.assistBinder == null || !MainActivity.this.assistBinder.isBinderAlive()) {
                        MainActivity.this.setStatus("Rokid音声に接続できません。グラス装着状態を確認してください。", -256);
                    } else {
                        MainActivity.this.speakWithRokidChunked(str);
                    }
                }
            }, 1000L);
        } else {
            final String[] strArrSplitForTts = splitForTts(str);
            Log.i(TAG, "TTS chunk count=" + strArrSplitForTts.length);
            if (strArrSplitForTts.length == 0) {
                return;
            }
            new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.41
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        for (String str2 : strArrSplitForTts) {
                            MainActivity.this.sendTtsChunk(str2);
                            Thread.sleep(Math.max(1200L, Math.min(4500L, ((long) str2.length()) * 80)));
                        }
                        MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.41.1
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.setStatus("読み上げ完了", Color.rgb(90, 220, 120));
                            }
                        });
                    } catch (Exception e) {
                        MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.41.2
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.setStatus("表示は完了。読み上げエラー: " + e.getMessage(), -256);
                            }
                        });
                    }
                }
            }, "RokidTtsChunks").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void playBeepTest() {
        setStatus("Beep test playing", Color.rgb(90, 220, 120));
        new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.42
            /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
                jadx.core.utils.exceptions.JadxRuntimeException: Can't find top splitter block for handler:B:26:0x00e3
                	at jadx.core.utils.BlockUtils.getTopSplitterForHandler(BlockUtils.java:1182)
                	at jadx.core.dex.visitors.regions.maker.ExcHandlersRegionMaker.collectHandlerRegions(ExcHandlersRegionMaker.java:53)
                	at jadx.core.dex.visitors.regions.maker.ExcHandlersRegionMaker.process(ExcHandlersRegionMaker.java:38)
                	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:27)
                */
            @Override // java.lang.Runnable
            public void run() {
                /*
                    Method dump skipped, instruction units count: 245
                    To view this dump add '--comments-level debug' option
                */
                throw new UnsupportedOperationException("Method not decompiled: com.example.rokidkeyboardbridge.MainActivity.AnonymousClass42.run():void");
            }
        }, "RokidBeepTest").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendTtsChunk(String str) throws Exception {
        Log.i(TAG, "sendTtsChunk length=" + str.length() + " text=" + str);
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("ttsMsg", str);
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("type", "cmd_play_tts");
        jSONObject2.put("data", jSONObject);
        transactControl(jSONObject2.toString());
        Log.i(TAG, "sendTtsChunk transact done");
        this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.43
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.setStatus("Rokid女性音声で読み上げ中", Color.rgb(90, 220, 120));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void speakWithPhoneTts(String str) {
        Log.i(TAG, "speakWithPhoneTts length=" + (str == null ? 0 : str.length()) + " binder=" + (this.assistBinder != null) + " alive=" + (this.assistBinder != null && this.assistBinder.isBinderAlive()));
        if (this.assistBinder == null || !this.assistBinder.isBinderAlive()) {
            setStatus("PhoneTTS: reconnecting Rokid service", -256);
            bindAssistService();
            final String retryText = str;
            this.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (MainActivity.this.assistBinder != null && MainActivity.this.assistBinder.isBinderAlive()) {
                        MainActivity.this.speakWithPhoneTts(retryText);
                    } else {
                        MainActivity.this.setStatus("Rokid voice reconnect failed", -256);
                    }
                }
            }, 900L);
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject();
            if (str == null) {
                str = "";
            }
            jSONObject.put("content", str);
            jSONObject.put("interruptAssistant", true);
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("cmd", "Sys");
            jSONObject2.put("key", "Tts_SendPlayTts");
            jSONObject2.put("data", jSONObject.toString());
            JSONObject jSONObject3 = new JSONObject();
            jSONObject3.put("type", "cmd_phone_gatt_send_data");
            jSONObject3.put("data", jSONObject2);
            transactControl(jSONObject3.toString());
            setStatus("PhoneTTS sent", Color.rgb(90, 220, 120));
            Log.i(TAG, "speakWithPhoneTts transact done json=" + jSONObject3.toString());
        } catch (Exception e) {
            Log.e(TAG, "PhoneTTS failed", e);
            setStatus("PhoneTTS error: " + e.getMessage(), -256);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void speakWithPhoneTtsChunked(String str) {
        final String[] strArrSplitForTts = splitForTts(str);
        final int i = this.ttsGeneration + 1;
        this.ttsGeneration = i;
        Log.i(TAG, "PhoneTTS chunk count=" + strArrSplitForTts.length);
        if (strArrSplitForTts.length == 0) {
            setMascotMode(0);
            setConversationActive(false);
            if (this.voiceLoopMode) {
                restartVoiceLoopAfterDelay(900L);
                return;
            }
            return;
        }
        this.headGlanceWake = false;
        setGlanceHudVisible(true);
        wakeDisplayForGlance();
        setConversationActive(true);
        setMascotMode(2);
        new Thread(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.44
            @Override // java.lang.Runnable
            public void run() {
                for (int i2 = 0; i2 < strArrSplitForTts.length; i2++) {
                    try {
                        if (i != MainActivity.this.ttsGeneration) {
                            return;
                        }
                        final int chunkIndex = i2;
                        final String str2 = strArrSplitForTts[i2];
                        final int length = strArrSplitForTts.length;
                        MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.44.1
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.scrollAnswerForSpeech(chunkIndex, length);
                                MainActivity.this.speakWithPhoneTts(str2);
                            }
                        });
                        Thread.sleep(Math.max(2200L, Math.min(11000L, ((long) str2.length()) * 155)));
                    } catch (Exception e) {
                        Log.e(MainActivity.TAG, "PhoneTTS chunks failed", e);
                        MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.44.3
                            @Override // java.lang.Runnable
                            public void run() {
                                MainActivity.this.setStatus("PhoneTTS chunks error: " + e.getMessage(), -256);
                            }
                        });
                        return;
                    }
                }
                MainActivity.this.handler.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.44.2
                    @Override // java.lang.Runnable
                    public void run() {
                        if (i == MainActivity.this.ttsGeneration) {
                            if (MainActivity.this.voiceLoopMode) {
                                MainActivity.this.setConversationActive(true);
                                MainActivity.this.restartVoiceLoopAfterDelay(900L);
                            } else {
                                MainActivity.this.setConversationActive(false);
                                MainActivity.this.setGlanceHudVisible(false);
                            }
                            if (!MainActivity.this.voiceLoopMode) {
                                MainActivity.this.setMascotMode(0);
                            }
                        }
                    }
                });
            }
        }, "PhoneTtsChunks").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scrollAnswerToTop() {
        if (this.answerScroll == null) {
            return;
        }
        this.answerScroll.post(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.45
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.answerScroll.smoothScrollTo(0, 0);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scrollAnswerForSpeech(final int i, final int i2) {
        if (this.answerScroll == null || this.answer == null || i2 <= 1) {
            return;
        }
        this.answerScroll.postDelayed(new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.46
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.answerScroll.smoothScrollTo(0, (int) (Math.max(0, MainActivity.this.answer.getHeight() - MainActivity.this.answerScroll.getHeight()) * (i / Math.max(1, i2 - 1))));
            }
        }, 250L);
    }

    private String[] splitForTts(String str) {
        if (str == null) {
            str = "";
        }
        String strTrim = str.replace("*", "").replace("#", "").replace("`", "").replace("：", "、").replace(":", "、").replace("・", "、").replace("\r", "\n").replaceAll("\\n+", "。").replaceAll("\\s+", " ").trim();
        if (strTrim.isEmpty()) {
            return new String[0];
        }
        ArrayList arrayList = new ArrayList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strTrim.length(); i++) {
            char cCharAt = strTrim.charAt(i);
            sb.append(cCharAt);
            boolean z = cCharAt == 12290 || cCharAt == 12289 || cCharAt == 65281 || cCharAt == 65311;
            if ((sb.length() >= 90 && z) || sb.length() >= MAX_MAIL_SUMMARY_CHARS) {
                arrayList.add(sb.toString().trim());
                sb.setLength(0);
            }
            if (arrayList.size() >= 16) {
                break;
            }
        }
        if (sb.length() > 0 && arrayList.size() < 16) {
            arrayList.add(sb.toString().trim());
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    private void speakWithRokid(String str) {
        if (this.assistBinder == null || !this.assistBinder.isBinderAlive()) {
            setStatus("回答は表示済み。Rokid音声へ再接続中…", -256);
            bindAssistService();
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("ttsMsg", str);
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("type", "cmd_play_tts");
            jSONObject2.put("data", jSONObject);
            transactControl(jSONObject2.toString());
            setStatus("回答をRokid女性音声で読み上げています", Color.rgb(90, 220, 120));
        } catch (Exception e) {
            setStatus("回答は表示済み。読み上げエラー: " + e.getMessage(), -256);
        }
    }

    private void transactControl(String str) throws Exception {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(ASSIST_DESCRIPTOR);
            parcelObtain.writeString(getPackageName());
            parcelObtain.writeString(str);
            if (!this.assistBinder.transact(3, parcelObtain, parcelObtain2, 0)) {
                throw new IllegalStateException("Rokid音声サービスが命令を拒否しました");
            }
            parcelObtain2.readException();
        } finally {
            parcelObtain2.recycle();
            parcelObtain.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setStatus(String str, int i) {
        if (this.status != null) {
            this.status.setText(str);
            this.status.setTextColor(i);
        }
        updateMascotForStatus(str, i);
    }

    private final class MascotView extends View {
        private final RectF bitmapDst;
        private final Paint bitmapPaint;
        private final Rect bitmapSrc;
        private final Paint cut;
        private int expression;
        private final Paint fill;
        private int frame;
        private final Paint glow;
        private final Paint line;
        private Bitmap mascotSheet;
        private int mode;
        private final RectF oval;
        private final Path path;
        private final Runnable ticker;

        private int nextFrame() {
            MascotView mascotView = this;
            int i = mascotView.frame;
            mascotView.frame = i + 1;
            return i;
        }

        MascotView(Context context) {
            super(context);
            this.line = new Paint(1);
            this.fill = new Paint(1);
            this.cut = new Paint(1);
            this.glow = new Paint(1);
            this.path = new Path();
            this.bitmapSrc = new Rect();
            this.bitmapDst = new RectF();
            this.oval = new RectF();
            this.bitmapPaint = new Paint(7);
            this.ticker = new Runnable() { // from class: com.example.rokidkeyboardbridge.MainActivity.MascotView.1
                @Override // java.lang.Runnable
                public void run() {
                    MascotView.this.nextFrame();
                    MascotView.this.invalidate();
                    MascotView.this.postDelayed(this, MascotView.this.mode == 0 ? 620L : 180L);
                }
            };
            setWillNotDraw(false);
            setAlpha(0.96f);
            this.line.setColor(Color.rgb(90, 255, 130));
            this.line.setStyle(Paint.Style.STROKE);
            this.line.setStrokeWidth(2.0f);
            this.line.setStrokeCap(Paint.Cap.ROUND);
            this.line.setStrokeJoin(Paint.Join.ROUND);
            this.fill.setColor(Color.argb(150, 90, 255, 130));
            this.fill.setStyle(Paint.Style.FILL);
            this.cut.setColor(-16777216);
            this.cut.setStyle(Paint.Style.FILL);
            this.glow.setColor(Color.argb(0, 90, 255, 130));
            this.glow.setStyle(Paint.Style.FILL);
            try {
                InputStream inputStreamOpen = MainActivity.this.getAssets().open("mascot_sheet.png");
                try {
                    this.mascotSheet = BitmapFactory.decodeStream(inputStreamOpen);
                    inputStreamOpen.close();
                } catch (Throwable th) {
                    inputStreamOpen.close();
                    throw th;
                }
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "mascot sheet load failed", e);
            }
        }

        void setMode(int i) {
            this.mode = i;
            if (i == 2 && this.expression == 0) {
                this.expression = 1;
            } else if (i == 1 && this.expression == 0) {
                this.expression = 0;
            }
            invalidate();
        }

        void setExpression(int i) {
            this.expression = Math.max(0, Math.min(15, i));
            invalidate();
        }

        @Override // android.view.View
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            removeCallbacks(this.ticker);
            post(this.ticker);
        }

        @Override // android.view.View
        protected void onDetachedFromWindow() {
            removeCallbacks(this.ticker);
            super.onDetachedFromWindow();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            float fAbs;
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            double d = this.frame;
            double d2 = this.mode == 0 ? 0.45d : 0.9d;
            Double.isNaN(d);
            float fSin = (float) Math.sin(d * d2);
            if (this.mode == 2) {
                double d3 = this.frame;
                Double.isNaN(d3);
                fAbs = Math.abs((float) Math.sin(d3 * 1.7d));
            } else {
                fAbs = 0.0f;
            }
            float fAbs2 = this.mode == 1 ? Math.abs(fSin) : 0.0f;
            int iMax = Math.max(0, Math.min(15, this.expression));
            float f = 3.0f + ((this.mode == 0 ? 0.8f : 1.8f) * fSin);
            this.line.setStrokeWidth(Math.max(1.55f, width / 34.0f));
            int i = iMax;
            float f2 = fAbs2;
            float f3 = fAbs;
            if (!drawBitmapMascot(canvas, width, height, fSin, f3, f2, i)) {
                canvas.drawRoundRect(1.0f, 1.0f, width - 1.0f, height - 1.0f, 14.0f, 14.0f, this.glow);
                float f4 = width * 0.5f;
                this.path.reset();
                float f5 = width * 0.3f;
                float f6 = f4 - f5;
                float f7 = (0.28f * height) + f;
                this.path.moveTo(f6, f7);
                float f8 = width * 0.12f;
                this.path.quadTo(f6, f + (0.14f * height), f4 - f8, f + (0.1f * height));
                float f9 = (0.07f * height) + f;
                float f10 = f5 + f4;
                this.path.quadTo(f4 + f8, f9, f10, f7);
                float f11 = width * 0.31f;
                float f12 = f4 + f11;
                float f13 = (0.52f * height) + f;
                float f14 = 0.17f * width;
                float f15 = f + (0.67f * height);
                this.path.quadTo(f12, f13, f4 + f14, f15);
                this.path.quadTo(f4, f + (height * 0.8f), f4 - f14, f15);
                this.path.quadTo(f4 - f11, f13, f6, f7);
                canvas.drawPath(this.path, this.fill);
                canvas.drawPath(this.path, this.line);
                float f16 = 0.39f * width;
                float f17 = f + (height * 0.34f);
                float f18 = 0.27f * width;
                this.oval.set(f4 - f16, f17, f4 - f18, f13);
                canvas.drawOval(this.oval, this.fill);
                canvas.drawOval(this.oval, this.line);
                this.oval.set(f4 + f18, f17, f4 + f16, f13);
                canvas.drawOval(this.oval, this.fill);
                canvas.drawOval(this.oval, this.line);
                this.path.reset();
                float f19 = f4 - (width * 0.34f);
                this.path.moveTo(f19, f + (0.29f * height));
                float f20 = f + (height * 0.03f);
                this.path.quadTo(f4 - (0.33f * width), f20, f4 - (0.03f * width), f20);
                float f21 = f + (0.04f * height);
                this.path.quadTo(f10, f21, f12, f + (height * 0.3f));
                this.path.quadTo(f4 + (0.13f * width), f + (0.18f * height), f4 + (0.05f * width), f7);
                this.path.quadTo(f4 - (0.08f * width), f + (0.41f * height), f19, f + (0.37f * height));
                this.path.close();
                canvas.drawPath(this.path, this.cut);
                canvas.drawPath(this.path, this.line);
                float f22 = 0.36f * width;
                canvas.drawArc(f4 - f22, f21, f4 + f22, f + (height * 0.5f), 190.0f, 165.0f, false, this.line);
                canvas.drawLine(f4 - (width * 0.21f), f + (height * 0.2f), f4 - (width * 0.06f), f + (height * 0.12f), this.line);
                float f23 = f + (height * 0.44f);
                drawEyes(canvas, f4, f23, width, height, i, f2);
                drawNose(canvas, f4, f23, width, height);
                drawMouth(canvas, f4, f + (0.64f * height), width, height, i, f3);
                drawCheeks(canvas, f4, f, width, height, i);
                float f24 = width * 0.35f;
                float f25 = f4 - f24;
                float f26 = f + (height * 0.45f);
                float f27 = width * 0.45f;
                canvas.drawLine(f25, f26, f4 - f27, f15, this.line);
                canvas.drawLine(f4 + f24, f26, f4 + f27, f15, this.line);
                this.path.reset();
                float f28 = width * 0.42f;
                float f29 = f + (height * 0.96f);
                this.path.moveTo(f4 - f28, f29);
                float f30 = 0.16f * width;
                float f31 = f + (0.78f * height);
                this.path.lineTo(f4 - f30, f31);
                this.path.lineTo(f4, f + (0.92f * height));
                this.path.lineTo(f30 + f4, f31);
                this.path.lineTo(f4 + f28, f29);
                canvas.drawPath(this.path, this.line);
                drawMoodMarks(canvas, f4, f, width, height, i, f2, f3);
            }
        }

        private boolean drawBitmapMascot(Canvas canvas, float f, float f2, float f3, float f4, float f5, int i) {
            if (this.mascotSheet == null || this.mascotSheet.getWidth() <= 0 || this.mascotSheet.getHeight() <= 0) {
                return false;
            }
            int width = this.mascotSheet.getWidth() / 4;
            int height = this.mascotSheet.getHeight() / 4;
            int iMax = Math.max(0, Math.min(15, i));
            if (iMax == 9) {
                iMax = 14;
            }
            if (this.mode == 0) {
                int cycle = (this.frame / 10) % 8;
                if (cycle == 2) {
                    iMax = 15;
                } else if (cycle == 5) {
                    iMax = 2;
                } else if (cycle == 6) {
                    iMax = 9;
                } else {
                    iMax = 0;
                }
            } else if (this.mode == 1 && (iMax == 9 || iMax == 0)) {
                iMax = ((this.frame / 8) % 2 == 0) ? 0 : 1;
            }
            if (this.mode == 2) {
                int talkCycle = (this.frame / 4) % 4;
                if (talkCycle == 1 || talkCycle == 3) {
                    iMax = 3;
                } else if (iMax == 0 || iMax == 9 || iMax == 14) {
                    iMax = 1;
                }
            }
            int i2 = iMax % 4;
            int i3 = (iMax / 4) * height;
            int i4 = i2 * width;
            int insetX = Math.max(2, width / 64);
            int sourceTop = i3 + Math.max(1, height / 128);
            int sourceBottom = i3 + height - Math.max(1, height / 128);
            this.bitmapSrc.set(i4 + insetX, sourceTop, (i4 + width) - insetX, sourceBottom);
            float fMax = Math.max(0.0f, f * 0.015f);
            float f6 = (this.mode != 0 ? 0.025f : 0.015f) * f2 * f3;
            float scale = 0.70f;
            float drawW = f * scale;
            float drawH = f2 * scale;
            float left = (f - drawW) * 0.48f;
            float top = Math.max(0.0f, f6);
            this.bitmapDst.set(left, top, left + drawW, top + drawH);
            canvas.drawRoundRect(1.0f, 1.0f, f - 1.0f, f2 - 1.0f, 12.0f, 12.0f, this.glow);
            canvas.drawBitmap(this.mascotSheet, this.bitmapSrc, this.bitmapDst, this.bitmapPaint);
            if (this.mode == 1) {
                this.line.setStrokeWidth(Math.max(1.2f, f / 44.0f));
                float f10 = (2.0f * f5) + 3.0f;
                canvas.drawLine(f * 0.8f, f2 * 0.12f, f * 0.86f, (0.05f * f2) + f10, this.line);
                canvas.drawLine(0.88f * f, 0.18f * f2, 0.96f * f, (0.14f * f2) + f10, this.line);
            }
            return true;
        }

        private void drawEyes(Canvas canvas, float f, float f2, float f3, float f4, int i, float f5) {
            Canvas canvas2;
            float f6;
            float f7 = 0.0f;
            float f8 = 0.18f * f3;
            float f9 = f - f8;
            float f10 = 0.06f * f3;
            float f11 = f - f10;
            float f12 = f + f10;
            float f13 = f + f8;
            boolean z = i == 4 || i == 7 || i == 12 || i == 13;
            boolean z2 = i == 9 || i == 11 || i == 14;
            boolean z3 = i == 2 || i == 6 || i == 10 || i == 14;
            boolean z4 = i == 5 || i == 15;
            boolean z5 = i == 15;
            if (z) {
                float f14 = f2 - (f4 * 0.04f);
                float f15 = f2 + (0.05f * f4);
                canvas2 = canvas;
                canvas2.drawArc(f9, f14, f11, f15, 15.0f, 150.0f, false, this.line);
                canvas2.drawArc(f12, f14, f13, f15, 15.0f, 150.0f, false, this.line);
            } else {
                canvas2 = canvas;
                if (z2) {
                    float f16 = (f9 + f11) / 2.0f;
                    float f17 = 0.043f * f3;
                    canvas2.drawCircle(f16, f2, f17, this.line);
                    float f18 = (f12 + f13) / 2.0f;
                    canvas2.drawCircle(f18, f2, f17, this.line);
                    float f19 = 0.016f * f3;
                    canvas2.drawCircle(f16, f2, f19, this.cut);
                    canvas2.drawCircle(f18, f2, f19, this.cut);
                } else {
                    if (z3) {
                        f7 = 0.025f * f4;
                    } else if (z4) {
                        f7 = (-f4) * 0.02f;
                    } else {
                        f6 = f5;
                        float f20 = f2 + f6;
                        float f21 = f2 - 1.0f;
                        canvas2.drawLine(f9, f20, f11, f21, this.line);
                        canvas2 = canvas;
                        canvas2.drawLine(f12, f21, f13, f20, this.line);
                        float f22 = f2 + (f6 * 0.35f);
                        float f23 = 0.013f * f3;
                        canvas2.drawCircle((f9 + f11) / 2.0f, f22, f23, this.cut);
                        canvas2.drawCircle((f12 + f13) / 2.0f, f22, f23, this.cut);
                    }
                    f6 = f7;
                    float f202 = f2 + f6;
                    float f212 = f2 - 1.0f;
                    canvas2.drawLine(f9, f202, f11, f212, this.line);
                    canvas2 = canvas;
                    canvas2.drawLine(f12, f212, f13, f202, this.line);
                    float f222 = f2 + (f6 * 0.35f);
                    float f232 = 0.013f * f3;
                    canvas2.drawCircle((f9 + f11) / 2.0f, f222, f232, this.cut);
                    canvas2.drawCircle((f12 + f13) / 2.0f, f222, f232, this.cut);
                }
            }
            if (z4) {
                float f24 = f3 * 0.2f;
                float f25 = f2 - (0.09f * f4);
                float f26 = f3 * 0.055f;
                float f27 = f2 - (f4 * 0.04f);
                canvas2.drawLine(f - f24, f25, f - f26, f27, this.line);
                canvas.drawLine(f + f26, f27, f + f24, f25, this.line);
                return;
            }
            if (z3) {
                float f28 = f3 * 0.2f;
                float f29 = f2 - (0.045f * f4);
                float f30 = f3 * 0.055f;
                float f31 = f2 - (0.08f * f4);
                canvas.drawLine(f - f28, f29, f - f30, f31, this.line);
                canvas.drawLine(f + f30, f31, f + f28, f29, this.line);
                return;
            }
            if (z5) {
                float f32 = f3 * 0.2f;
                float f33 = f2 - (0.075f * f4);
                float f34 = f3 * 0.055f;
                canvas.drawLine(f - f32, f33, f - f34, f33, this.line);
                canvas.drawLine(f + f34, f33, f + f32, f33, this.line);
                return;
            }
            float f35 = f3 * 0.19f;
            float f36 = f2 - (f4 * 0.055f);
            float f37 = f3 * 0.055f;
            float f38 = f2 - (0.075f * f4);
            canvas.drawLine(f - f35, f36, f - f37, f38, this.line);
            canvas.drawLine(f + f37, f38, f + f35, f36, this.line);
        }

        private void drawNose(Canvas canvas, float f, float f2, float f3, float f4) {
            canvas.drawLine(f, f2 + (0.025f * f4), f - (0.02f * f3), f2 + (0.105f * f4), this.line);
            float f5 = 0.022f * f3;
            float f6 = (0.125f * f4) + f2;
            canvas.drawLine(f - f5, f6, f + f5, f6, this.line);
            float f7 = 0.035f * f3;
            float f8 = (0.13f * f4) + f2;
            canvas.drawCircle(f - f7, f8, 1.1f, this.line);
            canvas.drawCircle(f7 + f, f8, 1.1f, this.line);
        }

        private void drawMouth(Canvas canvas, float f, float f2, float f3, float f4, int i, float f5) {
            switch (i) {
                case 1:
                case 7:
                    float f6 = f3 * 0.11f;
                    canvas.drawArc(f - f6, f2 - (0.045f * f4), f + f6, f2 + (f4 * 0.08f), 10.0f, 160.0f, false, this.line);
                    break;
                case 2:
                default:
                    float f7 = 0.085f * f3;
                    canvas.drawLine(f - f7, f2, f + f7, f2, this.line);
                    break;
                case 3:
                case 4:
                    float f8 = 0.15f * f3;
                    canvas.drawArc(f - f8, f2 - (f4 * 0.08f), f + f8, f2 + (0.14f * f4), 10.0f, 160.0f, false, this.line);
                    float f9 = f3 * 0.08f;
                    float f10 = f2 + (f4 * 0.055f);
                    canvas.drawLine(f - f9, f10, f + f9, f10, this.line);
                    break;
                case 5:
                case 6:
                case 15:
                    float f11 = f3 * 0.11f;
                    canvas.drawArc(f - f11, f2 - (0.005f * f4), f + f11, f2 + (f4 * 0.12f), 200.0f, 140.0f, false, this.line);
                    break;
                case 8:
                    float f12 = f3 * 0.1f;
                    canvas.drawArc(f - f12, f2 - (0.01f * f4), f + f12, f2 + (f4 * 0.1f), 190.0f, 160.0f, false, this.line);
                    break;
                case 9:
                    float f13 = f3 * 0.055f;
                    canvas.drawOval(f - f13, f2 - (0.045f * f4), f + f13, f2 + (f4 * 0.12f), this.line);
                    break;
                case 10:
                    float f14 = f3 * 0.1f;
                    canvas.drawArc(f - f14, f2 - (0.02f * f4), f + f14, f2 + (f4 * 0.12f), 200.0f, 140.0f, false, this.line);
                    break;
                case 11:
                    float f15 = f3 * 0.09f;
                    canvas.drawOval(f - f15, f2 - (0.03f * f4), f + f15, f2 + (f4 * 0.09f), this.line);
                    break;
                case 12:
                    this.path.reset();
                    this.path.moveTo(f + (0.01f * f3), f2 - (0.04f * f4));
                    this.path.quadTo(f + (0.13f * f3), f2 + (f4 * 0.02f), f + (f3 * 0.02f), f2 + (f4 * 0.08f));
                    canvas.drawPath(this.path, this.line);
                    break;
                case 13:
                    float f16 = 0.09f * f3;
                    float f17 = f5 * f4;
                    float f18 = f2 + (0.025f * f17);
                    canvas.drawLine(f - f16, f18, f + f16, f18, this.line);
                    float f19 = f3 * 0.055f;
                    float f20 = f2 + (0.035f * f4) + (f17 * 0.03f);
                    canvas.drawLine(f - f19, f20, f + f19, f20, this.line);
                    break;
                case 14:
                    float f21 = f3 * 0.12f;
                    canvas.drawArc(f - f21, f2 - (0.01f * f4), f + f21, f2 + (0.13f * f4), 200.0f, 140.0f, false, this.line);
                    break;
            }
        }

        private void drawCheeks(Canvas canvas, float f, float f2, float f3, float f4, int i) {
            if (i == 8 || i == 10 || i == 14) {
                float f5 = f2 + (0.59f * f4);
                for (int i2 = 0; i2 < 3; i2++) {
                    float f6 = i2;
                    float f7 = 0.035f * f6;
                    float f8 = (0.24f - f7) * f3;
                    float f9 = f5 + f6;
                    float f10 = f3 * (0.2f - f7);
                    float f11 = (f5 - (0.025f * f4)) + f6;
                    canvas.drawLine(f - f8, f9, f - f10, f11, this.line);
                    canvas.drawLine(f + f10, f11, f + f8, f9, this.line);
                }
            }
            if (i == 14) {
                float f12 = 0.22f * f3;
                float f13 = f2 + (0.54f * f4);
                float f14 = f3 * 0.24f;
                float f15 = f2 + (0.68f * f4);
                canvas.drawLine(f - f12, f13, f - f14, f15, this.line);
                canvas.drawLine(f + f12, f13, f + f14, f15, this.line);
            }
        }

        private void drawMoodMarks(Canvas canvas, float f, float f2, float f3, float f4, int i, float f5, float f6) {
            if (this.mode == 1) {
                canvas.drawCircle((0.34f * f3) + f, (0.24f * f4) + f2, (2.0f * f5) + 2.2f, this.line);
            }
            if (this.mode == 2) {
                canvas.drawArc((0.26f * f3) + f, (0.2f * f4) + f2, (0.49f * f3) + f, (0.43f * f4) + f2, -40.0f, 80.0f + (40.0f * f6), false, this.line);
            }
            if (i == 11) {
                float f7 = f3 * 0.4f;
                float f8 = f2 + (0.12f * f4);
                float f9 = f3 * 0.35f;
                float f10 = f2 + (0.19f * f4);
                canvas.drawLine(f - f7, f8, f - f9, f10, this.line);
                canvas.drawLine(f + f7, f8, f + f9, f10, this.line);
                float f11 = f3 * 0.44f;
                float f12 = f2 + (0.22f * f4);
                float f13 = f3 * 0.37f;
                float f14 = f2 + (f4 * 0.25f);
                canvas.drawLine(f - f11, f12, f - f13, f14, this.line);
                canvas.drawLine(f + f11, f12, f + f13, f14, this.line);
            }
            if (i == 12) {
                canvas.drawCircle((0.31f * f3) + f, (0.33f * f4) + f2, 2.2f, this.line);
                canvas.drawCircle((0.38f * f3) + f, (0.25f * f4) + f2, 1.7f, this.line);
            }
            if (i == 14) {
                float f15 = 0.23f * f3;
                float f16 = (0.66f * f4) + f2;
                float f17 = 0.17f * f3;
                float f18 = (0.78f * f4) + f2;
                canvas.drawOval(f - f15, f16, f - f17, f18, this.line);
                canvas.drawOval(f17 + f, f16, f15 + f, f18, this.line);
            }
        }
    }

    private static final class VoiceResult {
        final String answer;
        final String transcript;

        VoiceResult(String str, String str2) {
            this.transcript = str == null ? "" : str;
            this.answer = str2 == null ? "" : str2;
        }
    }

    private static final class PhoneSttResponseException extends Exception {
        PhoneSttResponseException(String message) {
            super(message);
        }
    }

    private static final class GeminiHttpException extends Exception {
        private final int code;

        GeminiHttpException(int i, String str, String str2) {
            super(formatMessage(i, str, str2));
            this.code = i;
        }

        boolean isRetryable() {
            return this.code == 429 || this.code == 500 || this.code == 502 || this.code == 503 || this.code == 504;
        }

        boolean isQuotaLimited() {
            return this.code == 429;
        }

        boolean isServiceUnavailable() {
            return this.code == 503;
        }

        long cooldownMs() {
            if (this.code == 429) {
                return 240000L;
            }
            if (this.code == 503) {
                return 20000L;
            }
            return 15000L;
        }

        private static String formatMessage(int i, String str, String str2) {
            if (i == 503) {
                return "Gemini API 503: Geminiが混雑しています。少し待ってからもう一度送ってください。使用モデル: " + str;
            }
            if (i == 429) {
                return "Gemini API 429: 利用回数または利用枠の制限に当たっています。少し待ってから再試行してください。使用モデル: " + str;
            }
            if (i == 401 || i == 403) {
                return "Gemini API " + i + ": APIキー、権限、または請求設定を確認してください。使用モデル: " + str;
            }
            return "Gemini API " + i + " (" + str + "): " + str2;
        }
    }
}
