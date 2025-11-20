package cool.scx.http.x.http1;

import cool.scx.http.exception.BadRequestException;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.headers.ScxHttpHeadersWritable;
import cool.scx.http.method.ScxHttpMethod;
import cool.scx.http.peer_info.PeerInfo;
import cool.scx.http.peer_info.PeerInfoWritable;
import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.http.uri.ScxURI;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.headers.upgrade.ScxUpgrade;
import cool.scx.http.x.http1.request_line.Http1RequestLine;
import cool.scx.io.ByteInput;
import cool.scx.io.ByteOutput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;

import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static cool.scx.common.util.StringUtils.isBlank;
import static cool.scx.http.headers.HttpHeaderName.HOST;
import static cool.scx.http.method.HttpMethod.GET;
import static cool.scx.http.status_code.HttpStatusCode.*;
import static cool.scx.http.x.http1.headers.connection.Connection.UPGRADE;

/// Http1Helper
///
/// @author scx567888
/// @version 0.0.1
public final class Http1Helper {

    public static final byte[] CONTINUE_100_BYTES = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    public static final byte[] CRLF_BYTES = "\r\n".getBytes();
    public static final byte[] CRLF_CRLF_BYTES = "\r\n\r\n".getBytes();

    /// 检查是不是 升级请求 如果不是 返回 null
    public static ScxUpgrade checkUpgradeRequest(Http1RequestLine requestLine, Http1Headers headers) {
        return requestLine.method() == GET && headers.connection() == UPGRADE ? headers.upgrade() : null;
    }

    /// 发送 CONTINUE_100
    public static void sendContinue100(ByteOutput byteOutput) throws ScxIOException, AlreadyClosedException {
        byteOutput.write(CONTINUE_100_BYTES);
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

    /// 验证 Http/1.1 中的 Host, 这里我们只校验是否存在且只有一个值
    public static void validateHost(ScxHttpHeadersWritable headers) throws BadRequestException {
        var allHost = headers.getAll(HOST);
        int size = allHost.size();
        if (size == 0) {
            throw new BadRequestException("HOST header is empty");
        }
        if (size > 1) {
            throw new BadRequestException("HOST header contains more than one value");
        }
        var hostValue = allHost.get(0);
        if (isBlank(hostValue)) {
            throw new BadRequestException("HOST header is empty");
        }
    }

    /// 检查响应是否 存在响应体
    public static boolean checkResponseHasBody(ScxHttpStatusCode status) {
        return SWITCHING_PROTOCOLS != status &&
            NO_CONTENT != status &&
            NOT_MODIFIED != status;
    }

    /// 检查请求是否 存在请求体
    public static boolean checkRequestHasBody(ScxHttpMethod method) {
        return GET != method;
    }

    /// 推断 URI, 事实上我们无法拿到真正的地址 所以这里只是推测而已.
    public static ScxURI inferURI(ScxURI requestLineTarget, ScxHttpHeaders headers, Socket tcpSocket) {
        var uri = ScxURI.of(requestLineTarget);
        // 1, 有可能已经是全路径 我们判断一下是否存在协议
        if (uri.scheme() != null) {
            return uri;
        }
        // 2, 推测协议
        if (tcpSocket instanceof SSLSocket) {
            uri.scheme("https");
        } else {
            uri.scheme("http");
        }
        // 3, 开始推测 host 和端口号
        var host = headers.get(HOST);
        if (host != null) {
            var authority = ScxURI.ofAuthority(host);
            uri.host(authority.host()).port(authority.port());
        } else {
            var localAddress = (InetSocketAddress) tcpSocket.getLocalSocketAddress();
            uri.host(localAddress.getHostString()).port(localAddress.getPort());
        }
        return uri;
    }

    public static PeerInfoWritable getRemotePeer(Socket tcpSocket) {
        var address = (InetSocketAddress) tcpSocket.getRemoteSocketAddress();
        //todo 未完成 tls 信息没有写入
        return PeerInfo.of().address(address).host(address.getHostString()).port(address.getPort());
    }

    public static PeerInfoWritable getLocalPeer(Socket tcpSocket) {
        var address = (InetSocketAddress) tcpSocket.getLocalSocketAddress();
        //todo 未完成 tls 信息没有写入
        return PeerInfo.of().address(address).host(address.getHostString()).port(address.getPort());
    }

}
