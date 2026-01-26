package dev.scx.http.x.http1.io;

import dev.scx.http.exception.ScxHttpException;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.io.exception.ScxInputException;

import static dev.scx.http.status_code.HttpStatusCode.BAD_REQUEST;

/// ContentLengthBodyTooShortException
///
/// @author scx567888
/// @version 0.0.1
public final class ContentLengthBodyTooShortException extends ScxInputException implements ScxHttpException {

    /// 不允许外界创建
    ContentLengthBodyTooShortException(String message) {
        super(message);
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return BAD_REQUEST;
    }

}
