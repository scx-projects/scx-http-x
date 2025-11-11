package cool.scx.http.x.http1.request_line;

/// InvalidRequestLineException
///
/// 非法请求行异常, 表示整个 RequestLine 不正确.
///
/// @author scx567888
/// @version 0.0.1
public final class InvalidRequestLineException extends Exception {

    public final String requestLineStr;

    public InvalidRequestLineException(String requestLineStr) {
        this.requestLineStr = requestLineStr;
        super("Invalid RequestLine : " + requestLineStr);
    }

}
