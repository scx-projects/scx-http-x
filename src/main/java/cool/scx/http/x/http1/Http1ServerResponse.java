package cool.scx.http.x.http1;

import cool.scx.http.ScxHttpServerRequest;
import cool.scx.http.ScxHttpServerResponse;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.media.MediaWriter;
import cool.scx.http.sender.BodyAlreadySentException;
import cool.scx.http.sender.HttpSendException;
import cool.scx.http.status_code.HttpStatusCode;
import cool.scx.http.status_code.ScxHttpStatusCode;
import cool.scx.http.status_code.ScxHttpStatusCodeHelper;
import cool.scx.http.x.http1.byte_output.Http1ServerResponseByteOutput;
import cool.scx.http.x.http1.byte_output.HttpChunkedByteOutput;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.status_line.Http1StatusLine;
import cool.scx.io.ByteOutput;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.ScxIOException;

import static cool.scx.http.x.http1.Http1Helper.checkResponseHasBody;
import static cool.scx.http.x.http1.headers.connection.Connection.CLOSE;
import static cool.scx.http.x.http1.headers.connection.Connection.KEEP_ALIVE;
import static cool.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;
import static java.nio.charset.StandardCharsets.UTF_8;

/// Http1ServerResponse
///
/// @author scx567888
/// @version 0.0.1
public class Http1ServerResponse implements ScxHttpServerResponse {

    public final Http1ServerConnection connection;

    private final Http1ServerRequest request;
    private ScxHttpStatusCode statusCode;
    private Http1Headers headers;
    private String reasonPhrase;
    private ByteOutput byteOutput;

    Http1ServerResponse(Http1ServerConnection connection, Http1ServerRequest request) {
        this.connection = connection;
        this.request = request;
        this.statusCode = HttpStatusCode.OK;
        this.headers = new Http1Headers();
    }

    @Override
    public ScxHttpServerRequest request() {
        return request;
    }

    @Override
    public ScxHttpStatusCode statusCode() {
        return statusCode;
    }

    @Override
    public ScxHttpServerResponse statusCode(ScxHttpStatusCode code) {
        statusCode = code;
        return this;
    }

    @Override
    public Http1Headers headers() {
        return headers;
    }

    @Override
    public ScxHttpServerResponse headers(ScxHttpHeaders headers) {
        this.headers = new Http1Headers(headers);
        return this;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }

    public Http1ServerResponse reasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
        return this;
    }

    @Override
    public Void send(MediaWriter mediaWriter) throws BodyAlreadySentException {
        var expectedLength = mediaWriter.beforeWrite(headers, request.headers());
        try {
            mediaWriter.write(getByteOutput(expectedLength));
        } catch (ScxIOException e) {
            throw new HttpSendException("发送 HTTP 响应失败 !!!", e);
        } catch (AlreadyClosedException e) {
            throw new BodyAlreadySentException();
        }
        return null;
    }

    @Override
    public boolean isSent() {
        if (byteOutput == null) {
            return false;
        }
        return byteOutput.isClosed();
    }

    private String getReasonPhrase() {
        return reasonPhrase != null ? reasonPhrase : ScxHttpStatusCodeHelper.getReasonPhrase(statusCode, "unknown");
    }

    private ByteOutput getByteOutput(long expectedLength) {
        if (byteOutput == null) {
            byteOutput = sendHeaders(expectedLength);
        }
        return byteOutput;
    }

    private ByteOutput sendHeaders(long expectedLength) {
        // 1, 创建 响应行
        var statusLine = new Http1StatusLine(request.version(), statusCode, getReasonPhrase());

        var statusLineStr = statusLine.encode();

        // 处理头相关
        // 1, 处理 连接相关
        if (headers.connection() == null) {
            if (request.isKeepAlive()) {
                // 正常我们可以忽略设置 KEEP_ALIVE, 但是这里我们显式设置
                headers.connection(KEEP_ALIVE);
            } else {
                headers.connection(CLOSE);
            }
        }

        // 2, 处理 body 相关
        if (expectedLength < 0) {//表示不知道 body 的长度
            // 如果用户已经手动设置了 Content-Length, 我们便不再设置 分块传输
            if (headers.contentLength() == null) {
                headers.transferEncoding(CHUNKED);
            }
        } else if (expectedLength > 0) {//拥有指定长度的响应体
            // 如果用户已经手动设置 分块传输, 我们便不再设置 Content-Length
            if (headers.transferEncoding() != CHUNKED) {
                headers.contentLength(expectedLength);
            }
        } else {
            // body 长度为 0 时 , 分两种情况
            // 1, 是需要明确写入 Content-Length : 0 的
            // 2, 是不需要写入任何长度相关字段
            var hasBody = checkResponseHasBody(statusCode);
            if (hasBody) {
                // 这里同上, 进行分块传输判断
                if (headers.transferEncoding() != CHUNKED) {
                    headers.contentLength(expectedLength);
                }
            }
        }

        var responseHeaderStr = headers.encode();

        //先写入头部内容
        var h = statusLineStr + "\r\n" + responseHeaderStr + "\r\n";
        connection.dataWriter.write(h.getBytes(UTF_8));

        // 只有明确表示 close 的时候我们才关闭
        var closeConnection = headers.connection() == CLOSE;

        // 只有明确表示 分块的时候才使用分块
        var useChunkedTransfer = headers.transferEncoding() == CHUNKED;

        // todo 这里的 Http1ServerResponseOutputStream 应该根据 contentLength 进行限制, 可以使用 使用 LengthBoundedOutput
        var baseByteOutput = new Http1ServerResponseByteOutput(connection, closeConnection);

        // 判断是否采用分块传输
        return useChunkedTransfer ? new HttpChunkedByteOutput(baseByteOutput) : baseByteOutput;
    }

}
