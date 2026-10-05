package com.example.rokidzoomcamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CaptureFailure;
import android.util.Log;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int CAMERA_PERMISSION_REQUEST = 10;
    private static final float[] CAMERA_ROTATION_STEPS = {0f, 90f, 180f, 270f};
    private static final float PHONE_EXPORT_ROTATION_DEGREES = -90f;
    private static final float PREVIEW_SIZE_FRACTION = 0.50f;
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;
    private static final int VIDEO_BIT_RATE = 8_000_000;
    private static final int VIDEO_FRAME_RATE = 30;
    private static final long PREVIEW_STALL_CHECK_MS = 2500L;
    private static final long PREVIEW_SLEEP_TIMEOUT_MS = 15_000L;
    private static final float SLEEP_PREVIEW_ALPHA = 0.12f;
    private static final float[] ZOOM_STEPS = {1.0f, 1.5f, 2.0f, 3.0f, 4.0f};
    private static final String ACTION_SPRITE_BUTTON_UP = "com.android.action.ACTION_SPRITE_BUTTON_UP";
    private static final String ACTION_SPRITE_BUTTON_DOWN = "com.android.action.ACTION_SPRITE_BUTTON_DOWN";
    private static final String ACTION_SPRITE_BUTTON_LONG_PRESS = "com.android.action.ACTION_SPRITE_BUTTON_LONG_PRESS";
    private static final String ACTION_SPRITE_BUTTON_VERY_LONG_PRESS =
            "com.android.action.ACTION_SPRITE_BUTTON_VERY_VERY_LONG_PRESS";
    private static final String ACTION_SETTINGS_KEY = "com.android.action.ACTION_SETTINGS_KEY";

    private TextureView previewView;
    private OverlayView overlayView;
    private TextView recordingIndicator;
    private TextView statusHud;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader imageReader;
    private Rect sensorRect;
    private float maxZoom = 1.0f;
    private int zoomIndex = 0;
    private boolean arCapture = false;
    private int rotationIndex = 0;
    private float touchDownX;
    private long lastSwipeAt;
    private boolean pendingCaptureWhenReady;
    private BroadcastReceiver hardwareButtonReceiver;
    private BroadcastReceiver batteryReceiver;
    private long lastRokidButtonCaptureAt;
    private long lastRokidLongPressAt;
    private long lastVideoToggleAt;
    private boolean officialShortPressClaimed;
    private boolean surfaceListenerRegistered;
    private boolean activityActive;
    private boolean openingCamera;
    private long lastPreviewFrameAt;
    private MediaRecorder mediaRecorder;
    private ParcelFileDescriptor videoFileDescriptor;
    private Uri pendingVideoUri;
    private String pendingVideoName;
    private boolean recordingVideo;
    private int batteryPercent = -1;
    private boolean batteryCharging;
    private boolean previewSleeping;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable previewSleepRunnable = this::enterPreviewSleep;
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatusHud();
            long nextMinute = 60_000L - (System.currentTimeMillis() % 60_000L);
            uiHandler.postDelayed(this, nextMinute);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        registerBatteryStatus();
        registerHardwareButtonHooks();
        activityActive = true;
        handleCameraIntent(getIntent());
        activityActive = true;

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startWhenReady();
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff000000);
        FrameLayout previewContainer = new FrameLayout(this);
        previewView = new TextureView(this);
        overlayView = new OverlayView(this);
        FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().widthPixels * PREVIEW_SIZE_FRACTION),
                (int) (getResources().getDisplayMetrics().heightPixels * PREVIEW_SIZE_FRACTION),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        previewContainer.addView(previewView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        statusHud = new TextView(this);
        statusHud.setTextColor(0xfff2f7f8);
        statusHud.setTextSize(12f);
        statusHud.setGravity(Gravity.CENTER);
        statusHud.setPadding(8, 4, 8, 4);
        statusHud.setSingleLine(true);
        statusHud.setBackgroundColor(0x99000000);
        previewContainer.addView(statusHud, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        root.addView(previewContainer, previewParams);
        recordingIndicator = new TextView(this);
        recordingIndicator.setText(R.string.recording_indicator);
        recordingIndicator.setTextColor(0xffff3030);
        recordingIndicator.setTextSize(18f);
        recordingIndicator.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        recordingIndicator.setPadding(12, 6, 12, 6);
        recordingIndicator.setBackgroundColor(0x66000000);
        recordingIndicator.setVisibility(android.view.View.GONE);
        FrameLayout.LayoutParams recParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        recParams.setMarginStart(24);
        recParams.topMargin = 24;
        root.addView(recordingIndicator, recParams);
        setContentView(root);

        updateOverlayStatus();
        updateStatusHud();
    }

    private void registerBatteryStatus() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryStatus(intent);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent currentBattery;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentBattery = registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            currentBattery = registerReceiver(batteryReceiver, filter);
        }
        updateBatteryStatus(currentBattery);
    }

    private void updateBatteryStatus(Intent batteryIntent) {
        if (batteryIntent == null) {
            return;
        }
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        batteryPercent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
        int status = batteryIntent.getIntExtra(
                BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        batteryCharging = plugged != 0
                || status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        updateStatusHud();
    }

    private void updateStatusHud() {
        if (statusHud == null) {
            return;
        }
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String battery = batteryPercent >= 0 ? batteryPercent + "%" : "--%";
        String charging = getString(batteryCharging
                ? R.string.charging_on
                : R.string.charging_off);
        String sleep = previewSleeping ? getString(R.string.sleep_indicator) : "";
        statusHud.setText(getString(R.string.status_hud, time, battery, charging, sleep));
    }

    private void startIdleTracking() {
        uiHandler.removeCallbacks(clockRunnable);
        uiHandler.post(clockRunnable);
        noteUserActivity();
    }

    private void stopIdleTracking() {
        uiHandler.removeCallbacks(clockRunnable);
        uiHandler.removeCallbacks(previewSleepRunnable);
    }

    private void noteUserActivity() {
        if (!activityActive || previewView == null) {
            return;
        }
        uiHandler.removeCallbacks(previewSleepRunnable);
        if (previewSleeping || previewView.getAlpha() != 1f) {
            previewSleeping = false;
            previewView.setAlpha(1f);
            updateStatusHud();
        }
        if (!recordingVideo) {
            uiHandler.postDelayed(previewSleepRunnable, PREVIEW_SLEEP_TIMEOUT_MS);
        }
    }

    private void enterPreviewSleep() {
        if (!activityActive || recordingVideo || previewView == null) {
            return;
        }
        previewSleeping = true;
        previewView.setAlpha(SLEEP_PREVIEW_ALPHA);
        updateStatusHud();
    }

    // Rokid hardware events come from another process; pre-33 APIs have no exported flag.
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerHardwareButtonHooks() {
        hardwareButtonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                noteUserActivity();
                overlayView.log("broadcast " + action);
                KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null) {
                    handleHardwareKey(keyEvent, "broadcast");
                } else if (ACTION_SPRITE_BUTTON_UP.equals(action)) {
                    handleRokidButtonUp();
                } else if (ACTION_SPRITE_BUTTON_LONG_PRESS.equals(action)
                        || ACTION_SPRITE_BUTTON_VERY_LONG_PRESS.equals(action)) {
                    handleRokidButtonLongPress();
                } else if (RokidButtonAccessibilityService.ACTION_HARDWARE_CAPTURE.equals(action)) {
                    overlayView.log("accessibility shoot "
                            + intent.getStringExtra("keyName")
                            + " "
                            + intent.getIntExtra("keyCode", -1));
                    capturePhoto();
                } else if (Intent.ACTION_CAMERA_BUTTON.equals(action)) {
                    capturePhoto();
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CAMERA_BUTTON);
        filter.addAction(RokidButtonAccessibilityService.ACTION_HARDWARE_CAPTURE);
        filter.addAction(ACTION_SPRITE_BUTTON_DOWN);
        filter.addAction(ACTION_SPRITE_BUTTON_UP);
        filter.addAction(ACTION_SPRITE_BUTTON_LONG_PRESS);
        filter.addAction(ACTION_SPRITE_BUTTON_VERY_LONG_PRESS);
        filter.addAction(ACTION_SETTINGS_KEY);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hardwareButtonReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(hardwareButtonReceiver, filter);
        }
    }

    private void handleRokidButtonUp() {
        long now = System.currentTimeMillis();
        if (now - lastRokidLongPressAt < 1500L) {
            overlayView.log("rokid button up ignored after long press");
            return;
        }
        if (now - lastRokidButtonCaptureAt < 700L) {
            overlayView.log("rokid button ignored: debounce");
            return;
        }
        lastRokidButtonCaptureAt = now;
        if (recordingVideo) {
            overlayView.log("rokid button stop video");
            stopVideoRecording(true);
            return;
        }
        overlayView.log("rokid button shoot");
        capturePhoto();
    }

    private void handleRokidButtonLongPress() {
        long now = System.currentTimeMillis();
        lastRokidLongPressAt = now;
        if (now - lastVideoToggleAt < 1000L) {
            overlayView.log("video toggle ignored: debounce");
            return;
        }
        lastVideoToggleAt = now;
        if (recordingVideo) {
            stopVideoRecording(true);
        } else {
            startVideoRecording();
        }
    }

    private void claimOfficialShortPress() {
        officialShortPressClaimed = true;
        Intent guard = new Intent(this, RokidButtonRestoreService.class);
        guard.setAction(RokidButtonRestoreService.ACTION_START_GUARD);
        try {
            startService(guard);
        } catch (RuntimeException e) {
            overlayView.log("restore guard unavailable " + e.getClass().getSimpleName());
        }
        sendRokidSettingChange(RokidOfficialButtonControl.VALUE_OFF, "claim official shoot");
        previewView.postDelayed(() -> sendRokidSettingChange(
                RokidOfficialButtonControl.VALUE_OFF, "claim official shoot retry"), 500L);
    }

    private void restoreOfficialShortPress() {
        if (!officialShortPressClaimed) {
            sendRokidSettingChange(RokidOfficialButtonControl.VALUE_PICTURE, "restore official shoot forced");
            return;
        }
        officialShortPressClaimed = false;
        sendRokidSettingChange(RokidOfficialButtonControl.VALUE_PICTURE, "restore official shoot");
        stopService(new Intent(this, RokidButtonRestoreService.class));
    }

    private void sendRokidSettingChange(String value, String reason) {
        RokidOfficialButtonControl.setShortPressFunction(this, value, reason);
        overlayView.log(reason + " " + value);
    }

    private void startWhenReady() {
        if (!surfaceListenerRegistered) {
            surfaceListenerRegistered = true;
            previewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    configurePreviewTransform(width, height);
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    configurePreviewTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    closeCameraResources();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    lastPreviewFrameAt = System.currentTimeMillis();
                }
            });
        }

        if (previewView.isAvailable()) {
            openCamera();
        }
    }

    private void configurePreviewTransform(int viewWidth, int viewHeight) {
        if (viewWidth == 0 || viewHeight == 0) {
            return;
        }
        Matrix matrix = new Matrix();
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;
        float rotationDegrees = getCameraRotationDegrees();
        matrix.postRotate(rotationDegrees, centerX, centerY);

        if (rotationDegrees == 90f || rotationDegrees == 270f) {
            float fillScale = Math.max((float) viewWidth / (float) viewHeight, (float) viewHeight / (float) viewWidth);
            matrix.postScale(fillScale, fillScale, centerX, centerY);
        }
        previewView.setTransform(matrix);
    }

    private void openCamera() {
        if (!activityActive || openingCamera || cameraDevice != null) {
            return;
        }
        openingCamera = true;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = chooseCamera(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            Float zoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
            maxZoom = zoom == null ? 1.0f : Math.max(1.0f, zoom);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size jpegSize = chooseJpegSize(map);
            imageReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(), ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(reader -> {
                try (Image image = reader.acquireNextImage()) {
                    if (image != null) {
                        try { saveImage(image); }
                        catch (RuntimeException error) { Log.e("RokidZoomCamera", "JPEG save pipeline failed", error); showError("Image save failed: " + error.getMessage()); }
                    }
                }
            }, null);

            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                openingCamera = false;
                return;
            }
            manager.openCamera(cameraId, cameraStateCallback, null);
        } catch (CameraAccessException e) {
            openingCamera = false;
            showError("Camera access failed: " + e.getMessage());
        } catch (RuntimeException e) {
            openingCamera = false;
            showError("Camera open failed: " + e.getMessage());
        }
    }

    private String chooseCamera(CameraManager manager) throws CameraAccessException {
        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics c = manager.getCameraCharacteristics(id);
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        String[] ids = manager.getCameraIdList();
        if (ids.length == 0) {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "No camera found");
        }
        return ids[0];
    }

    private Size chooseJpegSize(StreamConfigurationMap map) {
        if (map == null) {
            return new Size(1280, 720);
        }
        Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
        if (sizes == null || sizes.length == 0) {
            return new Size(1280, 720);
        }
        // Bound the JPEG and rotation buffers on the memory-constrained glasses.
        Size best = null, smallest = sizes[0];
        for (Size size : sizes) {
            long pixels = (long) size.getWidth() * size.getHeight();
            if (pixels < (long) smallest.getWidth() * smallest.getHeight()) smallest = size;
            if (pixels <= 1920L * 1440L && (best == null || pixels > (long) best.getWidth() * best.getHeight())) best = size;
        }
        if (best == null) best = smallest;
        Log.i("RokidZoomCamera", "JPEG size selected " + best.getWidth() + "x" + best.getHeight());
        return best;
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            openingCamera = false;
            cameraDevice = camera;
            createPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            openingCamera = false;
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            openingCamera = false;
            camera.close();
            cameraDevice = null;
            showError("Camera error: " + error);
        }
    };

    private void createPreviewSession() {
        try {
            SurfaceTexture texture = previewView.getSurfaceTexture();
            if (texture == null || cameraDevice == null) {
                return;
            }
            texture.setDefaultBufferSize(1280, 720);
            Surface previewSurface = new Surface(texture);
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);
            applyZoom(previewRequestBuilder);
            cameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            captureSession = session;
                            lastPreviewFrameAt = System.currentTimeMillis();
                            updatePreview();
                            schedulePreviewStallCheck();
                            if (pendingCaptureWhenReady) {
                                pendingCaptureWhenReady = false;
                                previewView.postDelayed(() -> capturePhoto(), 500L);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            showError("Preview session failed");
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            showError("Preview failed: " + e.getMessage());
        }
    }

    private void schedulePreviewStallCheck() {
        previewView.postDelayed(() -> {
            if (!activityActive || recordingVideo || cameraDevice == null || captureSession == null) {
                return;
            }
            long age = System.currentTimeMillis() - lastPreviewFrameAt;
            if (age >= PREVIEW_STALL_CHECK_MS) {
                overlayView.log("preview stalled, restarting camera");
                closeCameraResources();
                previewView.postDelayed(() -> {
                    if (activityActive) {
                        openCamera();
                    }
                }, 350L);
            }
        }, PREVIEW_STALL_CHECK_MS);
    }

    private void updatePreview() {
        if (captureSession == null || previewRequestBuilder == null) {
            return;
        }
        try {
            applyZoom(previewRequestBuilder);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
            updateOverlayStatus();
        } catch (CameraAccessException e) {
            showError("Preview update failed: " + e.getMessage());
        }
    }

    private void applyZoom(CaptureRequest.Builder builder) {
        if (sensorRect == null) {
            return;
        }
        float requested = Math.min(ZOOM_STEPS[zoomIndex], maxZoom);
        int cropW = (int) (sensorRect.width() / requested);
        int cropH = (int) (sensorRect.height() / requested);
        int left = sensorRect.left + (sensorRect.width() - cropW) / 2;
        int top = sensorRect.top + (sensorRect.height() - cropH) / 2;
        builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(left, top, left + cropW, top + cropH));
    }

    private void capturePhoto() {
        Log.i("RokidZoomCamera", "capture trigger active=" + activityActive + " permission=" + checkSelfPermission(Manifest.permission.CAMERA) + " ready=" + (cameraDevice != null && captureSession != null && imageReader != null));
        if (!activityActive) {
            pendingCaptureWhenReady = true;
            return;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingCaptureWhenReady = true;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }
        noteUserActivity();
        if (recordingVideo) {
            stopVideoRecording(true);
            return;
        }
        if (cameraDevice == null || captureSession == null || imageReader == null) {
            overlayView.log("capture ignored: camera not ready");
            pendingCaptureWhenReady = true;
            return;
        }
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            applyZoom(captureBuilder);
            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    overlayView.log("photo captured");
                    Log.i("RokidZoomCamera", "Camera2 capture completed frame=" + result.getFrameNumber());
                }
                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    Log.e("RokidZoomCamera", "Camera2 capture failed reason=" + failure.getReason());
                    showError("Capture failed: " + failure.getReason());
                }
            }, null);
        } catch (CameraAccessException | RuntimeException e) {
            Log.e("RokidZoomCamera", "capture submission failed", e);
            showError("Capture failed: " + e.getMessage());
        }
    }

    private void startVideoRecording() {
        noteUserActivity();
        if (cameraDevice == null) {
            overlayView.log("video ignored: camera not ready");
            return;
        }
        SurfaceTexture texture = previewView.getSurfaceTexture();
        if (texture == null) {
            overlayView.log("video ignored: preview not ready");
            return;
        }
        try {
            closeCaptureSession();
            prepareMediaRecorder();
            texture.setDefaultBufferSize(VIDEO_WIDTH, VIDEO_HEIGHT);
            Surface previewSurface = new Surface(texture);
            Surface recorderSurface = mediaRecorder.getSurface();
            CaptureRequest.Builder recordBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            recordBuilder.addTarget(previewSurface);
            recordBuilder.addTarget(recorderSurface);
            applyZoom(recordBuilder);
            recordBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            cameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, recorderSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            captureSession = session;
                            previewRequestBuilder = recordBuilder;
                            try {
                                captureSession.setRepeatingRequest(recordBuilder.build(), null, null);
                                mediaRecorder.start();
                                recordingVideo = true;
                                noteUserActivity();
                                updateRecordingIndicator();
                                overlayView.log("video started");
                            } catch (CameraAccessException | IllegalStateException e) {
                                showError("Video start failed: " + e.getMessage());
                                releaseMediaRecorder();
                                discardPendingVideo();
                                createPreviewSession();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            showError("Video session failed");
                            releaseMediaRecorder();
                            discardPendingVideo();
                            createPreviewSession();
                        }
                    },
                    null);
        } catch (CameraAccessException | IOException | RuntimeException e) {
            showError("Video setup failed: " + e.getMessage());
            releaseMediaRecorder();
            discardPendingVideo();
            createPreviewSession();
        }
    }

    private void stopVideoRecording(boolean restartPreview) {
        if (!recordingVideo && mediaRecorder == null) {
            return;
        }
        recordingVideo = false;
        noteUserActivity();
        updateRecordingIndicator();
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.abortCaptures();
            }
        } catch (CameraAccessException e) {
            overlayView.log("video stop session " + e.getMessage());
        }
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                overlayView.log("video stopped");
                publishVideo();
            }
        } catch (RuntimeException e) {
            overlayView.log("video stop failed " + e.getMessage());
            discardPendingVideo();
        } finally {
            releaseMediaRecorder();
            closeCaptureSession();
            if (restartPreview && cameraDevice != null) {
                createPreviewSession();
            }
        }
    }

    private void prepareMediaRecorder() throws IOException {
        pendingVideoName = "RokidZoom_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, pendingVideoName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera");
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
        }
        pendingVideoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (pendingVideoUri == null) {
            throw new IOException("Could not create video file");
        }
        videoFileDescriptor = getContentResolver().openFileDescriptor(pendingVideoUri, "w");
        if (videoFileDescriptor == null) {
            throw new IOException("Could not open video file");
        }
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoFileDescriptor.getFileDescriptor());
        mediaRecorder.setVideoEncodingBitRate(VIDEO_BIT_RATE);
        mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOrientationHint(getVideoOrientationHint());
        mediaRecorder.prepare();
    }

    private void publishVideo() {
        if (pendingVideoUri == null || pendingVideoName == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues published = new ContentValues();
            published.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(pendingVideoUri, published, null, null);
        }
        MediaScannerConnection.scanFile(
                this,
                new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        + "/Camera/" + pendingVideoName},
                new String[]{"video/mp4"},
                null);
        String savedName = pendingVideoName;
        runOnUiThread(() -> Toast.makeText(this, "Saved " + savedName, Toast.LENGTH_SHORT).show());
        pendingVideoUri = null;
        pendingVideoName = null;
    }

    private void discardPendingVideo() {
        if (pendingVideoUri != null) {
            getContentResolver().delete(pendingVideoUri, null, null);
        }
        pendingVideoUri = null;
        pendingVideoName = null;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (videoFileDescriptor != null) {
            try {
                videoFileDescriptor.close();
            } catch (IOException e) {
                overlayView.log("video fd close " + e.getMessage());
            }
            videoFileDescriptor = null;
        }
    }

    private void closeCaptureSession() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        previewRequestBuilder = null;
    }

    private void closeCameraResources() {
        closeCaptureSession();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        openingCamera = false;
        lastPreviewFrameAt = 0L;
    }

    private void updateRecordingIndicator() {
        if (recordingIndicator == null) {
            return;
        }
        runOnUiThread(() -> recordingIndicator.setVisibility(
                recordingVideo ? android.view.View.VISIBLE : android.view.View.GONE));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleCameraIntent(intent);
    }

    private void handleCameraIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action)
                || MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action)
                || MediaStore.ACTION_IMAGE_CAPTURE.equals(action)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(action)
                || Intent.ACTION_CAMERA_BUTTON.equals(action)) {
            noteUserActivity();
            overlayView.log("intent " + action);
            capturePhoto();
        }
    }

    private void saveImage(Image image) {
        Log.i("RokidZoomCamera", "JPEG received bytes=" + image.getPlanes()[0].getBuffer().remaining());
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        byte[] output = arCapture ? composeArImage(bytes) : rotateJpegLeft(bytes);

        String name = "RokidZoom_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera");
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            showError("Could not create image file");
            return;
        }
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("MediaStore output stream unavailable");
            if (out != null) {
                out.write(output);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues published = new ContentValues();
                published.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, published, null, null);
            }
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                            + "/Camera/" + name},
                    new String[]{"image/jpeg"},
                    null);
            Log.i("RokidZoomCamera", "JPEG saved uri=" + uri + " bytes=" + output.length);
            runOnUiThread(() -> Toast.makeText(this, "Saved " + name, Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    private Bitmap decodeCaptureBitmap(byte[] jpegBytes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length, bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inSampleSize = 1;
        while ((long)(bounds.outWidth / options.inSampleSize) * (bounds.outHeight / options.inSampleSize) > 1920L * 1440L) options.inSampleSize *= 2;
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length, options);
    }

    private byte[] composeArImage(byte[] jpegBytes) {
        Bitmap source = decodeCaptureBitmap(jpegBytes);
        if (source == null) {
            return jpegBytes;
        }
        Bitmap rotated = rotateBitmap(source);
        Bitmap mutable = rotated.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);
        overlayView.drawOverlay(canvas, mutable.getWidth(), mutable.getHeight(), false);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        mutable.compress(Bitmap.CompressFormat.JPEG, 92, out);
        source.recycle();
        if (rotated != source) {
            rotated.recycle();
        }
        mutable.recycle();
        return out.toByteArray();
    }

    private byte[] rotateJpegLeft(byte[] jpegBytes) {
        Bitmap source = decodeCaptureBitmap(jpegBytes);
        if (source == null) {
            return jpegBytes;
        }
        Bitmap rotated = rotateBitmap(source);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        rotated.compress(Bitmap.CompressFormat.JPEG, 92, out);
        source.recycle();
        if (rotated != source) {
            rotated.recycle();
        }
        return out.toByteArray();
    }

    private Bitmap rotateBitmap(Bitmap source) {
        float rotation = getSavedImageRotationDegrees();
        if (rotation == 0f) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void toggleArCapture() {
        noteUserActivity();
        arCapture = !arCapture;
        overlayView.log("mode " + (arCapture ? "AR" : "NORMAL"));
        updateOverlayStatus();
    }

    private void rotateCameraView() {
        noteUserActivity();
        rotationIndex = (rotationIndex + 1) % CAMERA_ROTATION_STEPS.length;
        overlayView.log("rotation " + (int) getCameraRotationDegrees());
        configurePreviewTransform(previewView.getWidth(), previewView.getHeight());
        updateOverlayStatus();
    }

    private void changeZoom(int delta, String source) {
        noteUserActivity();
        int oldIndex = zoomIndex;
        zoomIndex = Math.max(0, Math.min(ZOOM_STEPS.length - 1, zoomIndex + delta));
        while (zoomIndex > 0 && ZOOM_STEPS[zoomIndex] > maxZoom) {
            zoomIndex--;
        }
        if (oldIndex != zoomIndex) {
            overlayView.log(source + " zoom " + String.format(Locale.US, "%.1fx", ZOOM_STEPS[zoomIndex]));
            updatePreview();
        } else {
            overlayView.log(source + " zoom limit");
        }
    }

    private void updateOverlayStatus() {
        float zoom = Math.min(ZOOM_STEPS[zoomIndex], maxZoom);
        String mode = arCapture ? "APP AR" : "NORMAL";
        overlayView.setStatus(mode, zoom);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        noteUserActivity();
        overlayView.log("key " + event.getKeyCode() + " " + keyAction(event));
        if (handleHardwareKey(event, "key")) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean handleHardwareKey(KeyEvent event, String source) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_CAMERA
                || keyCode == KeyEvent.KEYCODE_STEM_PRIMARY) {
            overlayView.log(source + " shoot " + keyCode);
            capturePhoto();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            changeZoom(1, source + " " + keyCode);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            changeZoom(-1, source + " " + keyCode);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_M) {
            toggleArCapture();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_R) {
            rotateCameraView();
            return true;
        }
        return false;
    }

    private String keyAction(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return "down";
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            return "up";
        }
        return String.valueOf(event.getAction());
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityActive = true;
        getSharedPreferences(RokidButtonAccessibilityService.PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(RokidButtonAccessibilityService.PREF_APP_ACTIVE, true)
                .apply();
        claimOfficialShortPress();
        startIdleTracking();
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startWhenReady();
        }
    }

    @Override
    protected void onPause() {
        activityActive = false;
        stopIdleTracking();
        stopVideoRecording(false);
        closeCameraResources();
        restoreOfficialShortPress();
        getSharedPreferences(RokidButtonAccessibilityService.PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(RokidButtonAccessibilityService.PREF_APP_ACTIVE, false)
                .apply();
        super.onPause();
    }

    @Override
    protected void onStop() {
        restoreOfficialShortPress();
        super.onStop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            noteUserActivity();
        }
        handleSwipeMotion(event, "touch");
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        noteUserActivity();
        overlayView.log("motion action=" + event.getAction() + " source=" + event.getSource());
        handleSwipeMotion(event, "motion");
        return super.dispatchGenericMotionEvent(event);
    }

    private void handleSwipeMotion(MotionEvent event, String source) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownX = event.getX();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - touchDownX;
            if (Math.abs(dx) > 80f && System.currentTimeMillis() - lastSwipeAt > 250L) {
                lastSwipeAt = System.currentTimeMillis();
                changeZoom(dx > 0 ? 1 : -1, source + " swipe");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWhenReady();
        } else {
            showError("Camera permission is required");
        }
    }

    @Override
    protected void onDestroy() {
        activityActive = false;
        stopIdleTracking();
        stopVideoRecording(false);
        restoreOfficialShortPress();
        if (hardwareButtonReceiver != null) {
            unregisterReceiver(hardwareButtonReceiver);
            hardwareButtonReceiver = null;
        }
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
            batteryReceiver = null;
        }
        closeCameraResources();
        super.onDestroy();
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            overlayView.log(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private float getCameraRotationDegrees() {
        return CAMERA_ROTATION_STEPS[rotationIndex];
    }

    private float getSavedImageRotationDegrees() {
        float rotation = PHONE_EXPORT_ROTATION_DEGREES + getCameraRotationDegrees();
        while (rotation <= -180f) {
            rotation += 360f;
        }
        while (rotation > 180f) {
            rotation -= 360f;
        }
        return rotation;
    }

    private int getVideoOrientationHint() {
        int hint = Math.round(getSavedImageRotationDegrees());
        hint %= 360;
        if (hint < 0) {
            hint += 360;
        }
        return hint;
    }
}
