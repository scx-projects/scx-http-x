package dev.scx.http.x.http2;

import dev.scx.http.ScxHttpClientResponse;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.media.MediaReader;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.version.HttpVersion;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import static dev.scx.http.version.HttpVersion.HTTP_2;

/// 占位 (可能永远都不会实现)
public class Http2ClientResponse implements ScxHttpClientResponse {

    @Override
    public ScxHttpStatusCode statusCode() {
        return null;
    }

    @Override
    public HttpVersion version() {
        return HTTP_2;
    }

    @Override
    public ScxHttpHeaders headers() {
        return null;
    }

    @Override
    public ByteInput body() {
        return null;
    }

    @Override
    public <T> T as(MediaReader<T> mediaReader) throws ScxIOException, AlreadyClosedException {
        return null;
    }

}
