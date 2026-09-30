package prts.user.authorization0;

// SettingsActivity.java
// 应用设置页：循环播放开关、应用中心开关、蓝牙名片交换（实验性功能）
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import prts.user.authorization0.card.BluetoothCardService;
import prts.user.authorization0.card.CardPrefs;
import prts.user.authorization0.card.CardProfile;
import prts.user.authorization0.card.CardProfileActivity;
import prts.user.authorization0.card.CardRecordsActivity;
import prts.user.authorization0.card.CardStore;

public class SettingsActivity extends Activity {

    private static final String PREFS_NAME = "player_prefs";
    private static final String KEY_LOOP_MODE = "loop_mode";
    private static final String KEY_APP_CENTER = "app_center_enabled";
    private static final int REQ_BT = 2001;

    private CheckBox cardExchangeCheckBox;
    private boolean suppressCardToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        CheckBox loopCheckBox = findViewById(R.id.checkbox_loop);
        loopCheckBox.setChecked(prefs.getBoolean(KEY_LOOP_MODE, false));
        loopCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_LOOP_MODE, isChecked).apply();
            }
        });

        // 应用中心默认禁用
        CheckBox appCenterCheckBox = findViewById(R.id.checkbox_app_center);
        appCenterCheckBox.setChecked(prefs.getBoolean(KEY_APP_CENTER, false));
        appCenterCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_APP_CENTER, isChecked).apply();
            }
        });

        // ===== 蓝牙名片交换（实验性功能）=====
        cardExchangeCheckBox = findViewById(R.id.checkbox_card_exchange);
        cardExchangeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (suppressCardToggle) {
                    return;
                }
                if (isChecked) {
                    if (!CardStore.hasProfile(SettingsActivity.this)) {
                        toast("请先填写游戏名片");
                        revertCardToggle();
                        return;
                    }
                    if (!ensureBlePrerequisites()) {
                        revertCardToggle();
                        return;
                    }
                    CardPrefs.setEnabled(SettingsActivity.this, true);
                    startCardService();
                } else {
                    CardPrefs.setEnabled(SettingsActivity.this, false);
                    stopCardService();
                }
            }
        });

        findViewById(R.id.button_edit_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, CardProfileActivity.class));
            }
        });
        findViewById(R.id.button_view_cards).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, CardRecordsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCardExchangeUi();
    }

    // ---------------- 开关状态与门禁 ----------------

    private void refreshCardExchangeUi() {
        if (cardExchangeCheckBox == null) {
            return;
        }
        boolean valid = CardStore.hasProfile(this);
        boolean enabled = CardPrefs.isEnabled(this);

        suppressCardToggle = true;
        cardExchangeCheckBox.setEnabled(valid);
        cardExchangeCheckBox.setChecked(valid && enabled);
        suppressCardToggle = false;

        if (!valid && enabled) {
            // 资料被清空等异常状态：强制关闭
            CardPrefs.setEnabled(this, false);
            stopCardService();
        }

        String code = CardPrefs.getOrCreateNodeCode(this);
        TextView tv = findViewById(R.id.text_node_code);
        if (valid) {
            tv.setText("本机节点码：RI_PRTS_" + code);
        } else {
            tv.setText("本机节点码：RI_PRTS_" + code + "\n（请先填写游戏名片后再开启）");
        }

        if (valid && enabled) {
            startCardService();
        }
    }

    private void revertCardToggle() {
        suppressCardToggle = true;
        cardExchangeCheckBox.setChecked(false);
        suppressCardToggle = false;
        CardPrefs.setEnabled(this, false);
    }

    private boolean ensureBlePrerequisites() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            toast("系统版本过低（需 Android 5.0+）");
            return false;
        }
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null || bm.getAdapter() == null) {
            toast("本机不支持蓝牙");
            return false;
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast("本机不支持 BLE");
            return false;
        }
        // API 31+ 起蓝牙相关权限是运行时权限（部分定制 ROM 在 targetSdk<31 时同样强制），必须显式申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] missing = missingPermissions(requiredBtPermissions());
            if (missing.length > 0) {
                requestPermissions(missing, REQ_BT);
                return false;
            }
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] missing = missingPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            if (missing.length > 0) {
                requestPermissions(missing, REQ_BT);
                return false;
            }
            if (!isLocationServiceOn()) {
                showLocationServiceDialog();
                return false;
            }
        }
        return true;
    }

    private static String[] requiredBtPermissions() {
        return new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
        };
    }

    private String[] missingPermissions(String[] wanted) {
        List<String> missing = new ArrayList<>();
        for (String p : wanted) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }
        return missing.toArray(new String[0]);
    }

    private boolean isLocationServiceOn() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return true;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return lm.isLocationEnabled();
            }
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return true;
        }
    }

    private void showLocationServiceDialog() {
        new AlertDialog.Builder(this)
            .setTitle("需要开启定位")
            .setMessage("扫描附近的蓝牙设备需要开启系统定位服务，请前往设置打开。")
            .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } catch (Exception ignored) {
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_BT) {
            boolean allGranted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted && ensureBlePrerequisites()) {
                CardPrefs.setEnabled(this, true);
                startCardService();
                refreshCardExchangeUi();
            } else {
                toast(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? "需要蓝牙权限才能交换名片"
                        : "需要定位权限才能扫描附近设备");
                revertCardToggle();
            }
        }
    }

    // ---------------- 服务启停 ----------------

    private void startCardService() {
        Intent intent = new Intent(this, BluetoothCardService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopCardService() {
        stopService(new Intent(this, BluetoothCardService.class));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
