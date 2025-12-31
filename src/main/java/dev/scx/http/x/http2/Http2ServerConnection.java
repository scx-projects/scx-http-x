package dev.scx.http.x.http2;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.io.ByteInput;
import dev.scx.io.ByteOutput;

import java.io.IOException;
import java.net.Socket;

import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.ScxIO.createByteOutput;

/// 占位 (可能永远都不会实现)
public class Http2ServerConnection implements AutoCloseable {

    private final Socket tcpSocket;
    private final ByteInput dataReader;
    private final ByteOutput dataWriter;

    public Http2ServerConnection(Socket tcpSocket, Http2ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) throws IOException {
        this.tcpSocket = tcpSocket;
        this.dataReader = createByteInput(this.tcpSocket.getInputStream());
        this.dataWriter = createByteOutput(this.tcpSocket.getOutputStream());
    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws IOException {
        tcpSocket.close();
    }

}
