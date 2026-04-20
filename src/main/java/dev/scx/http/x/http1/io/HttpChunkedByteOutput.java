package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxOutputException;
import dev.scx.io.output.LengthBoundedByteOutput;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/// HttpChunkedByteOutput
///
/// HTTP/1.1 分块传输编码的输出封装
///
/// 设计思路可以参考 [LengthBoundedByteOutput]
///
/// @author scx567888
/// @version 0.0.1
public final class HttpChunkedByteOutput implements ByteOutput {

    private static final ByteChunk CHUNKED_END_BYTES = ByteChunk.of("0\r\n\r\n".getBytes(ISO_8859_1));
    private static final ByteChunk CRLF_BYTES = ByteChunk.of("\r\n".getBytes(ISO_8859_1));

    private static final byte[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private final ByteOutput byteOutput;

    // 仅用于保证 close() 的幂等性, 避免重复执行 尾部写出() 以及底层 close().
    // 该标志只在 close() 成功完成后才会被置为 true.
    // 注意：该标志不表示底层 ByteOutput 是否已关闭, 也不用于校验 write/flush 等操作.
    private boolean closed;

    public HttpChunkedByteOutput(ByteOutput byteOutput) {
        this.byteOutput = byteOutput;
        this.closed = false;
    }

    @Override
    public void write(byte b) throws ScxOutputException, OutputAlreadyClosedException {
        write(ByteChunk.of(new byte[]{b}));
    }

    @Override
    public void write(ByteChunk b) throws ScxOutputException, OutputAlreadyClosedException {
        // 0 长度无需写入
        if (b.length == 0) {
            return;
        }
        // 写入分块
        writeHexLength(b.length);      // 1, 写入块大小
        byteOutput.write(CRLF_BYTES);  // 2, 写入 块大小 结束符
        byteOutput.write(b);           // 3, 写入 数据块 内容
        byteOutput.write(CRLF_BYTES);  // 4, 写入 分块 结束符
    }

    @Override
    public void flush() {
        byteOutput.flush();
    }

    @Override
    public boolean isClosed() {
        // isClosed() 直接代理到底层 ByteOutput.
        //
        // 设计原因:
        // 1) HttpChunkedByteOutput 只是一个对内容追加头尾分块的包装器, 不拥有底层资源的生命周期.
        // 2) write/flush 等操作的可用性应以底层状态为准, 而不是以本类的 close 幂等门闩为准.
        // 3) 本类中的 closed 字段仅用于 close() 幂等控制, 并不代表资源是否真正关闭.
        // 4) 因此 isClosed() 返回底层状态, 作为对底层资源状态的观测接口, 而非控制流判断依据.
        return byteOutput.isClosed();
    }

    @Override
    public void close() throws ScxOutputException {
        if (closed) {
            return;
        }

        // 写入终结分块
        try {
            byteOutput.write(CHUNKED_END_BYTES);
        } catch (OutputAlreadyClosedException e) {
            throw new ScxOutputException("byteOutput already closed", e);
        }

        // 关闭上层
        byteOutput.close();

        closed = true; // 只有成功 close 才置位
    }

    /// 直接写入十六进制表示的块大小
    private void writeHexLength(int value) {
        // 最大值 0xFFFFFFFF（32 位无符号整数）转换为十六进制最多 8 个字符
        var bytes = new byte[8];
        var pos = 8;
        do {
            pos = pos - 1;
            bytes[pos] = HEX_DIGITS[value & 0xF]; // 取最后 4 位
            value = value >>> 4; // 右移 4 位
        } while (value != 0);

        byteOutput.write(ByteChunk.of(bytes, pos, 8));
    }

}
