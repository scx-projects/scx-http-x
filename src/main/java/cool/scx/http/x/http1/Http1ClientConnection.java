package cool.scx.http.x.http1;

import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.media.MediaWriter;
import cool.scx.http.x.HttpClientRequest;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.io.ByteInput;
import cool.scx.io.ByteOutput;
import cool.scx.io.DefaultByteInput;
import cool.scx.io.ScxIO;

import java.io.IOException;
import java.net.Socket;

import static cool.scx.http.x.http1.Http1Reader.*;
import static cool.scx.http.x.http1.Http1Writer.sendRequestHeaders;
import static cool.scx.io.supplier.ClosePolicyByteSupplier.noCloseDrain;

/// Http1ClientConnection
///
/// @author scx567888
/// @version 0.0.1
public class Http1ClientConnection {

    public final Socket tcpSocket;
    public final ByteInput dataReader;
    public final ByteOutput dataWriter;

    private final Http1ClientConnectionOptions options;

    public Http1ClientConnection(Socket tcpSocket, Http1ClientConnectionOptions options) throws IOException {
        this.tcpSocket = tcpSocket;
        this.dataReader = ScxIO.createByteInput(tcpSocket.getInputStream());
        this.dataWriter = ScxIO.createByteOutput(tcpSocket.getOutputStream());
        this.options = options;
    }

    public Http1ClientConnection sendRequest(HttpClientRequest request, MediaWriter writer) throws IOException {
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
