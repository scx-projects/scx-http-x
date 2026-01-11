package dev.scx.http.x.http1;

import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.media.MediaWriter;
import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.x.SocketIO;
import dev.scx.http.x.http1.io.ContentLengthBodyTooLargeException;
import dev.scx.http.x.http1.io.HeaderTooLargeException;
import dev.scx.http.x.http1.io.Http1Reader;
import dev.scx.http.x.http1.io.StatusLineToLongException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineHttpVersionException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineStatusCodeException;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxIOException;

import static dev.scx.http.sender.ScxHttpSenderStatus.NOT_SENT;
import static dev.scx.http.x.http1.io.Http1Writer.sendRequestHeaders;
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

    public Http1ClientConnection sendRequest(Http1ClientRequest request, MediaWriter mediaWriter) throws ScxIOException, AlreadyClosedException {
        // 检查发送状态
        if (request.senderStatus() != NOT_SENT) {
            throw new IllegalSenderStateException(request.senderStatus());
        }

        // 处理 headers 以及获取 请求长度
        var expectedLength = mediaWriter.beforeWrite(request.headers(), ScxHttpHeaders.of());

        // 发送头
        var byteOutput = sendRequestHeaders(expectedLength, request, this);

        // 调用处理器
        mediaWriter.write(byteOutput);

        return this;
    }

    /// 读取响应
    public Http1ClientResponse readResponse() throws ScxIOException, AlreadyClosedException, NoMoreDataException, InvalidStatusLineException, StatusLineToLongException, InvalidStatusLineStatusCodeException, InvalidStatusLineHttpVersionException, HeaderTooLargeException, ContentLengthBodyTooLargeException {
        // 1, 读取 状态行
        var statusLine = Http1Reader.readStatusLine(socketIO.in, options.maxStatusLineSize());

        // 2, 读取 响应头
        var headers = Http1Reader.readHeaders(socketIO.in, options.maxHeaderSize());

        // 3, 读取 响应体
        var bodyByteSupplier = Http1Reader.readBodyByteSupplier(headers, socketIO.in, options.maxPayloadSize());

        // 创建一个 ByteInput, 要求如下:
        // 1, 要隔离 底层 close.
        // 2, 同时在 close 的时候还要排空流.
        var bodyByteInput = createByteInput(noCloseDrain(bodyByteSupplier));

        return new Http1ClientResponse(statusLine, headers, bodyByteInput);
    }

}
