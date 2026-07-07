package dev.scx.http.x.test;

import dev.scx.http.parameters.ParametersImpl;
import dev.scx.http.x.http1.request_line.Http1RequestLine;
import dev.scx.http.x.http1.request_line.InvalidRequestLineException;
import dev.scx.http.x.http1.request_line.InvalidRequestLineHttpVersionException;
import dev.scx.http.x.http1.request_line.request_target.AbsoluteForm;
import dev.scx.http.x.http1.request_line.request_target.AsteriskForm;
import dev.scx.http.x.http1.request_line.request_target.AuthorityForm;
import dev.scx.http.x.http1.request_line.request_target.OriginForm;
import org.testng.annotations.Test;

import static dev.scx.http.method.HttpMethod.GET;
import static org.testng.Assert.*;

public class Http1RequestLineTest {

    public static void main(String[] args) throws Exception {
        testOriginForm();
        testAbsoluteForm();
        testAuthorityForm();
        testAsteriskForm();
        testCustomMethod();
        testInvalidSpaces();
        testInvalidRequestTarget();
        testInvalidVersion();
        testInvalidCombinations();
        test1();
        test2();
        test3();
        testAllEdgeCases();
    }


    // --------- 合法请求 ---------
    @Test
    public static void testOriginForm() throws Exception {
        String[] requests = {
            "GET / HTTP/1.1",
            "GET /foo/bar HTTP/1.1",
            "GET /foo?bar=baz HTTP/1.1",
            "GET /foo?bar=baz#fragment HTTP/1.1",
            "OPTIONS /foo HTTP/1.1"
        };

        for (String req : requests) {
            Http1RequestLine line = Http1RequestLine.of(req);
            assertNotNull(line);
            assertTrue(line.requestTarget() instanceof OriginForm);
        }
    }

    @Test
    public static void testAbsoluteForm() throws Exception {
        String[] requests = {
            "GET http://example.com/index.html HTTP/1.1",
            "GET https://example.com:8443/path?query HTTP/1.1"
        };

        for (String req : requests) {
            Http1RequestLine line = Http1RequestLine.of(req);
            assertNotNull(line);
            assertTrue(line.requestTarget() instanceof AbsoluteForm);
        }
    }

    @Test
    public static void testAuthorityForm() throws Exception {
        String[] requests = {
            "CONNECT example.com:443 HTTP/1.1",
            "CONNECT [2001:db8::1]:443 HTTP/1.1"
        };

        for (String req : requests) {
            Http1RequestLine line = Http1RequestLine.of(req);
            assertNotNull(line);
            assertTrue(line.requestTarget() instanceof AuthorityForm);
        }
    }

    @Test
    public static void testAsteriskForm() throws Exception {
        String req = "OPTIONS * HTTP/1.1";
        Http1RequestLine line = Http1RequestLine.of(req);
        assertNotNull(line);
        assertTrue(line.requestTarget() instanceof AsteriskForm);
    }

    @Test
    public static void testCustomMethod() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        String[] requests = {
            "GETÁ /foo HTTP/1.1",
            "123 /foo HTTP/1.1",
        };
        // 我们允许这种方法
        for (String req : requests) {
            var http1RequestLine = Http1RequestLine.of(req);
        }
    }

    // --------- 非法请求 ---------
    @Test
    public static void testInvalidSpaces() {
        String[] requests = {
            "GET  /foo HTTP/1.1",
            "GET /foo  HTTP/1.1",
            " GET /foo HTTP/1.1",
            "GET /foo HTTP/1.1 ",
            "GET\t/foo HTTP/1.1"
        };

        for (String req : requests) {
            assertThrows(InvalidRequestLineException.class, () -> Http1RequestLine.of(req));
        }
    }

    @Test
    public static void testInvalidRequestTarget() {
        String[] requests = {
            "GET * HTTP/1.1",
            "GET example.com/index.html HTTP/1.1",
            "GET /foo%ZZ HTTP/1.1",
            "CONNECT example.com HTTP/1.1",
            "CONNECT example.com:99999 HTTP/1.1",
            "CONNECT example.com:abc HTTP/1.1",
            "CONNECT :443 HTTP/1.1",
            "CONNECT [2001:db8::1 HTTP/1.1"
        };

        for (String req : requests) {
            assertThrows(InvalidRequestLineException.class, () -> Http1RequestLine.of(req));
        }
    }

    @Test
    public static void testInvalidVersion() {
        String[] requests = {
            "GET /foo HTTP/1.0",
            "GET /foo HTTP/2.0",
            "GET /foo HTTP/1",
            "GET /foo HTTPS/1.1"
        };

        for (String req : requests) {
            assertThrows(InvalidRequestLineHttpVersionException.class, () -> Http1RequestLine.of(req));
        }
    }

    @Test
    public static void testInvalidCombinations() {
        String[] requests = {
            "POST * HTTP/1.1",
            "CONNECT example.com HTTP/1.0",
            "GET  http://example.com HTTP/1.0"
        };

        assertThrows(InvalidRequestLineException.class, () -> Http1RequestLine.of(requests[0]));
        assertThrows(InvalidRequestLineHttpVersionException.class, () -> Http1RequestLine.of(requests[1]));
        assertThrows(InvalidRequestLineException.class, () -> Http1RequestLine.of(requests[2]));
    }

    @Test
    public static void test1() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {

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
    public static void test2() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {

        var s = Http1RequestLine.of("GET http://www.test.com/a/b/c/ HTTP/1.1");

        var c = Http1RequestLine.of("CONNECT www.test.com:443 HTTP/1.1");

        System.out.println(s.requestTarget());
        System.out.println(c.requestTarget());

    }

    @Test
    public static void test3() throws InvalidRequestLineException, InvalidRequestLineHttpVersionException {
        var http1RequestLine = new Http1RequestLine(GET, new OriginForm("/中文/bar", new ParametersImpl<String, String>().add("aaa", "bbb"), null)).encode();
        assertEquals(http1RequestLine, "GET /%E4%B8%AD%E6%96%87/bar?aaa=bbb HTTP/1.1");
    }

    @Test
    public static void testAllEdgeCases() {
        // --------- 合法请求 ---------
        String[] validRequests = {
            "GET / HTTP/1.1",
            "GET /foo/bar HTTP/1.1",
            "GET /foo?bar=baz HTTP/1.1",
            "GET /foo?bar=baz#fragment HTTP/1.1",
            "OPTIONS /foo HTTP/1.1",
            "GET http://example.com/index.html HTTP/1.1",
            "GET https://example.com:8443/path?query HTTP/1.1",
            "CONNECT example.com:443 HTTP/1.1",
            "CONNECT [::1]:65535 HTTP/1.1",
            "OPTIONS * HTTP/1.1"
        };

        for (String req : validRequests) {
            try {
                Http1RequestLine line = Http1RequestLine.of(req);
                assertNotNull(line, "Parsed line should not be null: " + req);

                // 验证 request-target 类型
                if (req.startsWith("GET /") || req.startsWith("OPTIONS /")) {
                    assertTrue(line.requestTarget() instanceof OriginForm, req);
                } else if (req.startsWith("GET http")) {
                    assertTrue(line.requestTarget() instanceof AbsoluteForm, req);
                } else if (req.startsWith("CONNECT")) {
                    assertTrue(line.requestTarget() instanceof AuthorityForm, req);
                } else if (req.startsWith("OPTIONS *")) {
                    assertTrue(line.requestTarget() instanceof AsteriskForm, req);
                }
            } catch (Exception e) {
                fail("Valid request threw exception: " + req + " -> " + e);
            }
        }

        // --------- 非法请求 ---------
        String[] invalidRequests = {
            // 尾部/前导/多空格
            "GET  /foo HTTP/1.1",
            "GET /foo  HTTP/1.1",
            " GET /foo HTTP/1.1",
            "GET /foo HTTP/1.1 ",
            "GET\t/foo HTTP/1.1",
            // request-target 异常
            "GET * HTTP/1.1",
            "GET example.com/index.html HTTP/1.1",
            "GET /%ZZ HTTP/1.1",
            "GET /%2G HTTP/1.1",
            "GET /% HTTP/1.1",
            "CONNECT example.com HTTP/1.1",
            "CONNECT example.com:0 HTTP/1.1",
            "CONNECT example.com:65536 HTTP/1.1",
            "CONNECT example.com:abc HTTP/1.1",
            "CONNECT :443 HTTP/1.1",
            "CONNECT [2001:db8::1 HTTP/1.1",
            // HTTP 版本异常
            "GET /foo HTTP/1.0",
            "GET /foo HTTP/2.0",
            "GET /foo HTTP/1",
            "GET /foo HTTPS/1.1",
            // 组合边界
            "POST * HTTP/1.1",
            "CONNECT example.com HTTP/1.0",
            "GET  http://example.com HTTP/1.0",
            // 空方法名
            " /foo HTTP/1.1",
            "\t/foo HTTP/1.1"
        };

        for (String req : invalidRequests) {
            try {
                Http1RequestLine.of(req);
            } catch (InvalidRequestLineException | InvalidRequestLineHttpVersionException e) {
                // pass
            } catch (Exception e) {
                fail("Unexpected exception for invalid request: " + req + " -> " + e);
            }
        }

        // --------- 额外输出验证 ---------
        try {
            Http1RequestLine line = Http1RequestLine.of("GET http://www.test.com/a/b/c/ HTTP/1.1");
            assertTrue(line.requestTarget() instanceof AbsoluteForm);
            Http1RequestLine connLine = Http1RequestLine.of("CONNECT www.test.com:443 HTTP/1.1");
            assertTrue(connLine.requestTarget() instanceof AuthorityForm);
        } catch (Exception e) {
            fail("Unexpected exception in extra output validation: " + e);
        }
    }

}
