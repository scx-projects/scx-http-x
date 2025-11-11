package cool.scx.http.x.proxy;

import java.net.SocketAddress;

/// ProxyImpl
///
/// @author scx567888
/// @version 0.0.1
class ProxyImpl implements Proxy {

    private final SocketAddress proxyAddress;

    public ProxyImpl(SocketAddress proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    @Override
    public SocketAddress proxyAddress() {
        return this.proxyAddress;
    }

}
