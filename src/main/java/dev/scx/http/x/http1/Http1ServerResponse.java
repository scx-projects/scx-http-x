package dev.scx.http.x.http1;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.ScxHttpServerResponse;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.http.status_code.HttpStatusCode;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.sender.AbstractHttpSender;

/// Http1ServerResponse
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerResponse extends AbstractHttpSender<Void> implements ScxHttpServerResponse {

    /// 对外公开 connection 字段, 以便 实现更底层功能.
    public final Http1ServerConnection connection;

    private final Http1ServerRequest request;
    private final Http1Headers headers;
    private ScxHttpStatusCode statusCode;
    private String reasonPhrase;

    Http1ServerResponse(Http1ServerRequest request, Http1ServerConnection connection) {
        this.request = request;
        this.connection = connection;
        this.statusCode = HttpStatusCode.OK;
        this.headers = new Http1Headers();
        this.reasonPhrase = null;
    }

    @Override
    public Http1ServerRequest request() {
        return request;
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return statusCode;
    }

    @Override
    public ScxHttpServerResponse statusCode(ScxHttpStatusCode statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    @Override
    public Http1Headers headers() {
        return headers;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }

    public Http1ServerResponse reasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    @Override
    public Void send0(BodyWriter bodyWriter) throws ScxHttpSendException, ScxWrappedException {
        connection.sendResponse(this, bodyWriter);
        return null;
    }

}
