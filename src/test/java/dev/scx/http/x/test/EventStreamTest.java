package dev.scx.http.x.test;

import dev.scx.http.media.event_stream.SseEvent;
import dev.scx.http.media.event_stream.event.EventClientEventStream;
import dev.scx.http.x.HttpClient;
import dev.scx.http.x.HttpServer;
import org.testng.annotations.Test;

import java.io.IOException;

public class EventStreamTest {

    public static void main(String[] args) throws IOException {
        test1();
    }

    @Test
    public static void test1() throws IOException {
        var httpServer = new HttpServer();
        httpServer.onRequest(c -> {
            System.out.println("连接了");
            c.response().setHeader("Access-Control-Allow-Origin", "*");
            var eventStream = c.response().sendEventStream();
            try (eventStream) {
                for (int i = 0; i < 100; i = i + 1) {
                    eventStream.send(SseEvent.of("hello\r\n换行😀🥀🌴\r\n" + i).id("123").event("message").comment("这是注释"));
                    sleep(20);
                }
            }
            System.out.println("全部发送完成");
            httpServer.stop();
        });
        httpServer.start(8080);

        var client = new HttpClient();
        var eventStream = client.request().uri("http://127.0.0.1:8080").send().body().asEventStream();
        EventClientEventStream.of(eventStream).onEvent(event -> {
            System.err.println(event.event() + " " + event.data());
        }).start();

    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

}
