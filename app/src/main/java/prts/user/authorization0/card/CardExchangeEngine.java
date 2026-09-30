package prts.user.authorization0.card;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// CardExchangeEngine.java
// 一个 3 分钟窗口内：BLE 广播 + 扫描发现同伴，再用 BLE GATT 交换名片。
// 注意：BLE 扫描拿到的是 LE 地址（常常是随机地址），无法用于经典 RFCOMM 连接，
// 因此传输必须留在 BLE 域内。
public class CardExchangeEngine {

    private static final String TAG = "RICard";

    // 名片交换的 GATT 服务与特征值
    public static final UUID CARD_XFER_UUID =
            UUID.fromString("7a1c9e10-4b3d-4f2a-9c88-2e5b6d7f0a11");
    public static final UUID CARD_CHAR_UUID =
            UUID.fromString("7a1c9e11-4b3d-4f2a-9c88-2e5b6d7f0a11");

    private static final int RSSI_MIN = -75;

    private final Context appContext;
    private final Handler handler;

    private final BleAdvertiser advertiser = new BleAdvertiser();
    private final BleScanner scanner = new BleScanner();

    private final Set<String> attempted =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final List<CardGattClient> openClients =
            Collections.synchronizedList(new ArrayList<CardGattClient>());

    private CardGattServer gattServer;

    private volatile boolean windowActive;
    private volatile String myCode;
    private volatile CardProfile profile;
    private BluetoothAdapter adapter;

    public CardExchangeEngine(Context context, Handler handler) {
        this.appContext = context.getApplicationContext();
        this.handler = handler;
        BluetoothManager bm = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = (bm == null) ? null : bm.getAdapter();
    }

    // ---------------- 蓝牙名 ----------------

    // 启用时记住原名并把本机改为 RI_PRTS_XXXXXX
    public void prepareName() {
        if (adapter == null) {
            return;
        }
        String current = safeAdapterName();
        if (CardPrefs.getOriginalBtName(appContext) == null) {
            String original = (current != null && !current.isEmpty()) ? current : Build.MODEL;
            CardPrefs.setOriginalBtName(appContext, original);
        }
        String code = CardPrefs.getOrCreateNodeCode(appContext);
        try {
            adapter.setName("RI_PRTS_" + code);
        } catch (Exception e) {
            Log.w(TAG, "setName failed", e);
        }
    }

    // 关闭功能时恢复原蓝牙名（仅在当前名仍是本功能改的才恢复）
    public void restoreOriginalName() {
        if (adapter != null) {
            String current = safeAdapterName();
            if (current != null && current.startsWith("RI_PRTS_")) {
                String original = CardPrefs.getOriginalBtName(appContext);
                try {
                    adapter.setName((original != null && !original.isEmpty()) ? original : Build.MODEL);
                } catch (Exception e) {
                    Log.w(TAG, "restore name failed", e);
                }
            }
        }
        CardPrefs.clearOriginalBtName(appContext);
    }

    private String safeAdapterName() {
        try {
            return adapter == null ? null : adapter.getName();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- 窗口控制 ----------------

    public void startWindow() {
        if (windowActive) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || adapter == null) {
            Log.w(TAG, "Bluetooth unavailable, skip window");
            return;
        }
        try {
            if (!adapter.isEnabled()) {
                adapter.enable();
            }
        } catch (SecurityException e) {
            // Android 12+ 缺少 BLUETOOTH_CONNECT 时会抛 SecurityException，不能让它杀掉进程
            Log.w(TAG, "bluetooth permission missing, skip window", e);
            return;
        } catch (Exception e) {
            Log.w(TAG, "cannot enable bluetooth", e);
        }
        myCode = CardPrefs.getOrCreateNodeCode(appContext);
        profile = CardStore.readProfile(appContext);
        if (profile == null || !profile.isValid()) {
            Log.w(TAG, "profile invalid, skip window");
            return;
        }
        windowActive = true;
        attempted.clear();
        Log.i(TAG, "window start, node=" + myCode);

        advertiser.start(adapter, myCode);
        scanner.start(adapter, new BleScanner.Listener() {
            @Override
            public void onPeer(BluetoothDevice device, String peerName, String peerCode, int rssi) {
                CardExchangeEngine.this.onPeerFound(device, peerName, peerCode, rssi);
            }
        });
        startServer();
    }

    public void stopWindow() {
        windowActive = false;
        Log.i(TAG, "window stop");
        scanner.stop();
        advertiser.stop();
        stopServer();
        cancelClients();
        attempted.clear();
        myCode = null;
        profile = null;
    }

    // ---------------- 发现 ----------------

    private void onPeerFound(BluetoothDevice device, String peerName, String peerCode, int rssi) {
        if (!windowActive || device == null || peerCode == null) {
            return;
        }
        if (peerCode.equals(myCode)) {
            return;
        }
        if (rssi < RSSI_MIN) {
            return;
        }
        // 对称规则：节点码小的一方主动连接（两端计算结果必然一致）
        if (myCode == null || myCode.compareTo(peerCode) >= 0) {
            return;
        }
        if (!attempted.add(peerCode)) {
            return;
        }
        Log.i(TAG, "peer found: " + peerName + " code=" + peerCode + " rssi=" + rssi + " -> client");
        final BluetoothDevice dev = device;
        startWorker(new Runnable() {
            @Override
            public void run() {
                connectAndExchange(dev);
            }
        });
    }

    // ---------------- GATT ----------------

    private void startServer() {
        gattServer = new CardGattServer(appContext, new CardGattServer.Listener() {
            @Override
            public byte[] localCard() {
                return localCardBytes();
            }

            @Override
            public void onPeerCard(BluetoothDevice device, JSONObject card) {
                Log.i(TAG, "accepted card from " + address(device));
                savePeer(card, address(device));
            }
        });
        if (!gattServer.start()) {
            gattServer = null;
        }
    }

    private void stopServer() {
        CardGattServer s = gattServer;
        gattServer = null;
        if (s != null) {
            s.stop();
        }
    }

    private void connectAndExchange(BluetoothDevice device) {
        final CardGattClient client = new CardGattClient(
                appContext, handler, localCardBytes(), new CardGattClient.Listener() {
            @Override
            public void onPeerCard(JSONObject card) {
                savePeer(card, address(device));
            }

            @Override
            public void onFinished(boolean success) {
                Log.i(TAG, "GATT exchange " + (success ? "ok" : "failed")
                        + " with " + address(device));
                openClients.remove(this);
            }
        });
        openClients.add(client);
        client.start(device);
    }

    private byte[] localCardBytes() {
        try {
            return CardProtocol.encode(buildOutgoingJson());
        } catch (JSONException e) {
            Log.w(TAG, "build json failed", e);
            return null;
        }
    }

    private JSONObject buildOutgoingJson() throws JSONException {
        CardProfile p = profile;
        JSONObject o = new JSONObject();
        o.put("v", CardProtocol.PROTO_VERSION);
        o.put("type", "card");
        o.put("node", myCode);
        o.put("name", "RI_PRTS_" + myCode);
        o.put("game", p == null ? null : p.game);
        o.put("uid", p == null ? null : p.uid);
        o.put("region", p == null ? null : p.region);
        o.put("ts", CardStore.nowEpochSec());
        return o;
    }

    private void savePeer(JSONObject peer, String peerMac) {
        String peerNode = optStr(peer, "node");
        String peerName = optStr(peer, "name");
        if (peerName == null) {
            peerName = (peerNode == null) ? "unknown" : ("RI_PRTS_" + peerNode);
        }
        ReceivedCard card = new ReceivedCard();
        card.receivedAt = CardStore.nowEpochSec();
        card.receivedAtStr = CardStore.toUtc8String(card.receivedAt);
        card.peerNode = peerNode;
        card.peerName = peerName;
        card.peerMac = peerMac;
        card.game = optStr(peer, "game");
        card.uid = optStr(peer, "uid");
        card.region = optStr(peer, "region");
        card.proto = peer.optInt("v", 1);
        CardStore.appendReceived(appContext, card);
        Log.i(TAG, "card exchanged with " + peerName + " (" + peerNode + ")");
    }

    // ---------------- 清理 ----------------

    private void cancelClients() {
        List<CardGattClient> copy;
        synchronized (openClients) {
            copy = new ArrayList<CardGattClient>(openClients);
        }
        for (CardGattClient c : copy) {
            c.cancel();
        }
        openClients.clear();
    }

    private void startWorker(Runnable r) {
        new Thread(r, "ri-card-worker").start();
    }

    private static String address(BluetoothDevice d) {
        if (d == null) {
            return "unknown";
        }
        try {
            return d.getAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String optStr(JSONObject o, String key) {
        if (o == null || o.isNull(key)) {
            return null;
        }
        String v = o.optString(key, null);
        return (v == null || v.isEmpty()) ? null : v;
    }
}
