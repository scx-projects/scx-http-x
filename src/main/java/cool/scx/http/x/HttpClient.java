package cool.scx.http.x;

import cool.scx.http.ScxHttpClient;
import cool.scx.http.uri.ScxURI;
import cool.scx.http.x.http1.Http1ClientConnection;
import cool.scx.tcp.ScxTCPSocket;
import cool.scx.tcp.TCPClient;
import cool.scx.tcp.tls.TLS;

import java.io.IOException;

import static cool.scx.http.media.empty.EmptyMediaWriter.EMPTY_MEDIA_WRITER;
import static cool.scx.http.method.HttpMethod.CONNECT;
import static cool.scx.http.status_code.HttpStatusCode.OK;
import static cool.scx.http.x.HttpXHelper.*;
import static cool.scx.http.x.http1.request_line.RequestTargetForm.AUTHORITY_FORM;

/// HttpClient
///
/// @author scx567888
/// @version 0.0.1
public class HttpClient implements ScxHttpClient {

    private final HttpClientOptions options;

    public HttpClient(HttpClientOptions options) {
        this.options = options;
    }

    public HttpClient() {
        this(new HttpClientOptions());
    }

    private static ScxTCPSocket configTLS(ScxTCPSocket tcpSocket, TLS tls, ScxURI uri, String... applicationProtocols) throws IOException {
        // 手动升级
        try {
            tcpSocket.upgradeToTLS(tls);
        } catch (IOException e) {
            tryCloseSocket(tcpSocket, e);
            throw new IOException("升级到 TLS 时发生错误 !!!", e);
        }
        tcpSocket.tlsManager().setUseClientMode(true);
        tcpSocket.tlsManager().setApplicationProtocols(applicationProtocols);
        tcpSocket.tlsManager().setServerNames(uri.host());
        try {
            tcpSocket.startHandshake();
        } catch (IOException e) {
            tryCloseSocket(tcpSocket, e);
            throw new IOException("处理 TLS 握手 时发生错误 !!!", e);
        }
        return tcpSocket;
    }

    @Override
    public HttpClientRequest request() {
        return new HttpClientRequest(this);
    }

    public HttpClientOptions options() {
        return options;
    }

    // 创建一个 TCP 连接
    // todo 后期可以创建一个 连接池 用来复用 未断开的 tcp 连接
    public ScxTCPSocket createTCPSocket(ScxURI uri, String... applicationProtocols) throws IOException {
        //判断是否 tls
        var isTLS = checkIsTLS(uri);
        //判断是否使用代理
        var withProxy = options.proxy() != null;

        if (isTLS) {
            return withProxy ? createTLSTCPSocketWithProxy(uri, applicationProtocols) : createTLSTCPSocket(uri, applicationProtocols);
        } else {
            return withProxy ? createPlainTCPSocketWithProxy() : createPlainTCPSocket(uri);
        }

    }

    /// 创建 明文 socket
    public ScxTCPSocket createPlainTCPSocket(ScxURI uri) throws IOException {
        var tcpClient = new TCPClient();
        var remoteAddress = getRemoteAddress(uri);
        return tcpClient.connect(remoteAddress, options.timeout());
    }

    /// 创建 tls socket
    public ScxTCPSocket createTLSTCPSocket(ScxURI uri, String... applicationProtocols) throws IOException {
        var tcpClient = new TCPClient();
        var remoteAddress = getRemoteAddress(uri);
        var tcpSocket = tcpClient.connect(remoteAddress, options.timeout());
        //配置一下 tls
        return configTLS(tcpSocket, options.tls(), uri, applicationProtocols);
    }

    /// 创建 具有代理 的 明文 socket
    public ScxTCPSocket createPlainTCPSocketWithProxy() throws IOException {
        var tcpClient = new TCPClient();
        //我们连接代理地址
        var remoteAddress = options.proxy().proxyAddress();
        return tcpClient.connect(remoteAddress, options.timeout());
    }

    /// 创建 具有代理 的 tls socket
    public ScxTCPSocket createTLSTCPSocketWithProxy(ScxURI uri, String... applicationProtocols) throws IOException {
        //1, 我们明文连接代理地址
        var tcpSocket = createPlainTCPSocketWithProxy();

        //2, 和代理服务器 握手
        var proxyResponse = new Http1ClientConnection(tcpSocket, options.http1ClientConnectionOptions()).sendRequest(
                (HttpClientRequest) new HttpClientRequest(this)
                    .requestTargetForm(AUTHORITY_FORM)
                    .method(CONNECT)
                    .addHeader("proxy-connection", "keep-alive")
                    .uri(uri),
                EMPTY_MEDIA_WRITER
            )
            .waitResponse();

        //3, 握手成功
        if (proxyResponse.statusCode() != OK) {
            throw new IOException("代理连接失败 :" + proxyResponse.statusCode());
        }

        //4, 这种情况下我们信任所有证书
        return configTLS(tcpSocket, TLS.ofTrustAny(), uri, applicationProtocols);
    }

}
