package dev.scx.http.x.helper;

import dev.scx.http.x.endpoint.SocketByteEndpoint;

import java.io.IOException;
import java.net.Socket;

/// SocketByteEndpointHelper
///
/// @author scx567888
/// @version 0.0.1
public final class SocketByteEndpointHelper {

    /// 失败内部会关闭 Socket
    public static SocketByteEndpoint createSocketByteEndpoint(Socket tcpSocket) throws IOException {
        try {
            return new SocketByteEndpoint(tcpSocket);
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
