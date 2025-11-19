package cool.scx.http.x.http1;

import cool.scx.http.ScxHttpClientResponse;
import cool.scx.http.body.ScxHttpBody;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.http.version.HttpVersion;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.status_line.Http1StatusLine;
import cool.scx.io.ByteInput;

import static cool.scx.http.version.HttpVersion.HTTP_1_1;

/// HTTP/1.1 ClientResponse
///
/// @author scx567888
/// @version 0.0.1
public class Http1ClientResponse implements ScxHttpClientResponse {

    private final Http1StatusLine statusLine;
    private final Http1Headers headers;
    private final ScxHttpBody body;

    public Http1ClientResponse(Http1StatusLine statusLine, Http1Headers headers, ByteInput bodyByteInput) {
        this.statusLine = statusLine;
        this.headers = headers;
        this.body = new Http1Body(bodyByteInput, this.headers);
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
    public ScxHttpBody body() {
        return body;
    }

}
