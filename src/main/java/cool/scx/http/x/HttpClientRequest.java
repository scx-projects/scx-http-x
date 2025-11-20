package cool.scx.http.x;

import cool.scx.http.ScxHttpClientRequest;
import cool.scx.http.ScxHttpClientResponse;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.headers.ScxHttpHeadersWritable;
import cool.scx.http.media.MediaWriter;
import cool.scx.http.method.ScxHttpMethod;
import cool.scx.http.sender.HttpSendException;
import cool.scx.http.sender.IllegalSenderStateException;
import cool.scx.http.sender.ScxHttpSenderStatus;
import cool.scx.http.uri.ScxURI;
import cool.scx.http.uri.ScxURIWritable;
import cool.scx.http.version.HttpVersion;
import cool.scx.http.x.http1.Http1ClientConnection;
import cool.scx.http.x.http1.Http1ClientRequest;
import cool.scx.http.x.http1.request_line.RequestTargetForm;
import cool.scx.http.x.http2.Http2ClientConnection;
import cool.scx.http.x.http2.Http2ClientRequest;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import static cool.scx.http.method.HttpMethod.GET;
import static cool.scx.http.sender.ScxHttpSenderStatus.NOT_SENT;
import static cool.scx.http.version.HttpVersion.HTTP_1_1;
import static cool.scx.http.version.HttpVersion.HTTP_2;
import static cool.scx.http.x.http1.request_line.RequestTargetForm.ABSOLUTE_FORM;
import static cool.scx.http.x.http1.request_line.RequestTargetForm.ORIGIN_FORM;

/// 支持动态 选择协议的 Request.
///
/// @author scx567888
/// @version 0.0.1
public class HttpClientRequest implements Http1ClientRequest, Http2ClientRequest {

    private final HttpClient httpClient;
    private final HttpClientOptions options;
    private final ReentrantLock sendLock;
    private HttpVersion[] httpVersions;
    private ScxHttpMethod method;
    private ScxURIWritable uri;
    private ScxHttpHeadersWritable headers;
    private RequestTargetForm requestTargetForm;
    private ScxHttpSenderStatus senderStatus;

    public HttpClientRequest(HttpClient httpClient, HttpVersion... httpVersions) {
        this.httpClient = httpClient;
        this.options = httpClient.options();
        this.sendLock = new ReentrantLock();
        this.httpVersions = httpVersions; // 空列表 表示自动协商 暂时没用到 因为现在只支持 http1.1
        this.method = GET;
        this.uri = ScxURI.of();
        this.headers = ScxHttpHeaders.of();
        this.requestTargetForm = ORIGIN_FORM;
        this.senderStatus = ScxHttpSenderStatus.NOT_SENT;
    }

    private ScxHttpClientResponse send0(MediaWriter mediaWriter) throws HttpSendException {

        // 检查发送状态
        if (senderStatus != NOT_SENT) {
            throw new IllegalSenderStateException(senderStatus);
        }

        Socket tcpSocket;

        try {
            tcpSocket = httpClient.createTCPSocket(uri, getApplicationProtocols());
        } catch (IOException e) {
            throw new HttpSendException("创建连接失败 !!!", e);
        }

        var useHttp2 = false;

        if (tcpSocket instanceof SSLSocket sslSocket) {
            var applicationProtocol = sslSocket.getApplicationProtocol();
            useHttp2 = "h2".equals(applicationProtocol);
        }

        if (useHttp2) {
            return new Http2ClientConnection(tcpSocket, options.http2ClientConnectionOptions()).sendRequest(this, mediaWriter).waitResponse();
        } else {
            // 仅当 http 协议 (不是 SSL) 并且开启代理的时候才使用 绝对路径
            if (!(tcpSocket instanceof SSLSocket) && options.proxy() != null) {
                this.requestTargetForm = ABSOLUTE_FORM;
            }
            try {
                return new Http1ClientConnection(tcpSocket, options.http1ClientConnectionOptions()).sendRequest(this, mediaWriter).waitResponse();
            } catch (IOException e) {
                throw new HttpSendException("发送 HTTP 请求失败 !!!", e);
            }
        }

    }

    @Override
    public ScxHttpClientResponse send(MediaWriter mediaWriter) throws IllegalSenderStateException, HttpSendException {
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
    public ScxHttpHeadersWritable headers() {
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
        this.headers = ScxHttpHeaders.of(headers);
        return this;
    }

    @Override
    public RequestTargetForm requestTargetForm() {
        return requestTargetForm;
    }

    @Override
    public HttpClientRequest requestTargetForm(RequestTargetForm requestTargetForm) {
        this.requestTargetForm = requestTargetForm;
        return this;
    }

    @Override
    public void _setSenderStatus(ScxHttpSenderStatus senderStatus) {
        this.senderStatus = senderStatus;
    }

}
