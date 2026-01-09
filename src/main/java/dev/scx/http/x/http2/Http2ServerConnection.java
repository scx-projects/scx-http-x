package dev.scx.http.x.http2;

import dev.scx.http.x.HttpServerContext;
import dev.scx.http.x.SocketIO;

/// 占位 (可能永远都不会实现)
public class Http2ServerConnection {

    public static void start(SocketIO socketIO, HttpServerContext context) {
        socketIO.closeQuietly();
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
