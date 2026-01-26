package dev.scx.http.x.http1;

import dev.scx.http.ScxHttpClientRequest;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.sender.HttpSenderStatus;

/// Http1ClientRequest
///
/// @author scx567888
/// @version 0.0.1
public interface Http1ClientRequest extends ScxHttpClientRequest {

    @Override
    Http1Headers headers();

    /// 是否正在使用代理 (内部方法 不应由用户调用)
    boolean _useProxy();

    /// 设置发送器状态 (内部方法 不应由用户调用)
    void _setSenderStatus(HttpSenderStatus senderStatus);

}
