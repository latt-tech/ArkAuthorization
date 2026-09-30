package prts.user.authorization0.card;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

// CardGattClient.java
// 中心侧：连上对端的 GATT 服务，写入自己的名片，再读回对方的名片
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CardGattClient {

    private static final String TAG = "RICard";
    private static final long TOTAL_TIMEOUT_MS = 20000L;
    // 等服务发现完成后再发起 MTU 协商；若协商迟迟不回，用这个兜底直接写
    private static final long MTU_FALLBACK_MS = 1500L;

    public interface Listener {
        void onPeerCard(JSONObject card);

        // 无论成功失败都会回调一次，用于收尾清理
        void onFinished(boolean success);
    }

    private final Context appContext;
    private final Handler handler;
    private final byte[] payload;
    private final Listener listener;

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic characteristic;
    private boolean finished;
    private boolean servicesReady;
    private boolean writeStarted;

    private final Runnable timeout = new Runnable() {
        @Override
        public void run() {
            Log.w(TAG, "GATT client timeout");
            finish(false);
        }
    };

    private final Runnable mtuFallback = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "MTU not negotiated, write with default MTU");
            writePayload();
        }
    };

    public CardGattClient(Context c, Handler handler, byte[] payload, Listener l) {
        this.appContext = c.getApplicationContext();
        this.handler = handler;
        this.payload = payload;
        this.listener = l;
    }

    public void start(BluetoothDevice device) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                gatt = device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE);
            } else {
                gatt = device.connectGatt(appContext, false, callback);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "connectGatt denied: missing bluetooth permission");
            finish(false);
            return;
        } catch (Exception e) {
            Log.w(TAG, "connectGatt failed", e);
            finish(false);
            return;
        }
        if (gatt == null) {
            Log.w(TAG, "connectGatt returned null");
            finish(false);
            return;
        }
        handler.postDelayed(timeout, TOTAL_TIMEOUT_MS);
    }

    public void cancel() {
        finish(false);
    }

    private void finish(boolean success) {
        if (finished) {
            return;
        }
        finished = true;
        handler.removeCallbacks(timeout);
        handler.removeCallbacks(mtuFallback);
        BluetoothGatt g = gatt;
        gatt = null;
        if (g != null) {
            try {
                g.disconnect();
            } catch (Exception ignored) {
            }
            try {
                g.close();
            } catch (Exception ignored) {
            }
        }
        listener.onFinished(success);
    }

    // ---------------- 流程 ----------------

    private void discover() {
        BluetoothGatt g = gatt;
        if (g == null) {
            return;
        }
        try {
            g.discoverServices();
        } catch (Exception e) {
            Log.w(TAG, "discoverServices failed", e);
            finish(false);
        }
    }

    private void writePayload() {
        if (writeStarted || characteristic == null) {
            return;
        }
        writeStarted = true;
        BluetoothGatt g = gatt;
        if (g == null) {
            return;
        }
        try {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(payload);
            if (!g.writeCharacteristic(characteristic)) {
                Log.w(TAG, "writeCharacteristic rejected");
                finish(false);
            }
        } catch (Exception e) {
            Log.w(TAG, "writeCharacteristic failed", e);
            finish(false);
        }
    }

    private void readPayload() {
        BluetoothGatt g = gatt;
        if (g == null) {
            return;
        }
        try {
            if (!g.readCharacteristic(characteristic)) {
                Log.w(TAG, "readCharacteristic rejected");
                finish(false);
            }
        } catch (Exception e) {
            Log.w(TAG, "readCharacteristic failed", e);
            finish(false);
        }
    }

    private void onCardBytes(byte[] value) {
        JSONObject card = CardProtocol.decode(value);
        if (card == null) {
            Log.w(TAG, "peer card undecodable");
            finish(false);
            return;
        }
        listener.onPeerCard(card);
        finish(true);
    }

    // ---------------- 回调 ----------------

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            Log.i(TAG, "GATT conn state status=" + status + " newState=" + newState);
            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                // 先做服务发现，MTU 协商留到发现完成之后，避免两个操作互相阻塞
                discover();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                finish(false);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "service discovery status=" + status);
                finish(false);
                return;
            }
            BluetoothGattService svc = g.getService(CardExchangeEngine.CARD_XFER_UUID);
            characteristic = (svc == null) ? null : svc.getCharacteristic(CardExchangeEngine.CARD_CHAR_UUID);
            if (characteristic == null) {
                Log.w(TAG, "card characteristic not found");
                finish(false);
                return;
            }
            servicesReady = true;
            // 服务发现完成后才发起 MTU 协商；协商失败或不回则靠兜底定时器直接写
            try {
                if (!g.requestMtu(517)) {
                    Log.i(TAG, "requestMtu rejected, write with default MTU");
                    writePayload();
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "requestMtu failed, write with default MTU", e);
                writePayload();
                return;
            }
            handler.postDelayed(mtuFallback, MTU_FALLBACK_MS);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic ch, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "write status=" + status);
                finish(false);
                return;
            }
            readPayload();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic ch, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "read status=" + status);
                finish(false);
                return;
            }
            onCardBytes(ch.getValue());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic ch,
                                         byte[] value, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "read status=" + status);
                finish(false);
                return;
            }
            onCardBytes(value);
        }

        @Override
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            Log.i(TAG, "MTU=" + mtu + " status=" + status);
            handler.removeCallbacks(mtuFallback);
            if (servicesReady) {
                writePayload();
            }
        }
    };
}
