package dev.scx.http.x;

import dev.scx.http.uri.ScxURI;
import dev.scx.http.x.http1.Http1ClientConnection;
import dev.scx.http.x.http1.Http1ClientConnectionOptions;
import dev.scx.http.x.http2.Http2ClientConnection;
import dev.scx.http.x.http2.Http2ClientConnectionOptions;
import dev.scx.tcp.tls.TLS;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import static dev.scx.http.x.http1.io.Http1Writer.getDefaultPort;

/// HttpClientHelper (内部工具类)
///
/// @author scx567888
/// @version 0.0.1
final class HttpClientHelper {

    /// 注意要小心处理 异常和 close.
    private static SSLSocket configClientTLS0(Socket tcpSocket, TLS tls, String host, String... applicationProtocols) throws IOException {
        // 1, 手动升级
        var sslSocket = tls.upgradeToTLS(tcpSocket);

        // 2, 配置 参数
        sslSocket.setUseClientMode(true);

        var sslParameters = sslSocket.getSSLParameters();

        sslParameters.setApplicationProtocols(applicationProtocols);
        sslParameters.setServerNames(List.of(new SNIHostName(host)));

        // 别忘了写回 参数
        sslSocket.setSSLParameters(sslParameters);

        // 3, 开始握手
        sslSocket.startHandshake();

        return sslSocket;

    }

    /// 失败内部会关闭 Socket
    public static SSLSocket configClientTLS(Socket tcpSocket, TLS tls, String host, String... applicationProtocols) throws IOException {
        try {
            return configClientTLS0(tcpSocket, tls, host, applicationProtocols);
        } catch (IOException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

    public static boolean checkIsTLS(String scheme) {
        scheme = scheme.toLowerCase();
        return switch (scheme) {
            case "http", "ws" -> false;
            case "https", "wss" -> true;
            default -> throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        };
    }

    public static InetSocketAddress getRemoteAddress(ScxURI uri) {
        var host = uri.host();
        var port = uri.port();
        if (port == null) {
            port = getDefaultPort(uri.scheme());
        }
        return new InetSocketAddress(host, port);
    }

}
