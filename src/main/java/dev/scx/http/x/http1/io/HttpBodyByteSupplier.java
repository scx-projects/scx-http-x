package dev.scx.http.x.http1.io;

import dev.scx.io.supplier.ByteSupplier;

public interface HttpBodyByteSupplier extends ByteSupplier {

    Long bodyLength();

}
