package dev.scx.http.x.http2;

import dev.scx.http.media.MediaWriter;

import java.net.Socket;

/// 占位 (可能永远都不会实现)
public class Http2ClientConnection {

    public Http2ClientConnection(Socket tcpSocket, Http2ClientConnectionOptions options) {

    }

    public Http2ClientConnection sendRequest(Http2ClientRequest request, MediaWriter mediaWriter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Http2ClientResponse waitResponse() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
