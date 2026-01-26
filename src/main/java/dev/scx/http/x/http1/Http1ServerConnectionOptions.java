package dev.scx.http.x.http1;

import dev.scx.http.x.http1.headers.upgrade.ScxUpgrade;

import java.util.HashMap;
import java.util.Map;

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
    private Map<ScxUpgrade, Http1UpgradeRequestFactory<?>> upgradeRequestFactories; // 协议升级列表

    public Http1ServerConnectionOptions() {
        this.maxRequestLineSize = 1024 * 64; // 默认 64 KB
        this.maxHeaderSize = 1024 * 128; // 默认 128 KB
        this.maxPayloadSize = 1024 * 1024 * 16; // 默认 16 MB
        this.autoRespond100Continue = true; // 默认自动响应
        this.validateHost = true; // 默认验证 Host
        this.upgradeRequestFactories = new HashMap<>(); // 升级列表
    }

    public Http1ServerConnectionOptions(Http1ServerConnectionOptions oldOptions) {
        maxRequestLineSize(oldOptions.maxRequestLineSize());
        maxHeaderSize(oldOptions.maxHeaderSize());
        maxPayloadSize(oldOptions.maxPayloadSize());
        autoRespond100Continue(oldOptions.autoRespond100Continue());
        validateHost(oldOptions.validateHost());
        upgradeRequestFactories(oldOptions.upgradeRequestFactories());
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

    public Map<ScxUpgrade, Http1UpgradeRequestFactory<?>> upgradeRequestFactories() {
        return upgradeRequestFactories;
    }

    public Http1ServerConnectionOptions upgradeRequestFactories(Map<ScxUpgrade, Http1UpgradeRequestFactory<?>> upgradeRequestFactories) {
        this.upgradeRequestFactories = new HashMap<>(upgradeRequestFactories);
        return this;
    }

    public Http1ServerConnectionOptions addUpgradeRequestFactory(Http1UpgradeRequestFactory<?>... upgradeRequestFactories) {
        for (var upgradeRequestFactory : upgradeRequestFactories) {
            this.upgradeRequestFactories.put(upgradeRequestFactory.upgradeProtocol(), upgradeRequestFactory);
        }
        return this;
    }

}
