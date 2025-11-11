package cool.scx.http.x.http1.request_line;

/// InvalidRequestLineHttpVersionException
///
/// RequestLine 中非法 Version 异常, 表示 Version 段不正确.
///
/// @author scx567888
/// @version 0.0.1
public final class InvalidRequestLineHttpVersionException extends Exception {

    public final String httpVersionStr;

    public InvalidRequestLineHttpVersionException(String httpVersionStr) {
        this.httpVersionStr = httpVersionStr;
        super("Invalid HttpVersion : " + httpVersionStr);
    }

}
