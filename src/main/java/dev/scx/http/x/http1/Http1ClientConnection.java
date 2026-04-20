package dev.scx.http.x.http1;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.sender.ScxHttpReceiveException;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.http.sender.ScxHttpSender.BodyWriter;
import dev.scx.http.x.endpoint.SocketByteEndpoint;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.io.*;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.http.x.http1.status_line.InvalidStatusLineException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineHttpVersionException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineStatusCodeException;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxInputException;

import static dev.scx.http.method.HttpMethod.CONNECT;
import static dev.scx.http.method.HttpMethod.HEAD;
import static dev.scx.http.status_code.HttpStatusCode.OK;
import static dev.scx.http.status_code.HttpStatusCode.SWITCHING_PROTOCOLS;
import static dev.scx.http.x.http1.Http1ClientConnectionHelper.configRequestHeaders;
import static dev.scx.http.x.http1.Http1ClientConnectionHelper.createRequestLine;
import static dev.scx.http.x.http1.headers.connection.Connection.CLOSE;
import static dev.scx.http.x.sender.HttpSenderStatus.FAILED;
import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.ScxIO.drainOnClose;

/// Http1ClientConnection
///
/// ### 关于资源泄露 :
///
/// - 在 readResponse 成功返回 Http1ClientResponse 之前,
///   本类保证其职责范围内 (如解析响应状态行/响应头) 不存在 Socket 资源泄露;
///   若 readResponse 过程中发生任何异常, 将直接关闭连接作为兜底.
///
/// - 在 sendRequest 过程中,
///   一旦发生任何异常 (包括写入请求行、请求头或请求体),
///   本类将立即关闭连接,
///   以避免连接处于不可判定的协议状态,
///   从而确保不存在 Socket 资源泄露.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientConnection {

    /// 对外公开 endpoint 字段, 以便 实现更底层功能.
    public final SocketByteEndpoint endpoint;

    private final Http1ClientConnectionOptions options;

    private volatile boolean stopped;

    public Http1ClientConnection(SocketByteEndpoint endpoint, Http1ClientConnectionOptions options) {
        this.endpoint = endpoint;
        this.options = options;
        this.stopped = false;
    }

    /// 读取 响应
    private Http1ClientResponse readResponse0(Http1RequestLine requestLine) throws ScxInputException, InputAlreadyClosedException, NoMoreDataException, InvalidStatusLineException, StatusLineToLongException, InvalidStatusLineStatusCodeException, InvalidStatusLineHttpVersionException, HeaderTooLargeException, ContentLengthBodyTooLargeException {
        // 1, 读取 状态行
        var statusLine = Http1Reader.readStatusLine(endpoint.in, options.maxStatusLineSize());

        // 2, 读取 响应头
        var headers = Http1Reader.readHeaders(endpoint.in, options.maxHeaderSize());

        // 3, 创建 基本 输入流
        var baseByteInput = new Http1ClientResponseByteInput(this, requestLine, statusLine, headers);

        // 4, 判断是否需要 跳过 body 的读取.
        var skipBodyRead = requestLine.method() == HEAD;

        HttpBodyByteSupplier bodyByteSupplier;

        // 4.1, 需要跳过
        if (skipBodyRead) {
            bodyByteSupplier = new NullContentByteSupplier(baseByteInput);
        } else {
            // 4.2, 读取 响应体
            bodyByteSupplier = Http1Reader.createBodyByteSupplier(headers, baseByteInput, options.maxPayloadSize());
        }

        // 5, 创建一个 ByteInput, 要在 close 的时候排空流.
        var bodyByteInput = createByteInput(drainOnClose(bodyByteSupplier));

        // 6, 创建 Http1ClientResponse
        return new Http1ClientResponse(statusLine, headers, bodyByteSupplier.bodyLength(), bodyByteInput, this);
    }

    /// 接收响应 (注意在发生错误时 关闭 socket)
    public Http1ClientResponse readResponse(Http1RequestLine requestLine) throws ScxHttpReceiveException {
        try {
            return readResponse0(requestLine);
        } catch (Throwable e) {
            // 关闭连接
            endpoint.closeQuietly();
            throw new ScxHttpReceiveException(e);
        }
    }

    /// 发送请求 (注意在发生错误时 关闭 socket)
    public Http1RequestLine sendRequest(Http1ClientRequest request, BodyWriter bodyWriter) throws ScxHttpSendException, ScxWrappedException {

        // 1, 处理 headers 以及获取 请求长度
        var bodyLength = bodyWriter.bodyLength();

        // 2, 创建请求行
        var requestLine = createRequestLine(request);

        // 3, 配置头
        var headers = configRequestHeaders(request, bodyLength);

        // 4, 将 请求行 和 头 写入远端
        try {
            Http1Writer.writeRequestLineAndHeaders(endpoint.out, requestLine, headers);
        } catch (Throwable e) {
            // 发生 任何异常 我们都需要关闭 socket. 因为无法保证数据依然处于正确协议状态
            request._setSenderStatus(FAILED);
            endpoint.closeQuietly();
            throw new ScxHttpSendException(e);
        }

        // 5, 创建 基本 输出流
        var baseByteOutput = new Http1ClientRequestByteOutput(request, this);

        // 5.1, 创建 byteOutput
        var byteOutput = Http1Writer.createBodyByteOutput(baseByteOutput, headers);

        try {
            // 5.2, 写出 body
            bodyWriter.write(byteOutput);
        } catch (Throwable e) {
            // 发生 任何异常 我们都需要关闭 socket. 因为无法保证数据依然处于正确协议状态
            request._setSenderStatus(FAILED);
            endpoint.closeQuietly();
            throw new ScxWrappedException(e);
        }

        return requestLine;
    }

    /// 当一个 HTTP message 完整结束后, 连接只能进入以下三种后续状态:
    ///
    /// 1. Close Connection
    ///    - 关闭底层 Socket
    ///    - 触发条件: Connection: close / 错误 / 策略关闭
    ///
    /// 2. Connection Handoff
    ///    - HTTP 协议层停止管理该 Socket
    ///    - Socket 被交由其他协议栈继续使用
    ///    - 典型场景: 101 Switching Protocols / WebSocket / CONNECT
    ///
    /// 3. Connection Reuse
    ///    - 继续在该连接上处理下一条 HTTP message
    ///    - 典型场景: HTTP/1.1 keep-alive
    ///
    /// 本方法的职责是根据当前响应状态决定连接的后续走向.
    ///
    /// Client 实现中默认不复用连接, 因此在正常情况下会关闭 Socket.
    public void onResponseEnd(Http1RequestLine requestLine, Http1StatusLine statusLine, Http1Headers responseHeaders) {

        // 1, 判断 是否是 close (优先级最高).
        if (responseHeaders.connection() == CLOSE) {
            // 如果是 close 我们终止 底层 Socket 连接
            endpoint.closeQuietly();
            return;
        }

        // 2, 判断 是否是 隧道响应
        if (requestLine.method() == CONNECT && statusLine.statusCode() == OK) {
            return;
        }

        // 3, 判断 是否是 升级响应.
        if (statusLine.statusCode() == SWITCHING_PROTOCOLS) {
            // 升级响应 不再处理.
            return;
        }

        // 4, 判断 用户是否 手动关闭 比如用于 代理或者自定义协议.
        if (stopped) {
            return;
        }

        // 5, 我们在此处不引入任何连接池复用(保持客户端的简单性). 直接关闭.
        endpoint.closeQuietly();

    }

    /// 停止 连接 自动复用
    public void stop() {
        stopped = true;
    }

}
