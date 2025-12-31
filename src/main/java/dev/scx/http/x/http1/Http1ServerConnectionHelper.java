package dev.scx.http.x.http1;

import dev.scx.http.exception.BadRequestException;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.headers.upgrade.ScxUpgrade;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import static dev.scx.http.headers.HttpHeaderName.HOST;
import static dev.scx.http.method.HttpMethod.GET;
import static dev.scx.http.x.http1.headers.connection.Connection.UPGRADE;

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

    /// 消耗 Body
    public static void consumeBodyByteInput(ByteInput bodyByteInput) {
        // 因为我们的 Body 流 在 close 时会自动排空, 这里直接 close 即可
        try {
            bodyByteInput.close();
        } catch (AlreadyClosedException | ScxIOException e) {
            // 忽略异常
        }
    }

}
