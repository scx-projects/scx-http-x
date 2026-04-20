package dev.scx.http.x.http1.request_line.request_target;

import dev.scx.http.uri.ScxURI;

import java.net.URI;
import java.net.URISyntaxException;

/// 例: `CONNECT example.com:443 HTTP/1.1`
///
/// 仅与 `CONNECT` 方法一起使用
///
/// @author scx567888
/// @version 0.0.1
public record AuthorityForm(String host, int port) implements RequestTarget {

    public static AuthorityForm of(String authority) throws URISyntaxException {
        var colon = authority.lastIndexOf(":");
        if (colon == -1) {
            throw new URISyntaxException(authority, "Invalid authority");
        }
        var hostStr = authority.substring(0, colon);
        var portStr = authority.substring(colon + 1);

        String host;
        int port;

        // 我们需要校验 portStr 必须存在 + 是一个数组 + 范围在 1 - 65535 中
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                throw new URISyntaxException(authority, "Invalid port");
            }
        } catch (NumberFormatException e) {
            // (这里已经间接校验 portStr = "" 的情况了)
            throw new URISyntaxException(authority, "Invalid port");
        }

        // 我们需要校验 hostStr 是一个合法的 host, 必须是 RFC3986 意义上的合法 host
        // 这里借用 new URI 来完成校验, 不报错即是合法 host (这里已经间接校验 hostStr = "" 的情况了)
        new URI("scx", hostStr, null, null);
        host = hostStr;

        return new AuthorityForm(host, port);
    }

    @Override
    public ScxURI toScxURI() {
        return ScxURI.of()
            .host(host)
            .port(port);
    }

    @Override
    public String encode() {
        return host + ":" + port;
    }

}
