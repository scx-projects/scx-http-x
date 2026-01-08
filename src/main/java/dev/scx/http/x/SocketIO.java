package dev.scx.http.x;

import dev.scx.io.ByteInput;
import dev.scx.io.ByteOutput;

import java.io.IOException;
import java.net.Socket;

import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.ScxIO.createByteOutput;

public final class SocketIO implements AutoCloseable {

    public final Socket tcpSocket;
    public final ByteInput in;
    public final ByteOutput out;

    public SocketIO(Socket tcpSocket) throws IOException {
        this.tcpSocket = tcpSocket;
        this.in = createByteInput(this.tcpSocket.getInputStream());
        this.out = createByteOutput(this.tcpSocket.getOutputStream());
    }

    @Override
    public void close() throws IOException {
        this.tcpSocket.close();
    }

    public void closeQuietly() {
        try {
            this.close();
        } catch (IOException _) {
            // 忽略关闭的异常
        }
    }

}
