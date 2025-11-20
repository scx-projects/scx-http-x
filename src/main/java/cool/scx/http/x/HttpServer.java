package cool.scx.http.x;

import cool.scx.function.Function1Void;
import cool.scx.http.ScxHttpServer;
import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.error_handler.ScxHttpServerErrorHandler;
import cool.scx.http.x.http1.Http1ServerConnection;
import cool.scx.http.x.http2.Http2ServerConnection;
import cool.scx.tcp.ScxTCPServer;
import cool.scx.tcp.TCPServer;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.lang.System.Logger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static cool.scx.http.version.HttpVersion.HTTP_1_1;
import static cool.scx.http.version.HttpVersion.HTTP_2;
import static java.lang.System.Logger.Level.TRACE;

/// Http 服务器
///
/// @author scx567888
/// @version 0.0.1
public final class HttpServer implements ScxHttpServer {

    private static final Logger LOGGER = System.getLogger(HttpServer.class.getName());

    private final HttpServerOptions options;
    private final ScxTCPServer tcpServer;
    private Function1Void<ScxHttpServerRequest, ?> requestHandler;
    private ScxHttpServerErrorHandler errorHandler;

    public HttpServer(HttpServerOptions options) {
        this.options = options;
        this.tcpServer = new TCPServer(options.tcpServerOptions());
        this.tcpServer.onConnect(this::handle);
    }

    public HttpServer() {
        this(new HttpServerOptions());
    }

    private void handle(Socket tcpSocket) throws IOException {
        // 1. 配置 tls
        if (options.tls() != null) {
            // 临时对象
            SSLSocket sslSocket;
            try {
                sslSocket = options.tls().upgradeToTLS(tcpSocket);
                // 重新赋值 tcpSocket 以便后续使用
                tcpSocket = sslSocket;
            } catch (IOException e) {
                try {
                    tcpSocket.close();
                } catch (IOException ex) {
                    e.addSuppressed(ex);
                }
                LOGGER.log(TRACE, "升级到 TLS 时发生错误 !!!", e);
                return;
            }
            sslSocket.setUseClientMode(false);
            sslSocket.setHandshakeApplicationProtocolSelector((_, protocols) -> options.enableHttp2() && protocols.contains(HTTP_2.alpnValue()) ? HTTP_2.alpnValue() : protocols.contains(HTTP_1_1.alpnValue()) ? HTTP_1_1.alpnValue() : null);
            // 开始握手
            try {
                sslSocket.startHandshake();
            } catch (IOException e) {
                try {
                    tcpSocket.close();
                } catch (IOException ex) {
                    e.addSuppressed(ex);
                }
                LOGGER.log(TRACE, "处理 TLS 握手 时发生错误 !!!", e);
                return;
            }
        }

        // 2, 检测是否使用 Http2
        var useHttp2 = false;

        if (tcpSocket instanceof SSLSocket sslSocket) {
            var applicationProtocol = sslSocket.getApplicationProtocol();
            useHttp2 = HTTP_2.alpnValue().equals(applicationProtocol);
        }

        // 3, 根据协议不同选择不同的连接处理器
        if (useHttp2) {
            // start 为阻塞方法
            new Http2ServerConnection(tcpSocket, options.http2ServerConnectionOptions(), requestHandler, errorHandler).start();
        } else {
            // 此处的 Http1 特指 HTTP/1.1
            // start 为阻塞方法
            new Http1ServerConnection(tcpSocket, options.http1ServerConnectionOptions(), requestHandler, errorHandler).start();
        }

        // 4, todo 这里用不用防御式 关闭 tcpSocket ? start 中一定是同步的吗?

    }

    @Override
    public ScxHttpServer onRequest(Function1Void<ScxHttpServerRequest, ?> requestHandler) {
        this.requestHandler = requestHandler;
        return this;
    }

    @Override
    public ScxHttpServer onError(ScxHttpServerErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public void start(SocketAddress localAddress) throws IOException {
        tcpServer.start(localAddress);
    }

    @Override
    public void stop() {
        tcpServer.stop();
    }

    @Override
    public InetSocketAddress localAddress() {
        return tcpServer.localAddress();
    }

    public HttpServerOptions options() {
        return options;
    }

}
