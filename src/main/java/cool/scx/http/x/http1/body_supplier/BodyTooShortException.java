package cool.scx.http.x.http1.body_supplier;

import cool.scx.http.exception.ScxHttpExceptionLike;
import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.io.exception.ScxIOException;

/// BodyToShortException
public class BodyTooShortException extends ScxIOException implements ScxHttpExceptionLike {

    private final ScxHttpStatusCode statusCode;

    /// 不允许外界创建
    BodyTooShortException(ScxHttpStatusCode statusCode) {
        super();
        this.statusCode = statusCode;
    }

    /// 不允许外界创建
    BodyTooShortException(ScxHttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return statusCode;
    }

}
