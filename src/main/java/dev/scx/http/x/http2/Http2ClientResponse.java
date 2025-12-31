package dev.scx.http.x.http2;

import dev.scx.http.ScxHttpClientResponse;
import dev.scx.http.body.ScxHttpBody;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.version.HttpVersion;

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
    public ScxHttpBody body() {
        return null;
    }

}
