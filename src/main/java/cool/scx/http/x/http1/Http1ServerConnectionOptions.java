package cool.scx.http.x.http1;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.addAll;

/// Http1ServerConnectionOptions
///
/// @author scx567888
/// @version 0.0.1
public final class Http1ServerConnectionOptions {

    private int maxRequestLineSize; // 最大请求行大小
    private int maxHeaderSize; // 最大请求头大小
    private long maxPayloadSize; // 最大请求体大小
    private boolean autoRespond100Continue; // 自动响应 100-continue
    private boolean validateHost; // 验证 Host 字段
    private List<Http1UpgradeHandler<?>> upgradeHandlerList; // 协议升级列表

    public Http1ServerConnectionOptions() {
        this.maxRequestLineSize = 1024 * 64; // 默认 64 KB
        this.maxHeaderSize = 1024 * 128; // 默认 128 KB
        this.maxPayloadSize = 1024 * 1024 * 16; // 默认 16 MB
        this.autoRespond100Continue = true; // 默认自动响应
        this.validateHost = true; // 默认验证 Host
        this.upgradeHandlerList = new ArrayList<>();
    }

    public Http1ServerConnectionOptions(Http1ServerConnectionOptions oldOptions) {
        maxRequestLineSize(oldOptions.maxRequestLineSize());
        maxHeaderSize(oldOptions.maxHeaderSize());
        maxPayloadSize(oldOptions.maxPayloadSize());
        autoRespond100Continue(oldOptions.autoRespond100Continue());
        validateHost(oldOptions.validateHost());
        upgradeHandlerList(oldOptions.upgradeHandlerList());
    }

    public int maxRequestLineSize() {
        return maxRequestLineSize;
    }

    public Http1ServerConnectionOptions maxRequestLineSize(int maxRequestLineSize) {
        this.maxRequestLineSize = maxRequestLineSize;
        return this;
    }

    public int maxHeaderSize() {
        return maxHeaderSize;
    }

    public Http1ServerConnectionOptions maxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
        return this;
    }

    public long maxPayloadSize() {
        return maxPayloadSize;
    }

    public Http1ServerConnectionOptions maxPayloadSize(long maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
        return this;
    }

    public boolean autoRespond100Continue() {
        return autoRespond100Continue;
    }

    public Http1ServerConnectionOptions autoRespond100Continue(boolean autoRespond100Continue) {
        this.autoRespond100Continue = autoRespond100Continue;
        return this;
    }

    public boolean validateHost() {
        return validateHost;
    }

    public Http1ServerConnectionOptions validateHost(boolean validateHost) {
        this.validateHost = validateHost;
        return this;
    }

    public List<Http1UpgradeHandler<?>> upgradeHandlerList() {
        return upgradeHandlerList;
    }

    public Http1ServerConnectionOptions upgradeHandlerList(List<Http1UpgradeHandler<?>> upgradeHandlerList) {
        this.upgradeHandlerList = upgradeHandlerList;
        return this;
    }

    public Http1ServerConnectionOptions addUpgradeHandler(Http1UpgradeHandler<?>... upgradeHandlerList) {
        addAll(this.upgradeHandlerList, upgradeHandlerList);
        return this;
    }

}
