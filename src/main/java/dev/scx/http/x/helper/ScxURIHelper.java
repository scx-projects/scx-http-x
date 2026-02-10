package dev.scx.http.x.helper;

import dev.scx.http.uri.ScxURI;

import java.net.InetSocketAddress;

import static dev.scx.http.x.helper.SchemeHelper.getDefaultPort;

/// ScxURIHelper
///
/// @author scx567888
/// @version 0.0.1
public final class ScxURIHelper {

    public static InetSocketAddress toRemoteAddress(ScxURI uri) {
        var host = uri.host();
        var port = uri.port();
        if (port == null) {
            port = getDefaultPort(uri.scheme());
        }
        return new InetSocketAddress(host, port);
    }

}
