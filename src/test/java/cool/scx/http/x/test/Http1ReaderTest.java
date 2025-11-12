package cool.scx.http.x.test;

import cool.scx.http.exception.ContentTooLargeException;
import cool.scx.http.exception.URITooLongException;
import cool.scx.http.x.http1.CloseConnectionException;
import cool.scx.http.x.http1.Http1Reader;
import cool.scx.http.x.http1.body_supplier.BodyTooLargeException;
import cool.scx.http.x.http1.body_supplier.BodyTooShortException;
import cool.scx.http.x.http1.body_supplier.HttpChunkedParseException;
import cool.scx.http.x.http1.byte_output.HttpChunkedByteOutput;
import cool.scx.http.x.http1.exception.HttpVersionNotSupportedException;
import cool.scx.http.x.http1.exception.InvalidHttpRequestLineException;
import cool.scx.http.x.http1.exception.RequestHeaderFieldsTooLargeException;
import cool.scx.http.x.http1.headers.Http1Headers;
import cool.scx.http.x.http1.request_line.RequestTargetForm;
import cool.scx.io.ByteArrayByteOutput;
import cool.scx.io.DefaultByteInput;
import cool.scx.io.ScxIO;
import cool.scx.io.exception.AlreadyClosedException;
import cool.scx.io.exception.NoMoreDataException;
import cool.scx.io.exception.ScxIOException;
import cool.scx.io.supplier.InputStreamByteSupplier;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;

import static cool.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;

public class Http1ReaderTest {

    public static void main(String[] args) throws NoMoreDataException {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
        test9();
        test10();
        test11();
        test12();
        test13();
        test14();
        test15();
        test16();
        test17();
        test18();
        test19_invalidChunkSize();
        test20_incompleteChunkData();
        test21_missingChunkEndCRLF();
        test22_tailNotEmpty();
        test23_exceedMaxLength();
        test24_EOFBeforeComplete();
        test25_invalidChunkSize();
        test26_incompleteChunkData();
        test27_missingChunkEndCRLF();
        test28_tailNotEmpty();
        test29_EOFBeforeComplete();
        test30_multipleTinyChunks();
        test31_singleChunkExactlyMaxLength();
        test32_zeroLengthChunkNormal();
        test33_singleByteChunk();
        test34_multipleVariableChunks();
        test35_fragmentedStream();
        test36_CRLFError();
        test37_largeChunkWithinMaxLength();
    }

    @Test
    public static void test1() {
        // 测试超长
        var rawInput = ScxIO.createByteInput("GET /hello HTTP/1.1\r\n".getBytes());
        Assert.assertThrows(URITooLongException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 10);
        });
    }

    @Test
    public static void test2() {
        // 测试正常解析
        var rawInput = ScxIO.createByteInput("GET /hello?a=100 HTTP/1.1\r\n".getBytes());
        var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        Assert.assertEquals(http1RequestLine.encode(), "GET /hello?a=100 HTTP/1.1");
    }

    @Test
    public static void test3() {
        // 测试正常解析
        var rawInput = ScxIO.createByteInput("GET http://www.abc.com/hello HTTP/1.1\r\n".getBytes());
        var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        Assert.assertEquals(http1RequestLine.encode(RequestTargetForm.ABSOLUTE_FORM), "GET http://www.abc.com/hello HTTP/1.1");
    }

    @Test
    public static void test4() {
        // 测试正常解析
        var rawInput = ScxIO.createByteInput("GET http://www.abc.com:9989/hello HTTP/1.1\r\n".getBytes());
        var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        Assert.assertEquals(http1RequestLine.encode(RequestTargetForm.ABSOLUTE_FORM), "GET http://www.abc.com:9989/hello HTTP/1.1");
    }

    @Test
    public static void test5() {
        // 测试不正常 头
        var rawInput = ScxIO.createByteInput("xxxx\r\n".getBytes());
        Assert.assertThrows(InvalidHttpRequestLineException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test6() {
        // 测试不正常 版本号
        var rawInput = ScxIO.createByteInput("GET /hello HTTP/1.0\r\n".getBytes());
        Assert.assertThrows(HttpVersionNotSupportedException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test7() {
        // 测试不正常的 流
        var rawInput = ScxIO.createByteInput("GET /hello HTTP/1.1\r\n".getBytes());
        rawInput.close();
        Assert.assertThrows(CloseConnectionException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test8() {
        // 测试不正常的 流
        var rawInput = new DefaultByteInput(() -> {
            throw new ScxIOException("socket 异常");
        });
        Assert.assertThrows(CloseConnectionException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test9() {
        // 测试空头
        var rawInput = ScxIO.createByteInput("\r\n".getBytes());
        var http1Headers = Http1Reader.readHeaders(rawInput, 9999);
        Assert.assertEquals(http1Headers.encode(), "");
    }

    @Test
    public static void test10() {
        // 测试正常头
        var rawInput = ScxIO.createByteInput("a:10\r\nb:20\r\n\r\n".getBytes());
        var http1Headers = Http1Reader.readHeaders(rawInput, 9999);
        Assert.assertEquals(http1Headers.encode(), "a: 10\r\nb: 20\r\n");
    }

    @Test
    public static void test11() {
        // 测试正常头
        var rawInput = ScxIO.createByteInput("a:10\r\nb:20\r\n\r\n".getBytes());
        rawInput.close();
        Assert.assertThrows(CloseConnectionException.class, () -> {
            var http1Headers = Http1Reader.readHeaders(rawInput, 9999);
        });
    }

    @Test
    public static void test12() {
        // 测试请求头过长
        var rawInput = ScxIO.createByteInput("a:10\r\nb:20\r\n\r\n".getBytes());
        Assert.assertThrows(RequestHeaderFieldsTooLargeException.class, () -> {
            var http1Headers = Http1Reader.readHeaders(rawInput, 10);
        });
    }

    @Test
    public static void test13() {
        // 数据不足
        var rawInput = ScxIO.createByteInput("body".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput((Http1Headers) new Http1Headers().contentLength(10), rawInput, 9999);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(BodyTooShortException.class, () -> {
            var bodyStr = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test14() {
        // contentLength 太大
        var rawInput = ScxIO.createByteInput("body".getBytes());
        Assert.assertThrows(ContentTooLargeException.class, () -> {
            var byteSupplier = Http1Reader.readBodyByteInput((Http1Headers) new Http1Headers().contentLength(999), rawInput, 100);
        });
    }

    @Test
    public static void test15() {
        // 数据异常
        var rawInput = ScxIO.createByteInput("body".getBytes());
        rawInput.close();
        var byteSupplier = Http1Reader.readBodyByteInput((Http1Headers) new Http1Headers().contentLength(10), rawInput, 9999);
        var bodyInput = new DefaultByteInput(byteSupplier);
        // 这里应该直接 ScxIOException 异常
        Assert.assertThrows(ScxIOException.class, () -> {
            var bodyStr = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test16() {
        // 创建数据
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("hello ".getBytes());
        b.write("world".getBytes());
        b.close();

        var rawInput = ScxIO.createByteInput(a.bytes());

        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 100);

        var bodyInput = new DefaultByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "hello world");
    }

    @Test
    public static void test17() {
        // 创建数据
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("hello ".getBytes());
        b.write("world".getBytes());
        b.close();

        var rawInput = ScxIO.createByteInput(a.bytes());

        // 长度太长
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 5);

        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(BodyTooLargeException.class, () -> {
            var data = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test18() {
        // 错误数据
        var rawInput = ScxIO.createByteInput("abc".getBytes());

        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);

        var bodyInput = new DefaultByteInput(byteSupplier);

        Assert.assertThrows(HttpChunkedParseException.class, () -> {
            var data = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test19_invalidChunkSize() {
        var rawInput = ScxIO.createByteInput("Z\r\nabc\r\n0\r\n\r\n".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test20_incompleteChunkData() {
        var rawInput = ScxIO.createByteInput("5\r\nabc\r\n0\r\n\r\n".getBytes()); // 5长度但只给了3字节
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test21_missingChunkEndCRLF() {
        var rawInput = ScxIO.createByteInput("3\r\nabc0\r\n\r\n".getBytes()); // 3字节块没有 \r\n
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test22_tailNotEmpty() {
        var rawInput = ScxIO.createByteInput("0\r\nX\r\n".getBytes()); // 0长度尾部非空
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test23_exceedMaxLength() {
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("abc".getBytes());
        b.write("def".getBytes());
        b.close();
        var rawInput = ScxIO.createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 5); // 限制 maxLength=5
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(BodyTooLargeException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test24_EOFBeforeComplete() {
        var rawInput = ScxIO.createByteInput("3\r\nab".getBytes()); // EOF 提前到达
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test25_invalidChunkSize() {
        var rawInput = ScxIO.createByteInput("Z\r\nabc\r\n0\r\n\r\n".getBytes()); // Z 不是 hex
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test26_incompleteChunkData() {
        var rawInput = ScxIO.createByteInput("5\r\nabc\r\n0\r\n\r\n".getBytes()); // 5 长度，但只给 3 字节
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test27_missingChunkEndCRLF() {
        var rawInput = ScxIO.createByteInput("3\r\nabc0\r\n\r\n".getBytes()); // 数据块没 \r\n
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test28_tailNotEmpty() {
        var rawInput = ScxIO.createByteInput("0\r\nX\r\n".getBytes()); // 0 长度块尾部非空
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test29_EOFBeforeComplete() {
        var rawInput = ScxIO.createByteInput("3\r\nab".getBytes()); // 数据只提供了 2 字节
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test30_multipleTinyChunks() throws ScxIOException, AlreadyClosedException, NoMoreDataException {
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);

        // 连续写 5 个 1 字节块
        b.write("a".getBytes());
        b.write("b".getBytes());
        b.write("c".getBytes());
        b.write("d".getBytes());
        b.write("e".getBytes());
        b.close();

        var rawInput = ScxIO.createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 100);
        var bodyInput = new DefaultByteInput(byteSupplier);

        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "abcde");
    }

    @Test
    public static void test31_singleChunkExactlyMaxLength() throws ScxIOException, AlreadyClosedException, NoMoreDataException {
        var content = "hello"; // 5 字节
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write(content.getBytes());
        b.close();

        var rawInput = ScxIO.createByteInput(a.bytes());

        // maxLength 恰好等于内容长度
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, content.length());
        var bodyInput = new DefaultByteInput(byteSupplier);

        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, content);
    }

    @Test
    public static void test32_zeroLengthChunkNormal() throws ScxIOException, AlreadyClosedException, NoMoreDataException {
        // 正确的 0 长度 chunk
        var rawInput = ScxIO.createByteInput("0\r\n\r\n".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = new DefaultByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "");
    }

    @Test
    public static void test33_singleByteChunk() throws ScxIOException, AlreadyClosedException, NoMoreDataException {
        // 单字节 chunk
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("x".getBytes());
        b.close();
        var rawInput = ScxIO.createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = new DefaultByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "x");
    }

    @Test
    public static void test34_multipleVariableChunks() throws ScxIOException, AlreadyClosedException, NoMoreDataException {
        // 多个大小不同的 chunk
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("ab".getBytes());  // 2 字节
        b.write("c".getBytes());   // 1 字节
        b.write("defg".getBytes()); // 4 字节
        b.close();
        var rawInput = ScxIO.createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = new DefaultByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "abcdefg");
    }

    @Test
    public static void test35_fragmentedStream() throws ScxIOException, AlreadyClosedException, NoMoreDataException {
        // 模拟网络分片读取，每次只提供 1 字节
        var rawInput = new DefaultByteInput(new InputStreamByteSupplier(new ByteArrayInputStream("3\r\nabc\r\n0\r\n\r\n".getBytes()),1));
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = new DefaultByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "abc");
    }

    @Test
    public static void test36_CRLFError() {
        // 仅 \n 而非 \r\n，应抛异常
        var rawInput = ScxIO.createByteInput("3\nabc\n0\n\n".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = new DefaultByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test37_largeChunkWithinMaxLength() throws ScxIOException, AlreadyClosedException, NoMoreDataException {
        // 单块很大，但未超过 maxLength
        var content = "abcdefghij"; // 10 字节
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write(content.getBytes());
        b.close();
        var rawInput = ScxIO.createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 20);
        var bodyInput = new DefaultByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, content);
    }


}
