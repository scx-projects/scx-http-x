package dev.scx.http.x.test;

import dev.scx.http.method.HttpMethod;
import dev.scx.http.received.ScxHttpReceived;
import dev.scx.http.sender.ScxHttpSender;
import dev.scx.http.x.HttpClient;
import dev.scx.http.x.HttpServer;
import dev.scx.http.x.http1.Http1ClientResponse;
import dev.scx.http.x.http1.Http1ServerRequest;
import dev.scx.io.ByteOutput;
import dev.scx.io.ScxIO;
import dev.scx.io.exception.InputAlreadyClosedException;
import dev.scx.io.exception.OutputAlreadyClosedException;
import dev.scx.io.exception.ScxInputException;
import dev.scx.io.exception.ScxOutputException;
import dev.scx.tcp.TCPClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

// 测试代理功能
public class HttpProxyServerTest {

    public static void main(String[] args) throws IOException {
        test1();
    }

    /// 只做流量转发 不做任何解析
    public static void test1() throws IOException {
        var httpServer = new HttpServer();
        httpServer.onRequest(c -> {

            var request = (Http1ServerRequest) c;

            // https 隧道请求
            if (c.method() == HttpMethod.CONNECT) {
                System.out.println("收到 HTTPS 代理请求 : " + c.uri());
                //1, 获取连接对象
                var serverConnection = request.connection;
                //2, 交接 Socket 所有权
                serverConnection.stop();
                //3, 获取 当前连接的 底层 tcpSocket 内容
                var serverTCPSocket = serverConnection.endpoint.socket;
                //4, 创建 远端连接
                var tcpClient = new TCPClient();
                Socket clientTCPSocket;
                try {
                    clientTCPSocket = tcpClient.connect(new InetSocketAddress(c.uri().host(), c.uri().port()));
                } catch (IOException e) {
                    throw new ScxInputException("连接远端失败 : " + c.uri(), e);
                }

                //5, 通知代理连接成功
                request.response().reasonPhrase("连接成功!!!").statusCode(200).send();

                //开启两个线程 进行数据相互传输 其中 tls 相关内容 我们直接原封不动传输
                Thread.ofVirtual().start(() -> {
                    try {
                        var outputStream = serverTCPSocket.getOutputStream();
                        var inputStream = clientTCPSocket.getInputStream();
                        inputStream.transferTo(outputStream);
                    } catch (IOException e) {
                        try {
                            serverTCPSocket.close();
                            clientTCPSocket.close();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });

                Thread.ofVirtual().start(() -> {
                    try {
                        var outputStream = clientTCPSocket.getOutputStream();
                        var inputStream = serverTCPSocket.getInputStream();
                        inputStream.transferTo(outputStream);
                    } catch (IOException e) {
                        try {
                            serverTCPSocket.close();
                            clientTCPSocket.close();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });

            } else {
                // 普通 Http 代理

                System.out.println("收到 HTTP 代理请求 : " + c.uri());

                var httpClient = new HttpClient();

                var response = (Http1ClientResponse) httpClient.request()
                    .method(request.method())
                    .uri(request.uri())
                    .headers(request.headers())
                    .send(new HttpReceivedWriter(request));

                request.response()
                    .reasonPhrase(response.reasonPhrase())
                    .statusCode(response.statusCode())
                    .headers(response.headers())
                    .send(new HttpReceivedWriter(response));

            }
        });

        httpServer.start(17890);
    }

    /// 尝试解码内容 可以拓展为抓包工具
    public static void test2() {
        // todo 这里 https 需要 伪造 tls 证书, 完成握手之后 后续步骤和 test1 相同 即可解密内容
    }

    public record HttpReceivedWriter(ScxHttpReceived httpReceived) implements ScxHttpSender.BodyWriter {

        @Override
        public Long bodyLength() {
            return httpReceived.bodyLength();
        }

        @Override
        public void write(ByteOutput byteOutput) throws ScxInputException, InputAlreadyClosedException, ScxOutputException, OutputAlreadyClosedException {
            var byteInput = httpReceived.body();
            try (byteOutput; byteInput) {
                ScxIO.transferToAll(byteInput, byteOutput);
            }
        }

    }

}
