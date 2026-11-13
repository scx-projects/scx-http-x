package dev.scx.http.x.http1.io;

/// StatusLineTooLongException
///
/// @author scx567888
public final class StatusLineTooLongException extends RuntimeException {

    /// 不允许外界创建
    StatusLineTooLongException(String message) {
        super(message);
    }

}
