package cool.scx.http.x.http1.body_supplier;

import cool.scx.io.ByteChunk;
import cool.scx.io.ByteInput;
import cool.scx.io.consumer.ByteChunkByteConsumer;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.NoMoreDataException;
import cool.scx.io.exception.ScxIOException;
import cool.scx.io.supplier.ByteSupplier;

import static cool.scx.http.status_code.HttpStatusCode.BAD_REQUEST;

/// ContentLengthByteSupplier
///
/// @author scx567888
/// @version 0.0.1
public final class ContentLengthByteSupplier implements ByteSupplier {

    private final ByteInput byteInput;
    private final ByteChunkByteConsumer consumer;
    private final long contentLength;
    private long remaining;

    public ContentLengthByteSupplier(ByteInput byteInput, long contentLength) {
        this.byteInput = byteInput;
        this.consumer = new ByteChunkByteConsumer();
        this.contentLength = contentLength;
        this.remaining = contentLength;
    }

    @Override
    public ByteChunk get() throws ScxIOException {
        // 读取够了
        if (remaining <= 0) {
            return null;
        }
        try {
            // 这里我们直接引用 原始 byteInput 中的 ByteChunk, 避免了数组的多次拷贝
            byteInput.read(consumer, remaining);// 我们只尝试拉取一次
            var byteChunk = consumer.byteChunk();
            remaining -= byteChunk.length;
            return byteChunk;
        } catch (NoMoreDataException e) {
            // 如果底层 ByteInput 没数据了, 但是还仍为填满 contentLength 则抛出异常
            if (remaining > 0) {
                throw new BodyTooShortException(BAD_REQUEST, "Content-Length : " + contentLength + ", Body-Length : " + (contentLength - remaining));
            }
            return null;
        } catch (AlreadyClosedException e) {
            throw new ScxIOException("byteInput already closed", e);
        }
    }

    @Override
    public void close() throws ScxIOException {
        try {
            this.byteInput.close();
        } catch (AlreadyClosedException _) {
            // 忽略
        }
    }

    public ByteInput byteInput() {
        return byteInput;
    }

}
