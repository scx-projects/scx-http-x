package cool.scx.http.x.http1.body_supplier;

import cool.scx.io.ByteChunk;
import cool.scx.io.ByteOutput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;
import cool.scx.io.supplier.ByteSupplier;

import static cool.scx.http.x.http1.Http1Helper.sendContinue100;

/// 当初次读取的时候 自动响应 Continue-100 响应
public class AutoContinueByteSupplier implements ByteSupplier {

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
                sendContinue100(out);
                continueSent = true;
            } catch (ScxIOException | AlreadyClosedException e) {
                throw new ScxIOException("发送 Continue100 时发生错误,", e);
            }
        }
    }

}
