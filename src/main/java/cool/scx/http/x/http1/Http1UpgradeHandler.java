package cool.scx.http.x.http1;

import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.headers.upgrade.ScxUpgrade;
import cool.scx.http.x.http1.request_line.Http1RequestLine;
import cool.scx.io.ByteInput;

/// Http1UpgradeHandler
///
/// @author scx567888
/// @version 0.0.1
public interface Http1UpgradeHandler<T extends ScxHttpServerRequest> {

    /// 是否能够处理
    boolean canUpgrade(ScxUpgrade scxUpgrade);

    /// 创建升级之后的 Request
    T createUpgradedRequest(Http1ServerConnection connection, Http1RequestLine requestLine, Http1Headers headers, ByteInput bodyByteInput);

}
