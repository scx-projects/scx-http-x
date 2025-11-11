package cool.scx.http.x.test;

import cool.scx.http.uri.ScxURI;
import cool.scx.http.x.http1.request_line.Http1RequestLine;
import cool.scx.http.x.http1.request_line.InvalidRequestLineException;
import cool.scx.http.x.http1.request_line.InvalidRequestLineHttpVersionException;
import org.testng.annotations.Test;

import static cool.scx.http.method.HttpMethod.GET;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class Http1RequestLineTest {

    public static void main(String[] args) throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        test1();
        test2();
        test3();
    }

    @Test
    public static void test1() throws  InvalidRequestLineException, InvalidRequestLineHttpVersionException {

        //这是正确的
        var http1RequestLine = Http1RequestLine.of("GET /foo HTTP/1.1");

        //这里是非法http版本
        assertThrows(InvalidRequestLineHttpVersionException.class, () -> {
            var requestLine = Http1RequestLine.of("GET /foo HTTP/1.3");
        });


        //这里是 Http/0.9 理论上应该抛出 400
        assertThrows(InvalidRequestLineException.class, () -> {
            var requestLine = Http1RequestLine.of("GET /foo");
        });

        //这里是 多余空格 理论上应该抛出 400
        assertThrows(InvalidRequestLineException.class, () -> {
            var requestLine = Http1RequestLine.of("GET /foo abc HTTP/1.1");
        });

        //这里是 不可解析的路径 理论上应该抛出 400
        assertThrows(InvalidRequestLineException.class, () -> {
            var requestLine = Http1RequestLine.of("GET /% HTTP/1.1");
        });

    }

    @Test
    public static void test2() {
        var http1RequestLine = new Http1RequestLine(GET, ScxURI.of().path("/中文/bar").addQuery("aaa", "bbb")).encode();
        assertEquals(http1RequestLine, "GET /%E4%B8%AD%E6%96%87/bar?aaa=bbb HTTP/1.1");
    }

    @Test
    public static void test3() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {

        var s = Http1RequestLine.of("GET http://www.test.com/a/b/c/ HTTP/1.1");

        var c = Http1RequestLine.of("CONNECT www.test.com:443 HTTP/1.1");

        System.out.println(s.requestTarget());
        System.out.println(c.requestTarget());

    }

}
