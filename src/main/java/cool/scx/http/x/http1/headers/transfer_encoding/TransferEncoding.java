package cool.scx.http.x.http1.headers.transfer_encoding;

/// TransferEncoding
///
/// @author scx567888
/// @version 0.0.1
public enum TransferEncoding implements ScxTransferEncoding {

    CHUNKED("chunked");

    private final String value;

    TransferEncoding(String value) {
        this.value = value;
    }

    /// @param v v
    /// @return 未找到抛出异常
    public static TransferEncoding of(String v) {
        //数量较少时 switch 性能要高于 Map
        var h = find(v);
        if (h == null) {
            throw new IllegalArgumentException("Unknown transfer encoding : " + v);
        }
        return h;
    }

    /// @param v v
    /// @return 未找到返回 null
    public static TransferEncoding find(String v) {
        var v1 = v.toLowerCase();
        //数量较少时 switch 性能要高于 Map
        return switch (v1) {
            case "chunked" -> CHUNKED;
            default -> null;
        };
    }

    @Override
    public String value() {
        return value;
    }

}
