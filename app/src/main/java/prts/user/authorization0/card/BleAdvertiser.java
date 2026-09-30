package prts.user.authorization0.card;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

// BleAdvertiser.java
// BLE 广播：主包只放带节点码的 128-bit UUID，SCAN_RSP 包放本机蓝牙名
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleAdvertiser {

    private static final String TAG = "RICard";

    private BluetoothLeAdvertiser advertiser;

    private final AdvertiseCallback callback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "BLE advertise started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "BLE advertise failed: " + errorCode);
        }
    };

    public boolean start(BluetoothAdapter adapter, String nodeCode) {
        stop();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || adapter == null) {
            return false;
        }
        try {
            if (!adapter.isMultipleAdvertisementSupported()) {
                Log.w(TAG, "BLE advertising not supported on this device");
                return false;
            }
            advertiser = adapter.getBluetoothLeAdvertiser();
        } catch (SecurityException e) {
            Log.w(TAG, "BLE advertising denied: missing bluetooth permission");
            return false;
        }
        if (advertiser == null) {
            return false;
        }

        AdvertiseData adv = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(NodeCode.buildUuid(nodeCode)))
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build();
        AdvertiseData rsp = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                // 必须可连接：同伴要用 GATT 连上来交换名片
                .setConnectable(true)
                .setTimeout(0)
                .build();
        try {
            advertiser.startAdvertising(settings, adv, rsp, callback);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "startAdvertising exception", e);
            return false;
        }
    }

    public void stop() {
        if (advertiser != null) {
            try {
                advertiser.stopAdvertising(callback);
            } catch (Exception ignored) {
            }
            advertiser = null;
        }
    }
}
