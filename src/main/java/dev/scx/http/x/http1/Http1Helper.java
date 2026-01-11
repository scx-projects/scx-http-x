package dev.scx.http.x.http1;

import dev.scx.http.exception.BadRequestException;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.peer_info.PeerInfo;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.uri.ScxURI;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.headers.upgrade.ScxUpgrade;
import dev.scx.http.x.http1.io.ContentLengthByteOutput;
import dev.scx.http.x.http1.io.HttpChunkedByteOutput;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.request_line.request_target.*;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteInput;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import java.net.InetSocketAddress;
import java.net.Socket;

import static dev.scx.http.headers.HttpHeaderName.HOST;
import static dev.scx.http.method.HttpMethod.*;
import static dev.scx.http.sender.ScxHttpSenderStatus.SENDING;
import static dev.scx.http.status_code.HttpStatusCode.*;
import static dev.scx.http.status_code.ScxHttpStatusCodeHelper.getReasonPhrase;
import static dev.scx.http.x.http1.headers.connection.Connection.*;
import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;
import static java.nio.charset.StandardCharsets.UTF_8;

final class Http1Helper {

    /// 验证 Http/1.1 中的 Host, 这里我们只校验是否存在且只有一个值
    public static void validateHost(ScxHttpHeaders headers) throws BadRequestException {
        var allHost = headers.getAll(HOST);
        int size = allHost.size();
        if (size == 0) {
            throw new BadRequestException("HOST header is empty");
        }
        if (size > 1) {
            throw new BadRequestException("HOST header contains more than one value");
        }
        var hostValue = allHost.get(0);
        if (hostValue.isBlank()) {
            throw new BadRequestException("HOST header is empty");
        }
    }

    /// 检查是不是 升级请求 如果不是 返回 null
    public static ScxUpgrade checkUpgradeRequest(Http1RequestLine requestLine, Http1Headers headers) {
        return requestLine.method() == GET && headers.connection() == UPGRADE ? headers.upgrade() : null;
    }

    /// 消耗 Body
    public static void consumeBodyByteInput(ByteInput bodyByteInput) {
        // 因为我们的 Body 流 在 close 时会自动排空, 这里直接 close 即可
        try {
            bodyByteInput.close();
        } catch (AlreadyClosedException | ScxIOException e) {
            // 忽略异常
        }
    }


    public static Http1StatusLine createStatusLine(Http1ServerResponse response) {
        var statusCode = response.statusCode();
        var reasonPhrase = response.reasonPhrase() != null ? response.reasonPhrase() : getReasonPhrase(statusCode, "unknown");
        return new Http1StatusLine(statusCode, reasonPhrase);
    }

    public static Http1Headers configResponseHeaders(Http1Headers headers, long expectedLength) {

    }

    public static ByteOutput createResponseByteOutput(Http1ServerResponse response) {
        // 创建 基本 输出流
        var baseByteOutput = new Http1ServerResponseByteOutput(response);

        // 只有明确表示 分块的时候才使用分块
        var useChunkedTransfer = response.headers().transferEncoding() == CHUNKED;
        var contentLength = response.headers().contentLength();

        // 判断是否采用分块传输
        return useChunkedTransfer ?
            new HttpChunkedByteOutput(baseByteOutput) :
            new ContentLengthByteOutput(baseByteOutput, contentLength);
    }


    /// 检查响应是否 存在响应体
    public static boolean checkResponseHasBody(ScxHttpStatusCode status) {
        return SWITCHING_PROTOCOLS != status &&
            NO_CONTENT != status &&
            NOT_MODIFIED != status;
    }

    /// 检查请求是否 存在请求体
    public static boolean checkRequestHasBody(ScxHttpMethod method) {
        return GET != method;
    }

    public static int getDefaultPort(String scheme) throws IllegalArgumentException {
        scheme = scheme.toLowerCase();
        return switch (scheme) {
            case "http", "ws" -> 80;
            case "https", "wss" -> 443;
            default -> throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        };
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


    public static ByteOutput sendResponseHeaders(long expectedLength, Http1ServerResponse response) throws ScxIOException, AlreadyClosedException {
        // 0, 准备参数
        var httpVersion = request.version();
        var statusCode = response.statusCode();
        var reasonPhrase = response.reasonPhrase() != null ? response.reasonPhrase() : getReasonPhrase(statusCode, "unknown");
        var connection = response.connection;

        // 1, 创建 响应行
        var statusLine = new Http1StatusLine(httpVersion, statusCode, reasonPhrase);

        // 编码
        var statusLineStr = statusLine.encode();

        // 处理头相关
        // 1, 处理 连接 相关
        if (headers.connection() == null) {
            if (request.isKeepAlive()) {
                // 正常我们可以忽略设置 KEEP_ALIVE, 但是这里我们显式设置
                headers.connection(KEEP_ALIVE);
            } else {
                headers.connection(CLOSE);
            }
        }

        // 2, 处理 body 相关
        if (expectedLength < 0) {// 表示不知道 body 的长度
            // 如果用户已经手动设置了 Content-Length, 我们便不再设置 分块传输
            if (headers.contentLength() == null) {
                headers.transferEncoding(CHUNKED);
            } else {
                // 否则使用用户已经设置的 contentLength
                expectedLength = headers.contentLength();
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
            var hasBody = checkResponseHasBody(statusCode);
            if (hasBody) {
                // 这里同上, 进行分块传输判断
                if (headers.transferEncoding() != CHUNKED) {
                    headers.contentLength(expectedLength);
                }
            }
        }

        var responseHeaderStr = headers.encode();

        // 标记发送中
        response._setSenderStatus(SENDING);

        // 构建头部内容字节
        var h = statusLineStr + "\r\n" + responseHeaderStr + "\r\n";
        var hb = h.getBytes(UTF_8);

        // 写入 socket
        socketIO.out.write(hb);

        // 只有明确表示 close 的时候我们才关闭
        var closeConnection = headers.connection() == CLOSE;

        // 只有明确表示 分块的时候才使用分块
        var useChunkedTransfer = headers.transferEncoding() == CHUNKED;

        // 创建 基本 输出流
        var baseByteOutput = new Http1ServerResponseByteOutput(response);

        // 判断是否采用分块传输
        return useChunkedTransfer ?
            new HttpChunkedByteOutput(baseByteOutput) :
            new ContentLengthByteOutput(baseByteOutput, expectedLength);

    }

    public static ByteOutput sendRequestHeaders(long expectedLength, Http1ClientRequest request, Http1ClientConnection connection) throws ScxIOException, AlreadyClosedException {
        // 0, 准备参数
        var method = request.method();
        var uri = request.uri();
        var requestTarget = createRequestTarget(method, uri, request._useProxy());

        // 1, 创建 请求行
        var requestLine = new Http1RequestLine(method, requestTarget);

        // 编码
        var requestLineStr = requestLine.encode();

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
        if (expectedLength < 0) {// 表示不知道 body 的长度
            // 如果用户已经手动设置了 Content-Length, 我们便不再设置 分块传输
            if (headers.contentLength() == null) {
                // todo 这里在 NullContentByteSupplier 会写入 多余的 CHUNKED 怎么处理? 问题参看代理示例
                headers.transferEncoding(CHUNKED);
            } else {
                // 否则使用用户已经设置的 contentLength
                expectedLength = headers.contentLength();
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

        var requestHeaderStr = headers.encode();

        // 标记发送中
        request._setSenderStatus(SENDING);

        // 构建头部内容字节
        var h = requestLineStr + "\r\n" + requestHeaderStr + "\r\n";
        var hb = h.getBytes(UTF_8);

        // 写入 socket
        connection.socketIO.out.write(hb);

        // 只有明确表示 分块的时候才使用分块
        var useChunkedTransfer = headers.transferEncoding() == CHUNKED;

        // 创建 基本 输出流
        var baseByteOutput = new Http1ClientRequestByteOutput(connection, request);

        // 判断是否采用分块传输
        return useChunkedTransfer ?
            new HttpChunkedByteOutput(baseByteOutput) :
            new ContentLengthByteOutput(baseByteOutput, expectedLength);

    }

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
