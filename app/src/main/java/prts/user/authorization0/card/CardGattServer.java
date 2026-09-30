package prts.user.authorization0.card;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// CardGattServer.java
// 外设侧：暴露一个 GATT 特征值，对端写入它的名片，再读走我们的名片。
// 之所以不用经典 RFCOMM：BLE 广播/扫描拿到的是 LE 地址（往往是随机地址），
// 无法用来建立经典蓝牙连接。
public class CardGattServer {

    private static final String TAG = "RICard";

    public interface Listener {
        // 我方名片字节；返回 null 表示当前不可用
        byte[] localCard();

        void onPeerCard(BluetoothDevice device, JSONObject card);
    }

    private final Context appContext;
    private final Listener listener;

    // 长写（prepare write）时的分片累积缓冲
    private final Map<String, byte[]> pending = new ConcurrentHashMap<String, byte[]>();

    private BluetoothGattServer server;

    public CardGattServer(Context c, Listener l) {
        this.appContext = c.getApplicationContext();
        this.listener = l;
    }

    public boolean start() {
        stop();
        try {
            BluetoothManager bm =
                    (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bm == null) {
                return false;
            }
            BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(
                    CardExchangeEngine.CARD_CHAR_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ
                            | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ
                            | BluetoothGattCharacteristic.PERMISSION_WRITE);
            BluetoothGattService service = new BluetoothGattService(
                    CardExchangeEngine.CARD_XFER_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            service.addCharacteristic(ch);
            server = bm.openGattServer(appContext, callback);
            if (server == null) {
                Log.w(TAG, "openGattServer returned null");
                return false;
            }
            boolean ok = server.addService(service);
            Log.i(TAG, "GATT server " + (ok ? "listening" : "addService failed"));
            return ok;
        } catch (SecurityException e) {
            Log.w(TAG, "GATT server denied: missing bluetooth permission");
            return false;
        } catch (Exception e) {
            Log.w(TAG, "GATT server start failed", e);
            return false;
        }
    }

    public void stop() {
        pending.clear();
        BluetoothGattServer s = server;
        server = null;
        if (s != null) {
            try {
                s.clearServices();
            } catch (Exception ignored) {
            }
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }

    // ---------------- 回调 ----------------

    private final BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattCharacteristic ch) {
            byte[] card = listener.localCard();
            if (card == null) {
                respond(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                return;
            }
            if (offset < 0 || offset > card.length) {
                respond(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null);
                return;
            }
            respond(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    Arrays.copyOfRange(card, offset, card.length));
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic ch, boolean preparedWrite,
                                                 boolean responseNeeded, int offset, byte[] value) {
            if (responseNeeded) {
                respond(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            if (device == null || value == null) {
                return;
            }
            if (preparedWrite) {
                accumulate(address(device), offset, value);
                return;
            }
            // 单次写：offset 非 0 时拼到已有分片后面
            byte[] body = (offset == 0) ? value : concat(pending.get(address(device)), offset, value);
            handleCard(device, body);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            respond(device, requestId, execute ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE,
                    0, null);
            if (device == null) {
                return;
            }
            byte[] body = pending.remove(address(device));
            if (execute) {
                handleCard(device, body);
            }
        }
    };

    private void handleCard(BluetoothDevice device, byte[] body) {
        JSONObject card = CardProtocol.decode(body);
        if (card == null) {
            Log.w(TAG, "GATT write rejected: undecodable payload");
            return;
        }
        listener.onPeerCard(device, card);
    }

    private void accumulate(String addr, int offset, byte[] value) {
        pending.put(addr, concat(pending.get(addr), offset, value));
    }

    private static byte[] concat(byte[] base, int offset, byte[] value) {
        int len = Math.max(offset, base == null ? 0 : base.length) + value.length;
        byte[] out = new byte[len];
        if (base != null) {
            System.arraycopy(base, 0, out, 0, base.length);
        }
        System.arraycopy(value, 0, out, offset, value.length);
        return out;
    }

    private void respond(BluetoothDevice device, int requestId, int status, int offset, byte[] value) {
        BluetoothGattServer s = server;
        if (s == null || device == null) {
            return;
        }
        try {
            s.sendResponse(device, requestId, status, offset, value);
        } catch (Exception e) {
            Log.w(TAG, "sendResponse failed", e);
        }
    }

    private static String address(BluetoothDevice d) {
        try {
            return d.getAddress();
        } catch (Exception e) {
            return "?";
        }
    }
}
