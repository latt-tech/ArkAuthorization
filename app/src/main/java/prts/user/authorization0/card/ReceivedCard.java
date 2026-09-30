package prts.user.authorization0.card;

import org.json.JSONException;
import org.json.JSONObject;

// ReceivedCard.java
// 收到的一张游戏名片记录（存于 received_cards.json）
public class ReceivedCard {

    public long receivedAt;         // UTC 秒
    public String receivedAtStr;    // UTC+8 可读时间
    public String peerNode;         // 对方节点码
    public String peerName;         // 对方蓝牙名
    public String peerMac;          // 对方蓝牙 MAC
    public String game;
    public String uid;
    public String region;
    public int proto = 1;

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("received_at", receivedAt);
            o.put("received_at_str", receivedAtStr);
            o.put("peer_node", peerNode);
            o.put("peer_name", peerName);
            o.put("peer_mac", peerMac);
            o.put("game", game);
            o.put("uid", uid);
            o.put("region", region);
            o.put("proto", proto);
        } catch (JSONException ignored) {
        }
        return o;
    }

    public static ReceivedCard fromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        ReceivedCard c = new ReceivedCard();
        c.receivedAt = o.optLong("received_at", 0L);
        c.receivedAtStr = optStr(o, "received_at_str");
        c.peerNode = optStr(o, "peer_node");
        c.peerName = optStr(o, "peer_name");
        c.peerMac = optStr(o, "peer_mac");
        c.game = optStr(o, "game");
        c.uid = optStr(o, "uid");
        c.region = optStr(o, "region");
        c.proto = o.optInt("proto", 1);
        return c;
    }

    private static String optStr(JSONObject o, String key) {
        if (o.isNull(key)) {
            return null;
        }
        return o.optString(key, null);
    }
}
