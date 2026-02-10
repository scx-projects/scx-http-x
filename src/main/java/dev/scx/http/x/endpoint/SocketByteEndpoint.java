package dev.scx.http.x.endpoint;

import dev.scx.io.ByteInput;
import dev.scx.io.ByteOutput;
import dev.scx.io.endpoint.ByteEndpoint;

import java.io.IOException;
import java.net.Socket;

import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.ScxIO.createByteOutput;

/// SocketByteEndpoint
///
/// @author scx567888
/// @version 0.0.1
public final class SocketByteEndpoint implements ByteEndpoint {

    public final Socket socket;
    public final ByteInput in;
    public final ByteOutput out;

    public SocketByteEndpoint(Socket socket) throws IOException {
        this.socket = socket;
        this.in = createByteInput(this.socket.getInputStream());
        this.out = createByteOutput(this.socket.getOutputStream());
    }

    @Override
    public ByteInput in() {
        return in;
    }

    @Override
    public ByteOutput out() {
        return out;
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }

    public void closeQuietly() {
        try {
            this.close();
        } catch (IOException _) {
            // 忽略关闭的异常
        }
    }

}
