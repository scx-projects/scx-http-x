package dev.scx.http.x.http1;

import dev.scx.http.sender.ScxHttpSenderStatus;
import dev.scx.http.x.SocketIO;
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

    private final SocketIO socketIO;
    private final Http1ServerResponse response;

    public Http1ServerResponseByteOutput(SocketIO socketIO, Http1ServerResponse response) {
        this.socketIO = socketIO;
        this.response = response;
    }

    @Override
    public void write(byte b) {
        ensureOpen();

        socketIO.out.write(b);
    }

    @Override
    public void write(ByteChunk b) throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        socketIO.out.write(b);
    }

    @Override
    public void flush() {
        ensureOpen();

        socketIO.out.flush();
    }

    @Override
    public void close() throws ScxIOException, AlreadyClosedException {
        ensureOpen();

        // 通知 Http1ServerResponse 流已关闭
        response.onByteOutputClose();

        closed = true; // 只有成功关闭才算作 关闭
        response._setSenderStatus(ScxHttpSenderStatus.SUCCESS);
    }

}
