package cool.scx.http.x.http1.exception;

import cool.scx.http.exception.ScxHttpException;

import static cool.scx.http.status_code.HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED;

/// HttpVersionNotSupportedException
///
/// @author scx567888
/// @version 0.0.1
public class HttpVersionNotSupportedException extends ScxHttpException {

    public HttpVersionNotSupportedException() {
        super(HTTP_VERSION_NOT_SUPPORTED);
    }

    public HttpVersionNotSupportedException(String message) {
        super(HTTP_VERSION_NOT_SUPPORTED, message);
    }

    public HttpVersionNotSupportedException(Throwable cause) {
        super(HTTP_VERSION_NOT_SUPPORTED, cause);
    }

    public HttpVersionNotSupportedException(String message, Throwable cause) {
        super(HTTP_VERSION_NOT_SUPPORTED, message, cause);
    }

}
