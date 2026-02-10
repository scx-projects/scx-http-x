package dev.scx.http.x.http1;

import dev.scx.http.exception.BadRequestException;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.headers.upgrade.ScxUpgrade;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.ScxInputException;

import static dev.scx.http.headers.HttpHeaderName.HOST;
import static dev.scx.http.method.HttpMethod.GET;
import static dev.scx.http.status_code.HttpStatusCode.*;
import static dev.scx.http.status_code.ScxHttpStatusCodeHelper.getReasonPhrase;
import static dev.scx.http.x.http1.headers.connection.Connection.*;
import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;

/// Http1ServerConnectionHelper
///
/// @author scx567888
/// @version 0.0.1
final class Http1ServerConnectionHelper {

    /// 验证 Http/1.1 中的 Host, 这里我们只校验是否存在且只有一个值
    public static void validateHost(ScxHttpHeaders headers) throws BadRequestException {
        var allHost = headers.getAll(HOST);
        int size = allHost.size();
        if (size == 0) {
            throw new BadRequestException("HOST header is empty");
        }
        if (size > 1) {
            throw new BadRequestException("HOST header contains more than one value");
        }
        var hostValue = allHost.get(0);
        if (hostValue.isBlank()) {
            throw new BadRequestException("HOST header is empty");
        }
    }

    /// 检查是不是 升级请求 如果不是 返回 null
    public static ScxUpgrade checkUpgradeRequest(Http1RequestLine requestLine, Http1Headers headers) {
        return requestLine.method() == GET && headers.connection() == UPGRADE ? headers.upgrade() : null;
    }

    /// 检查响应是否 存在响应体
    public static boolean checkResponseHasBody(ScxHttpStatusCode status) {
        return SWITCHING_PROTOCOLS != status &&
            NO_CONTENT != status &&
            NOT_MODIFIED != status;
    }

    /// 消耗 Body
    public static void consumeBodyByteInput(ByteInput bodyByteInput) {
        // 因为我们的 Body 流 在 close 时会自动排空, 这里直接 close 即可
        try {
            bodyByteInput.close();
        } catch (ScxInputException | InputAlreadyClosedException e) {
            // 忽略异常
        }
    }

    public static Http1StatusLine createStatusLine(Http1ServerResponse response) {
        var statusCode = response.statusCode();
        var reasonPhrase = response.reasonPhrase() != null ? response.reasonPhrase() : getReasonPhrase(statusCode, "unknown");
        return new Http1StatusLine(statusCode, reasonPhrase);
    }

    public static Http1Headers configResponseHeaders(Http1ServerResponse response, Long expectedLength) {
        var headers = response.headers();
        var statusCode = response.statusCode();
        var isKeepAlive = response.request().isKeepAlive();

        // 处理头相关
        // 1, 处理 连接 相关
        if (headers.connection() == null) {
            if (isKeepAlive) {
                // 正常我们可以忽略设置 KEEP_ALIVE, 但是这里我们显式设置
                headers.connection(KEEP_ALIVE);
            } else {
                headers.connection(CLOSE);
            }
        }

        // 2, 处理 body 相关
        if (expectedLength == null) {// 表示不知道 body 的长度
            // 如果用户已经手动设置了 Content-Length, 我们便不再设置 分块传输
            if (headers.contentLength() == null) {
                headers.transferEncoding(CHUNKED);
            }
        } else if (expectedLength > 0) {// 拥有指定长度的响应体
            // 如果用户已经手动设置 分块传输, 我们便不再设置 Content-Length
            if (headers.transferEncoding() != CHUNKED) {
                headers.contentLength(expectedLength);
            }
        } else {
            // body 长度为 0 时 , 分两种情况
            // 1, 是需要明确写入 Content-Length : 0 的
            // 2, 是不需要写入任何长度相关字段
            var hasBody = checkResponseHasBody(statusCode);
            if (hasBody) {
                // 这里同上, 进行分块传输判断
                if (headers.transferEncoding() != CHUNKED) {
                    headers.contentLength(expectedLength);
                }
            }
        }

        return headers;
    }

}
