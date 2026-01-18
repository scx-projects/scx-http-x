package dev.scx.http.x.test;

import dev.scx.http.x.HttpClient;
import dev.scx.http.x.http1.Http1ClientResponse;

import java.io.IOException;

import static dev.scx.http.method.HttpMethod.POST;

public class HttpClientTest {

    public static void main(String[] args) throws IOException {
        test1();
    }

    public static void test1() throws IOException {
        HttpServerTest.test1();

        var client = new HttpClient();
        var response = client.request()
            .uri("http://localhost:8899/中文路径😎😎😎😎?a=1&b=llll")
            .addHeader("a", "b")
            .method(POST)
            .gzip()
            .send("这是来自客户端的内容 😂😂😂😂😂😂😂");

        var bodyStr = response.gzip().asString();
        System.out.println("收到服务端响应:");
        System.out.println("***************************************************");
        System.out.println(response.statusCode() + " " + ((Http1ClientResponse) response).reasonPhrase());
        System.out.println(response.headers().encode());
        System.out.println(bodyStr);
        System.out.println("***************************************************");
    }

}

