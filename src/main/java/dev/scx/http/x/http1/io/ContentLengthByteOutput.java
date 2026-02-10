package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxOutputException;

/// ContentLengthByteOutput
///
/// 本质上是一个对 "源 byteOutput", 进行长度校验的 ByteOutput 装饰器.
///
/// @author scx567888
/// @version 0.0.1
public final class ContentLengthByteOutput implements ByteOutput {

    private final ByteOutput byteOutput;
    private final long contentLength;
    private long bytesWritten;

    public ContentLengthByteOutput(ByteOutput byteOutput, long contentLength) {
        this.byteOutput = byteOutput;
        this.contentLength = contentLength;
        this.bytesWritten = 0;
    }

    private void ensureMax(int length) throws ScxOutputException {
        if (bytesWritten + length > contentLength) {
            throw new ScxOutputException("写入超出最大长度: 已写入 " + bytesWritten + ", 本次写入 " + length + ", 最大允许 " + contentLength);
        }
    }

    private void ensureMin() throws ScxOutputException {
        if (bytesWritten < contentLength) {
            throw new ScxOutputException("写入长度不足: 已写入 " + bytesWritten + ", 最小长度要求 " + contentLength);
        }
    }

    @Override
    public void write(byte b) throws ScxOutputException, OutputAlreadyClosedException {
        ensureMax(1);
        byteOutput.write(b);
        bytesWritten += 1;
    }

    @Override
    public void write(ByteChunk b) throws ScxOutputException, OutputAlreadyClosedException {
        ensureMax(b.length);
        byteOutput.write(b);
        bytesWritten += b.length;
    }

    @Override
    public void flush() throws ScxOutputException, OutputAlreadyClosedException {
        byteOutput.flush();
    }

    @Override
    public boolean isClosed() {
        return byteOutput.isClosed();
    }

    @Override
    public void close() throws ScxOutputException, OutputAlreadyClosedException {
        ensureMin();
        byteOutput.close();
    }

}
