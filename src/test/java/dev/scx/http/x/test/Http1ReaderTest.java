package dev.scx.http.x.test;

import dev.scx.http.x.http1.headers.Http1Headers;
import dev.scx.http.x.http1.io.*;
import dev.scx.http.x.http1.request_line.InvalidRequestLineException;
import dev.scx.http.x.http1.request_line.InvalidRequestLineHttpVersionException;
import dev.scx.io.exception.AlreadyClosedException;
import dev.scx.io.exception.NoMoreDataException;
import dev.scx.io.exception.ScxIOException;
import dev.scx.io.output.ByteArrayByteOutput;
import dev.scx.io.supplier.InputStreamByteSupplier;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;

import static dev.scx.http.x.http1.headers.transfer_encoding.TransferEncoding.CHUNKED;
import static dev.scx.io.ScxIO.createByteInput;

public class Http1ReaderTest {

    public static void main(String[] args) throws NoMoreDataException, InvalidRequestLineException, InvalidRequestLineHttpVersionException, RequestLineTooLongException, HeaderTooLargeException, ContentLengthBodyTooLargeException {
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
        var rawInput = createByteInput("GET /hello HTTP/1.1\r\n".getBytes());
        Assert.assertThrows(RequestLineTooLongException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 10);
        });
    }

    @Test
    public static void test2() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException, RequestLineTooLongException {
        // 测试正常解析
        var rawInput = createByteInput("GET /hello?a=100 HTTP/1.1\r\n".getBytes());
        var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        Assert.assertEquals(http1RequestLine.encode(), "GET /hello?a=100 HTTP/1.1");
    }

    @Test
    public static void test3() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException, RequestLineTooLongException {
        // 测试正常解析
        var rawInput = createByteInput("GET http://www.abc.com/hello HTTP/1.1\r\n".getBytes());
        var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        Assert.assertEquals(http1RequestLine.encode(), "GET http://www.abc.com/hello HTTP/1.1");
    }

    @Test
    public static void test4() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException, RequestLineTooLongException {
        // 测试正常解析
        var rawInput = createByteInput("GET http://www.abc.com:9989/hello HTTP/1.1\r\n".getBytes());
        var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        Assert.assertEquals(http1RequestLine.encode(), "GET http://www.abc.com:9989/hello HTTP/1.1");
    }

    @Test
    public static void test5() {
        // 测试不正常 头
        var rawInput = createByteInput("xxxx\r\n".getBytes());
        Assert.assertThrows(InvalidRequestLineException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test6() {
        // 测试不正常 版本号
        var rawInput = createByteInput("GET /hello HTTP/1.0\r\n".getBytes());
        Assert.assertThrows(InvalidRequestLineHttpVersionException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test7() {
        // 测试不正常的 流
        var rawInput = createByteInput("GET /hello HTTP/1.1\r\n".getBytes());
        rawInput.close();
        Assert.assertThrows(AlreadyClosedException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test8() {
        // 测试不正常的 流
        var rawInput = createByteInput(() -> {
            throw new ScxIOException("socket 异常");
        });
        Assert.assertThrows(ScxIOException.class, () -> {
            var http1RequestLine = Http1Reader.readRequestLine(rawInput, 9999);
        });
    }

    @Test
    public static void test9() throws HeaderTooLargeException {
        // 测试空头
        var rawInput = createByteInput("\r\n".getBytes());
        var http1Headers = Http1Reader.readHeaders(rawInput, 9999);
        Assert.assertEquals(http1Headers.encode(), "");
    }

    @Test
    public static void test10() throws HeaderTooLargeException {
        // 测试正常头
        var rawInput = createByteInput("a:10\r\nb:20\r\n\r\n".getBytes());
        var http1Headers = Http1Reader.readHeaders(rawInput, 9999);
        Assert.assertEquals(http1Headers.encode(), "a: 10\r\nb: 20\r\n");
    }

    @Test
    public static void test11() {
        // 测试正常头
        var rawInput = createByteInput("a:10\r\nb:20\r\n\r\n".getBytes());
        rawInput.close();
        Assert.assertThrows(AlreadyClosedException.class, () -> {
            var http1Headers = Http1Reader.readHeaders(rawInput, 9999);
        });
    }

    @Test
    public static void test12() {
        // 测试请求头过长
        var rawInput = createByteInput("a:10\r\nb:20\r\n\r\n".getBytes());
        Assert.assertThrows(HeaderTooLargeException.class, () -> {
            var http1Headers = Http1Reader.readHeaders(rawInput, 10);
        });
    }

    @Test
    public static void test13() throws ContentLengthBodyTooLargeException {
        // 数据不足
        var rawInput = createByteInput("body".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput((Http1Headers) new Http1Headers().contentLength(10), rawInput, 9999);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(ContentLengthBodyTooShortException.class, () -> {
            var bodyStr = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test14() {
        // contentLength 太大
        var rawInput = createByteInput("body".getBytes());
        Assert.assertThrows(ContentLengthBodyTooLargeException.class, () -> {
            var byteSupplier = Http1Reader.readBodyByteInput((Http1Headers) new Http1Headers().contentLength(999), rawInput, 100);
        });
    }

    @Test
    public static void test15() throws ContentLengthBodyTooLargeException {
        // 数据异常
        var rawInput = createByteInput("body".getBytes());
        rawInput.close();
        var byteSupplier = Http1Reader.readBodyByteInput((Http1Headers) new Http1Headers().contentLength(10), rawInput, 9999);
        var bodyInput = createByteInput(byteSupplier);
        // 这里应该直接 ScxIOException 异常
        Assert.assertThrows(ScxIOException.class, () -> {
            var bodyStr = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test16() throws ContentLengthBodyTooLargeException {
        // 创建数据
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("hello ".getBytes());
        b.write("world".getBytes());
        b.close();

        var rawInput = createByteInput(a.bytes());

        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 100);

        var bodyInput = createByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "hello world");
    }

    @Test
    public static void test17() throws ContentLengthBodyTooLargeException {
        // 创建数据
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("hello ".getBytes());
        b.write("world".getBytes());
        b.close();

        var rawInput = createByteInput(a.bytes());

        // 长度太长
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 5);

        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedBodyTooLargeException.class, () -> {
            var data = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test18() throws ContentLengthBodyTooLargeException {
        // 错误数据
        var rawInput = createByteInput("abc".getBytes());

        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);

        var bodyInput = createByteInput(byteSupplier);

        Assert.assertThrows(HttpChunkedParseException.class, () -> {
            var data = new String(bodyInput.readAll());
        });
    }

    @Test
    public static void test19_invalidChunkSize() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("Z\r\nabc\r\n0\r\n\r\n".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test20_incompleteChunkData() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("5\r\nabc\r\n0\r\n\r\n".getBytes()); // 5长度但只给了3字节
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test21_missingChunkEndCRLF() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("3\r\nabc0\r\n\r\n".getBytes()); // 3字节块没有 \r\n
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test22_tailNotEmpty() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("0\r\nX\r\n".getBytes()); // 0长度尾部非空
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test23_exceedMaxLength() throws ContentLengthBodyTooLargeException {
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("abc".getBytes());
        b.write("def".getBytes());
        b.close();
        var rawInput = createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 5); // 限制 maxLength=5
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedBodyTooLargeException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test24_EOFBeforeComplete() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("3\r\nab".getBytes()); // EOF 提前到达
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test25_invalidChunkSize() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("Z\r\nabc\r\n0\r\n\r\n".getBytes()); // Z 不是 hex
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test26_incompleteChunkData() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("5\r\nabc\r\n0\r\n\r\n".getBytes()); // 5 长度，但只给 3 字节
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test27_missingChunkEndCRLF() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("3\r\nabc0\r\n\r\n".getBytes()); // 数据块没 \r\n
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test28_tailNotEmpty() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("0\r\nX\r\n".getBytes()); // 0 长度块尾部非空
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test29_EOFBeforeComplete() throws ContentLengthBodyTooLargeException {
        var rawInput = createByteInput("3\r\nab".getBytes()); // 数据只提供了 2 字节
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 1000);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test30_multipleTinyChunks() throws ScxIOException, AlreadyClosedException, NoMoreDataException, ContentLengthBodyTooLargeException {
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);

        // 连续写 5 个 1 字节块
        b.write("a".getBytes());
        b.write("b".getBytes());
        b.write("c".getBytes());
        b.write("d".getBytes());
        b.write("e".getBytes());
        b.close();

        var rawInput = createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 100);
        var bodyInput = createByteInput(byteSupplier);

        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "abcde");
    }

    @Test
    public static void test31_singleChunkExactlyMaxLength() throws ScxIOException, AlreadyClosedException, NoMoreDataException, ContentLengthBodyTooLargeException {
        var content = "hello"; // 5 字节
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write(content.getBytes());
        b.close();

        var rawInput = createByteInput(a.bytes());

        // maxLength 恰好等于内容长度
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, content.length());
        var bodyInput = createByteInput(byteSupplier);

        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, content);
    }

    @Test
    public static void test32_zeroLengthChunkNormal() throws ScxIOException, AlreadyClosedException, NoMoreDataException, ContentLengthBodyTooLargeException {
        // 正确的 0 长度 chunk
        var rawInput = createByteInput("0\r\n\r\n".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = createByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "");
    }

    @Test
    public static void test33_singleByteChunk() throws ScxIOException, AlreadyClosedException, NoMoreDataException, ContentLengthBodyTooLargeException {
        // 单字节 chunk
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("x".getBytes());
        b.close();
        var rawInput = createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = createByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "x");
    }

    @Test
    public static void test34_multipleVariableChunks() throws ScxIOException, AlreadyClosedException, NoMoreDataException, ContentLengthBodyTooLargeException {
        // 多个大小不同的 chunk
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write("ab".getBytes());  // 2 字节
        b.write("c".getBytes());   // 1 字节
        b.write("defg".getBytes()); // 4 字节
        b.close();
        var rawInput = createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = createByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "abcdefg");
    }

    @Test
    public static void test35_fragmentedStream() throws ScxIOException, AlreadyClosedException, NoMoreDataException, ContentLengthBodyTooLargeException {
        // 模拟网络分片读取，每次只提供 1 字节
        var rawInput = createByteInput(new InputStreamByteSupplier(new ByteArrayInputStream("3\r\nabc\r\n0\r\n\r\n".getBytes()), 1));
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = createByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, "abc");
    }

    @Test
    public static void test36_CRLFError() throws ContentLengthBodyTooLargeException {
        // 仅 \n 而非 \r\n，应抛异常
        var rawInput = createByteInput("3\nabc\n0\n\n".getBytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 10);
        var bodyInput = createByteInput(byteSupplier);
        Assert.assertThrows(HttpChunkedParseException.class, () -> bodyInput.readAll());
    }

    @Test
    public static void test37_largeChunkWithinMaxLength() throws ScxIOException, AlreadyClosedException, NoMoreDataException, ContentLengthBodyTooLargeException {
        // 单块很大，但未超过 maxLength
        var content = "abcdefghij"; // 10 字节
        var a = new ByteArrayByteOutput();
        var b = new HttpChunkedByteOutput(a);
        b.write(content.getBytes());
        b.close();
        var rawInput = createByteInput(a.bytes());
        var byteSupplier = Http1Reader.readBodyByteInput(new Http1Headers().transferEncoding(CHUNKED), rawInput, 20);
        var bodyInput = createByteInput(byteSupplier);
        var data = new String(bodyInput.readAll());
        Assert.assertEquals(data, content);
    }


}
