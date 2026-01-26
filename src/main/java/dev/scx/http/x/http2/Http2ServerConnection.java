package dev.scx.http.x.http2;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.http.x.SocketIO;

/// 占位 (可能永远都不会实现)
public class Http2ServerConnection {

    private final SocketIO socketIO;

    public Http2ServerConnection(SocketIO socketIO, Http2ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) {
        this.socketIO = socketIO;
    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
