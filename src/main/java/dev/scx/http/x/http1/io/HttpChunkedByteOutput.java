package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxOutputException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/// HttpChunkedByteOutput
///
/// HTTP/1.1 分块传输编码的输出封装
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

    public HttpChunkedByteOutput(ByteOutput byteOutput) {
        this.byteOutput = byteOutput;
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
        return byteOutput.isClosed();
    }

    /// 这里的非幂等性 靠底层的 byteOutput 自身来保证.
    @Override
    public void close() {
        // 写入终结分块
        byteOutput.write(CHUNKED_END_BYTES);
        byteOutput.close();
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
