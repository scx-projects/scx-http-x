package dev.scx.http.x.helper;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/// SocketHelper
///
/// @author scx567888
/// @version 0.0.1
public final class SocketHelper {

    /// 失败内部会关闭 Socket
    public static void configSocket(Socket tcpSocket, boolean tcpNoDelay) throws SocketException {
        try {
            tcpSocket.setTcpNoDelay(tcpNoDelay);
        } catch (SocketException e) {
            try {
                tcpSocket.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

}
