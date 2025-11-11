package cool.scx.http.x.http1.request_line;

/// RequestTargetForm
///
/// @author scx567888
/// @version 0.0.1
public enum RequestTargetForm {

    /// 例: `GET /index.html?foo=1 HTTP/1.1`
    ///
    /// 用于大多数客户端请求
    ORIGIN_FORM,

    /// 例: `GET http://example.com/index.html HTTP/1.1`
    ///
    /// 通过 `HTTP代理` 发出请求时使用
    ABSOLUTE_FORM,

    /// 例: `CONNECT example.com:443 HTTP/1.1`
    ///
    /// 仅与 `CONNECT` 方法一起使用
    AUTHORITY_FORM,

    /// 例: `OPTIONS * HTTP/1.1`
    ///
    /// 仅与 `OPTIONS` 方法一起使用, 针对整个服务器
    ASTERISK_FORM

}
