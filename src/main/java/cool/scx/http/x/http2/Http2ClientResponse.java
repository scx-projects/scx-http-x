package cool.scx.http.x.http2;

import cool.scx.http.ScxHttpClientResponse;
import cool.scx.http.body.ScxHttpBody;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.http.version.HttpVersion;

import static cool.scx.http.version.HttpVersion.HTTP_2;

// 待完成
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
