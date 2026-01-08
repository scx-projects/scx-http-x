package dev.scx.http.x.http1;

import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.media.MediaWriter;
import dev.scx.http.x.SocketIO;
import dev.scx.http.x.http1.headers.Http1Headers;
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

import java.io.IOException;

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

    public Http1ClientConnection sendRequest(Http1ClientRequest request, MediaWriter writer) throws ScxIOException, AlreadyClosedException {
        // 复制一份头
        var tempHeaders = new Http1Headers(request.headers());

        // 处理 headers 以及获取 请求长度
        var expectedLength = writer.beforeWrite(tempHeaders, ScxHttpHeaders.of());

        // 发送头
        var byteOutput = sendRequestHeaders(expectedLength, request, this, tempHeaders);

        // 调用处理器
        writer.write(byteOutput);

        return this;
    }

    // 这里的异常需要精细化处理
    public Http1ClientResponse waitResponse() throws ScxIOException, AlreadyClosedException, NoMoreDataException, InvalidStatusLineException, StatusLineToLongException, InvalidStatusLineStatusCodeException, InvalidStatusLineHttpVersionException, HeaderTooLargeException, ContentLengthBodyTooLargeException {
        // 1, 读取状态行
        var statusLine = Http1Reader.readStatusLine(socketIO.in, options.maxStatusLineSize());

        // 2, 读取响应头
        var headers = Http1Reader.readHeaders(socketIO.in, options.maxHeaderSize());

        // 3, 读取响应体
        var bodyByteSupplier = Http1Reader.readBodyByteInput(headers, socketIO.in, options.maxPayloadSize());

        // 创建一个 ByteInput, 要求如下:
        // 1, 要隔离 底层 close.
        // 2, 同时在 close 的时候还要排空流.
        var bodyByteInput = createByteInput(noCloseDrain(bodyByteSupplier));

        return new Http1ClientResponse(statusLine, headers, bodyByteInput);
    }

}
