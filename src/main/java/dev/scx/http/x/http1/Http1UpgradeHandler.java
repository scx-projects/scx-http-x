package dev.scx.http.x.http1;

import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.x.HttpServerContext;
import dev.scx.http.x.SocketIO;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.headers.upgrade.ScxUpgrade;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.io.ByteInput;

/// Http1UpgradeHandler
///
/// @author scx567888
/// @version 0.0.1
public interface Http1UpgradeHandler<T extends ScxHttpServerRequest> {

    /// 升级协议
    ScxUpgrade upgradeProtocol();

    /// 创建升级之后的 Request
    T createUpgradedRequest(Http1RequestLine requestLine, Http1Headers headers, ByteInput bodyByteInput, SocketIO socketIO, HttpServerContext context);

}
