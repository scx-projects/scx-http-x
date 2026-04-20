package dev.scx.http.x.http1.io;

import dev.scx.io.ByteChunk;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.ScxInputException;

/// NullContentByteSupplier
///
/// @author scx567888
/// @version 0.0.1
public final class NullContentByteSupplier implements HttpBodyByteSupplier {

    private final ByteInput byteInput;

    public NullContentByteSupplier(ByteInput byteInput) {
        this.byteInput = byteInput;
    }

    @Override
    public ByteChunk get() {
        return null;
    }

    @Override
    public void close() throws ScxInputException {
        byteInput.close();
    }

    @Override
    public Long bodyLength() {
        return 0L;
    }

}
