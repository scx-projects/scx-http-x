package dev.scx.http.x;

import dev.scx.http.ScxHttpClientRequest;
import dev.scx.http.ScxHttpClientResponse;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.headers.ScxHttpHeadersWritable;
import dev.scx.http.media.MediaWriter;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.sender.ScxHttpSenderStatus;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.uri.ScxURIWritable;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.Http1ClientConnection;
import dev.scx.http.x.http1.Http1ClientRequest;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http2.Http2ClientConnection;
import dev.scx.http.x.http2.Http2ClientRequest;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import static dev.scx.http.method.HttpMethod.GET;
import static dev.scx.http.sender.ScxHttpSenderStatus.NOT_SENT;
import static dev.scx.http.version.HttpVersion.HTTP_1_1;
import static dev.scx.http.version.HttpVersion.HTTP_2;
import static dev.scx.http.x.SocketIOHelper.createSocketIO;

/// 支持动态 选择协议的 Request (只支持发送一次).
///
/// @author scx567888
/// @version 0.0.1
public final class HttpClientRequest implements Http1ClientRequest, Http2ClientRequest {

    private final HttpClient httpClient;
    private final HttpClientOptions options;
    private final ReentrantLock sendLock;
    private HttpVersion[] httpVersions;
    private ScxHttpMethod method;
    private ScxURIWritable uri;
    private Http1Headers headers;
    private boolean useProxy;
    private ScxHttpSenderStatus senderStatus;

    public HttpClientRequest(HttpClient httpClient, HttpVersion... httpVersions) {
        this.httpClient = httpClient;
        this.options = httpClient.options();
        this.sendLock = new ReentrantLock();
        this.httpVersions = httpVersions; // 空列表 表示自动协商 暂时没用到 因为现在只支持 http1.1
        this.method = GET;
        this.uri = ScxURI.of();
        this.headers = new Http1Headers();
        this.useProxy = false;
        this.senderStatus = ScxHttpSenderStatus.NOT_SENT;
    }

    private ScxHttpClientResponse send0(MediaWriter mediaWriter) throws IllegalSenderStateException, ScxIOException, AlreadyClosedException {

        // 检查发送状态
        if (senderStatus != NOT_SENT) {
            throw new IllegalSenderStateException(senderStatus);
        }

        Socket tcpSocket;

        try {
            tcpSocket = httpClient.createSocket(uri, getApplicationProtocols());
        } catch (IOException e) {
            throw new ScxIOException("创建连接失败 !!!", e);
        }

        SocketIO socketIO;

        try {
            socketIO = createSocketIO(tcpSocket);
        } catch (IOException e) {
            throw new ScxIOException("创建 SocketIO 失败 !!!", e);
        }

        var useHttp2 = false;

        if (socketIO.tcpSocket instanceof SSLSocket sslSocket) {
            var applicationProtocol = sslSocket.getApplicationProtocol();
            useHttp2 = "h2".equals(applicationProtocol);
        }

        if (useHttp2) {
            return new Http2ClientConnection(socketIO, options.http2ClientConnectionOptions()).sendRequest(this, mediaWriter).readResponse();
        } else {
            // 仅当 http 协议 (不是 SSL) 并且开启代理的时候才使用 绝对路径
            if (!(socketIO.tcpSocket instanceof SSLSocket) && options.proxy() != null) {
                this.useProxy = true;
            }
            return new Http1ClientConnection(socketIO, options.http1ClientConnectionOptions()).sendRequest(this, mediaWriter).readResponse();
        }

    }

    @Override
    public ScxHttpClientResponse send(MediaWriter mediaWriter) throws IllegalSenderStateException, ScxIOException, AlreadyClosedException {
        sendLock.lock();
        try {
            return send0(mediaWriter);
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public ScxHttpSenderStatus senderStatus() {
        return senderStatus;
    }

    private String[] getApplicationProtocols() {
        if (this.options.enableHttp2()) {
            return new String[]{HTTP_1_1.alpnValue(), HTTP_2.alpnValue()};
        } else {
            return new String[]{HTTP_1_1.alpnValue()};
        }
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
    public ScxHttpClientRequest headers(ScxHttpHeaders headers) {
        this.headers = new Http1Headers(headers);
        return this;
    }

    @Override
    public boolean _useProxy() {
        return useProxy;
    }

    @Override
    public void _setSenderStatus(ScxHttpSenderStatus senderStatus) {
        this.senderStatus = senderStatus;
    }

}
