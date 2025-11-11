package cool.scx.http.x.http1.headers.transfer_encoding;

/// 虽然 TransferEncoding 理论上支持 多个按照 "," 分割的值
/// 但是在现代的服务器和客户端实现中, 几乎只存在 一个单值
/// 所以这里我们也仅支持 单个值
///
/// @author scx567888
/// @version 0.0.1
public sealed interface ScxTransferEncoding permits TransferEncoding, ScxTransferEncodingImpl {

    static ScxTransferEncoding of(String v) {
        // 优先使用 TransferEncoding
        var m = TransferEncoding.find(v);
        return m != null ? m : new ScxTransferEncodingImpl(v);
    }

    String value();

}
