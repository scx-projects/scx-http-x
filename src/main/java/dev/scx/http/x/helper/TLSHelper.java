package dev.scx.http.x.helper;

import dev.scx.tcp.tls.TLS;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.function.BiFunction;

/// TLSHelper
///
/// @author scx567888
/// @version 0.0.1
public final class TLSHelper {

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

}
