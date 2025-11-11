package cool.scx.http.x.http2;

import cool.scx.function.Function1Void;
import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.error_handler.ScxHttpServerErrorHandler;
import cool.scx.tcp.ScxTCPSocket;

// todo 待完成
public class Http2ServerConnection {

    public Http2ServerConnection(ScxTCPSocket tcpSocket, Http2ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) {

    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
