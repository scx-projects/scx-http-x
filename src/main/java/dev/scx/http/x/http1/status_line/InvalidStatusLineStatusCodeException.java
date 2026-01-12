package dev.scx.http.x.http1.status_line;

/// InvalidStatusLineStatusCodeException
///
/// StatusLine 中 StatusCode 段不正确.
///
/// @author scx567888
/// @version 0.0.1
public final class InvalidStatusLineStatusCodeException extends RuntimeException {

    public final String statusCodeStr;

    /// 不允许外界创建
    InvalidStatusLineStatusCodeException(String statusCodeStr) {
        this.statusCodeStr = statusCodeStr;
        super("Invalid StatusCode : " + statusCodeStr);
    }

}
