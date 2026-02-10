package dev.scx.http.x.http1;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.io.ByteInput;
import dev.scx.io.ByteInputMark;
import dev.scx.io.ByteMatchResult;
import dev.scx.io.consumer.ByteConsumer;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.NoMatchFoundException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxInputException;
import dev.scx.io.indexer.ByteIndexer;

/// Http1ClientResponseByteInput
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ClientResponseByteInput implements ByteInput {

    private final Http1ClientConnection connection;
    private final Http1StatusLine statusLine;
    private final Http1Headers headers;
    private boolean closed;

    public Http1ClientResponseByteInput(Http1ClientConnection connection, Http1StatusLine statusLine, Http1Headers headers) {
        this.connection = connection;
        this.statusLine = statusLine;
        this.headers = headers;
        this.closed = false;
    }

    /// 确保现在是打开状态.
    private void ensureOpen() throws InputAlreadyClosedException {
        if (this.closed) {
            throw new InputAlreadyClosedException();
        }
    }

    @Override
    public byte read() throws NoMoreDataException, ScxInputException, InputAlreadyClosedException {
        ensureOpen();

        return connection.endpoint.in.read();
    }

    @Override
    public void read(ByteConsumer byteConsumer, long maxLength) throws NoMoreDataException, ScxInputException, InputAlreadyClosedException, ScxWrappedException {
        ensureOpen();

        connection.endpoint.in.read(byteConsumer, maxLength);
    }

    @Override
    public void readUpTo(ByteConsumer byteConsumer, long length) throws NoMoreDataException, ScxInputException, InputAlreadyClosedException, ScxWrappedException {
        ensureOpen();

        connection.endpoint.in.readUpTo(byteConsumer, length);
    }

    @Override
    public void readFully(ByteConsumer byteConsumer, long length) throws NoMoreDataException, ScxInputException, InputAlreadyClosedException, ScxWrappedException {
        ensureOpen();

        connection.endpoint.in.readFully(byteConsumer, length);
    }

    @Override
    public byte peek() throws NoMoreDataException, ScxInputException, InputAlreadyClosedException {
        ensureOpen();

        return connection.endpoint.in.peek();
    }

    @Override
    public void peek(ByteConsumer byteConsumer, long maxLength) throws NoMoreDataException, ScxInputException, InputAlreadyClosedException, ScxWrappedException {
        ensureOpen();

        connection.endpoint.in.peek(byteConsumer, maxLength);
    }

    @Override
    public void peekUpTo(ByteConsumer byteConsumer, long length) throws NoMoreDataException, ScxInputException, InputAlreadyClosedException, ScxWrappedException {
        ensureOpen();

        connection.endpoint.in.peekUpTo(byteConsumer, length);
    }

    @Override
    public void peekFully(ByteConsumer byteConsumer, long length) throws NoMoreDataException, ScxInputException, InputAlreadyClosedException, ScxWrappedException {
        ensureOpen();

        connection.endpoint.in.peekFully(byteConsumer, length);
    }

    @Override
    public ByteMatchResult indexOf(ByteIndexer indexer, long maxLength) throws NoMatchFoundException, NoMoreDataException, ScxInputException, InputAlreadyClosedException {
        ensureOpen();

        return connection.endpoint.in.indexOf(indexer, maxLength);
    }

    @Override
    public ByteInputMark mark() throws InputAlreadyClosedException {
        ensureOpen();

        return connection.endpoint.in.mark();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws ScxInputException, InputAlreadyClosedException {
        ensureOpen();

        // 这里中断 close,

        closed = true; // 只有成功关闭才算作 关闭

        // 通知 Http1ClientConnection 流已结束
        connection.onResponseEnd(statusLine, headers);

    }

}
