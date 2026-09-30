package prts.user.authorization0.card;

import java.util.Random;
import java.util.UUID;

// NodeCode.java
// 节点码（6 位大写字母/数字）与 BLE 广播 UUID 的编解码。
//
// 广播 UUID 的 16 字节布局：
//   b0..b6   = "RI_PRTS" 的 ASCII
//   b7..b12  = 6 位节点码的 ASCII
//   b13..b15 = 0
// 因此可以用「掩码 UUID」只匹配前 7 字节，从而过滤出所有同伴节点。
public final class NodeCode {

    // 节点码位全 0 的基址 UUID：52495f50-5254-5300-0000-000000000000
    public static final UUID BASE_UUID = new UUID(0x52495f5052545300L, 0L);
    // 只匹配前 7 字节（"RI_PRTS"）：ffffffff-ffff-ff00-0000-000000000000
    public static final UUID MASK_UUID = new UUID(0xffffffffffffff00L, 0L);

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final Random RANDOM = new Random();

    private NodeCode() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    public static boolean isValid(String code) {
        if (code == null || code.length() != 6) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            if (!isCodeChar(code.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isCodeChar(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    // 把节点码编进广播 UUID
    public static UUID buildUuid(String code) {
        byte[] b = new byte[16];
        b[0] = 'R';
        b[1] = 'I';
        b[2] = '_';
        b[3] = 'P';
        b[4] = 'R';
        b[5] = 'T';
        b[6] = 'S';
        if (code != null) {
            for (int i = 0; i < 6 && i < code.length(); i++) {
                b[7 + i] = (byte) code.charAt(i);
            }
        }
        long msb = 0L;
        long lsb = 0L;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (b[i] & 0xffL);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (b[i] & 0xffL);
        }
        return new UUID(msb, lsb);
    }

    // 从广播 UUID 解出节点码，非本协议的 UUID 返回 null
    public static String extractCode(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] b = new byte[16];
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            b[i] = (byte) (lsb >>> (8 * (15 - i)));
        }
        if (b[0] != 'R' || b[1] != 'I' || b[2] != '_' || b[3] != 'P'
                || b[4] != 'R' || b[5] != 'T' || b[6] != 'S') {
            return null;
        }
        StringBuilder sb = new StringBuilder(6);
        for (int i = 7; i <= 12; i++) {
            char c = (char) (b[i] & 0xff);
            if (!isCodeChar(c)) {
                return null;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
