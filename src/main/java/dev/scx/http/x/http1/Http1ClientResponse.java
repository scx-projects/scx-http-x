package dev.scx.http.x.http1;

import dev.scx.http.ScxHttpClientResponse;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.media.MediaReader;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import static dev.scx.http.version.HttpVersion.HTTP_1_1;

/// Http1ClientResponse
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientResponse implements ScxHttpClientResponse {

    private final Http1StatusLine statusLine;
    private final Http1Headers headers;
    private final ByteInput body;

    public Http1ClientResponse(Http1StatusLine statusLine, Http1Headers headers, ByteInput body) {
        this.statusLine = statusLine;
        this.headers = headers;
        this.body = body;
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return statusLine.statusCode();
    }

    @Override
    public HttpVersion version() {
        return HTTP_1_1;
    }

    public String reasonPhrase() {
        return statusLine.reasonPhrase();
    }

    @Override
    public ScxHttpHeaders headers() {
        return headers;
    }

    @Override
    public ByteInput body() {
        return body;
    }

    @Override
    public <T> T as(MediaReader<T> mediaReader) throws ScxIOException, AlreadyClosedException {
        return mediaReader.read(body, headers);
    }

}
