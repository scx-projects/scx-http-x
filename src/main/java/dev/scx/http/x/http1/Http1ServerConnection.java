package dev.scx.http.x.http1;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.http.exception.BadRequestException;
import dev.scx.http.media.MediaWriter;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.parameters.Parameters;
import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.x.SocketIO;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.io.*;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.request_line.InvalidRequestLineException;
import dev.scx.http.x.http1.request_line.InvalidRequestLineHttpVersionException;
import dev.scx.http.x.http1.request_line.request_target.OriginForm;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.input.NullByteInput;

import java.lang.System.Logger;

import static dev.scx.http.error_handler.ErrorPhase.SYSTEM;
import static dev.scx.http.error_handler.ErrorPhase.USER;
import static dev.scx.http.sender.ScxHttpSenderStatus.FAILED;
import static dev.scx.http.sender.ScxHttpSenderStatus.NOT_SENT;
import static dev.scx.http.x.error_handler.DefaultHttpServerErrorHandler.DEFAULT_HTTP_SERVER_ERROR_HANDLER;
import static dev.scx.http.x.http1.Http1ServerConnectionHelper.*;
import static dev.scx.http.x.http1.headers.connection.Connection.CLOSE;
import static dev.scx.http.x.http1.headers.expect.Expect.CONTINUE;
import static dev.scx.http.x.http1.io.AutoContinueByteSupplier.sendContinue100;
import static dev.scx.http.x.http1.io.Http1Writer.sendResponseHeaders;
import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.supplier.ClosePolicyByteSupplier.noCloseDrain;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;

/// Http1ServerConnection
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerConnection {

    private final static Logger LOGGER = getLogger(Http1ServerConnection.class.getName());

    /// 对外公开 SocketIO 字段, 以便 实现更底层功能.
    public final SocketIO socketIO;
    private final Http1ServerConnectionOptions options;
    private final Function1Void<ScxHttpServerRequest, ?> requestHandler;
    private final ScxHttpServerErrorHandler errorHandler;
    private final String threadName;
    private ScxHttpServerRequest lastRequest;
    private boolean stopped;

    public Http1ServerConnection(SocketIO socketIO, Http1ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) {
        this.socketIO = socketIO;
        this.options = options;
        this.requestHandler = requestHandler;
        this.errorHandler = errorHandler == null ? DEFAULT_HTTP_SERVER_ERROR_HANDLER : errorHandler; // 没有就回退到默认
        this.threadName = "Http1ServerConnection-Handler-" + socketIO.tcpSocket.getRemoteSocketAddress();
        this.lastRequest = null;
    }

    public void sendResponse(Http1ServerResponse response, MediaWriter mediaWriter) throws IllegalSenderStateException {
        // 检查发送状态
        if (response.senderStatus() != NOT_SENT) {
            throw new IllegalSenderStateException(response.senderStatus());
        }

        // 处理 headers 以及获取 请求长度
        var expectedLength = mediaWriter.beforeWrite(response.headers(), response.request().headers());

        // 发送头过程中出现错误 应该立即关闭连接
        ByteOutput byteOutput;
        try {
            byteOutput = sendResponseHeaders(expectedLength, response);
        } catch (ScxIOException | AlreadyClosedException e) {
            // 标记发送失败
            response._setSenderStatus(FAILED);
            // 直接终止 底层 Socket 连接
            socketIO.closeQuietly();
            throw e;
        }

        try {
            mediaWriter.write(byteOutput);
        } catch (ScxIOException e) {
            // 标记发送失败
            response._setSenderStatus(FAILED);
            // 直接终止 底层 Socket 连接
            socketIO.closeQuietly();
            throw e;
        } catch (AlreadyClosedException e) {
            throw new IllegalSenderStateException(response.senderStatus());
        }

    }

    /// 读取 请求
    public ScxHttpServerRequest readRequest() throws ScxIOException, AlreadyClosedException, NoMoreDataException, InvalidRequestLineException, InvalidRequestLineHttpVersionException, RequestLineTooLongException, HeaderTooLargeException, ContentLengthBodyTooLargeException, BadRequestException {
        // 1, 读取 请求行
        var requestLine = Http1Reader.readRequestLine(socketIO.in, options.maxRequestLineSize());

        // 2, 读取 请求头
        var headers = Http1Reader.readHeaders(socketIO.in, options.maxHeaderSize());

        // 3, 读取 请求体
        var bodyByteSupplier = Http1Reader.readBodyByteInput(headers, socketIO.in, options.maxPayloadSize());

        // 4, 在交给用户处理器进行处理之前, 我们需要做一些预处理

        // 4.1, 验证 请求头
        if (options.validateHost()) {
            validateHost(headers);
        }

        // 4.2, 处理 100-continue 临时请求
        if (headers.expect() == CONTINUE) {
            // 如果启用了自动响应 我们直接发送
            if (options.autoRespond100Continue()) {
                sendContinue100(socketIO.out);
            } else {
                // 否则交给用户去处理
                bodyByteSupplier = new AutoContinueByteSupplier(bodyByteSupplier, socketIO.out);
            }
        }

        // 创建一个 ByteInput, 要求如下:
        // 1, 要隔离 底层 close.
        // 2, 同时在 close 的时候还要排空流.
        var bodyByteInput = createByteInput(noCloseDrain(bodyByteSupplier));

        // 5, 判断是否为 升级请求 并创建对应请求
        var upgrade = checkUpgradeRequest(requestLine, headers);

        if (upgrade != null) {
            var http1UpgradeHandler = options.upgradeHandlers().get(upgrade);
            if (http1UpgradeHandler != null) {
                return http1UpgradeHandler.createUpgradedRequest(requestLine, headers, bodyByteInput, this);
            }
        }

        // 否则创建普通请求
        return new Http1ServerRequest(requestLine, headers, bodyByteInput, this);
    }

    /// 启动虚拟线程进行读取.
    public void requestNext() {
        // 创建虚拟线程 处理请求
        Thread.ofVirtual()
            .name(threadName)
            .start(this::handle);
    }

    public void onResponseCompleted(Http1ServerResponse response){
        // 是否是 close
        var closeConnection = response.headers().connection() == CLOSE;
        if (closeConnection) {
            // 如果明确表示 close 我们终止 底层 Socket 连接
            socketIO.closeQuietly();
        } else {
            // 否则继续下一次读取

            // 用户处理器可能没有消费完请求体 这里我们帮助消费用户未消费的数据
            consumeBodyByteInput(response.request().body().byteInput());

            // 开启下一次 读取
            requestNext();

        }
    }

    /// 终止连接读取
    public void stop() {
        stopped = true;
    }

    private void handle() {
        // 开始读取 Http 请求

        // 1, 我们先读取请求 (只要是在 读取 Request 阶段发生错误, 我们就认为当前连接应该直接作废.)
        ScxHttpServerRequest request;
        try {
            request = readRequest();
        } catch (ScxIOException | AlreadyClosedException | NoMoreDataException e) {
            // 如果是 IO 类异常 直接终止, 其余都不做, 甚至不打印日志 (因为完全属于干扰项).
            socketIO.closeQuietly();
            return;
        } catch (Throwable e) {
            // 其余异常, 我们尝试 响应到远端.
            // 2, 调用系统错误处理器 (尽可能的向远端发送信息)
            handleSystemException(e);
            socketIO.closeQuietly();
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
            new NullByteInput(),
            this
        );

        // 调用错误处理器 (这里我们不保证 远端一定可用)
        try {
            errorHandler.accept(e, fakeRequest, SYSTEM);
        } catch (Exception _) {
            // 如果错误处理器 出现异常 (比如无法发送到远端), 我们才打印 (只是 DEBUG 级别).
            LOGGER.log(DEBUG, e);
        }

    }

    /// 处理用户级别错误
    private void handleUserException(Throwable e, ScxHttpServerRequest request) {

        // 调用错误处理器 (这里我们不保证 远端一定可用)
        try {
            errorHandler.accept(e, request, USER);
        } catch (Exception ex) {
            // 如果错误处理器 出现异常 (比如无法发送到远端), 我们才打印 (只是 DEBUG 级别).
            LOGGER.log(DEBUG, e);
        }

    }

}
