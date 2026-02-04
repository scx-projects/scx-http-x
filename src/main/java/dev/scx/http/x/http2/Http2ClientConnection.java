package dev.scx.http.x.http2;

import dev.scx.http.sender.ScxHttpSender.BodyWriter;
import dev.scx.http.x.SocketIO;

/// 占位 (可能永远都不会实现)
public class Http2ClientConnection {

    private final SocketIO socketIO;

    public Http2ClientConnection(SocketIO socketIO, Http2ClientConnectionOptions options) {
        this.socketIO = socketIO;
    }

    public Http2ClientConnection sendRequest(Http2ClientRequest request, BodyWriter bodyWriter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Http2ClientResponse readResponse() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
