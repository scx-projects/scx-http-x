package dev.scx.http.x.http1;

import dev.scx.http.ScxHttpClientResponse;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.peer_info.PeerInfo;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteInput;

import static dev.scx.http.x.helper.PeerInfoHelper.getLocalPeer;
import static dev.scx.http.x.helper.PeerInfoHelper.getRemotePeer;

/// Http1ClientResponse
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientResponse implements ScxHttpClientResponse {

    /// 对外公开 connection 字段, 以便 实现更底层功能.
    public final Http1ClientConnection connection;

    private final ScxHttpStatusCode statusCode;
    private final HttpVersion version;
    private final String reasonPhrase;
    private final Http1Headers headers;
    private final Long bodyLength;
    private final ByteInput body;
    private final PeerInfo remotePeer;
    private final PeerInfo localPeer;

    public Http1ClientResponse(Http1StatusLine statusLine, Http1Headers headers, Long bodyLength, ByteInput bodyByteInput, Http1ClientConnection connection) {
        this.statusCode = statusLine.statusCode();
        this.version = statusLine.httpVersion();
        this.reasonPhrase = statusLine.reasonPhrase();
        this.headers = headers;
        this.bodyLength = bodyLength;
        this.body = bodyByteInput;
        this.connection = connection;
        this.remotePeer = getRemotePeer(this.connection.endpoint.socket);
        this.localPeer = getLocalPeer(this.connection.endpoint.socket);
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return statusCode;
    }

    @Override
    public HttpVersion version() {
        return version;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public ScxHttpHeaders headers() {
        return headers;
    }

    @Override
    public Long bodyLength() {
        return bodyLength;
    }

    @Override
    public ByteInput body() {
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

}
