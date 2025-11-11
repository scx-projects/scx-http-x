package cool.scx.http.x.test;

import cool.scx.common.util.$;
import cool.scx.http.media.event_stream.SseEvent;
import cool.scx.http.media.event_stream.event.EventClientEventStream;
import cool.scx.http.x.HttpClient;
import cool.scx.http.x.HttpServer;

import java.io.IOException;

public class EventStreamTest {

    public static void main(String[] args) throws IOException {
        test1();
        test2();
    }

    public static void test1() throws IOException {
        var httpServer = new HttpServer();
        httpServer.onRequest(c -> {
            System.out.println("连接了");
            c.response().setHeader("Access-Control-Allow-Origin", "*");
            var eventStream = c.response().sendEventStream();
            try (eventStream) {
                for (int i = 0; i < 20; i = i + 1) {
                    eventStream.send(SseEvent.of("hello" + i).id("123").event("message").comment("这是注释"));
                    $.sleep(100);
                }
            }
            System.out.println("全部发送完成");
        });
        httpServer.start(8080);

    }

    public static void test2() {
        var client = new HttpClient();
        var eventStream = client.request().uri("http://127.0.0.1:8080").send().body().asEventStream();
        EventClientEventStream.of(eventStream).onEvent(event -> {
            System.err.println(event.event() + " " + event.data());
        }).start();
    }

}
