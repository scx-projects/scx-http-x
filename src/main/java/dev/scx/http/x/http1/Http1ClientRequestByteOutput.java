package dev.scx.http.x.http1;

import dev.scx.http.sender.ScxHttpSenderStatus;
import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.output.AbstractByteOutput;

/// Http1ClientRequestByteOutput
///
/// 本质上是一个 `隔离底层 ByteOutput.close()` + `close() 时触发回调` 的装饰器.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientRequestByteOutput extends AbstractByteOutput {

    private final ByteOutput byteOutput;
    private final Http1ClientRequest request;

    public Http1ClientRequestByteOutput(ByteOutput byteOutput, Http1ClientRequest request) {
        this.byteOutput = byteOutput;
        this.request = request;
    }

    @Override
    public void write(byte b) throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        this.byteOutput.write(b);
    }

    @Override
    public void write(ByteChunk b) throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        this.byteOutput.write(b);
    }

    @Override
    public void flush() throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        this.byteOutput.flush();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        // 这里中断 close, 改为刷新
        this.byteOutput.flush();

        closed = true; // 只有成功关闭才算作 关闭
        request._setSenderStatus(ScxHttpSenderStatus.SUCCESS);

    }

    public ByteOutput byteOutput() {
        return byteOutput;
    }

}
