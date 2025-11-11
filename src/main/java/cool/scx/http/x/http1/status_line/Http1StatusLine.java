package cool.scx.http.x.http1.status_line;

import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.http.version.HttpVersion;

/// Http1StatusLine
///
/// @author scx567888
/// @version 0.0.1
public record Http1StatusLine(HttpVersion httpVersion, ScxHttpStatusCode statusCode, String reasonPhrase) {

    public static Http1StatusLine of(String statusLineStr) throws InvalidStatusLineException, InvalidStatusLineHttpVersionException, InvalidStatusLineStatusCodeException {
        return Http1StatusLineHelper.parseStatusLine(statusLineStr);
    }

    public String encode() {
        return Http1StatusLineHelper.encodeStatusLine(this);
    }

}
