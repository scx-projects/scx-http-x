package dev.scx.http.x.http1.io;

import dev.scx.http.exception.ScxHttpException;
import dev.scx.http.status_code.ScxHttpStatusCode;

import static dev.scx.http.status_code.HttpStatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE;

/// HeaderTooLargeException
///
/// @author scx567888
/// @version 0.0.1
public final class HeaderTooLargeException extends RuntimeException implements ScxHttpException {

    /// 不允许外界创建
    HeaderTooLargeException(String message) {
        super(message);
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return REQUEST_HEADER_FIELDS_TOO_LARGE;
    }

}
