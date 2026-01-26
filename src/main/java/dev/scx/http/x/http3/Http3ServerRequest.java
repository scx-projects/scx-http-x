package dev.scx.http.x.http3;

import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.ScxHttpServerResponse;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.peer_info.PeerInfo;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.version.HttpVersion;
import dev.scx.io.ByteInput;

import static dev.scx.http.version.HttpVersion.HTTP_3;

/// 占位 (可能永远都不会实现)
public class Http3ServerRequest implements ScxHttpServerRequest {

    @Override
    public ScxHttpServerResponse response() {
        return null;
    }

    @Override
    public ScxHttpMethod method() {
        return null;
    }

    @Override
    public ScxURI uri() {
        return null;
    }

    @Override
    public HttpVersion version() {
        return HTTP_3;
    }

    @Override
    public ScxHttpHeaders headers() {
        return null;
    }

    @Override
    public ByteInput body() {
        return null;
    }

    @Override
    public PeerInfo remotePeer() {
        return null;
    }

    @Override
    public PeerInfo localPeer() {
        return null;
    }

}
