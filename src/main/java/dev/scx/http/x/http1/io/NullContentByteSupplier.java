package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.supplier.ByteSupplier;

/// NullContentByteSupplier
///
/// @author scx567888
/// @version 0.0.1
public final class NullContentByteSupplier implements ByteSupplier {

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
            // 忽略 AlreadyClosedException, 因为 ByteSupplier 的 close 要求是幂等的
        }
    }

}
