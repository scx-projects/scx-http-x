package dev.scx.http.x;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServer;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.http.x.http1.Http1ServerConnection;
import dev.scx.http.x.http2.Http2ServerConnection;
import dev.scx.tcp.ScxTCPServer;
import dev.scx.tcp.TCPServer;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.lang.System.Logger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;

import static dev.scx.http.version.HttpVersion.HTTP_1_1;
import static dev.scx.http.version.HttpVersion.HTTP_2;
import static java.lang.System.Logger.Level.TRACE;

/// Http 服务器
///
/// HTTP/2/3 是在错误目标驱动下, 引入系统性复杂度的协议; 它们在性能之外, 几乎在所有工程维度上都是倒退.
/// 所以此 HttpServer 目前甚至永远都不会提供 HTTP/2/3 支持.
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

    private String protocolSelector(SSLSocket sslSocket, List<String> protocols) {
        if (options.enableHttp2()) {
            if (protocols.contains(HTTP_2.alpnValue())) {
                return HTTP_2.alpnValue();
            }
        }
        if (protocols.contains(HTTP_1_1.alpnValue())) {
            return HTTP_1_1.alpnValue();
        }
        return null;
    }

    private void handle(Socket socket) {
        // 兜底 Socket 关闭, 防止资源泄露
        try (socket) {
            handle0(socket);
        } catch (IOException _) {
            // 这里的 IOException 异常都是发生在 调用用户处理器之前, 所以没有什么记录价值.
        }
    }

    /// 注意这里 整个 handle0 是完全同步阻塞的.
    private void handle0(Socket tcpSocket) throws IOException {
        // 1. 处理 TLS
        if (options.tls() != null) {
            // 临时对象
            SSLSocket sslSocket;
            try {
                sslSocket = options.tls().upgradeToTLS(tcpSocket);
                // 重新赋值 tcpSocket 以便后续使用
                tcpSocket = sslSocket;
            } catch (IOException e) {
                LOGGER.log(TRACE, "升级到 TLS 时发生错误 !!!", e);
                return;
            }
            sslSocket.setUseClientMode(false);
            sslSocket.setHandshakeApplicationProtocolSelector(this::protocolSelector);
            // 开始握手
            try {
                sslSocket.startHandshake();
            } catch (IOException e) {
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
            var http2ServerConnection = new Http2ServerConnection(tcpSocket, options.http2ServerConnectionOptions(), requestHandler, errorHandler);
            // start 为阻塞方法
            http2ServerConnection.start();
        } else {
            // 此处的 Http1 特指 HTTP/1.1
            var http1ServerConnection = new Http1ServerConnection(tcpSocket, options.http1ServerConnectionOptions(), requestHandler, errorHandler);
            // start 为阻塞方法
            http1ServerConnection.start();
        }

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
