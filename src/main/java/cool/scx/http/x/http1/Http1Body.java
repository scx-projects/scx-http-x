package cool.scx.http.x.http1;

import cool.scx.http.body.BodyAlreadyConsumedException;
import cool.scx.http.body.BodyReadException;
import cool.scx.http.body.ScxHttpBody;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.media.MediaReader;
import cool.scx.io.ByteInput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;

public record Http1Body(ByteInput byteInput, ScxHttpHeaders headers) implements ScxHttpBody {

    @Override
    public <T> T as(MediaReader<T> mediaReader) throws BodyAlreadyConsumedException, BodyReadException {
        try {
            return mediaReader.read(byteInput, headers);
        } catch (ScxIOException e) {
            throw new BodyReadException(e);
        } catch (AlreadyClosedException e) {
            throw new BodyAlreadyConsumedException();
        }
    }

}
