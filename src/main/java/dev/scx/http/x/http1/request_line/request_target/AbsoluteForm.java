package dev.scx.http.x.http1.request_line.request_target;

import dev.scx.http.parameters.Parameters;
import dev.scx.http.uri.ScxURI;

import java.net.URI;
import java.net.URISyntaxException;

import static dev.scx.http.uri.ScxURIHelper.decodeQuery;

/// 例: `GET http://example.com/index.html HTTP/1.1`
///
/// 通过 `HTTP代理` 发出请求时使用
///
/// - 所有字段和 [ScxURI] 一样都是 存储的 "原始未编码" 值, 所以可以直接用于创建 [ScxURI]
///
/// @author scx567888
/// @version 0.0.1
public record AbsoluteForm(String scheme,
                           String host,
                           Integer port,
                           String path,
                           Parameters<String, String> query,
                           String fragment) implements RequestTarget {

    public static AbsoluteForm of(String absolute) throws URISyntaxException {
        // 我们借用 URI 来作为 解析器
        var u = new URI(absolute);

        var scheme = u.getScheme();
        var host = u.getHost();
        var port = u.getPort();
        var path = u.getPath();
        var rawQuery = u.getRawQuery();

        // 根据 HTTP 规范, 不应该允许 fragment 但是我们这里选择支持
        var fragment = u.getFragment();

        if (scheme == null) {
            throw new URISyntaxException(absolute, "scheme is null");
        }

        if (host == null) {
            throw new URISyntaxException(absolute, "host is null");
        }

        return new AbsoluteForm(
            scheme,
            host,
            port == -1 ? null : port,
            path,
            decodeQuery(Parameters.of(), rawQuery),
            fragment
        );
    }

    @Override
    public ScxURI toScxURI() {
        return ScxURI.of()
            .scheme(scheme)
            .host(host)
            .port(port)
            .path(path)
            .query(query)
            .fragment(fragment);
    }

    @Override
    public String encode() {
        return toScxURI().encode(true);
    }

}
