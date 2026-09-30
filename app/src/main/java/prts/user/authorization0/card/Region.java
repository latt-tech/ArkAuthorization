package prts.user.authorization0.card;

// Region.java
// 游戏区域枚举：CN / CN_Bilibili / HK / JP / KR / US
public enum Region {

    CN("CN"),
    CN_Bilibili("CN_Bilibili"),
    HK("HK"),
    JP("JP"),
    KR("KR"),
    US("US");

    public final String code;

    Region(String code) {
        this.code = code;
    }

    public static Region fromCode(String code) {
        for (Region r : values()) {
            if (r.code.equals(code)) {
                return r;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }

    // 供弹窗列表使用的字符串数组
    public static String[] codes() {
        Region[] values = values();
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i].code;
        }
        return out;
    }
}
