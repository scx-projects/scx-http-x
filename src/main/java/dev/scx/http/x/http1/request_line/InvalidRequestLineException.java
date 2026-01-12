package dev.scx.http.x.http1.request_line;

import dev.scx.http.exception.ScxHttpException;
import dev.scx.http.status_code.ScxHttpStatusCode;

import static dev.scx.http.status_code.HttpStatusCode.BAD_REQUEST;

/// InvalidRequestLineException
///
/// @author scx567888
/// @version 0.0.1
public final class InvalidRequestLineException extends RuntimeException implements ScxHttpException {

    public final String requestLineStr;

    /// 不允许外界创建
    InvalidRequestLineException(String requestLineStr) {
        super("Invalid RequestLine : " + requestLineStr);
        this.requestLineStr = requestLineStr;
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return BAD_REQUEST;
    }

}
