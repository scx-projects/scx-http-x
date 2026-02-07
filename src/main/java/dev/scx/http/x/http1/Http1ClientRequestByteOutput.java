package dev.scx.http.x.http1;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxOutputException;

import static dev.scx.http.x.sender.HttpSenderStatus.SUCCESS;

/// Http1ClientRequestByteOutput
///
/// 本质上是一个 `隔离底层 ByteOutput.close()` + `close() 时触发回调` 的装饰器.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientRequestByteOutput implements ByteOutput {

    private final Http1ClientRequest request;
    private final Http1ClientConnection connection;

    private boolean closed;

    public Http1ClientRequestByteOutput(Http1ClientRequest request, Http1ClientConnection connection) {
        this.request = request;
        this.connection = connection;
        this.closed = false;
    }

    /// 确保现在是打开状态.
    private void ensureOpen() throws OutputAlreadyClosedException {
        if (closed) {
            throw new OutputAlreadyClosedException();
        }
    }

    @Override
    public void write(byte b) throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        connection.endpoint.out.write(b);
    }

    @Override
    public void write(ByteChunk b) throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        connection.endpoint.out.write(b);
    }

    @Override
    public void flush() throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        connection.endpoint.out.flush();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        // 这里中断 close, 改为刷新
        connection.endpoint.out.flush();

        closed = true; // 只有成功关闭才算作 关闭
        request._setSenderStatus(SUCCESS);

    }

}
