package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteInput;
import dev.scx.io.consumer.ByteChunkByteConsumer;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxInputException;

/// ContentLengthByteSupplier
///
/// 本质上是一个对 "源 byteInput", 进行长度校验的 ByteSupplier 装饰器.
///
/// @author scx567888
/// @version 0.0.1
public final class ContentLengthByteSupplier implements HttpBodyByteSupplier {

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
    public ByteChunk get() throws ScxInputException {
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
                throw new ContentLengthBodyTooShortException("Content-Length : " + contentLength + ", Body-Length : " + (contentLength - remaining));
            }
            return null;
        } catch (InputAlreadyClosedException e) {
            // 降级为 ScxInputException 因为在 ContentLengthByteSupplier 的视角来看 就是 输入异常.
            throw new ScxInputException("byteInput already closed", e);
        }
    }

    @Override
    public void close() throws ScxInputException {
        try {
            this.byteInput.close();
        } catch (InputAlreadyClosedException _) {
            // 忽略异常 保证幂等
        }
    }

    @Override
    public Long bodyLength() {
        return contentLength;
    }

}
