package dev.scx.http.x.http1.io;

/// StatusLineToLongException
///
/// @author scx567888
public final class StatusLineToLongException extends RuntimeException {

    /// 不允许外界创建
    StatusLineToLongException(String message) {
        super(message);
    }

}
