package cool.scx.http.x.http1.status_line;

import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.http.version.HttpVersion;

import static cool.scx.http.version.HttpVersion.HTTP_1_1;

/// Http1StatusLineHelper
///
/// @author scx567888
/// @version 0.0.1
public final class Http1StatusLineHelper {

    public static Http1StatusLine parseStatusLine(String statusLineStr) throws InvalidStatusLineException, InvalidStatusLineHttpVersionException, InvalidStatusLineStatusCodeException {
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
        return statusLine.httpVersion().protocolVersion() + " " + statusLine.statusCode() + " " + statusLine.reasonPhrase();
    }

}
