package cool.scx.http.x.http1.headers.upgrade;

/// Upgrade
///
/// @author scx567888
/// @version 0.0.1
public enum Upgrade implements ScxUpgrade {

    WEB_SOCKET("websocket");

    private final String value;

    Upgrade(String value) {
        this.value = value;
    }

    /// @param v v
    /// @return 未找到抛出异常
    public static Upgrade of(String v) {
        //数量较少时 switch 性能要高于 Map
        var h = find(v);
        if (h == null) {
            throw new IllegalArgumentException("Unknown upgrade : " + v);
        }
        return h;
    }

    /// @param v v
    /// @return 未找到返回 null
    public static Upgrade find(String v) {
        var v1 = v.toLowerCase();
        //数量较少时 switch 性能要高于 Map
        return switch (v1) {
            case "websocket" -> WEB_SOCKET;
            default -> null;
        };
    }

    @Override
    public String value() {
        return value;
    }

}
