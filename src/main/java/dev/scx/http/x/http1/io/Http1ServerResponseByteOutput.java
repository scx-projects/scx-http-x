package dev.scx.http.x.http1.io;

import dev.scx.function.Function0Void;
import dev.scx.http.x.http1.Http1ServerConnection;
import dev.scx.io.ByteChunk;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.output.AbstractByteOutput;

/// Http1ServerResponseByteOutput
///
/// 本质上是一个 `隔离底层 ByteOutput.close()` + `close() 时特殊处理 + 触发回调` 的装饰器.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerResponseByteOutput extends AbstractByteOutput {

    private final Http1ServerConnection connection;
    private final boolean closeConnection;
    private final Function0Void<RuntimeException> onClose;

    public Http1ServerResponseByteOutput(Http1ServerConnection connection, boolean closeConnection, Function0Void<RuntimeException> onClose) {
        this.connection = connection;
        this.closeConnection = closeConnection;
        this.onClose = onClose;
    }

    @Override
    public void write(byte b) {
        ensureOpen();

        connection.dataWriter.write(b);
    }

    @Override
    public void write(ByteChunk b) throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        connection.dataWriter.write(b);
    }

    @Override
    public void flush() {
        ensureOpen();

        connection.dataWriter.flush();
    }

    @Override
    public void close() throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        if (closeConnection) {
            // 如果明确表示 close 我们终止 连接
            this.connection.stop();
        } else {
            // 否则只是刷新
            connection.dataWriter.flush();
        }

        closed = true; // 只有成功关闭才算作 关闭
        onClose.apply();

    }

}
