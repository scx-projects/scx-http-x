package cool.scx.http.x.http1;

public class Http1ClientConnectionOptions {

    private int maxStatusLineSize;// 最大响应行大小
    private int maxHeaderSize;// 最大请求头大小
    private int maxPayloadSize;// 最大请求体大小

    public Http1ClientConnectionOptions() {
        this.maxStatusLineSize = 1024 * 64; // 默认 64 KB
        this.maxHeaderSize = 1024 * 128; // 默认 128 KB
        this.maxPayloadSize = 1024 * 1024 * 16; // 默认 16 MB
    }

    public Http1ClientConnectionOptions(Http1ClientConnectionOptions oldOptions) {
        maxStatusLineSize(oldOptions.maxStatusLineSize());
        maxHeaderSize(oldOptions.maxHeaderSize());
        maxPayloadSize(oldOptions.maxPayloadSize());
    }

    public int maxStatusLineSize() {
        return maxStatusLineSize;
    }

    public Http1ClientConnectionOptions maxStatusLineSize(int maxStatusLineSize) {
        this.maxStatusLineSize = maxStatusLineSize;
        return this;
    }

    public int maxHeaderSize() {
        return maxHeaderSize;
    }

    public Http1ClientConnectionOptions maxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
        return this;
    }

    public int maxPayloadSize() {
        return maxPayloadSize;
    }

    public Http1ClientConnectionOptions maxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
        return this;
    }

}
