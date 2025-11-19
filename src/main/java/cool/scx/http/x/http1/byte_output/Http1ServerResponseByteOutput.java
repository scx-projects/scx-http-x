package cool.scx.http.x.http1.byte_output;

import cool.scx.function.Function0Void;
import cool.scx.http.x.http1.Http1ServerConnection;
import cool.scx.io.ByteChunk;
import cool.scx.io.ByteOutput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;

import java.io.IOException;

/// Http1ServerResponseByteOutput
///
/// @author scx567888
/// @version 0.0.1
public class Http1ServerResponseByteOutput implements ByteOutput {

    private final Http1ServerConnection connection;
    private final boolean closeConnection;
    private final Function0Void<RuntimeException> onClose;
    private boolean closed;

    public Http1ServerResponseByteOutput(Http1ServerConnection connection, boolean closeConnection, Function0Void<RuntimeException> onClose) {
        this.connection = connection;
        this.closeConnection = closeConnection;
        this.onClose = onClose;
        this.closed = false;
    }

    private void ensureOpen() throws AlreadyClosedException {
        if (closed) {
            throw new AlreadyClosedException();
        }
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
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        // 只有明确表示 close 的时候我们才真正关闭底层
        if (closeConnection) {
            try {
                this.connection.close();// 服务器也需要显式关闭连接
            } catch (IOException e) {
                throw new ScxIOException("关闭 Http1ServerConnection 时发生错误 !!!", e);
            }
        } else {
            // 否则只是刷新
            connection.dataWriter.flush();
        }

        closed = true;
        onClose.apply();

    }

}
