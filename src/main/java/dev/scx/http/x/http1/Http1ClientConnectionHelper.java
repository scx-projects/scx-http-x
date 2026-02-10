package dev.scx.http.x.http1;

import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.request_line.request_target.*;

import static dev.scx.http.headers.HttpHeaderName.HOST;
import static dev.scx.http.method.HttpMethod.*;
import static dev.scx.http.x.helper.SchemeHelper.getDefaultPort;
import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;

/// Http1ClientConnectionHelper
///
/// @author scx567888
/// @version 0.0.1
final class Http1ClientConnectionHelper {

    /// 检查请求是否 存在请求体
    public static boolean checkRequestHasBody(ScxHttpMethod method) {
        return GET != method;
    }

    /// 创建 RequestTarget
    public static RequestTarget createRequestTarget(ScxHttpMethod method, ScxURI uri, boolean useProxy) {
        var scheme = uri.scheme();
        var host = uri.host();
        var port = uri.port();
        var path = uri.path();
        var query = uri.query();
        var fragment = uri.fragment();
        if (method == CONNECT) {
            if (port == null) {
                port = getDefaultPort(scheme);
            }
            return new AuthorityForm(host, port);
        } else if (method == OPTIONS) {
            // 如果 uri 所有组件都是 null 就表示 是 AsteriskForm
            if (scheme == null && host == null && port == null && path == null && query.isEmpty() && fragment == null) {
                return AsteriskForm.of();
            }
        }
        if (useProxy) {
            return new AbsoluteForm(scheme, host, port, path, query, fragment);
        } else {
            // OriginForm 必须 / 起始, 我们在此处 处理 null 和 "" -> "/" 的兼容
            var finalPath = path == null || path.isEmpty() ? "/" : path;
            return new OriginForm(finalPath, query, fragment);
        }
    }

    public static Http1RequestLine createRequestLine(Http1ClientRequest request) {
        // 0, 准备参数
        var method = request.method();
        var uri = request.uri();
        var requestTarget = createRequestTarget(method, uri, request._useProxy());

        // 1, 创建 请求行
        return new Http1RequestLine(method, requestTarget);
    }

    public static Http1Headers configRequestHeaders(Http1ClientRequest request, Long expectedLength) {
        var method = request.method();
        var uri = request.uri();
        var headers = request.headers();

        // 处理头相关
        // 1, 处理 HOST 相关
        if (!headers.contains(HOST)) {
            var port = uri.port();
            if (port != null) {
                headers.set(HOST, uri.host() + ":" + port);
            } else {
                headers.set(HOST, uri.host());
            }
        }

        // 2, 处理 body 相关
        if (expectedLength == null) {// 表示不知道 body 的长度
            // 如果用户已经手动设置了 Content-Length, 我们便不再设置 分块传输
            if (headers.contentLength() == null) {
                headers.transferEncoding(CHUNKED);
            }
        } else if (expectedLength > 0) {// 拥有指定长度的响应体
            // 如果用户已经手动设置 分块传输, 我们便不再设置 Content-Length
            if (headers.transferEncoding() != CHUNKED) {
                headers.contentLength(expectedLength);
            }
        } else {
            // body 长度为 0 时 , 分两种情况
            // 1, 是需要明确写入 Content-Length : 0 的
            // 2, 是不需要写入任何长度相关字段
            var hasBody = checkRequestHasBody(method);
            if (hasBody) {
                // 这里同上, 进行分块传输判断
                if (headers.transferEncoding() != CHUNKED) {
                    headers.contentLength(expectedLength);
                }
            }
        }

        return headers;
    }

}
