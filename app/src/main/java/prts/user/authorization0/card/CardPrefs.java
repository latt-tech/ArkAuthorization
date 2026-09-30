package prts.user.authorization0.card;

import android.content.Context;
import android.content.SharedPreferences;

// CardPrefs.java
// 名片交换的开关状态与节点码（SharedPreferences: card_prefs）
public final class CardPrefs {

    private static final String NAME = "card_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_NODE_CODE = "node_code";
    private static final String KEY_ORIGINAL_BT_NAME = "original_bt_name";

    private CardPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) {
        return sp(c).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_ENABLED, value).apply();
    }

    public static String getNodeCode(Context c) {
        return sp(c).getString(KEY_NODE_CODE, null);
    }

    public static void setNodeCode(Context c, String code) {
        sp(c).edit().putString(KEY_NODE_CODE, code).apply();
    }

    // 首次使用时生成并持久化节点码
    public static String getOrCreateNodeCode(Context c) {
        String code = getNodeCode(c);
        if (!NodeCode.isValid(code)) {
            code = NodeCode.generate();
            setNodeCode(c, code);
        }
        return code;
    }

    public static String getOriginalBtName(Context c) {
        return sp(c).getString(KEY_ORIGINAL_BT_NAME, null);
    }

    public static void setOriginalBtName(Context c, String name) {
        sp(c).edit().putString(KEY_ORIGINAL_BT_NAME, name).apply();
    }

    public static void clearOriginalBtName(Context c) {
        sp(c).edit().remove(KEY_ORIGINAL_BT_NAME).apply();
    }
}
