package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.supplier.ByteSupplier;

/// 当初次读取的时候 自动响应 Continue-100 响应
///
/// @author scx567888
/// @version 0.0.1
public final class AutoContinueByteSupplier implements ByteSupplier {

    private static final ByteChunk CONTINUE_100_BYTES = ByteChunk.of("HTTP/1.1 100 Continue\r\n\r\n");

    private final ByteSupplier byteSupplier;
    private final ByteOutput out;
    private boolean continueSent;

    public AutoContinueByteSupplier(ByteSupplier byteSupplier, ByteOutput out) {
        this.byteSupplier = byteSupplier;
        this.out = out;
        this.continueSent = false;
    }

    /// 发送 CONTINUE_100
    public static void sendContinue100(ByteOutput byteOutput) throws ScxIOException, AlreadyClosedException {
        byteOutput.write(CONTINUE_100_BYTES);
    }

    @Override
    public ByteChunk get() throws ScxIOException {
        trySendContinueResponse();
        return byteSupplier.get();
    }

    @Override
    public void close() throws ScxIOException {
        byteSupplier.close();
    }

    private void trySendContinueResponse() {
        if (!continueSent) {
            try {
                sendContinue100(out);
                continueSent = true;
            } catch (ScxIOException | AlreadyClosedException e) {
                throw new ScxIOException("发送 Continue100 时发生错误,", e);
            }
        }
    }

}
