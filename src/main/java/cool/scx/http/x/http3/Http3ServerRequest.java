package cool.scx.http.x.http3;

import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.ScxHttpServerResponse;
import cool.scx.http.body.ScxHttpBody;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.method.ScxHttpMethod;
import cool.scx.http.peer_info.PeerInfo;
import cool.scx.http.uri.ScxURI;
import cool.scx.http.version.HttpVersion;

import static cool.scx.http.version.HttpVersion.HTTP_3;

// 待实现
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
    public ScxHttpBody body() {
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
