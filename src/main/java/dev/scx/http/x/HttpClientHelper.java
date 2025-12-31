package dev.scx.http.x;

import dev.scx.http.uri.ScxURI;
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

    /// 注意和 HttpServer 不同, 我们没办法使用 "try-with-resources" 帮我们兜底, 所以要小心处理 异常和 close.
    public static SSLSocket configTLS(Socket tcpSocket, TLS tls, ScxURI uri, String... applicationProtocols) throws IOException {
        SSLSocket sslSocket;
        // 1, 手动升级
        try {
            sslSocket = tls.upgradeToTLS(tcpSocket);
        } catch (IOException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw new IOException("升级到 TLS 时发生错误 !!!", e);
        }

        // 2, 配置 参数
        sslSocket.setUseClientMode(true);

        var sslParameters = sslSocket.getSSLParameters();

        sslParameters.setApplicationProtocols(applicationProtocols);
        sslParameters.setServerNames(List.of(new SNIHostName(uri.host())));

        // 别忘了写回 参数
        sslSocket.setSSLParameters(sslParameters);

        // 3, 开始握手
        try {
            sslSocket.startHandshake();
        } catch (IOException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw new IOException("处理 TLS 握手 时发生错误 !!!", e);
        }

        return sslSocket;
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
