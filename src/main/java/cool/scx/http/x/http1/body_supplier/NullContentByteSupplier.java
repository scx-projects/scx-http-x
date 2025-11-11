package cool.scx.http.x.http1.body_supplier;

import cool.scx.io.ByteChunk;
import cool.scx.io.ByteInput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;
import cool.scx.io.supplier.ByteSupplier;

public class NullContentByteSupplier implements ByteSupplier {

    private final ByteInput byteInput;

    public NullContentByteSupplier(ByteInput byteInput) {
        this.byteInput = byteInput;
    }

    @Override
    public ByteChunk get() {
        return null;
    }

    @Override
    public void close() throws ScxIOException {
        try {
            byteInput.close();
        } catch (AlreadyClosedException _) {
            // 忽略
        }
    }

}
