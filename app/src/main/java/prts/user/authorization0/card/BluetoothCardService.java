package prts.user.authorization0.card;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

// BluetoothCardService.java
// 前台服务：每 5 分钟开启一个 3 分钟的交换窗口
public class BluetoothCardService extends Service {

    private static final String TAG = "RICard";
    private static final long PERIOD_MS = 5 * 60 * 1000L;
    private static final long WINDOW_MS = 3 * 60 * 1000L;
    private static final int NOTIF_ID = 0x5249;
    private static final String CHANNEL_ID = "ri_card";

    private HandlerThread ht;
    private Handler h;
    private CardExchangeEngine engine;
    private PowerManager.WakeLock wakeLock;
    private boolean windowScheduled;

    private final Runnable startWindow = new Runnable() {
        @Override
        public void run() {
            if (!CardPrefs.isEnabled(BluetoothCardService.this)) {
                stopSelf();
                return;
            }
            Log.i(TAG, "--- exchange window begin ---");
            acquireWakeLock(WINDOW_MS + 15000L);
            engine.startWindow();
            h.postDelayed(stopWindow, WINDOW_MS);
        }
    };

    private final Runnable stopWindow = new Runnable() {
        @Override
        public void run() {
            engine.stopWindow();
            releaseWakeLock();
            Log.i(TAG, "--- exchange window end ---");
            h.postDelayed(startWindow, PERIOD_MS - WINDOW_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ht = new HandlerThread("ri-card");
        ht.start();
        h = new Handler(ht.getLooper());
        engine = new CardExchangeEngine(this, h);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundNotification();
        if (!CardPrefs.isEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        h.post(new Runnable() {
            @Override
            public void run() {
                engine.prepareName();
                if (!windowScheduled) {
                    windowScheduled = true;
                    h.removeCallbacks(startWindow);
                    h.removeCallbacks(stopWindow);
                    h.post(startWindow);
                }
            }
        });
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (h != null) {
            h.removeCallbacksAndMessages(null);
        }
        if (engine != null) {
            engine.stopWindow();
            engine.restoreOriginalName();
        }
        releaseWakeLock();
        windowScheduled = false;
        if (ht != null) {
            ht.quitSafely();
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------------- 前后台与唤醒 ----------------

    private void startForegroundNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "蓝牙名片交换", NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(ch);
            }
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        b.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("蓝牙名片交换")
                .setContentText("正在后台寻找附近的同伴")
                .setOngoing(true);
        startForeground(NOTIF_ID, b.build());
    }

    private void acquireWakeLock(long timeoutMs) {
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm == null) {
                return;
            }
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RI:CardExchange");
                wakeLock.setReferenceCounted(false);
            }
            wakeLock.acquire(timeoutMs);
        } catch (Exception e) {
            Log.w(TAG, "acquireWakeLock failed", e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {
        }
    }
}
