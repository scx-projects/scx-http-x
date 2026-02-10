package dev.scx.http.x.http1.request_line;

import dev.scx.http.exception.ScxHttpException;
import dev.scx.http.status_code.ScxHttpStatusCode;

import static dev.scx.http.status_code.HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED;

/// InvalidRequestLineHttpVersionException
///
/// @author scx567888
/// @version 0.0.1
public final class InvalidRequestLineHttpVersionException extends RuntimeException implements ScxHttpException {

    public final String httpVersionStr;

    /// 不允许外界创建
    InvalidRequestLineHttpVersionException(String httpVersionStr) {
        this.httpVersionStr = httpVersionStr;
        super("Invalid HttpVersion : " + httpVersionStr);
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return HTTP_VERSION_NOT_SUPPORTED;
    }

}
