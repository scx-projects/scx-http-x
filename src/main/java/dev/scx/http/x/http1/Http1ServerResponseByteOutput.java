package dev.scx.http.x.http1;

import dev.scx.http.sender.ScxHttpSenderStatus;
import dev.scx.io.ByteChunk;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.output.AbstractByteOutput;

import static dev.scx.http.x.http1.Http1ServerConnectionHelper.consumeBodyByteInput;

/// Http1ServerResponseByteOutput
///
/// 本质上是一个 `隔离底层 ByteOutput.close()` + `close() 时特殊处理 + 触发回调` 的装饰器.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerResponseByteOutput extends AbstractByteOutput {

    private final Http1ServerConnection connection;
    private final boolean closeConnection;
    private final Http1ServerResponse response;

    public Http1ServerResponseByteOutput(Http1ServerConnection connection, boolean closeConnection, Http1ServerResponse response) {
        this.connection = connection;
        this.closeConnection = closeConnection;
        this.response = response;
    }

    @Override
    public void write(byte b) {
        ensureOpen();

        connection.socketIO.out.write(b);
    }

    @Override
    public void write(ByteChunk b) throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        connection.socketIO.out.write(b);
    }

    @Override
    public void flush() {
        ensureOpen();

        connection.socketIO.out.flush();
    }

    @Override
    public void close() throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        if (closeConnection) {
            // 如果明确表示 close 我们终止 底层 Socket 连接
            this.connection.socketIO.closeQuietly();
        } else {
            // 否则只是刷新
            connection.socketIO.out.flush();

            // 用户处理器可能没有消费完请求体 这里我们帮助消费用户未消费的数据
            consumeBodyByteInput(response.request().body().byteInput());

            // 开启下一次 读取
            connection.start();
        }

        closed = true; // 只有成功关闭才算作 关闭
        response._setSenderStatus(ScxHttpSenderStatus.SUCCESS);
    }

}
