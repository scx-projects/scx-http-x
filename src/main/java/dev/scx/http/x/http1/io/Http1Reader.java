package dev.scx.http.x.http1.io;

import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.request_line.InvalidRequestLineException;
import dev.scx.http.x.http1.request_line.InvalidRequestLineHttpVersionException;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.http.x.http1.status_line.InvalidStatusLineException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineHttpVersionException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineStatusCodeException;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.NoMatchFoundException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxInputException;

import java.util.Arrays;

import static dev.scx.http.headers.ScxHttpHeadersHelper.parseHeaders;
import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/// 读取 HTTP/1.1 请求和响应的工具类
///
/// @author scx567888
/// @version 0.0.1
public final class Http1Reader {

    private static final byte[] CRLF_BYTES = "\r\n".getBytes(ISO_8859_1);
    private static final byte[] CRLF_CRLF_BYTES = "\r\n\r\n".getBytes(ISO_8859_1);

    /// 读取 请求行
    public static Http1RequestLine readRequestLine(ByteInput dataReader, int maxRequestLineSize) throws ScxInputException, InputAlreadyClosedException, NoMoreDataException, RequestLineTooLongException, InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        try {
            // 1, 尝试读取到 第一个 \r\n 为止
            var requestLineBytes = dataReader.readUntil(CRLF_BYTES, maxRequestLineSize);
            var requestLineStr = new String(requestLineBytes, ISO_8859_1);
            return Http1RequestLine.of(requestLineStr);
        } catch (NoMatchFoundException e) {
            // 在指定长度内未匹配到 表示 RequestLine 过长
            throw new RequestLineTooLongException("请求行过长 !!!");
        }
    }

    /// 读取 响应行
    public static Http1StatusLine readStatusLine(ByteInput byteInput, int maxStatusLineSize) throws ScxInputException, InputAlreadyClosedException, NoMoreDataException, StatusLineToLongException, InvalidStatusLineException, InvalidStatusLineStatusCodeException, InvalidStatusLineHttpVersionException {
        try {
            var statusLineBytes = byteInput.readUntil(CRLF_BYTES, maxStatusLineSize);
            var statusLineStr = new String(statusLineBytes, ISO_8859_1);
            return Http1StatusLine.of(statusLineStr);
        } catch (NoMatchFoundException e) {
            // 在指定长度内未匹配到 这里抛出响应行过大异常.
            throw new StatusLineToLongException("响应行过长 !!!");
        }
    }

    /// 读取 headers
    public static Http1Headers readHeaders(ByteInput byteInput, int maxHeaderSize) throws ScxInputException, InputAlreadyClosedException, NoMoreDataException, HeaderTooLargeException {
        try {
            // 1, 尝试检查空头的情况, 即请求行后紧跟 \r\n
            var b = byteInput.peekFully(2);
            if (Arrays.equals(b, CRLF_BYTES)) {
                byteInput.skipFully(2);
                return new Http1Headers();
            }

            // 2, 尝试正常读取, 读取到 第一个 \r\n\r\n 为止
            var headerBytes = byteInput.readUntil(CRLF_CRLF_BYTES, maxHeaderSize);
            var headerStr = new String(headerBytes, ISO_8859_1);
            return parseHeaders(new Http1Headers(), headerStr, true); // 使用严格模式解析
        } catch (NoMatchFoundException e) {
            // 在指定长度内未匹配到 表示 Heders 过大
            throw new HeaderTooLargeException("Heders 过大 !!!");
        }
    }

    /// 这里我们返回的 ByteSupplier 是直接关联了 ByteInput 的 ByteSupplier,
    /// 也就是说 如果 调用 ByteSupplier 的 close, 会连带关闭 ByteInput 的 close.
    /// 如果想做到 中断 close 请在上层二次包装.
    public static HttpBodyByteSupplier createBodyByteSupplier(Http1Headers headers, ByteInput byteInput, long maxPayloadSize) throws ContentLengthBodyTooLargeException {
        // HTTP/1.1 本质上只有两种请求体格式 1, 分块传输 2, 指定长度 (当然也可以没有长度 那就表示没有请求体)

        // 1, 因为 分块传输的优先级高于 contentLength 所以先判断是否为分块传输
        var transferEncoding = headers.transferEncoding();
        if (transferEncoding == CHUNKED) {
            return new HttpChunkedByteSupplier(byteInput, maxPayloadSize);
        }

        // 2, 判断请求体是不是有 指定长度
        var contentLength = headers.contentLength();
        if (contentLength != null) {
            // 请求体长度过大 这里抛出异常
            if (contentLength > maxPayloadSize) {
                throw new ContentLengthBodyTooLargeException("Body 过大 !!!");
            }
            return new ContentLengthByteSupplier(byteInput, contentLength);
        }

        // 3, 没有长度的空请求体
        return new NullContentByteSupplier(byteInput);
    }

}
