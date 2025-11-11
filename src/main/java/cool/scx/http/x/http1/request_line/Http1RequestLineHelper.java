package cool.scx.http.x.http1.request_line;

import cool.scx.http.method.ScxHttpMethod;
import cool.scx.http.uri.ScxURI;
import cool.scx.http.uri.ScxURIWritable;
import cool.scx.http.version.HttpVersion;

import java.net.URI;

import static cool.scx.http.method.HttpMethod.CONNECT;
import static cool.scx.http.version.HttpVersion.HTTP_1_1;
import static cool.scx.http.x.HttpXHelper.getDefaultPort;

/// Http1RequestLineHelper
///
/// @author scx567888
/// @version 0.0.1
public final class Http1RequestLineHelper {

    /// 解析 请求行
    public static Http1RequestLine parseRequestLine(String requestLineStr) throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        var parts = requestLineStr.split(" ");

        //如果长度等于 2, 则可能是 HTTP/0.9 请求
        //如果长度大于 3, 则可能是 路径中包含意外的空格
        //但是此处我们没必要细化异常 全部抛出 InvalidHttpRequestLineException 异常
        if (parts.length != 3) {
            throw new InvalidRequestLineException(requestLineStr);
        }

        var methodStr = parts[0];
        var requestTargetStr = parts[1];
        var httpVersionStr = parts[2];

        var method = ScxHttpMethod.of(methodStr);
        var httpVersion = HttpVersion.find(httpVersionStr);

        ScxURI requestTarget;

        if (method == CONNECT) {
            requestTarget = ScxURI.ofAuthority(requestTargetStr);  // CONNECT 使用 Authority 格式
        } else {
            // 处理空请求路径
            if ("".equals(requestTargetStr)) {
                requestTargetStr = "/";  // 空路径默认处理为 "/"
            }
            //尝试解码路径 如果解析失败, 则可能是路径中包含非法字符
            //此处我们同样不去细化异常 直接抛出 InvalidHttpRequestLineException 异常
            URI decodedPath;
            try {
                decodedPath = URI.create(requestTargetStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestLineException(requestLineStr);
            }

            requestTarget = ScxURI.of(decodedPath);
        }

        //这里我们强制 版本号必须是 HTTP/1.1 , 这里需要细化一下 异常
        if (httpVersion != HTTP_1_1) {
            throw new InvalidRequestLineHttpVersionException(httpVersionStr);
        }

        return new Http1RequestLine(method, requestTarget, httpVersion);
    }

    /// 编码请求行
    public static String encodeRequestLine(Http1RequestLine requestLine, RequestTargetForm requestTargetForm) {
        var methodStr = requestLine.method().value();

        var requestTarget = ScxURI.of(requestLine.requestTarget());
        //处理空请求路径
        if ("".equals(requestTarget.path())) {
            requestTarget.path("/");
        }

        var requestTargetStr = getRequestTargetStr(requestTarget, requestTargetForm);

        //此处我们强制使用 HTTP/1.1 , 忽略 requestLine 的版本号
        var versionStr = HTTP_1_1.protocolVersion();

        //拼接返回
        return methodStr + " " + requestTargetStr + " " + versionStr;
    }

    public static String getRequestTargetStr(ScxURIWritable requestTarget, RequestTargetForm requestTargetForm) {
        return switch (requestTargetForm) {
            case ORIGIN_FORM -> requestTarget.scheme(null).host(null).encode(true);
            case ABSOLUTE_FORM -> {
                // 确保统一小写
                var scheme = requestTarget.scheme().toLowerCase();
                //注意转换 ws -> http
                if ("ws".equals(scheme)) {
                    scheme = "http";
                } else if ("wss".equals(scheme)) {
                    scheme = "https";
                }
                yield requestTarget.scheme(scheme).encode(true);
            }
            case AUTHORITY_FORM -> {
                var port = requestTarget.port();
                if (port == null) {
                    port = getDefaultPort(requestTarget.scheme());
                }
                yield requestTarget.host() + ":" + port;
            }
            case ASTERISK_FORM -> "*";
        };
    }

}
