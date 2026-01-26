package dev.scx.http.x.helper;

import dev.scx.http.peer_info.PeerInfo;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.net.Socket;

/// PeerInfoHelper
///
/// @author scx567888
/// @version 0.0.1
public final class PeerInfoHelper {

    public static PeerInfo getRemotePeer(Socket tcpSocket) {
        var address = tcpSocket.getRemoteSocketAddress();
        var peerInfo = PeerInfo.of(address);
        if (tcpSocket instanceof SSLSocket ssl) {
            var session = ssl.getSession();
            try {
                peerInfo.tlsPrincipal(session.getPeerPrincipal());
            } catch (SSLPeerUnverifiedException _) {
                // 忽略异常
            }
            try {
                peerInfo.tlsCertificates(session.getPeerCertificates());
            } catch (SSLPeerUnverifiedException ignored) {
                // 忽略异常
            }
        }
        return peerInfo;
    }

    public static PeerInfo getLocalPeer(Socket tcpSocket) {
        var address = tcpSocket.getLocalSocketAddress();
        var peerInfo = PeerInfo.of(address);
        if (tcpSocket instanceof SSLSocket ssl) {
            var session = ssl.getSession();
            peerInfo.tlsPrincipal(session.getLocalPrincipal());
            peerInfo.tlsCertificates(session.getLocalCertificates());
        }
        return peerInfo;
    }

}
