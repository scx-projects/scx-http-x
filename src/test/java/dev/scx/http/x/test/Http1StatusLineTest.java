package dev.scx.http.x.test;

import dev.scx.http.version.HttpVersion;
import dev.scx.http.x.http1.status_line.Http1StatusLine;
import dev.scx.http.x.http1.status_line.InvalidStatusLineException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineHttpVersionException;
import dev.scx.http.x.http1.status_line.InvalidStatusLineStatusCodeException;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.scx.http.status_code.HttpStatusCode.OK;

public class Http1StatusLineTest {

    public static void main(String[] args) throws InvalidStatusLineStatusCodeException, InvalidStatusLineException, InvalidStatusLineHttpVersionException {
        test1();
        test2();
        test3();
        test4();
    }

    @Test
    public static void test1() throws InvalidStatusLineStatusCodeException, InvalidStatusLineException, InvalidStatusLineHttpVersionException {
        var str = "HTTP/1.1 200 OK";
        var statusLine = Http1StatusLine.of(str);
        Assert.assertEquals(statusLine.httpVersion(), HttpVersion.HTTP_1_1);
        Assert.assertEquals(statusLine.statusCode(), OK);
        Assert.assertEquals(statusLine.reasonPhrase(), "OK");
    }

    @Test
    public static void test2() {
        var str = "HTTP/1.0 200 OK";
        Assert.assertThrows(InvalidStatusLineHttpVersionException.class, () -> Http1StatusLine.of(str));
    }

    @Test
    public static void test3() {
        var str = "HTTP/1.1 abc OK";
        Assert.assertThrows(InvalidStatusLineStatusCodeException.class, () -> Http1StatusLine.of(str));
    }

    @Test
    public static void test4() {
        var str = "absdcbnasjdbjh";
        Assert.assertThrows(InvalidStatusLineException.class, () -> Http1StatusLine.of(str));
    }

}
