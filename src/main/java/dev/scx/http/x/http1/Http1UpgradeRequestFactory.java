package dev.scx.http.x.http1;

import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.headers.upgrade.ScxUpgrade;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.io.ByteInput;

/// Http1UpgradeRequestFactory (不直接升级, 依然需要用户手动升级)
///
/// 本质上 可以看作一个 特殊 Request 类型的工厂
///
/// @author scx567888
/// @version 0.0.1
public interface Http1UpgradeRequestFactory<T extends ScxHttpServerRequest> {

    /// 升级协议
    ScxUpgrade upgradeProtocol();

    /// 创建一个特殊的 升级 Request
    T createUpgradeRequest(Http1RequestLine requestLine, Http1Headers headers, Long bodyLength, ByteInput bodyByteInput, Http1ServerConnection connection);

}
