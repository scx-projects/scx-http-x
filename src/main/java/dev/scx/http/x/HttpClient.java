package dev.scx.http.x;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.ScxHttpClient;
import dev.scx.http.sender.ScxHttpReceiveException;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.helper.SocketHelper;
import dev.scx.http.x.http1.Http1ClientConnection;
import dev.scx.tcp.TCPClient;
import dev.scx.tcp.tls.TLS;

import java.io.IOException;
import java.net.Socket;

import static dev.scx.http.media.empty.EmptyMediaWriter.EMPTY_MEDIA_WRITER;
import static dev.scx.http.method.HttpMethod.CONNECT;
import static dev.scx.http.status_code.HttpStatusCode.OK;
import static dev.scx.http.version.HttpVersion.HTTP_1_1;
import static dev.scx.http.x.helper.SchemeHelper.isTLS;
import static dev.scx.http.x.helper.ScxURIHelper.toRemoteAddress;
import static dev.scx.http.x.helper.SocketByteEndpointHelper.createSocketByteEndpoint;
import static dev.scx.http.x.helper.TLSHelper.configClientTLS;

/// HttpClient
///
/// 我们不复用任何连接(指使用连接池) 将来也不会, 因为收益太小.
///
/// ### 关于资源泄露:
///
/// - 所有 createSocket 方法, 在失败路径均不存在资源泄露.
///
/// @author scx567888
/// @version 0.0.1
public final class HttpClient implements ScxHttpClient {

    private final HttpClientOptions options;

    public HttpClient(HttpClientOptions options) {
        this.options = options;
    }

    public HttpClient() {
        this(new HttpClientOptions());
    }

    @Override
    public HttpClientRequest request(HttpVersion... httpVersions) {
        return new HttpClientRequest(this, httpVersions);
    }

    public HttpClientOptions options() {
        return options;
    }

    /// 创建一个 TCP 连接
    public Socket createSocket(ScxURI uri, String... applicationProtocols) throws IOException, ScxHttpSendException, ScxWrappedException {
        // 判断是否是 tls
        var useTLS = isTLS(uri.scheme());
        // 判断是否使用代理
        var useProxy = options.proxy() != null;

        if (useProxy) {
            return useTLS ?
                createTLSSocketWithProxy(uri, applicationProtocols) :
                createPlainSocketWithProxy();
        } else {
            return useTLS ?
                createTLSSocket(uri, applicationProtocols) :
                createPlainSocket(uri);
        }

    }

    /// 创建 明文 socket
    private Socket createPlainSocket(ScxURI uri) throws IOException {
        var tcpClient = new TCPClient();
        var remoteAddress = toRemoteAddress(uri);
        var tcpSocket = tcpClient.connect(remoteAddress, options.timeout());

        // 配置 Socket
        SocketHelper.configSocket(tcpSocket, options().tcpNoDelay());

        return tcpSocket;
    }

    /// 创建 tls socket
    private Socket createTLSSocket(ScxURI uri, String... applicationProtocols) throws IOException {
        var tcpSocket = createPlainSocket(uri);
        // 配置一下 tls
        return configClientTLS(tcpSocket, options.tls(), uri.host(), applicationProtocols);
    }

    /// 创建 具有代理 的 明文 socket
    private Socket createPlainSocketWithProxy() throws IOException {
        var tcpClient = new TCPClient();
        // 我们连接代理地址
        var remoteAddress = options.proxy().proxyAddress();
        var tcpSocket = tcpClient.connect(remoteAddress, options.timeout());

        // 配置 Socket
        SocketHelper.configSocket(tcpSocket, options().tcpNoDelay());

        return tcpSocket;
    }

    /// 创建 具有代理 的 tls socket
    private Socket createTLSSocketWithProxy(ScxURI uri, String... applicationProtocols) throws IOException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        // 1, 我们明文连接代理地址
        var tcpSocket = createPlainSocketWithProxy();

        var endpoint = createSocketByteEndpoint(tcpSocket);

        // 2, 和代理服务器 握手 (Http1ClientConnection 保证不存在资源泄漏)
        var proxyResponse = new Http1ClientConnection(endpoint, options.http1ClientConnectionOptions())
            .sendRequest(
                (HttpClientRequest) new HttpClientRequest(this, HTTP_1_1)
                    .method(CONNECT)
                    .addHeader("proxy-connection", "keep-alive")
                    .uri(uri),
                EMPTY_MEDIA_WRITER
            )
            .readResponse();

        // 3, 消耗掉 可能存在的响应体.
        String str;
        try {
            str = proxyResponse.asString();
        } catch (Throwable e) {
            endpoint.closeQuietly();
            throw new IOException("Failed to read proxy response", e);
        }

        // 4, 检查握手是否成功
        if (proxyResponse.statusCode() != OK) {
            // 失败需要关闭连接
            endpoint.closeQuietly();
            throw new IOException("代理拒绝连接 : " + proxyResponse.statusCode() + " 响应 : " + str);
        }

        // 5, 这种情况下我们信任所有证书
        return configClientTLS(tcpSocket, TLS.ofTrustAny(), uri.host(), applicationProtocols);
    }

}
