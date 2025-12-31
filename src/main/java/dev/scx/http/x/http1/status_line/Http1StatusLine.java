package dev.scx.http.x.http1.status_line;

import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.version.HttpVersion;

/// Http1StatusLine
///
/// @author scx567888
/// @version 0.0.1
public record Http1StatusLine(HttpVersion httpVersion, ScxHttpStatusCode statusCode, String reasonPhrase) {

    public Http1StatusLine(ScxHttpStatusCode statusCode, String reasonPhrase) {
        this(HttpVersion.HTTP_1_1, statusCode, reasonPhrase);
    }

    public static Http1StatusLine of(String statusLineStr) throws InvalidStatusLineException, InvalidStatusLineHttpVersionException, InvalidStatusLineStatusCodeException {
        return Http1StatusLineHelper.parseStatusLine(statusLineStr);
    }

    public String encode() {
        return Http1StatusLineHelper.encodeStatusLine(this);
    }

}
