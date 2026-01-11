package dev.scx.http.x.http1.io;

import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;

/// Http1Writer
///
/// @author scx567888
/// @version 0.0.1
public final class Http1Writer {

    private static final ByteChunk CRLF_BYTES = ByteChunk.of("\r\n");

    /// 写入 请求行
    public static void writeRequestLine(ByteOutput byteOutput, Http1RequestLine requestLine) throws IllegalArgumentException, ScxIOException, AlreadyClosedException {
        var requestLineStr = requestLine.encode();
        var requestLineBytes = requestLineStr.getBytes();
        byteOutput.write(requestLineBytes);
        byteOutput.write(CRLF_BYTES);
    }

    /// 写入 响应行
    public static void writeStatusLine(ByteOutput byteOutput, Http1StatusLine statusLine) throws IllegalArgumentException, ScxIOException, AlreadyClosedException {
        var statusLineStr = statusLine.encode();
        var statusLineBytes = statusLineStr.getBytes();
        byteOutput.write(statusLineBytes);
        byteOutput.write(CRLF_BYTES);
    }

    /// 写入 headers
    public static void writeHeaders(ByteOutput byteOutput, Http1Headers headers) throws ScxIOException, AlreadyClosedException {
        // todo 空头发什么?
        var headersStr = headers.encode();
        var headersBytes = headersStr.getBytes();
        byteOutput.write(headersBytes);
        byteOutput.write(CRLF_BYTES);
    }


    public static ByteOutput createBodyByteOutput(Http1Headers headers, ByteOutput byteOutput) {

        var transferEncoding = headers.transferEncoding();
        if (transferEncoding == CHUNKED) {
            return new HttpChunkedByteOutput(byteOutput);
        }

        var contentLength = headers.contentLength();

        return new ContentLengthByteOutput(byteOutput, contentLength);
    }

}
