package dev.scx.http.x.http1.request_line;

import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.request_line.request_target.RequestTarget;

import static dev.scx.http.version.HttpVersion.HTTP_1_1;

/// Http 1.x 的请求行
///
/// @author scx567888
/// @version 0.0.1
public record Http1RequestLine(ScxHttpMethod method, RequestTarget requestTarget, HttpVersion httpVersion) {

    public Http1RequestLine(ScxHttpMethod method, RequestTarget requestTarget) {
        this(method, requestTarget, HTTP_1_1);
    }

    public static Http1RequestLine of(String requestLineStr) throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        return Http1RequestLineHelper.parseRequestLine(requestLineStr);
    }

    public String encode() throws IllegalArgumentException {
        return Http1RequestLineHelper.encodeRequestLine(this);
    }

}
