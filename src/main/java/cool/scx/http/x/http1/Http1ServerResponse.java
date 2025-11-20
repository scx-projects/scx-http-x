package cool.scx.http.x.http1;

import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.ScxHttpServerResponse;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.media.MediaWriter;
import cool.scx.http.sender.HttpSendException;
import cool.scx.http.sender.IllegalSenderStateException;
import cool.scx.http.sender.ScxHttpSenderStatus;
import cool.scx.http.status_code.HttpStatusCode;
import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.io.ByteOutput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import static cool.scx.http.sender.ScxHttpSenderStatus.FAILED;
import static cool.scx.http.sender.ScxHttpSenderStatus.NOT_SENT;
import static cool.scx.http.x.http1.Http1Writer.sendResponseHeaders;

/// Http1ServerResponse
///
/// @author scx567888
/// @version 0.0.1
public class Http1ServerResponse implements ScxHttpServerResponse {

    public final Http1ServerConnection connection;

    private final Http1ServerRequest request;
    private final ReentrantLock sendLock;
    private ScxHttpStatusCode statusCode;
    private Http1Headers headers;
    private String reasonPhrase;
    private ScxHttpSenderStatus senderStatus;

    Http1ServerResponse(Http1ServerConnection connection, Http1ServerRequest request) {
        this.connection = connection;
        this.request = request;
        this.sendLock = new ReentrantLock();
        this.statusCode = HttpStatusCode.OK;
        this.headers = new Http1Headers();
        this.reasonPhrase = null;
        this.senderStatus = NOT_SENT;
    }

    @Override
    public ScxHttpServerRequest request() {
        return request;
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return statusCode;
    }

    @Override
    public ScxHttpServerResponse statusCode(ScxHttpStatusCode code) {
        statusCode = code;
        return this;
    }

    @Override
    public Http1Headers headers() {
        return headers;
    }

    @Override
    public ScxHttpServerResponse headers(ScxHttpHeaders headers) {
        this.headers = new Http1Headers(headers);
        return this;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }

    public Http1ServerResponse reasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    private Void send0(MediaWriter mediaWriter) throws IllegalSenderStateException, HttpSendException {

        // 检查发送状态
        if (senderStatus != NOT_SENT) {
            throw new IllegalSenderStateException(senderStatus);
        }

        // 复制一份头
        var tempHeaders = new Http1Headers(this.headers);

        // 处理 headers 以及获取 请求长度
        var expectedLength = mediaWriter.beforeWrite(tempHeaders, request.headers());

        // 发送头过程中出现错误 应该立即关闭连接
        ByteOutput byteOutput;
        try {
            byteOutput = sendResponseHeaders(expectedLength, request, this, tempHeaders);
        } catch (ScxIOException | AlreadyClosedException e) {
            // 标记发送失败
            senderStatus = FAILED;
            // todo 这里如果不关闭呢? 由上层处理 ?
            // 关闭底层连接
            try {
                connection.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw new HttpSendException("发送 HTTP 响应头失败 !!!", e);
        }

        try {
            mediaWriter.write(byteOutput);
        } catch (ScxIOException e) {
            // 标记发送失败
            senderStatus = FAILED;
            // 关闭底层连接
            try {
                connection.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw new HttpSendException("发送 HTTP 响应体失败 !!!", e);
        } catch (AlreadyClosedException e) {
            throw new IllegalSenderStateException(senderStatus);
        }

        return null;
    }

    @Override
    public Void send(MediaWriter mediaWriter) throws IllegalSenderStateException, HttpSendException {
        sendLock.lock();
        try {
            return send0(mediaWriter);
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public ScxHttpSenderStatus senderStatus() {
        return senderStatus;
    }

    /// 内部方法 只应该由 Http1Writer 调用
    void _setSenderStatus(ScxHttpSenderStatus senderStatus) {
        this.senderStatus = senderStatus;
    }

}
