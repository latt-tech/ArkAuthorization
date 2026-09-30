package prts.user.authorization0.card;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;

// CardProtocol.java
// 名片载荷编解码：UTF-8 JSON 字节数组，直接通过 GATT 特征值读写
public final class CardProtocol {

    public static final int PROTO_VERSION = 1;
    public static final int MAX_BYTES = 512;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private CardProtocol() {
    }

    public static boolean isValidPayload(byte[] body) {
        return body != null && body.length > 0 && body.length <= MAX_BYTES;
    }

    public static byte[] encode(JSONObject json) {
        return json.toString().getBytes(UTF8);
    }

    public static JSONObject decode(byte[] body) {
        if (!isValidPayload(body)) {
            return null;
        }
        try {
            return new JSONObject(new String(body, UTF8));
        } catch (JSONException e) {
            return null;
        }
    }
}
