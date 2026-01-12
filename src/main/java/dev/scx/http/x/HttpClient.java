package dev.scx.http.x;

import dev.scx.http.ScxHttpClient;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.Http1ClientConnection;
import dev.scx.tcp.TCPClient;
import dev.scx.tcp.tls.TLS;

import java.io.IOException;
import java.net.Socket;

import static dev.scx.http.media.empty.EmptyMediaWriter.EMPTY_MEDIA_WRITER;
import static dev.scx.http.method.HttpMethod.CONNECT;
import static dev.scx.http.status_code.HttpStatusCode.OK;
import static dev.scx.http.version.HttpVersion.HTTP_1_1;
import static dev.scx.http.x.HttpClientHelper.*;
import static dev.scx.http.x.SocketIOHelper.createSocketIO;

/// HttpClient
///
/// 我们不复用任何连接(指使用连接池) 将来也不会, 因为收益太小.
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
    public Socket createSocket(ScxURI uri, String... applicationProtocols) throws IOException {
        // 判断是否是 tls
        var isTLS = checkIsTLS(uri.scheme());

        // 判断是否使用代理
        if (options.proxy() != null) {
            return isTLS ?
                createTLSSocketWithProxy(uri, applicationProtocols) :
                createPlainSocketWithProxy();
        } else {
            return isTLS ?
                createTLSSocket(uri, applicationProtocols) :
                createPlainSocket(uri);
        }

    }

    /// 创建 明文 socket
    public Socket createPlainSocket(ScxURI uri) throws IOException {
        var tcpClient = new TCPClient();
        var remoteAddress = getRemoteAddress(uri);
        return tcpClient.connect(remoteAddress, options.timeout());
    }

    /// 创建 tls socket
    public Socket createTLSSocket(ScxURI uri, String... applicationProtocols) throws IOException {
        var tcpSocket = createPlainSocket(uri);
        // 配置一下 tls
        return configClientTLS(tcpSocket, options.tls(), uri.host(), applicationProtocols);
    }

    /// 创建 具有代理 的 明文 socket
    public Socket createPlainSocketWithProxy() throws IOException {
        var tcpClient = new TCPClient();
        // 我们连接代理地址
        var remoteAddress = options.proxy().proxyAddress();
        return tcpClient.connect(remoteAddress, options.timeout());
    }

    /// 创建 具有代理 的 tls socket
    public Socket createTLSSocketWithProxy(ScxURI uri, String... applicationProtocols) throws IOException {
        // 1, 我们明文连接代理地址
        var tcpSocket = createPlainSocketWithProxy();

        var socketIO = createSocketIO(tcpSocket);

        // 2, 和代理服务器 握手
        var proxyResponse = new Http1ClientConnection(socketIO, options.http1ClientConnectionOptions())
            .sendRequest(
                (HttpClientRequest) new HttpClientRequest(this, HTTP_1_1)
                    .method(CONNECT)
                    .addHeader("proxy-connection", "keep-alive")
                    .uri(uri),
                EMPTY_MEDIA_WRITER
            )
            .readResponse();

        // 3, 握手成功
        if (proxyResponse.statusCode() != OK) {
            try {
                tcpSocket.close();
            } catch (IOException _) {
                // 忽略这个异常 (没什么意义)
            }
            throw new IOException("代理连接失败 :" + proxyResponse.statusCode());
        }

        // 4, 这种情况下我们信任所有证书
        return configClientTLS(tcpSocket, TLS.ofTrustAny(), uri.host(), applicationProtocols);
    }

}
