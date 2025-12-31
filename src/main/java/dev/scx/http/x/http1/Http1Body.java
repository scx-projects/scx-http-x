package dev.scx.http.x.http1;

import dev.scx.http.body.ScxHttpBody;
import dev.scx.http.media.MediaReader;
import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.io.ByteInput;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.ScxIOException;

/// Http1Body
///
/// @author scx567888
/// @version 0.0.1
public record Http1Body(ByteInput byteInput, Http1Headers headers) implements ScxHttpBody {

    @Override
    public <T> T as(MediaReader<T> mediaReader) throws ScxIOException, AlreadyClosedException {
        return mediaReader.read(byteInput, headers);
    }

}
