package prts.user.authorization0.card;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Collections;
import java.util.List;

// BleScanner.java
// BLE 扫描：用掩码 UUID 过滤出同伴节点，并做一次软件二次校验
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleScanner {

    public interface Listener {
        void onPeer(BluetoothDevice device, String peerName, String peerCode, int rssi);
    }

    private static final String TAG = "RICard";

    private BluetoothLeScanner scanner;
    private Listener listener;

    private final ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handle(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (results == null) {
                return;
            }
            for (ScanResult r : results) {
                handle(r);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "BLE scan failed: " + errorCode);
        }
    };

    private void handle(ScanResult result) {
        if (result == null || listener == null) {
            return;
        }
        ScanRecord rec = result.getScanRecord();
        if (rec == null || rec.getServiceUuids() == null) {
            return;
        }
        String peerCode = null;
        for (ParcelUuid pu : rec.getServiceUuids()) {
            peerCode = NodeCode.extractCode(pu.getUuid());
            if (peerCode != null) {
                break;
            }
        }
        if (peerCode == null) {
            return;
        }
        String peerName = rec.getDeviceName();
        if (peerName == null || peerName.isEmpty()) {
            peerName = "RI_PRTS_" + peerCode;
        }
        listener.onPeer(result.getDevice(), peerName, peerCode, result.getRssi());
    }

    public boolean start(BluetoothAdapter adapter, Listener l) {
        stop();
        this.listener = l;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || adapter == null) {
            return false;
        }
        try {
            scanner = adapter.getBluetoothLeScanner();
        } catch (SecurityException e) {
            Log.w(TAG, "BLE scanning denied: missing bluetooth permission");
            return false;
        }
        if (scanner == null) {
            Log.w(TAG, "BLE scanner unavailable");
            return false;
        }
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(NodeCode.BASE_UUID), new ParcelUuid(NodeCode.MASK_UUID))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
        try {
            scanner.startScan(Collections.singletonList(filter), settings, callback);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "startScan exception", e);
            return false;
        }
    }

    public void stop() {
        if (scanner != null) {
            try {
                scanner.stopScan(callback);
            } catch (Exception ignored) {
            }
            scanner = null;
        }
        listener = null;
    }
}
