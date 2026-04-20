package dev.scx.http.x.http1.status_line;

/// InvalidStatusLineVersionException
///
/// StatusLine 中非法 Version 异常, 表示 Version 段不正确.
///
/// @author scx567888
/// @version 0.0.1
public final class InvalidStatusLineHttpVersionException extends RuntimeException {

    public final String httpVersionStr;

    /// 不允许外界创建
    InvalidStatusLineHttpVersionException(String httpVersionStr) {
        this.httpVersionStr = httpVersionStr;
        super("Invalid HttpVersion : " + httpVersionStr);
    }

}
