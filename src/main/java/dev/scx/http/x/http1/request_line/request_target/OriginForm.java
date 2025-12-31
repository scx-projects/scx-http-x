package dev.scx.http.x.http1.request_line.request_target;

import dev.scx.http.parameters.Parameters;
import dev.scx.http.uri.ScxURI;

import java.net.URI;
import java.net.URISyntaxException;

import static dev.scx.http.uri.ScxURIHelper.decodeQuery;

/// 例: `GET /index.html?foo=1 HTTP/1.1`
///
/// 用于大多数客户端请求
///
/// - 所有字段和 [ScxURI] 一样都是 存储的 "原始未编码" 值, 所以可以直接用于创建 [ScxURI]
///
/// @author scx567888
/// @version 0.0.1
public record OriginForm(String path, Parameters<String, String> query, String fragment) implements RequestTarget {

    public static OriginForm of(String origin) throws URISyntaxException {
        // 这里我们假定 origin 必定是 "/" 起始的
        // 我们借用 URI 来作为 解析器
        var u = new URI("scx://scx.dev" + origin);
        var path = u.getPath();
        var rawQuery = u.getRawQuery();

        // 根据 HTTP 规范, 不应该允许 fragment 但是我们这里选择支持
        var fragment = u.getFragment();

        return new OriginForm(path, decodeQuery(Parameters.of(), rawQuery), fragment);
    }

    @Override
    public ScxURI toScxURI() {
        return ScxURI.of()
            .path(path)
            .query(query)
            .fragment(fragment);
    }

    @Override
    public String encode() {
        return toScxURI().encode(true);
    }

}
