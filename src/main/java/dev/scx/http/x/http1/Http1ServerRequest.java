package dev.scx.http.x.http1;

import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.media.MediaReader;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.peer_info.PeerInfo;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import static dev.scx.http.x.http1.Http1ServerHelper.getLocalPeer;
import static dev.scx.http.x.http1.Http1ServerHelper.getRemotePeer;
import static dev.scx.http.x.http1.headers.connection.Connection.CLOSE;

/// Http1ServerRequest
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerRequest implements ScxHttpServerRequest {

    /// 对外公开 connection 字段, 以便 实现更底层功能.
    public final Http1ServerConnection connection;

    private final ScxHttpMethod method;
    private final ScxURI uri;
    private final HttpVersion version;
    private final Http1Headers headers;
    private final ByteInput body;
    private final PeerInfo remotePeer;
    private final PeerInfo localPeer;
    private final Http1ServerResponse response;

    public Http1ServerRequest(Http1RequestLine requestLine, Http1Headers headers, ByteInput bodyByteInput, Http1ServerConnection connection) {
        this.method = requestLine.method();
        this.uri = requestLine.requestTarget().toScxURI();
        this.version = requestLine.httpVersion();
        this.headers = headers;
        this.body = bodyByteInput;
        this.connection = connection;
        this.remotePeer = getRemotePeer(this.connection.socketIO.tcpSocket);
        this.localPeer = getLocalPeer(this.connection.socketIO.tcpSocket);
        this.response = new Http1ServerResponse(this, this.connection);
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
    public ByteInput body() {
        return body;
    }

    @Override
    public <T> T as(MediaReader<T> mediaReader) throws ScxIOException, AlreadyClosedException {
        return mediaReader.read(body, headers);
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
