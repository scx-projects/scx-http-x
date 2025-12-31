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

    /// 注意要小心处理 异常和 close.
    public static SSLSocket configServerTLS(Socket tcpSocket, TLS tls, BiFunction<SSLSocket, List<String>, String> protocolSelector) throws IOException {
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
            throw e;
        }

        // 2, 配置 参数
        sslSocket.setUseClientMode(false);

        sslSocket.setHandshakeApplicationProtocolSelector(protocolSelector);

        // 3, 开始握手
        try {
            sslSocket.startHandshake();
        } catch (IOException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }

        return sslSocket;

    }

}
