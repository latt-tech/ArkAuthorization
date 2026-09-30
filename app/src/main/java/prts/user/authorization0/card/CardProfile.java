package prts.user.authorization0.card;

import org.json.JSONException;
import org.json.JSONObject;

// CardProfile.java
// 用户自己的游戏名片资料（临时保存在 App data 目录的 profile.json）
public class CardProfile {

    public static final int VERSION = 1;

    public String game;     // 游戏名
    public String uid;      // 游戏内 UID
    public String region;   // 游戏区域（Region.code）
    public String node;     // 本机节点码
    public String name;     // 本机蓝牙名 RI_PRTS_XXXXXX
    public long updatedAt;  // UTC 秒

    public boolean isValid() {
        if (game == null || game.trim().isEmpty() || game.trim().length() > 32) {
            return false;
        }
        if (uid == null || !uid.trim().matches("[A-Za-z0-9_\\-]{1,32}")) {
            return false;
        }
        return Region.isValid(region);
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("version", VERSION);
            o.put("game", game);
            o.put("uid", uid);
            o.put("region", region);
            o.put("node", node);
            o.put("name", name);
            o.put("updated_at", updatedAt);
        } catch (JSONException ignored) {
        }
        return o;
    }

    public static CardProfile fromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        CardProfile p = new CardProfile();
        p.game = optStr(o, "game");
        p.uid = optStr(o, "uid");
        p.region = optStr(o, "region");
        p.node = optStr(o, "node");
        p.name = optStr(o, "name");
        p.updatedAt = o.optLong("updated_at", 0L);
        return p;
    }

    private static String optStr(JSONObject o, String key) {
        if (o.isNull(key)) {
            return null;
        }
        return o.optString(key, null);
    }
}
