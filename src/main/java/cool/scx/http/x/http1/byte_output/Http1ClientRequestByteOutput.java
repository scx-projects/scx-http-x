package cool.scx.http.x.http1.byte_output;

import cool.scx.http.sender.ScxHttpSenderStatus;
import cool.scx.http.x.HttpClientRequest;
import cool.scx.io.ByteChunk;
import cool.scx.io.ByteOutput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;

/// Http1ClientRequestByteOutput
///
/// @author scx567888
/// @version 0.0.1
public class Http1ClientRequestByteOutput implements ByteOutput {

    private final ByteOutput byteOutput;
    private final HttpClientRequest request;

    private boolean closed;

    public Http1ClientRequestByteOutput(ByteOutput byteOutput, HttpClientRequest request) {
        this.byteOutput = byteOutput;
        this.request = request;
        this.closed = false;
    }

    private void ensureOpen() throws AlreadyClosedException {
        if (closed) {
            throw new AlreadyClosedException();
        }
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
        request.senderStatus = ScxHttpSenderStatus.SENT;
    }

    public ByteOutput byteOutput() {
        return byteOutput;
    }

}
