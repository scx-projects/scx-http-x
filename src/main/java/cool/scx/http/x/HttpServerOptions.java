package cool.scx.http.x;

import cool.scx.http.x.http1.Http1ServerConnectionOptions;
import cool.scx.http.x.http1.Http1UpgradeHandler;
import cool.scx.http.x.http2.Http2ServerConnectionOptions;
import dev.scx.tcp.TCPServerOptions;
import dev.scx.tcp.tls.TLS;

import java.util.List;

/// Http 服务器配置
///
/// @author scx567888
/// @version 0.0.1
public final class HttpServerOptions {

    private final TCPServerOptions tcpServerOptions; // TCP 服务器配置
    private final Http1ServerConnectionOptions http1ServerConnectionOptions;// Http1 配置
    private final Http2ServerConnectionOptions http2ServerConnectionOptions;// Http2 配置
    private TLS tls; // tls
    private boolean enableHttp2; // 是否开启 Http2

    public HttpServerOptions() {
        this.tcpServerOptions = new TCPServerOptions();
        this.http1ServerConnectionOptions = new Http1ServerConnectionOptions();
        this.http2ServerConnectionOptions = new Http2ServerConnectionOptions();
        this.tls = null; // 默认没有 tls
        this.enableHttp2 = false;// 默认不启用 http2
    }

    public HttpServerOptions(HttpServerOptions oldOptions) {
        this.tcpServerOptions = new TCPServerOptions(oldOptions.tcpServerOptions());
        this.http1ServerConnectionOptions = new Http1ServerConnectionOptions(oldOptions.http1ServerConnectionOptions());
        this.http2ServerConnectionOptions = new Http2ServerConnectionOptions(oldOptions.http2ServerConnectionOptions());
        tls(oldOptions.tls());
        enableHttp2(oldOptions.enableHttp2());
    }

    /// 因为涉及到一些底层实现, 所以不允许外界访问
    TCPServerOptions tcpServerOptions() {
        return tcpServerOptions;
    }

    public Http1ServerConnectionOptions http1ServerConnectionOptions() {
        return http1ServerConnectionOptions;
    }

    public Http2ServerConnectionOptions http2ServerConnectionOptions() {
        return http2ServerConnectionOptions;
    }

    public boolean enableHttp2() {
        return enableHttp2;
    }

    public HttpServerOptions enableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
        return this;
    }

    public int backlog() {
        return tcpServerOptions.backlog();
    }

    public HttpServerOptions backlog(int backlog) {
        this.tcpServerOptions.backlog(backlog);
        return this;
    }

    public TLS tls() {
        return tls;
    }

    public HttpServerOptions tls(TLS tls) {
        this.tls = tls;
        return this;
    }

    // ******************** 简易操作 **********************

    public HttpServerOptions maxRequestLineSize(int maxRequestLineSize) {
        this.http1ServerConnectionOptions.maxRequestLineSize(maxRequestLineSize);
        return this;
    }

    public HttpServerOptions maxHeaderSize(int maxHeaderSize) {
        this.http1ServerConnectionOptions.maxHeaderSize(maxHeaderSize);
        return this;
    }

    public HttpServerOptions maxPayloadSize(long maxPayloadSize) {
        this.http1ServerConnectionOptions.maxPayloadSize(maxPayloadSize);
        return this;
    }

    public HttpServerOptions autoRespond100Continue(boolean autoRespond100Continue) {
        this.http1ServerConnectionOptions.autoRespond100Continue(autoRespond100Continue);
        return this;
    }

    public HttpServerOptions validateHost(boolean validateHost) {
        this.http1ServerConnectionOptions.validateHost(validateHost);
        return this;
    }

    public HttpServerOptions upgradeHandlerList(List<Http1UpgradeHandler<?>> upgradeHandlerList) {
        this.http1ServerConnectionOptions.upgradeHandlerList(upgradeHandlerList);
        return this;
    }

    public HttpServerOptions addUpgradeHandler(Http1UpgradeHandler<?>... upgradeHandlerList) {
        this.http1ServerConnectionOptions.addUpgradeHandler(upgradeHandlerList);
        return this;
    }

}
