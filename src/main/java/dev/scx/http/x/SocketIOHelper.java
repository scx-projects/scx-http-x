package dev.scx.http.x;

import java.io.IOException;
import java.net.Socket;

public final class SocketIOHelper {

    /// 失败内部会关闭 Socket
    public static SocketIO createSocketIO(Socket tcpSocket) throws IOException {
        try {
            return new SocketIO(tcpSocket);
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
