package dev.scx.http.x.http1;

import dev.scx.http.peer_info.PeerInfo;

import java.net.InetSocketAddress;
import java.net.Socket;

/// PeerInfoHelper
///
/// @author scx567888
/// @version 0.0.1
final class PeerInfoHelper {

    public static PeerInfo getRemotePeer(Socket tcpSocket) {
        var address = (InetSocketAddress) tcpSocket.getRemoteSocketAddress();
        // todo 未完成 tls 信息没有写入
        return PeerInfo.of().address(address).host(address.getHostString()).port(address.getPort());
    }

    public static PeerInfo getLocalPeer(Socket tcpSocket) {
        var address = (InetSocketAddress) tcpSocket.getLocalSocketAddress();
        // todo 未完成 tls 信息没有写入
        return PeerInfo.of().address(address).host(address.getHostString()).port(address.getPort());
    }

}
