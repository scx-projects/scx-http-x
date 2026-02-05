package dev.scx.http.x.http1.io;

import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxOutputException;

import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/// Http1Writer
///
/// @author scx567888
/// @version 0.0.1
public final class Http1Writer {

    private static final ByteChunk CONTINUE_100_BYTES = ByteChunk.of("HTTP/1.1 100 Continue\r\n\r\n".getBytes(ISO_8859_1));

    /// 写入 响应行 和 headers
    public static void writeStatusLineAndHeaders(ByteOutput byteOutput, Http1StatusLine statusLine, Http1Headers headers) throws IllegalArgumentException, ScxOutputException, OutputAlreadyClosedException {
        var statusLineAndHeadersStr = statusLine.encode() + "\r\n" + headers.encode() + "\r\n";
        var statusLineAndHeadersBytes = statusLineAndHeadersStr.getBytes(ISO_8859_1);
        byteOutput.write(statusLineAndHeadersBytes);
        byteOutput.flush();
    }

    /// 写入请求行 和 headers
    public static void writeRequestLineAndHeaders(ByteOutput byteOutput, Http1RequestLine requestLine, Http1Headers headers) {
        var requestLineAndHeadersStr = requestLine.encode() + "\r\n" + headers.encode() + "\r\n";
        var requestLineAndHeadersStrBytes = requestLineAndHeadersStr.getBytes(ISO_8859_1);
        byteOutput.write(requestLineAndHeadersStrBytes);
        byteOutput.flush();
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

}
