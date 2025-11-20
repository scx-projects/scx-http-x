package cool.scx.http.x.test;

import cool.scx.http.method.HttpMethod;
import cool.scx.http.x.HttpClient;
import cool.scx.http.x.HttpServer;
import cool.scx.http.x.http1.Http1ClientResponse;
import cool.scx.http.x.http1.Http1ServerRequest;
import cool.scx.tcp.TCPClient;

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
                //2, 停止循环
                serverConnection.stop();
                //3, 获取 当前连接的 底层 tcpSocket 内容
                var serverTCPSocket = serverConnection.tcpSocket;
                //4, 创建 远端连接
                var tcpClient = new TCPClient();
                Socket clientTCPSocket;
                try {
                    clientTCPSocket = tcpClient.connect(new InetSocketAddress(c.uri().host(), c.uri().port()));
                } catch (IOException e) {
                    throw new RuntimeException("连接远端失败 : " + c.uri(), e);
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
                            serverConnection.close();
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
                        .send(request.body().byteInput());

                request.response()
                        .reasonPhrase(response.reasonPhrase())
                        .statusCode(response.statusCode())
                        .headers(response.headers())
                        .send(response.body().byteInput());

            }
        });

        httpServer.start(17890);
    }

    /// 尝试解码内容 可以拓展为抓包工具
    public static void test2() {
        // todo 这里 https 需要 伪造 tls 证书, 完成握手之后 后续步骤和 test1 相同 即可解密内容
    }

}
