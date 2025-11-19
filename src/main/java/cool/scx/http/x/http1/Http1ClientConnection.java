package cool.scx.http.x.http1;

import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.media.MediaWriter;
import cool.scx.http.sender.ScxHttpSenderStatus;
import cool.scx.http.x.HttpClientRequest;
import cool.scx.http.x.http1.byte_output.ContentLengthByteOutput;
import cool.scx.http.x.http1.byte_output.Http1ClientRequestByteOutput;
import cool.scx.http.x.http1.byte_output.HttpChunkedByteOutput;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.request_line.Http1RequestLine;
import cool.scx.io.ByteInput;
import cool.scx.io.ByteOutput;
import cool.scx.io.DefaultByteInput;
import cool.scx.io.ScxIO;
import cool.scx.tcp.ScxTCPSocket;

import java.io.IOException;

import static cool.scx.http.headers.HttpHeaderName.HOST;
import static cool.scx.http.x.http1.Http1Helper.checkRequestHasBody;
import static cool.scx.http.x.http1.Http1Reader.*;
import static cool.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;
import static cool.scx.io.supplier.ClosePolicyByteSupplier.noCloseDrain;
import static java.nio.charset.StandardCharsets.UTF_8;

/// Http1ClientConnection
///
/// @author scx567888
/// @version 0.0.1
public class Http1ClientConnection {

    public final ScxTCPSocket tcpSocket;
    public final ByteInput dataReader;
    public final ByteOutput dataWriter;

    private final Http1ClientConnectionOptions options;

    public Http1ClientConnection(ScxTCPSocket tcpSocket, Http1ClientConnectionOptions options) {
        this.tcpSocket = tcpSocket;
        this.dataReader = ScxIO.createByteInput(tcpSocket.inputStream());
        this.dataWriter = ScxIO.createByteOutput(tcpSocket.outputStream());
        this.options = options;
    }

    private ByteOutput sendHeaders(long expectedLength, HttpClientRequest request, Http1Headers headers) {
        // 1, 创建 请求行
        var requestLine = new Http1RequestLine(request.method(), request.uri());

        // 根据 requestTargetForm 编码
        var requestLineStr = requestLine.encode(request.requestTargetForm());

        // 处理头相关
        // 1, 处理 HOST 相关
        if (!headers.contains(HOST)) {
            var port = request.uri().port();
            if (port != null) {
                headers.set(HOST, request.uri().host() + ":" + port);
            } else {
                headers.set(HOST, request.uri().host());
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
            var hasBody = checkRequestHasBody(request.method());
            if (hasBody) {
                // 这里同上, 进行分块传输判断
                if (headers.transferEncoding() != CHUNKED) {
                    headers.contentLength(expectedLength);
                }
            }
        }

        var requestHeaderStr = headers.encode();

        request.senderStatus = ScxHttpSenderStatus.SENDING;

        //先写入头部内容
        var h = requestLineStr + "\r\n" + requestHeaderStr + "\r\n";
        dataWriter.write(h.getBytes(UTF_8));

        // 只有明确表示 分块的时候才使用分块
        var useChunkedTransfer = headers.transferEncoding() == CHUNKED;

        // 创建 基本 输出流
        var baseByteOutput = new Http1ClientRequestByteOutput(dataWriter, () -> request.senderStatus = ScxHttpSenderStatus.SUCCESS);

        return useChunkedTransfer ?
            new HttpChunkedByteOutput(baseByteOutput) :
            new ContentLengthByteOutput(baseByteOutput, expectedLength);
    }

    public Http1ClientConnection sendRequest(HttpClientRequest request, MediaWriter writer) throws IOException {
        // 复制一份头
        var headers = new Http1Headers(request.headers());

        // 处理 headers 以及获取 请求长度
        var expectedLength = writer.beforeWrite(headers, ScxHttpHeaders.of());

        // 发送头
        var byteOutput = sendHeaders(expectedLength, request, headers);

        // 调用处理器
        writer.write(byteOutput);

        return this;
    }

    public Http1ClientResponse waitResponse() {
        //1, 读取状态行
        var statusLine = readStatusLine(dataReader, options.maxStatusLineSize());

        //2, 读取响应头
        var headers = readHeaders(dataReader, options.maxHeaderSize());

        //3, 读取响应体
        var bodyByteSupplier = readBodyByteInput(headers, dataReader, options.maxPayloadSize());

        //4, 创建一个 ByteInput, 要求是 1, 要隔离 底层 close, 2, 同时在 close 的时候还要排空流.
        var bodyByteInput = new DefaultByteInput(noCloseDrain(bodyByteSupplier));

        return new Http1ClientResponse(statusLine, headers, bodyByteInput);
    }

}
