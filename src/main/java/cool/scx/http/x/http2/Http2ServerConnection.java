package cool.scx.http.x.http2;

import dev.scx.function.Function1Void;
import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.error_handler.ScxHttpServerErrorHandler;

import java.net.Socket;

// 待完成
public class Http2ServerConnection {

    public Http2ServerConnection(Socket tcpSocket, Http2ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) {

    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
