package dev.scx.http.x;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.ScxHttpClientRequest;
import dev.scx.http.ScxHttpClientResponse;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.sender.ScxHttpReceiveException;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.uri.ScxURIWritable;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.endpoint.SocketByteEndpoint;
import dev.scx.http.x.http1.Http1ClientConnection;
import dev.scx.http.x.http1.Http1ClientRequest;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http2.Http2ClientConnection;
import dev.scx.http.x.http2.Http2ClientRequest;
import dev.scx.http.x.sender.AbstractHttpSender;

import javax.net.ssl.SSLSocket;
import java.net.Socket;

import static dev.scx.http.method.HttpMethod.GET;
import static dev.scx.http.version.HttpVersion.HTTP_1_1;
import static dev.scx.http.version.HttpVersion.HTTP_2;
import static dev.scx.http.x.helper.SocketByteEndpointHelper.createSocketByteEndpoint;

/// 支持动态 选择协议的 Request (只支持发送一次).
///
/// @author scx567888
/// @version 0.0.1
public final class HttpClientRequest extends AbstractHttpSender<ScxHttpClientResponse> implements Http1ClientRequest, Http2ClientRequest {

    private final HttpClient httpClient;
    private final HttpClientOptions options;
    private HttpVersion[] httpVersions;
    private ScxHttpMethod method;
    private ScxURIWritable uri;
    private Http1Headers headers;
    private boolean useProxy;

    public HttpClientRequest(HttpClient httpClient, HttpVersion... httpVersions) {
        this.httpClient = httpClient;
        this.options = httpClient.options();
        this.httpVersions = httpVersions; // 空列表 表示自动协商 暂时没用到 因为现在只支持 http1.1
        this.method = GET;
        this.uri = ScxURI.of();
        this.headers = new Http1Headers();
        this.useProxy = false;
    }

    @Override
    public ScxHttpMethod method() {
        return method;
    }

    @Override
    public ScxURIWritable uri() {
        return uri;
    }

    @Override
    public Http1Headers headers() {
        return headers;
    }

    @Override
    public ScxHttpClientRequest method(ScxHttpMethod method) {
        this.method = method;
        return this;
    }

    @Override
    public ScxHttpClientRequest uri(ScxURI uri) {
        this.uri = ScxURI.of(uri);
        return this;
    }

    @Override
    public boolean _useProxy() {
        return useProxy;
    }

    private String[] getApplicationProtocols() {
        if (this.options.enableHttp2()) {
            return new String[]{HTTP_1_1.alpnValue(), HTTP_2.alpnValue()};
        } else {
            return new String[]{HTTP_1_1.alpnValue()};
        }
    }

    @Override
    protected ScxHttpClientResponse send0(BodyWriter bodyWriter) throws ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {

        Socket tcpSocket;

        try {
            tcpSocket = httpClient.createSocket(uri, getApplicationProtocols());
        } catch (Throwable e) {
            throw new ScxHttpSendException("创建连接失败 !!!", e);
        }

        SocketByteEndpoint endpoint;

        try {
            endpoint = createSocketByteEndpoint(tcpSocket);
        } catch (Throwable e) {
            throw new ScxHttpSendException("创建 SocketByteEndpoint 失败 !!!", e);
        }

        var useHttp2 = false;

        if (endpoint.socket instanceof SSLSocket sslSocket) {
            var applicationProtocol = sslSocket.getApplicationProtocol();
            useHttp2 = "h2".equals(applicationProtocol);
        }

        if (useHttp2) {
            return new Http2ClientConnection(endpoint, options.http2ClientConnectionOptions()).sendRequest(this, bodyWriter).readResponse();
        } else {
            // 仅当 http 协议 (不是 SSL) 并且开启代理的时候才使用 绝对路径
            if (!(endpoint.socket instanceof SSLSocket) && options.proxy() != null) {
                this.useProxy = true;
            }
            return new Http1ClientConnection(endpoint, options.http1ClientConnectionOptions()).sendRequest(this, bodyWriter).readResponse();
        }

    }

}
