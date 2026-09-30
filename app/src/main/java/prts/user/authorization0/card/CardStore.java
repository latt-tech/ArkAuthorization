package prts.user.authorization0.card;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// CardStore.java
// JSON 文件存储：自己的资料 profile.json / 收到的名片 received_cards.json
// 全部落在 getFilesDir()，写盘用「临时文件 + rename」保证原子性
public final class CardStore {

    private static final Object LOCK = new Object();
    private static final String F_PROFILE = "profile.json";
    private static final String F_RECEIVED = "received_cards.json";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String TZ_UTC8 = "GMT+08:00";

    private CardStore() {
    }

    // ---------------- 时间 ----------------

    public static long nowEpochSec() {
        return System.currentTimeMillis() / 1000L;
    }

    public static String toUtc8String(long epochSec) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(TZ_UTC8));
        return sdf.format(new Date(epochSec * 1000L));
    }

    // ---------------- 自己的资料 ----------------

    public static CardProfile readProfile(Context c) {
        synchronized (LOCK) {
            return CardProfile.fromJson(readJson(c, F_PROFILE));
        }
    }

    public static boolean hasProfile(Context c) {
        CardProfile p = readProfile(c);
        return p != null && p.isValid();
    }

    public static boolean writeProfile(Context c, CardProfile p) {
        if (p == null) {
            return false;
        }
        synchronized (LOCK) {
            return atomicWrite(c, F_PROFILE, p.toJson());
        }
    }

    // ---------------- 收到的名片 ----------------

    public static List<ReceivedCard> readReceived(Context c) {
        synchronized (LOCK) {
            List<ReceivedCard> out = new ArrayList<ReceivedCard>();
            JSONObject doc = readJson(c, F_RECEIVED);
            if (doc == null) {
                return out;
            }
            JSONArray arr = doc.optJSONArray("cards");
            if (arr == null) {
                return out;
            }
            for (int i = 0; i < arr.length(); i++) {
                ReceivedCard card = ReceivedCard.fromJson(arr.optJSONObject(i));
                if (card != null) {
                    out.add(card);
                }
            }
            return out;
        }
    }

    // peer_node + uid 相同则更新，不追加
    public static void appendReceived(Context c, ReceivedCard card) {
        if (card == null) {
            return;
        }
        synchronized (LOCK) {
            JSONObject doc = readJson(c, F_RECEIVED);
            if (doc == null) {
                doc = emptyReceivedDoc();
            }
            JSONArray arr = doc.optJSONArray("cards");
            if (arr == null) {
                arr = new JSONArray();
                try {
                    doc.put("cards", arr);
                } catch (JSONException ignored) {
                }
            }
            int existing = -1;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                if (same(o, "peer_node", card.peerNode) && same(o, "uid", card.uid)) {
                    existing = i;
                    break;
                }
            }
            try {
                if (existing >= 0) {
                    arr.put(existing, card.toJson());
                } else {
                    arr.put(card.toJson());
                }
            } catch (JSONException ignored) {
            }
            atomicWrite(c, F_RECEIVED, doc);
        }
    }

    public static void clearReceived(Context c) {
        synchronized (LOCK) {
            atomicWrite(c, F_RECEIVED, emptyReceivedDoc());
        }
    }

    // ---------------- 内部 ----------------

    private static JSONObject emptyReceivedDoc() {
        JSONObject doc = new JSONObject();
        try {
            doc.put("version", 1);
            doc.put("cards", new JSONArray());
        } catch (JSONException ignored) {
        }
        return doc;
    }

    private static boolean same(JSONObject o, String key, String value) {
        if (value == null) {
            return o.isNull(key);
        }
        return value.equals(o.optString(key, null));
    }

    private static JSONObject readJson(Context c, String name) {
        File f = new File(c.getFilesDir(), name);
        if (!f.exists()) {
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return new JSONObject(new String(bos.toByteArray(), UTF8));
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static boolean atomicWrite(Context c, String name, JSONObject doc) {
        File dir = c.getFilesDir();
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }
        File tmp = new File(dir, name + ".tmp");
        File dst = new File(dir, name);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tmp);
            out.write(doc.toString().getBytes(UTF8));
            out.flush();
            out.close();
            out = null;
            if (tmp.renameTo(dst)) {
                return true;
            }
            if (dst.exists() && dst.delete() && tmp.renameTo(dst)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
