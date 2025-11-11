package cool.scx.http.x.http1.exception;

import cool.scx.http.exception.BadRequestException;

public class InvalidHttpRequestLineException extends BadRequestException {

    public InvalidHttpRequestLineException(String message) {
        super(message);
    }

}
