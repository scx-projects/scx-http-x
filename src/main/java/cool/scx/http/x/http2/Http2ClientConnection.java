package cool.scx.http.x.http2;

import cool.scx.http.media.MediaWriter;

import java.net.Socket;

// 待完成
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
