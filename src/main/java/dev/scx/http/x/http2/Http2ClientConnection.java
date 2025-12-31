package dev.scx.http.x.http2;

import dev.scx.http.media.MediaWriter;
import dev.scx.io.ByteInput;
import dev.scx.io.ByteOutput;

import java.io.IOException;
import java.net.Socket;

import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.ScxIO.createByteOutput;

/// 占位 (可能永远都不会实现)
public class Http2ClientConnection {

    private final Socket tcpSocket;
    private final ByteInput dataReader;
    private final ByteOutput dataWriter;

    public Http2ClientConnection(Socket tcpSocket, Http2ClientConnectionOptions options) throws IOException {
        this.tcpSocket = tcpSocket;
        this.dataReader = createByteInput(this.tcpSocket.getInputStream());
        this.dataWriter = createByteOutput(this.tcpSocket.getOutputStream());
    }

    public Http2ClientConnection sendRequest(Http2ClientRequest request, MediaWriter mediaWriter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Http2ClientResponse waitResponse() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
