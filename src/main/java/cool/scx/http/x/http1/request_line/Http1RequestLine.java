package cool.scx.http.x.http1.request_line;

import cool.scx.http.method.ScxHttpMethod;
import cool.scx.http.uri.ScxURI;
import cool.scx.http.version.HttpVersion;

import static cool.scx.http.version.HttpVersion.HTTP_1_1;

/// Http 1.x 的请求行
///
/// @param method
/// @param requestTarget
/// @param httpVersion
/// @author scx567888
/// @version 0.0.1
public record Http1RequestLine(ScxHttpMethod method, ScxURI requestTarget, HttpVersion httpVersion) {

    public Http1RequestLine(ScxHttpMethod method, ScxURI path) {
        this(method, path, HTTP_1_1);
    }

    public static Http1RequestLine of(String requestLineStr) throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        return Http1RequestLineHelper.parseRequestLine(requestLineStr);
    }

    public String encode() {
        return encode(RequestTargetForm.ORIGIN_FORM);
    }

    public String encode(RequestTargetForm requestTargetForm) {
        return Http1RequestLineHelper.encodeRequestLine(this, requestTargetForm);
    }

}
