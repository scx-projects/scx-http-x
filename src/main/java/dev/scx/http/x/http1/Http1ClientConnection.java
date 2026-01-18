package dev.scx.http.x.http1;

import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.sender.ScxHttpSender.BodyWriter;
import dev.scx.http.x.SocketIO;
import dev.scx.http.x.http1.io.*;
import dev.scx.http.x.http1.status_line.InvalidStatusLineException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineHttpVersionException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineStatusCodeException;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxIOException;

import static dev.scx.http.x.ScxHttpSenderStatus.NOT_SENT;
import static dev.scx.http.x.ScxHttpSenderStatus.SENDING;
import static dev.scx.http.x.http1.Http1ClientConnectionHelper.configRequestHeaders;
import static dev.scx.http.x.http1.Http1ClientConnectionHelper.createRequestLine;
import static dev.scx.io.ScxIO.createByteInput;
import static dev.scx.io.supplier.ClosePolicyByteSupplier.noCloseDrain;

/// Http1ClientConnection
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientConnection {

    /// 对外公开 tcpSocket 字段, 以便 实现更底层功能.
    public final SocketIO socketIO;

    private final Http1ClientConnectionOptions options;

    public Http1ClientConnection(SocketIO socketIO, Http1ClientConnectionOptions options) {
        this.socketIO = socketIO;
        this.options = options;
    }

    /// 读取 响应
    public Http1ClientResponse readResponse() throws ScxIOException, AlreadyClosedException, NoMoreDataException, InvalidStatusLineException, StatusLineToLongException, InvalidStatusLineStatusCodeException, InvalidStatusLineHttpVersionException, HeaderTooLargeException, ContentLengthBodyTooLargeException {
        // 1, 读取 状态行
        var statusLine = Http1Reader.readStatusLine(socketIO.in, options.maxStatusLineSize());

        // 2, 读取 响应头
        var headers = Http1Reader.readHeaders(socketIO.in, options.maxHeaderSize());

        // 3, 读取 响应体
        var bodyByteSupplier = Http1Reader.createBodyByteSupplier(headers, socketIO.in, options.maxPayloadSize());

        // 创建一个 ByteInput, 要求如下:
        // 1, 要隔离 底层 close.
        // 2, 同时在 close 的时候还要排空流.
        var bodyByteInput = createByteInput(noCloseDrain(bodyByteSupplier));

        return new Http1ClientResponse(statusLine, headers, bodyByteInput, this);
    }

    /// 发送请求
    public Http1ClientConnection sendRequest(Http1ClientRequest request, BodyWriter bodyWriter) throws ScxIOException, AlreadyClosedException {
        // 0, 检查发送状态
        if (request.senderStatus() != NOT_SENT) {
            throw new IllegalSenderStateException("状态错误 : " + request.senderStatus());
        }

        // 1, 处理 headers 以及获取 请求长度
        var bodyLength = bodyWriter.bodyLength();

        // 2, 标记发送中 (之所以在 beforeWrite 之后而不是 beforeWrite 之前, 是为了给用户 beforeWrite 失败后重试的可能)
        request._setSenderStatus(SENDING);

        // 3, 创建请求行
        var requestLine = createRequestLine(request);

        // 4, 配置头
        var headers = configRequestHeaders(request, bodyLength);

        // 5, 创建 基本 输出流
        var baseByteOutput = new Http1ClientRequestByteOutput(request, this);

        // 6, 创建 byteOutput
        var byteOutput = Http1Writer.createBodyByteOutput(baseByteOutput, headers);

        // 7, 写入远端

        // 7.1 写入请求行
        Http1Writer.writeRequestLine(socketIO.out, requestLine);

        // 7.2 写入头
        Http1Writer.writeHeaders(socketIO.out, headers);

        // 7.3, 写入 body
        bodyWriter.write(byteOutput);

        return this;
    }

}
