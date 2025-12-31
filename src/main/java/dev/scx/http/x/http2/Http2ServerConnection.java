package dev.scx.http.x.http2;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;

import java.io.IOException;
import java.net.Socket;

/// 占位 (可能永远都不会实现)
public class Http2ServerConnection implements AutoCloseable {

    private final Socket tcpSocket;

    public Http2ServerConnection(Socket tcpSocket, Http2ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) {
        this.tcpSocket = tcpSocket;
    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws IOException {
        tcpSocket.close();
    }

}
