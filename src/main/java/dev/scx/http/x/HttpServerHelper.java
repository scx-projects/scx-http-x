package dev.scx.http.x;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.http.x.http1.Http1ServerConnection;
import dev.scx.http.x.http1.Http1ServerConnectionOptions;
import dev.scx.http.x.http2.Http2ServerConnection;
import dev.scx.http.x.http2.Http2ServerConnectionOptions;
import dev.scx.tcp.tls.TLS;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.function.BiFunction;

/// HttpServerHelper (内部工具类)
///
/// @author scx567888
/// @version 0.0.1
final class HttpServerHelper {

    private static SSLSocket configServerTLS0(Socket tcpSocket, TLS tls, BiFunction<SSLSocket, List<String>, String> protocolSelector) throws IOException {
        // 1, 手动升级
        var sslSocket = tls.upgradeToTLS(tcpSocket);

        // 2, 配置 参数
        sslSocket.setUseClientMode(false);

        sslSocket.setHandshakeApplicationProtocolSelector(protocolSelector);

        // 3, 开始握手
        sslSocket.startHandshake();

        return sslSocket;
    }

    /// 失败内部会关闭 Socket
    public static SSLSocket configServerTLS(Socket tcpSocket, TLS tls, BiFunction<SSLSocket, List<String>, String> protocolSelector) throws IOException {
        try {
            return configServerTLS0(tcpSocket, tls, protocolSelector);
        } catch (IOException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

    /// 失败内部会关闭 Socket
    public static Http1ServerConnection createHttp1ServerConnection(Socket tcpSocket, Http1ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) throws IOException {
        try {
            return new Http1ServerConnection(tcpSocket, options, requestHandler, errorHandler);
        } catch (IOException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

    /// 失败内部会关闭 Socket
    public static Http2ServerConnection createHttp2ServerConnection(Socket tcpSocket, Http2ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) throws IOException {
        try {
            return new Http2ServerConnection(tcpSocket, options, requestHandler, errorHandler);
        } catch (IOException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

}
