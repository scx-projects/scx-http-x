package dev.scx.http.x.http1;

import dev.scx.http.ScxHttpServerResponse;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.media.MediaWriter;
import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.sender.ScxHttpSenderStatus;
import dev.scx.http.status_code.HttpStatusCode;
import dev.scx.http.status_code.ScxHttpStatusCode;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

import java.util.concurrent.locks.ReentrantLock;

import static dev.scx.http.sender.ScxHttpSenderStatus.NOT_SENT;

/// Http1ServerResponse
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerResponse implements ScxHttpServerResponse {

    private final Http1ServerRequest request;
    public final Http1ServerConnection connection;
    private final ReentrantLock sendLock; // 避免用户 多线程 send 搞乱状态
    private ScxHttpStatusCode statusCode;
    private Http1Headers headers;
    private String reasonPhrase;
    private ScxHttpSenderStatus senderStatus;

    Http1ServerResponse(Http1ServerRequest request, Http1ServerConnection connection) {
        this.request = request;
        this.connection = connection;
        this.sendLock = new ReentrantLock();
        this.statusCode = HttpStatusCode.OK;
        this.headers = new Http1Headers();
        this.reasonPhrase = null;
        this.senderStatus = NOT_SENT;
    }

    @Override
    public Http1ServerRequest request() {
        return request;
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return statusCode;
    }

    @Override
    public ScxHttpServerResponse statusCode(ScxHttpStatusCode statusCode) {
        this.statusCode = statusCode;
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

    @Override
    public Void send(MediaWriter mediaWriter) throws IllegalSenderStateException, ScxIOException, AlreadyClosedException {
        sendLock.lock();
        try {
            connection.sendResponse(this, mediaWriter);
            return null;
        } finally {
            sendLock.unlock();
        }
    }

    @Override
    public ScxHttpSenderStatus senderStatus() {
        return senderStatus;
    }

    /// 设置发送器状态 (内部方法 只应该由 框架 调用)
    public void _setSenderStatus(ScxHttpSenderStatus senderStatus) {
        this.senderStatus = senderStatus;
    }

}
