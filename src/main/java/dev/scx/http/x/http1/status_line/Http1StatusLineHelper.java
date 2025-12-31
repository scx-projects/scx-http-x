package dev.scx.http.x.http1.status_line;

import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.version.HttpVersion;

import static dev.scx.http.version.HttpVersion.HTTP_1_1;

/// Http1StatusLineHelper
///
/// @author scx567888
/// @version 0.0.1
final class Http1StatusLineHelper {

    public static Http1StatusLine parseStatusLine(String statusLineStr) throws InvalidStatusLineException, InvalidStatusLineHttpVersionException, InvalidStatusLineStatusCodeException {
        // 这里和 parseRequestLine 不同, 响应行是允许 空格的. 如 "HTTP/1.1 404 Not Found", 所以这里使用 limit = 3 限制一下.
        var parts = statusLineStr.split(" ", 3);

        if (parts.length != 3) {
            throw new InvalidStatusLineException(statusLineStr);
        }

        var httpVersionStr = parts[0];
        var statusCodeStr = parts[1];
        var reasonPhraseStr = parts[2];

        var httpVersion = HttpVersion.find(httpVersionStr);

        //这里我们强制 版本号必须是 HTTP/1.1 , 这里需要细化一下 异常
        if (httpVersion != HTTP_1_1) {
            throw new InvalidStatusLineHttpVersionException(httpVersionStr);
        }

        ScxHttpStatusCode statusCode;
        try {
            statusCode = ScxHttpStatusCode.of(Integer.parseInt(statusCodeStr));
        } catch (NumberFormatException e) {
            // 这里假设 无法转换成 数字 捕获一下错误
            throw new InvalidStatusLineStatusCodeException(statusCodeStr);
        }

        return new Http1StatusLine(httpVersion, statusCode, reasonPhraseStr);
    }

    public static String encodeStatusLine(Http1StatusLine statusLine) {
        var httpVersion = statusLine.httpVersion();
        var statusCode = statusLine.statusCode();
        var reasonPhrase = statusLine.reasonPhrase();
        // 此处我们强制使用 HTTP/1.1
        if (httpVersion != HTTP_1_1) {
            throw new IllegalArgumentException("httpVersion is not supported");
        }

        // 此处校验 reasonPhrase, 不允许出现 \r 或 \n
        if (reasonPhrase.contains("\r") || reasonPhrase.contains("\n")) {
            throw new IllegalArgumentException("reasonPhrase contains illegal CR or LF characters");
        }

        var httpVersionStr = httpVersion.protocolVersion();
        var statusCodeStr = statusCode.value() + "";

        // 直接拼接
        return httpVersionStr + " " + statusCodeStr + " " + reasonPhrase;
    }

}
