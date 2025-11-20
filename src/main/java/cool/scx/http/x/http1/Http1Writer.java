package cool.scx.http.x.http1;

import cool.scx.http.sender.ScxHttpSenderStatus;
import cool.scx.http.x.HttpClientRequest;
import cool.scx.http.x.http1.byte_output.ContentLengthByteOutput;
import cool.scx.http.x.http1.byte_output.Http1ClientRequestByteOutput;
import cool.scx.http.x.http1.byte_output.Http1ServerResponseByteOutput;
import cool.scx.http.x.http1.byte_output.HttpChunkedByteOutput;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.request_line.Http1RequestLine;
import cool.scx.http.x.http1.status_line.Http1StatusLine;
import cool.scx.io.ByteOutput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;

import static cool.scx.http.headers.HttpHeaderName.HOST;
import static cool.scx.http.sender.ScxHttpSenderStatus.SUCCESS;
import static cool.scx.http.status_code.ScxHttpStatusCodeHelper.getReasonPhrase;
import static cool.scx.http.x.http1.Http1Helper.checkRequestHasBody;
import static cool.scx.http.x.http1.Http1Helper.checkResponseHasBody;
import static cool.scx.http.x.http1.headers.connection.Connection.CLOSE;
import static cool.scx.http.x.http1.headers.connection.Connection.KEEP_ALIVE;
import static cool.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class Http1Writer {

    public static ByteOutput sendResponseHeaders(long expectedLength, Http1ServerRequest request, Http1ServerResponse response, Http1Headers headers) throws ScxIOException, AlreadyClosedException {
        // 0, 准备参数
        var httpVersion = request.version();
        var statusCode = response.statusCode();
        var reasonPhrase = response.reasonPhrase() != null ? response.reasonPhrase() : getReasonPhrase(statusCode, "unknown");
        var connection = response.connection;

        // 1, 创建 响应行
        var statusLine = new Http1StatusLine(httpVersion, statusCode, reasonPhrase);

        // 编码
        var statusLineStr = statusLine.encode();

        // 处理头相关
        // 1, 处理 连接 相关
        if (headers.connection() == null) {
            if (request.isKeepAlive()) {
                // 正常我们可以忽略设置 KEEP_ALIVE, 但是这里我们显式设置
                headers.connection(KEEP_ALIVE);
            } else {
                headers.connection(CLOSE);
            }
        }

        // 2, 处理 body 相关
        if (expectedLength < 0) {//表示不知道 body 的长度
            // 如果用户已经手动设置了 Content-Length, 我们便不再设置 分块传输
            if (headers.contentLength() == null) {
                headers.transferEncoding(CHUNKED);
            }
        } else if (expectedLength > 0) {//拥有指定长度的响应体
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

        var responseHeaderStr = headers.encode();

        // 标记发送中
        response.senderStatus = ScxHttpSenderStatus.SENDING;

        // 构建头部内容字节
        var h = statusLineStr + "\r\n" + responseHeaderStr + "\r\n";
        var hb = h.getBytes(UTF_8);

        // 写入 socket
        connection.dataWriter.write(hb);

        // 只有明确表示 close 的时候我们才关闭
        var closeConnection = headers.connection() == CLOSE;

        // 只有明确表示 分块的时候才使用分块
        var useChunkedTransfer = headers.transferEncoding() == CHUNKED;

        // 创建 基本 输出流
        var baseByteOutput = new Http1ServerResponseByteOutput(connection, closeConnection, () -> response.senderStatus = SUCCESS);

        // 判断是否采用分块传输
        return useChunkedTransfer ?
            new HttpChunkedByteOutput(baseByteOutput) :
            new ContentLengthByteOutput(baseByteOutput, expectedLength);

    }

    public static ByteOutput sendRequestHeaders(long expectedLength, HttpClientRequest request, Http1ClientConnection connection, Http1Headers headers) throws ScxIOException, AlreadyClosedException {
        // 0, 准备参数
        var method = request.method();
        var uri = request.uri();
        var requestTargetForm = request.requestTargetForm();

        // 1, 创建 请求行
        var requestLine = new Http1RequestLine(method, uri);

        // 根据 requestTargetForm 编码
        var requestLineStr = requestLine.encode(requestTargetForm);

        // 处理头相关
        // 1, 处理 HOST 相关
        if (!headers.contains(HOST)) {
            var port = uri.port();
            if (port != null) {
                headers.set(HOST, uri.host() + ":" + port);
            } else {
                headers.set(HOST, uri.host());
            }
        }

        // 2, 处理 body 相关
        if (expectedLength < 0) {//表示不知道 body 的长度
            // 如果用户已经手动设置了 Content-Length, 我们便不再设置 分块传输
            if (headers.contentLength() == null) {
                headers.transferEncoding(CHUNKED);
            }
        } else if (expectedLength > 0) {//拥有指定长度的响应体
            // 如果用户已经手动设置 分块传输, 我们便不再设置 Content-Length
            if (headers.transferEncoding() != CHUNKED) {
                headers.contentLength(expectedLength);
            }
        } else {
            // body 长度为 0 时 , 分两种情况
            // 1, 是需要明确写入 Content-Length : 0 的
            // 2, 是不需要写入任何长度相关字段
            var hasBody = checkRequestHasBody(method);
            if (hasBody) {
                // 这里同上, 进行分块传输判断
                if (headers.transferEncoding() != CHUNKED) {
                    headers.contentLength(expectedLength);
                }
            }
        }

        var requestHeaderStr = headers.encode();

        // 标记发送中
        request.senderStatus = ScxHttpSenderStatus.SENDING;

        // 构建头部内容字节
        var h = requestLineStr + "\r\n" + requestHeaderStr + "\r\n";
        var hb = h.getBytes(UTF_8);

        // 写入 socket
        connection.dataWriter.write(hb);

        // 只有明确表示 分块的时候才使用分块
        var useChunkedTransfer = headers.transferEncoding() == CHUNKED;

        // 创建 基本 输出流
        var baseByteOutput = new Http1ClientRequestByteOutput(connection.dataWriter, () -> request.senderStatus = ScxHttpSenderStatus.SUCCESS);

        // 判断是否采用分块传输
        return useChunkedTransfer ?
            new HttpChunkedByteOutput(baseByteOutput) :
            new ContentLengthByteOutput(baseByteOutput, expectedLength);

    }

}
