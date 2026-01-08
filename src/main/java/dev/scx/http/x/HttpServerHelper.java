package dev.scx.http.x;

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

}
