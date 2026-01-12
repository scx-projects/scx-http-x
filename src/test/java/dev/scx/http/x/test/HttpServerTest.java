package dev.scx.http.x.test;

import dev.scx.http.x.HttpServer;

import java.io.IOException;

public class HttpServerTest {

    public static void main(String[] args) throws IOException {
        test1();
    }

    public static void test1() throws IOException {
        var httpServer = new HttpServer();
        httpServer.onRequest(request -> {
            var bodyStr = request.asGzipReceiver().asString();
            System.out.println("收到客户端请求:");
            System.out.println("--------------------------------------------------");
            System.out.println(request.method() + " " + request.uri());
            System.out.println(request.headers().encode());
            System.out.println(bodyStr);
            System.out.println("--------------------------------------------------");
            request.response().sendGzip().send("这是来自服务端的内容 😍😍😍😍😍😍😍😍");
        });
        httpServer.start(8899);
        System.out.println("启动完成 !!! 端口号 : " + httpServer.localAddress().getPort());
    }

}
