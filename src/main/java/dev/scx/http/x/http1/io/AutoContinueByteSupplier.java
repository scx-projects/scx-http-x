package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.supplier.ByteSupplier;

import static dev.scx.http.x.http1.io.Http1Writer.writeContinue100;

/// 当初次读取的时候 自动响应 Continue-100 响应
///
/// @author scx567888
/// @version 0.0.1
public final class AutoContinueByteSupplier implements ByteSupplier {

    private final ByteSupplier byteSupplier;
    private final ByteOutput out;
    private boolean continueSent;

    public AutoContinueByteSupplier(ByteSupplier byteSupplier, ByteOutput out) {
        this.byteSupplier = byteSupplier;
        this.out = out;
        this.continueSent = false;
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
                writeContinue100(out);
                continueSent = true;
            } catch (ScxIOException | AlreadyClosedException e) {
                throw new ScxIOException("发送 Continue100 时发生错误 : ", e);
            }
        }
    }

}
