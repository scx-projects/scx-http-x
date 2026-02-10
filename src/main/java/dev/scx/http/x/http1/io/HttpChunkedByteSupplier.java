package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteInput;
import dev.scx.io.consumer.ByteChunkByteConsumer;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.NoMatchFoundException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxInputException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static dev.scx.io.ByteChunk.EMPTY_BYTE_CHUNK;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/// 用来解析 HttpChunked 分块传输数据 (不支持 chunk extensions 和 trailer headers, 因为根本没人用)
///
/// @author scx567888
/// @version 0.0.1
public final class HttpChunkedByteSupplier implements HttpBodyByteSupplier {

    private static final byte[] CRLF_BYTES = "\r\n".getBytes(ISO_8859_1);

    private final ByteInput byteInput;
    private final long maxLength;
    private final ByteChunkByteConsumer consumer;
    private long position;
    private Status status;
    private long currentChunkRemaining;

    public HttpChunkedByteSupplier(ByteInput byteInput) {
        this(byteInput, Long.MAX_VALUE);
    }

    public HttpChunkedByteSupplier(ByteInput byteInput, long maxLength) {
        this.byteInput = byteInput;
        this.maxLength = maxLength;
        this.consumer = new ByteChunkByteConsumer();
        this.position = 0;
        this.status = Status.READ_CHUNK_SIZE;
        this.currentChunkRemaining = 0;
    }

    public ByteChunk doFinished() {
        return null;
    }

    public ByteChunk doReadChunkSize() throws ScxInputException {
        // 1, 读取 分块长度
        byte[] chunkLengthBytes;
        try {
            chunkLengthBytes = byteInput.readUntil(CRLF_BYTES, 32);
        } catch (NoMatchFoundException e) {
            // 没有在限制长度内读取到 分块长度字段
            throw new HttpChunkedParseException("错误的分块长度 !!!");
        } catch (NoMoreDataException e) {
            throw new HttpChunkedParseException("数据流提前结束, 分块不完整 !!!");
        } catch (InputAlreadyClosedException e) {
            // 降级为 ScxInputException 因为在 HttpChunkedByteSupplier 的视角来看 就是 输入异常.
            throw new ScxInputException("byteInput already closed", e);
        }

        var chunkLengthStr = new String(chunkLengthBytes, StandardCharsets.US_ASCII);
        long chunkLength;
        try {
            chunkLength = Long.parseUnsignedLong(chunkLengthStr, 16);
        } catch (NumberFormatException e) {
            throw new HttpChunkedParseException("错误的分块长度 : " + chunkLengthStr);
        }

        //这里做最大长度限制检查
        checkMaxPayload(chunkLength);

        if (chunkLength == 0) {
            status = Status.READ_TAIL;
        } else {
            status = Status.READ_CHUNK_DATA;
            currentChunkRemaining = chunkLength;
        }

        return EMPTY_BYTE_CHUNK;
    }

    public ByteChunk doReadChunkData() throws ScxInputException {
        // 这里我们直接引用 原始 byteInput 中的 ByteChunk, 避免了数组的多次拷贝
        try {
            byteInput.read(consumer, currentChunkRemaining);
        } catch (NoMoreDataException e) {
            throw new HttpChunkedParseException("数据流提前结束, 分块不完整 !!!");
        } catch (InputAlreadyClosedException e) {
            // 降级为 ScxInputException 因为在 HttpChunkedByteSupplier 的视角来看 就是 输入异常.
            throw new ScxInputException("byteInput already closed", e);
        }

        var byteChunk = consumer.byteChunk();
        currentChunkRemaining -= byteChunk.length;
        // 读取结束跳过最后的分割符, 继续读取
        if (currentChunkRemaining == 0) {
            try {
                var bytes = byteInput.readFully(2);
                // 不是 \r\n
                if (!Arrays.equals(bytes, CRLF_BYTES)) {
                    throw new HttpChunkedParseException("错误的分块终结数据, 不是 \\r\\n !!!");
                }
            } catch (NoMoreDataException e) {
                throw new HttpChunkedParseException("数据流提前结束, 分块不完整 !!!");
            } catch (InputAlreadyClosedException e) {
                // 降级为 ScxInputException 因为在 HttpChunkedByteSupplier 的视角来看 就是 输入异常.
                throw new ScxInputException("byteInput already closed", e);
            }

            status = Status.READ_CHUNK_SIZE;
            return byteChunk;
        }
        return byteChunk;
    }

    public ByteChunk doReadTail() throws ScxInputException {
        byte[] endBytes;
        try {
            endBytes = byteInput.readUntil(CRLF_BYTES);
        } catch (NoMatchFoundException e) {
            throw new HttpChunkedParseException("错误的终结分块, 终结块不完整: 缺少 \\r\\n !!!");
        } catch (NoMoreDataException e) {
            throw new HttpChunkedParseException("数据流提前结束, 分块不完整 !!!");
        } catch (InputAlreadyClosedException e) {
            // 降级为 ScxInputException 因为在 HttpChunkedByteSupplier 的视角来看 就是 输入异常.
            throw new ScxInputException("byteInput already closed", e);
        }

        if (endBytes.length != 0) {
            throw new HttpChunkedParseException("错误的终结分块, 应为空块但发现了内容 !!!");
        }
        status = Status.FINISHED;
        return null;
    }

    public void checkMaxPayload(long chunkLength) throws HttpChunkedBodyTooLargeException {
        // 检查数据块大小是否超过最大值
        if (position + chunkLength > maxLength) {
            throw new HttpChunkedBodyTooLargeException("HttpChunk 长度超过最大接受大小 !!!");
        }
        position += chunkLength;
    }

    @Override
    public ByteChunk get() throws ScxInputException {
        return switch (status) {
            case FINISHED -> doFinished();
            case READ_CHUNK_SIZE -> doReadChunkSize();
            case READ_CHUNK_DATA -> doReadChunkData();
            case READ_TAIL -> doReadTail();
        };
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
        // 长度未知
        return null;
    }

    /// 读取状态
    private enum Status {
        // 读取 分块长度
        READ_CHUNK_SIZE,
        // 读取 分块数据
        READ_CHUNK_DATA,
        // 读取尾部
        READ_TAIL,
        // 结束
        FINISHED
    }

}
