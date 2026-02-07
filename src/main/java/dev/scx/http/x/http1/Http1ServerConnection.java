package dev.scx.http.x.http1;

import dev.scx.exception.ScxWrappedException;
import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.http.exception.BadRequestException;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.parameters.Parameters;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.http.sender.ScxHttpSender.BodyWriter;
import dev.scx.http.x.endpoint.SocketByteEndpoint;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.io.*;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.request_line.InvalidRequestLineException;
import dev.scx.http.x.http1.request_line.InvalidRequestLineHttpVersionException;
import dev.scx.http.x.http1.request_line.request_target.OriginForm;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxInputException;
import dev.scx.io.input.NullByteInput;

import java.lang.System.Logger;

import static dev.scx.http.error_handler.ErrorPhase.SYSTEM;
import static dev.scx.http.error_handler.ErrorPhase.USER;
import static dev.scx.http.status_code.HttpStatusCode.SWITCHING_PROTOCOLS;
import static dev.scx.http.x.error_handler.DefaultHttpServerErrorHandler.DEFAULT_HTTP_SERVER_ERROR_HANDLER;
import static dev.scx.http.x.http1.Http1ServerConnectionHelper.*;
import static dev.scx.http.x.http1.headers.connection.Connection.CLOSE;
import static dev.scx.http.x.http1.headers.expect.Expect.CONTINUE;
import static dev.scx.http.x.sender.HttpSenderStatus.FAILED;
import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.ScxIO.drainOnClose;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;

/// Http1ServerConnection
///
/// ### 关于资源泄露 :
///
/// - 在 ScxHttpServerRequest 被交付给 用户处理器 之前,
///   本类保证其职责范围内 (如解析请求行) 不存在 Socket 资源泄露.
///
/// - 在 sendResponse 过程中,
///   一旦发生任何异常 (包括写入响应行、响应头或响应体),
///   本类将立即关闭连接,
///   以避免连接处于不可判定的协议状态,
///   从而确保不存在 Socket 资源泄露.
///
/// - 当 requestHandler 在正常返回时,
///   因 未发送响应(send), 或未结束响应(未关闭 send 提供的 ByteOutput),
///   而导致连接被悬置 (Socket 资源未被回收).
///   我们在设计上允许这种状态.
///
/// - 当 requestHandler 抛出异常后, 异常将转交 errorHandler 处理,
///   如果此时 errorHandler 仍正常返回,
///   但依旧 未完成响应收口 (未发送响应或未结束响应),
///   则连接将彻底进入 不可再推进 的悬置状态,
///   我们在设计上允许这种状态.
///
/// - 当 errorHandler 抛出异常后, 我们会直接关闭连接, 做最后的兜底处理.
///
/// - 如果响应为 101 Switching Protocols, 或 用户 手动调用 stop() 停止自动复用,
///   本连接将不会再进入后续的 HTTP/1 连接复用流程 (不再 requestNext).
///   除非响应明确要求关闭连接 (Connection: close), 否则在交接之前,
///   连接层会确保请求体已消费到消息边界 (必要的收口清理).
///   此后 Socket 的推进方式与生命周期由 requestHandler / 升级协议栈自行定义.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerConnection {

    private final static Logger LOGGER = getLogger(Http1ServerConnection.class.getName());

    /// 对外公开 endpoint 字段, 以便 实现更底层功能.
    public final SocketByteEndpoint endpoint;

    private final Http1ServerConnectionOptions options;
    private final Function1Void<ScxHttpServerRequest, ?> requestHandler;
    private final ScxHttpServerErrorHandler errorHandler;
    private final String handlerThreadName;
    private volatile boolean stopped;

    public Http1ServerConnection(SocketByteEndpoint endpoint, Http1ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) {
        this.endpoint = endpoint;
        this.options = options;
        this.requestHandler = requestHandler;
        this.errorHandler = errorHandler == null ? DEFAULT_HTTP_SERVER_ERROR_HANDLER : errorHandler; // 没有就回退到默认
        this.handlerThreadName = "Http1ServerConnection-Handler-" + endpoint.socket.getRemoteSocketAddress();
        this.stopped = false;
    }

    /// 读取 请求 (在发生错误时 无需关闭 socket, 上层会处理)
    public ScxHttpServerRequest readRequest() throws ScxInputException, InputAlreadyClosedException, NoMoreDataException, InvalidRequestLineException, InvalidRequestLineHttpVersionException, RequestLineTooLongException, HeaderTooLargeException, ContentLengthBodyTooLargeException, BadRequestException {
        // 1, 读取 请求行
        var requestLine = Http1Reader.readRequestLine(endpoint.in, options.maxRequestLineSize());

        // 2, 读取 请求头
        var headers = Http1Reader.readHeaders(endpoint.in, options.maxHeaderSize());

        // 创建一个 ByteInput, 要隔离 底层 close.
        var baseByteInput = new Http1ServerRequestByteInput(this);

        // 3, 读取 请求体
        var bodyByteSupplier = Http1Reader.createBodyByteSupplier(headers, baseByteInput, options.maxPayloadSize());

        // 4, 在交给用户处理器进行处理之前, 我们需要做一些预处理

        // 4.1, 验证 请求头
        if (options.validateHost()) {
            validateHost(headers);
        }

        // 4.2, 处理 100-continue 临时请求
        if (headers.expect() == CONTINUE) {
            // 如果启用了自动响应 我们直接发送
            if (options.autoRespond100Continue()) {
                Http1Writer.writeContinue100(endpoint.out);
            } else {
                // 否则交给用户去处理
                bodyByteSupplier = new AutoContinueByteSupplier(bodyByteSupplier, endpoint.out);
            }
        }

        // 创建一个 ByteInput, 要在 close 的时候排空流.
        var bodyByteInput = createByteInput(drainOnClose(bodyByteSupplier));

        // 5, 判断是否为 升级请求 并创建对应请求
        var upgrade = checkUpgradeRequest(requestLine, headers);

        if (upgrade != null) {
            var http1UpgradeRequestFactory = options.upgradeRequestFactories().get(upgrade);
            if (http1UpgradeRequestFactory != null) {
                return http1UpgradeRequestFactory.createUpgradeRequest(requestLine, headers, bodyByteSupplier.bodyLength(), bodyByteInput, this);
            }
        }

        // 否则创建普通请求
        return new Http1ServerRequest(requestLine, headers, bodyByteSupplier.bodyLength(), bodyByteInput, this);
    }

    /// 发送响应 (注意在发生错误时 关闭 socket)
    public void sendResponse(Http1ServerResponse response, BodyWriter bodyWriter) throws ScxHttpSendException, ScxWrappedException {
        // 1, 处理 headers 以及获取 请求长度
        var bodyLength = bodyWriter.bodyLength();

        // 2, 创建响应行
        var statusLine = createStatusLine(response);

        // 3, 配置头
        var headers = configResponseHeaders(response, bodyLength);

        // 4, 创建 基本 输出流
        var baseByteOutput = new Http1ServerResponseByteOutput(response, this);

        // 5, 创建 byteOutput
        var byteOutput = Http1Writer.createBodyByteOutput(baseByteOutput, headers);

        // 6, 写入远端
        try {
            // 6.1, 写入 响应行 和 头
            Http1Writer.writeStatusLineAndHeaders(endpoint.out, statusLine, headers);
        } catch (Throwable e) {
            // 发生 任何异常 我们都需要关闭 socket. 因为无法保证数据依然处于正确协议状态
            response._setSenderStatus(FAILED);
            endpoint.closeQuietly();
            throw new ScxHttpSendException(e);
        }

        try {
            // 6.2, 写入 body
            bodyWriter.write(byteOutput);
        } catch (Throwable e) {
            // 发生 任何异常 我们都需要关闭 socket. 因为无法保证数据依然处于正确协议状态
            response._setSenderStatus(FAILED);
            endpoint.closeQuietly();
            throw new ScxWrappedException(e);
        }

    }

    /// 启动虚拟线程进行读取.
    public void requestNext() {
        // 创建虚拟线程 处理请求
        Thread.ofVirtual()
            .name(handlerThreadName)
            .start(this::handle);
    }

    public void onResponseEnd(Http1ServerResponse response) {

        // 1, 判断 是否是 close (优先级最高).
        if (response.headers().connection() == CLOSE) {
            // 如果是 close 我们终止 底层 Socket 连接
            endpoint.closeQuietly();
            return;
        }

        // 2, 若连接还可能被继续使用. 我们先要排空 请求体, 保证 流的边界正确.
        consumeBodyByteInput(response.request().body());

        // 3, 判断 是否是 升级响应.
        if (response.statusCode() == SWITCHING_PROTOCOLS) {
            // 升级响应 不再处理.
            return;
        }

        // 4, 判断 用户是否 手动关闭 比如用于 代理或者自定义协议.
        if (stopped) {
            return;
        }

        // 5, 可以复用当前连接, 继续下一次请求
        requestNext();

    }

    /// 停止 连接 自动复用
    public void stop() {
        stopped = true;
    }

    private void handle() {
        // 开始读取 Http 请求

        // 1, 我们先读取请求 (只要是在 读取 Request 阶段发生错误, 我们就认为当前连接应该直接作废.)
        ScxHttpServerRequest request;
        try {
            request = readRequest();
        } catch (ScxInputException | InputAlreadyClosedException | NoMoreDataException e) {
            // 如果是 IO 类异常 直接终止, 其余都不做, 甚至不打印日志 (因为完全属于干扰项).
            endpoint.closeQuietly();
            return;
        } catch (Throwable e) {
            // 其余异常, 我们尝试 响应到远端.
            // 2, 调用系统错误处理器 (尽可能的向远端发送信息)
            handleSystemException(e);
            return;
        }

        // 2, 交由用户处理器处理
        try {
            requestHandler.apply(request);
        } catch (Throwable e) {
            // 用户处理器 错误 我们尝试响应
            handleUserException(e, request);
        }

    }

    /// 处理系统级别错误
    private void handleSystemException(Throwable e) {

        // 此时我们并没有拿到一个完整的 request 对象 所以这里创建一个 虚拟 request 用于后续响应
        var fakeRequest = new Http1ServerRequest(
            new Http1RequestLine(ScxHttpMethod.of("UNKNOWN"), new OriginForm(null, Parameters.of(), null)),
            new Http1Headers().connection(CLOSE),
            0L,
            new NullByteInput(),
            this
        );

        // 调用错误处理器 (这里我们不保证 远端一定可用)
        try {
            errorHandler.handle(e, fakeRequest, SYSTEM);
        } catch (Throwable _) {
            // 如果错误处理器 出现异常 (比如无法发送到远端), 我们才打印 (只是 DEBUG 级别).
            LOGGER.log(DEBUG, e);
        } finally { // 无论成功与否 我们都不继续使用这个连接.
            endpoint.closeQuietly();
        }

    }

    /// 处理用户级别错误
    private void handleUserException(Throwable e, ScxHttpServerRequest request) {

        // 调用错误处理器 (这里我们不保证 远端一定可用)
        try {
            errorHandler.handle(e, request, USER);
        } catch (Throwable ex) {
            // 如果错误处理器 出现异常 (比如无法发送到远端), 我们才打印 (只是 DEBUG 级别).
            LOGGER.log(DEBUG, e);
            // 和 handleSystemException 不同, 我们只在 异常时 关闭连接.
            endpoint.closeQuietly();
        }

    }

}
