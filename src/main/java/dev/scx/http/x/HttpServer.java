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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;

import static dev.scx.http.version.HttpVersion.HTTP_1_1;
import static dev.scx.http.version.HttpVersion.HTTP_2;
import static dev.scx.http.x.SocketIOHelper.createSocketIO;
import static dev.scx.http.x.tls.TLSHelper.configServerTLS;

/// Http 服务器
///
/// HTTP/2/3 是在错误目标驱动下, 引入系统性复杂度的协议; 它们在性能之外, 几乎在所有工程维度上都是倒退.
/// 所以此 HttpServer 目前甚至永远都不会提供 HTTP/2/3 支持.
///
/// @author scx567888
/// @version 0.0.1
public final class HttpServer implements ScxHttpServer {

    private final HttpServerOptions options;
    private final ScxTCPServer tcpServer;
    private Function1Void<ScxHttpServerRequest, ?> requestHandler;
    private ScxHttpServerErrorHandler errorHandler;

    public HttpServer(HttpServerOptions options) {
        this.options = options;
        this.tcpServer = new TCPServer(options.tcpServerOptions());
        this.tcpServer.onConnect(this::handle);
        this.requestHandler = null;
        this.errorHandler = null;
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

    private void handle(Socket tcpSocket) {
        // 1. 处理 TLS
        if (options.tls() != null) {
            try {
                tcpSocket = configServerTLS(tcpSocket, options.tls(), this::protocolSelector);
            } catch (IOException e) {
                // 这里的异常是 升级到 TLS 时的异常 属于噪音 无需处理.
                return;
            }
        }

        // 2, 创建 SocketIO.
        SocketIO socketIO;
        try {
            socketIO = createSocketIO(tcpSocket);
        } catch (IOException e) {
            // 这里的异常是 获取 流 时的异常 属于噪音 无需处理.
            return;
        }

        // 3, 检测是否使用 Http2
        var useHttp2 = false;

        if (socketIO.tcpSocket instanceof SSLSocket sslSocket) {
            var applicationProtocol = sslSocket.getApplicationProtocol();
            useHttp2 = HTTP_2.alpnValue().equals(applicationProtocol);
        }

        // 4, 根据协议不同选择不同的连接处理器
        if (useHttp2) {
            var http2ServerConnection = new Http2ServerConnection(socketIO, options.http2ServerConnectionOptions(), requestHandler, errorHandler);
            http2ServerConnection.start();
        } else {
            var http1ServerConnection = new Http1ServerConnection(socketIO, options.http1ServerConnectionOptions(), requestHandler, errorHandler);
            http1ServerConnection.requestNext();
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
