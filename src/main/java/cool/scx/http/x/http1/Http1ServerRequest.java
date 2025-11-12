package cool.scx.http.x.http1;

import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.body.ScxHttpBody;
import cool.scx.http.method.ScxHttpMethod;
import cool.scx.http.peer_info.PeerInfo;
import cool.scx.http.uri.ScxURI;
import cool.scx.http.version.HttpVersion;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.request_line.Http1RequestLine;
import cool.scx.io.ByteInput;

import static cool.scx.http.x.http1.Http1Helper.*;
import static cool.scx.http.x.http1.headers.connection.Connection.CLOSE;

/// HTTP/1.1 ServerRequest
///
/// @author scx567888
/// @version 0.0.1
public class Http1ServerRequest implements ScxHttpServerRequest {

    public final Http1ServerConnection connection;

    private final ScxHttpMethod method;
    private final ScxURI uri;
    private final HttpVersion version;
    private final Http1Headers headers;
    private final ScxHttpBody body;
    private final PeerInfo remotePeer;
    private final PeerInfo localPeer;
    private final Http1ServerResponse response;

    public Http1ServerRequest(Http1ServerConnection connection, Http1RequestLine requestLine, Http1Headers headers, ByteInput bodyByteInput) {
        this.connection = connection;
        this.method = requestLine.method();
        this.uri = inferURI(requestLine.requestTarget(), headers, connection.tcpSocket);
        this.version = requestLine.httpVersion();
        this.headers = headers;
        this.body = new Http1Body(bodyByteInput, this.headers);
        this.remotePeer = getRemotePeer(connection.tcpSocket);
        this.localPeer = getLocalPeer(connection.tcpSocket);
        this.response = new Http1ServerResponse(connection, this);
    }

    @Override
    public Http1ServerResponse response() {
        return this.response;
    }

    @Override
    public ScxHttpMethod method() {
        return method;
    }

    @Override
    public ScxURI uri() {
        return uri;
    }

    @Override
    public HttpVersion version() {
        return version;
    }

    @Override
    public Http1Headers headers() {
        return headers;
    }

    @Override
    public ScxHttpBody body() {
        return body;
    }

    @Override
    public PeerInfo remotePeer() {
        return remotePeer;
    }

    @Override
    public PeerInfo localPeer() {
        return localPeer;
    }

    public boolean isKeepAlive() {
        return headers.connection() != CLOSE;
    }

}
