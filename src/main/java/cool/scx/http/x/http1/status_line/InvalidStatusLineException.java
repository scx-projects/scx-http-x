package cool.scx.http.x.http1.status_line;

/// InvalidHttpStatusLineException
///
/// 非法响应行异常, 表示整个 StatusLine 不正确.
///
/// @author scx567888
/// @version 0.0.1
public final class InvalidStatusLineException extends Exception {

    public final String statusLineStr;

    public InvalidStatusLineException(String statusLineStr) {
        this.statusLineStr = statusLineStr;
        super("Invalid StatusLine : " + statusLineStr);
    }

}
