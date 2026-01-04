package dev.scx.http.x.http1;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.http.exception.BadRequestException;
import dev.scx.http.method.ScxHttpMethod;
import dev.scx.http.parameters.Parameters;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.io.*;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.request_line.InvalidRequestLineException;
import dev.scx.http.x.http1.request_line.InvalidRequestLineHttpVersionException;
import dev.scx.http.x.http1.request_line.request_target.OriginForm;
import dev.scx.io.ByteInput;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.input.NullByteInput;

import java.io.IOException;
import java.lang.System.Logger;
import java.net.Socket;

import static dev.scx.http.x.error_handler.DefaultHttpServerErrorHandler.DEFAULT_HTTP_SERVER_ERROR_HANDLER;
import static dev.scx.http.error_handler.ErrorPhase.SYSTEM;
import static dev.scx.http.error_handler.ErrorPhase.USER;
import static dev.scx.http.x.http1.Http1ServerConnectionHelper.*;
import static dev.scx.http.x.http1.headers.connection.Connection.CLOSE;
import static dev.scx.http.x.http1.headers.expect.Expect.CONTINUE;
import static dev.scx.http.x.http1.io.AutoContinueByteSupplier.sendContinue100;
import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.ScxIO.createByteOutput;
import static dev.scx.io.supplier.ClosePolicyByteSupplier.noCloseDrain;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;

/// Http1ServerConnection
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerConnection implements AutoCloseable {

    private final static Logger LOGGER = getLogger(Http1ServerConnection.class.getName());

    /// 对外公开 tcpSocket 字段, 以便 实现更底层功能.
    public final Socket tcpSocket;
    /// 对外公开 dataReader 字段, 以便 实现更底层功能.
    public final ByteInput dataReader;
    /// 对外公开 dataWriter 字段, 以便 实现更底层功能.
    public final ByteOutput dataWriter;

    private final Http1ServerConnectionOptions options;
    private final Function1Void<ScxHttpServerRequest, ?> requestHandler;
    private final ScxHttpServerErrorHandler errorHandler;
    private boolean running; // 是否处于读取状态
    private boolean attached;// 是否拥有 Socket

    public Http1ServerConnection(Socket tcpSocket, Http1ServerConnectionOptions options, Function1Void<ScxHttpServerRequest, ?> requestHandler, ScxHttpServerErrorHandler errorHandler) throws IOException {
        this.tcpSocket = tcpSocket;
        this.dataReader = createByteInput(this.tcpSocket.getInputStream());
        this.dataWriter = createByteOutput(this.tcpSocket.getOutputStream());
        this.options = options;
        this.requestHandler = requestHandler;
        this.errorHandler = errorHandler;
        this.running = true;
        this.attached = true;
    }

    /// 这里我们只需要阻塞读取 无法处理跳出循环即可. 无需主动关闭 tcpSocket.
    public void start() {

        // 开始读取 Http 请求
        while (running) {

            // 1, 我们先读取请求 (只要是在 读取 Request 阶段发生错误, 我们就认为当前连接应该直接作废.)
            ScxHttpServerRequest request;
            try {
                request = readRequest();
            } catch (ScxIOException | AlreadyClosedException | NoMoreDataException e) {
                // 如果是 IO 类异常 直接终止, 其余都不做, 甚至不打印日志 (因为完全属于干扰项).
                // 1, 调用 stop 标记 循环终止.
                stop();
                // 2, 跳出循环
                break;
            } catch (Throwable e) {
                // 其余异常, 我们尝试 响应到远端.
                // 1, 调用 stop 标记 循环终止.
                stop();
                // 2, 调用系统错误处理器 (尽可能的向远端发送信息)
                handlerSystemException(e);
                // 3, 跳出循环
                break;
            }

            // 2, 交由用户处理器处理
            try {
                requestHandler.apply(request);
            } catch (Throwable e) {
                // 用户处理器 错误 我们尝试恢复
                handlerUserException(e, request);
            } finally {

                // 3, 如果 还是 running 说明需要继续复用当前 tcp 连接, 并进行下一次 Request 的读取
                if (running) {
                    // 4, 用户处理器可能没有消费完请求体 这里我们帮助消费用户未消费的数据
                    consumeBodyByteInput(request.body().byteInput());
                }

            }

        }

    }

    /// 读取 请求
    private ScxHttpServerRequest readRequest() throws ScxIOException, AlreadyClosedException, NoMoreDataException, InvalidRequestLineException, InvalidRequestLineHttpVersionException, RequestLineTooLongException, HeaderTooLargeException, ContentLengthBodyTooLargeException, BadRequestException {
        // 1, 读取 请求行
        var requestLine = Http1Reader.readRequestLine(dataReader, options.maxRequestLineSize());

        // 2, 读取 请求头
        var headers = Http1Reader.readHeaders(dataReader, options.maxHeaderSize());

        // 3, 读取 请求体流
        var bodyByteSupplier = Http1Reader.readBodyByteInput(headers, dataReader, options.maxPayloadSize());

        // 4, 在交给用户处理器进行处理之前, 我们需要做一些预处理

        // 4.1, 验证 请求头
        if (options.validateHost()) {
            validateHost(headers);
        }

        // 4.2, 处理 100-continue 临时请求
        if (headers.expect() == CONTINUE) {
            // 如果启用了自动响应 我们直接发送
            if (options.autoRespond100Continue()) {
                sendContinue100(dataWriter);
            } else {
                // 否则交给用户去处理
                bodyByteSupplier = new AutoContinueByteSupplier(bodyByteSupplier, dataWriter);
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
                return http1UpgradeHandler.createUpgradedRequest(this, requestLine, headers, bodyByteInput);
            }
        }

        // 否则创建普通请求
        return new Http1ServerRequest(this, requestLine, headers, bodyByteInput);
    }

    /// 停止读取 http 请求
    public void stop() {
        running = false;
    }

    /// 交接 Socket, 同时也会 stop()
    public void detach() {
        stop();
        attached = false;
    }

    /// 处理系统级别错误
    private void handlerSystemException(Throwable e) {
        // 此时我们并没有拿到一个完整的 request 对象 所以这里创建一个 虚拟 request 用于后续响应
        var fakeRequest = new Http1ServerRequest(
            this,
            new Http1RequestLine(ScxHttpMethod.of("UNKNOWN"), new OriginForm(null, Parameters.of(), null)),
            new Http1Headers().connection(CLOSE),
            new NullByteInput()
        );

        // 调用错误处理器 (这里我们不保证 远端一定可用)
        try {
            // 没有就回退到默认
            var eh = errorHandler != null ? errorHandler : DEFAULT_HTTP_SERVER_ERROR_HANDLER;
            eh.accept(e, fakeRequest, SYSTEM);
        } catch (Exception _) {
            // 如果错误处理器 出现异常 (比如无法发送到远端), 我们才打印 (只是 DEBUG 级别).
            LOGGER.log(DEBUG, e);
        }

    }

    /// 处理用户级别错误
    private void handlerUserException(Throwable e, ScxHttpServerRequest request) {

        // 调用错误处理器 (这里我们不保证 远端一定可用)
        try {
            // 没有就回退到默认
            var eh = errorHandler != null ? errorHandler : DEFAULT_HTTP_SERVER_ERROR_HANDLER;
            eh.accept(e, request, USER);
        } catch (Exception ex) {
            // 如果错误处理器 出现异常 (比如无法发送到远端), 我们才打印 (只是 DEBUG 级别).
            LOGGER.log(DEBUG, e);
        }

    }

    @Override
    public void close() throws IOException {
        // 只有在拥有 socket 所有权的情况下 我们才 close()
        if (attached) {
            tcpSocket.close();
        }
    }

}
