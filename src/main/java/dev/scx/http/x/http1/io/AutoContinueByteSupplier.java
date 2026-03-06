package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteOutput;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxInputException;
import dev.scx.io.exception.ScxOutputException;

import static dev.scx.http.x.http1.io.Http1Writer.writeContinue100;

/// 当初次读取的时候 自动响应 Continue-100 响应
///
/// @author scx567888
/// @version 0.0.1
public final class AutoContinueByteSupplier implements HttpBodyByteSupplier {

    private final HttpBodyByteSupplier byteSupplier;
    private final ByteOutput out;
    private boolean continueSent;

    public AutoContinueByteSupplier(HttpBodyByteSupplier byteSupplier, ByteOutput out) {
        this.byteSupplier = byteSupplier;
        this.out = out;
        this.continueSent = false;
    }

    @Override
    public ByteChunk get() throws ScxInputException {
        trySendContinueResponse();
        return byteSupplier.get();
    }

    @Override
    public void close() throws ScxInputException {
        byteSupplier.close();
    }

    private void trySendContinueResponse() throws ScxInputException {
        if (!continueSent) {
            try {
                writeContinue100(out);
                continueSent = true;
            } catch (ScxOutputException | OutputAlreadyClosedException e) {
                throw new ScxInputException("发送 Continue100 时发生错误 : ", e);
            }
        }
    }

    @Override
    public Long bodyLength() {
        // 返回上游长度
        return byteSupplier.bodyLength();
    }

}
