package dev.scx.http.x.http1.request_line;

import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.request_line.request_target.*;

import java.net.URISyntaxException;

import static dev.scx.http.method.HttpMethod.CONNECT;
import static dev.scx.http.method.HttpMethod.OPTIONS;
import static dev.scx.http.version.HttpVersion.HTTP_1_1;

/// Http1RequestLineHelper
///
/// @author scx567888
/// @version 0.0.1
final class Http1RequestLineHelper {

    /// 解析 请求行
    public static Http1RequestLine parseRequestLine(String requestLineStr) throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        var parts = requestLineStr.split(" ", -1);

        // 如果长度等于 2, 则可能是 HTTP/0.9 请求
        // 如果长度大于 3, 则可能是 路径中包含意外的空格
        // 但是此处我们没必要细化异常 全部抛出 InvalidHttpRequestLineException 异常
        if (parts.length != 3) {
            throw new InvalidRequestLineException(requestLineStr);
        }

        var methodStr = parts[0];
        var requestTargetStr = parts[1];
        var httpVersionStr = parts[2];

        var method = ScxHttpMethod.of(methodStr);
        var httpVersion = HttpVersion.find(httpVersionStr);

        // 这里我们强制 版本号必须是 HTTP/1.1 , 这里需要细化一下 异常
        if (httpVersion != HTTP_1_1) {
            throw new InvalidRequestLineHttpVersionException(httpVersionStr);
        }

        RequestTarget requestTarget;

        if (method == CONNECT) {
            try {
                requestTarget = AuthorityForm.of(requestTargetStr);  // CONNECT 使用 Authority 格式
            } catch (URISyntaxException e) {
                throw new InvalidRequestLineException(requestLineStr);
            }
        } else if (requestTargetStr.startsWith("/")) {
            try {
                requestTarget = OriginForm.of(requestTargetStr);
            } catch (URISyntaxException e) {
                throw new InvalidRequestLineException(requestLineStr);
            }
        } else if ("*".equals(requestTargetStr)) {
            // 只有 OPTIONS 允许 *
            if (method == OPTIONS) {
                requestTarget = AsteriskForm.of();
            } else {
                throw new InvalidRequestLineException(requestLineStr);
            }
        } else {
            // 这里只可能是 AbsoluteForm, 或者非法字符串
            try {
                requestTarget = AbsoluteForm.of(requestTargetStr);
            } catch (URISyntaxException e) {
                throw new InvalidRequestLineException(requestLineStr);
            }
        }

        return new Http1RequestLine(method, requestTarget, httpVersion);
    }

    /// 编码请求行
    public static String encodeRequestLine(Http1RequestLine requestLine) throws IllegalArgumentException {
        var method = requestLine.method();
        var requestTarget = requestLine.requestTarget();
        var httpVersion = requestLine.httpVersion();

        // 此处我们强制使用 HTTP/1.1
        if (httpVersion != HTTP_1_1) {
            throw new IllegalArgumentException("httpVersion is not supported");
        }

        // 校验 method 和 requestTarget 的组合是否成立或者内容是否正确.

        // 1, CONNECT 和 AuthorityForm 是一个组合 二者必须同时存在.
        if (method == CONNECT) {
            if (!(requestTarget instanceof AuthorityForm)) {
                throw new IllegalArgumentException("CONNECT must use AuthorityForm");
            }
        } else {
            switch (requestTarget) {
                // 这里 method 肯定不是 CONNECT, 直接抛异常.
                case AuthorityForm authorityForm -> {
                    throw new IllegalArgumentException("AuthorityForm only allowed with CONNECT");
                }
                // 2, AsteriskForm 只有在 OPTIONS 时才允许.
                case AsteriskForm asteriskForm -> {
                    if (method != OPTIONS) {
                        throw new IllegalArgumentException("AsteriskForm only allowed with OPTIONS");
                    }
                }
                // 3, OriginForm 必须以 "/" 起始
                case OriginForm originForm -> {
                    var path = originForm.path();
                    if (!path.startsWith("/")) {
                        throw new IllegalArgumentException("OriginForm path must start with \"/\"");
                    }
                }
                // 4, AbsoluteForm 的 scheme 和 host 必须存在.
                case AbsoluteForm absoluteForm -> {
                    var scheme = absoluteForm.scheme();
                    var host = absoluteForm.host();
                    if (scheme == null || host == null) {
                        throw new IllegalArgumentException("AbsoluteForm must have scheme and host");
                    }
                }
            }
        }

        var methodStr = method.value();
        var httpVersionStr = httpVersion.protocolVersion();
        var requestTargetStr = requestTarget.encode();

        // 拼接返回
        return methodStr + " " + requestTargetStr + " " + httpVersionStr;
    }

}
