package dev.scx.http.x.http1.io;

import dev.scx.http.exception.ScxHttpException;
import dev.scx.http.status_code.ScxHttpStatusCode;

import static dev.scx.http.status_code.HttpStatusCode.URI_TOO_LONG;

/// RequestLineTooLongException
///
/// @author scx567888
/// @version 0.0.1
public final class RequestLineTooLongException extends RuntimeException implements ScxHttpException {

    /// 不允许外界创建
    RequestLineTooLongException(String message) {
        super(message);
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return URI_TOO_LONG;
    }

}
