package dev.scx.http.x.http1.io;

import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxOutputException;

import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;

/// Http1Writer
///
/// @author scx567888
/// @version 0.0.1
public final class Http1Writer {

    private static final ByteChunk CONTINUE_100_BYTES = ByteChunk.of("HTTP/1.1 100 Continue\r\n\r\n".getBytes());
    private static final ByteChunk CRLF_BYTES = ByteChunk.of("\r\n".getBytes());

    /// 写入 请求行
    public static void writeRequestLine(ByteOutput byteOutput, Http1RequestLine requestLine) throws IllegalArgumentException, ScxOutputException, OutputAlreadyClosedException {
        var requestLineStr = requestLine.encode();
        var requestLineBytes = requestLineStr.getBytes();
        byteOutput.write(requestLineBytes);
        byteOutput.write(CRLF_BYTES);
    }

    /// 写入 响应行
    public static void writeStatusLine(ByteOutput byteOutput, Http1StatusLine statusLine) throws IllegalArgumentException, ScxOutputException, OutputAlreadyClosedException {
        var statusLineStr = statusLine.encode();
        var statusLineBytes = statusLineStr.getBytes();
        byteOutput.write(statusLineBytes);
        byteOutput.write(CRLF_BYTES);
    }

    /// 写入 headers
    public static void writeHeaders(ByteOutput byteOutput, Http1Headers headers) throws ScxOutputException, OutputAlreadyClosedException {
        var headersStr = headers.encode();
        var headersBytes = headersStr.getBytes();
        byteOutput.write(headersBytes);
        byteOutput.write(CRLF_BYTES);
    }

    /// 创建 Body 输出流
    public static ByteOutput createBodyByteOutput(ByteOutput byteOutput, Http1Headers headers) {
        // 分块的优先级 大于 contentLength
        var transferEncoding = headers.transferEncoding();
        if (transferEncoding == CHUNKED) {
            return new HttpChunkedByteOutput(byteOutput);
        }

        // 采用 contentLength
        var contentLength = headers.contentLength();

        // 没有我们看作 0
        if (contentLength == null) {
            contentLength = 0L;
        }

        return new ContentLengthByteOutput(byteOutput, contentLength);
    }

    /// 发送 CONTINUE_100
    public static void writeContinue100(ByteOutput byteOutput) throws ScxOutputException, OutputAlreadyClosedException {
        byteOutput.write(CONTINUE_100_BYTES);
    }

    /// 写入 响应行 和 headers
    public static void writeStatusLineAndHeaders(ByteOutput byteOutput, Http1StatusLine statusLine, Http1Headers headers) {
        Http1Writer.writeStatusLine(byteOutput, statusLine);
        Http1Writer.writeHeaders(byteOutput, headers);
    }

}
