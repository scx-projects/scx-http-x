package cool.scx.http.x.http1.headers.connection;

/// Connection
///
/// @author scx567888
/// @version 0.0.1
public enum Connection implements ScxConnection {

    KEEP_ALIVE("keep-alive"),
    CLOSE("close"),
    UPGRADE("upgrade");

    private final String value;

    Connection(String value) {
        this.value = value;
    }

    /// @param v v
    /// @return 未找到抛出异常
    public static Connection of(String v) {
        //数量较少时 switch 性能要高于 Map
        var h = find(v);
        if (h == null) {
            throw new IllegalArgumentException("Unknown connection : " + v);
        }
        return h;
    }

    public static Connection find(String v) {
        var v1 = v.toLowerCase();
        //数量较少时 switch 性能要高于 Map
        return switch (v1) {
            case "keep-alive" -> KEEP_ALIVE;
            case "close" -> CLOSE;
            case "upgrade" -> UPGRADE;
            default -> null;
        };
    }

    @Override
    public String value() {
        return value;
    }

}
