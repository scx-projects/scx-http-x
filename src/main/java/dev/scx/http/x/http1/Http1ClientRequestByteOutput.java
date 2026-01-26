package dev.scx.http.x.http1;

import dev.scx.io.ByteChunk;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxOutputException;
import dev.scx.io.output.AbstractByteOutput;

import static dev.scx.http.x.sender.HttpSenderStatus.SUCCESS;

/// Http1ClientRequestByteOutput
///
/// 本质上是一个 `隔离底层 ByteOutput.close()` + `close() 时触发回调` 的装饰器.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientRequestByteOutput extends AbstractByteOutput {

    private final Http1ClientRequest request;
    private final Http1ClientConnection connection;

    public Http1ClientRequestByteOutput(Http1ClientRequest request, Http1ClientConnection connection) {
        this.request = request;
        this.connection = connection;
    }

    @Override
    public void write(byte b) throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        connection.socketIO.out.write(b);
    }

    @Override
    public void write(ByteChunk b) throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        connection.socketIO.out.write(b);
    }

    @Override
    public void flush() throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        connection.socketIO.out.flush();
    }

    @Override
    public void close() throws ScxOutputException, OutputAlreadyClosedException {
        ensureOpen();

        // 这里中断 close, 改为刷新
        connection.socketIO.out.flush();

        closed = true; // 只有成功关闭才算作 关闭
        request._setSenderStatus(SUCCESS);

    }

}
