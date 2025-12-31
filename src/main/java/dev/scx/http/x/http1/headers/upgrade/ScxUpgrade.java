package dev.scx.http.x.http1.headers.upgrade;

/// ScxUpgrade (注意根据协议规定 值 不区分大小写)
///
/// @author scx567888
/// @version 0.0.1
public sealed interface ScxUpgrade permits Upgrade, ScxUpgradeImpl {

    static ScxUpgrade of(String v) {
        // 优先使用 Upgrade
        var m = Upgrade.find(v);
        return m != null ? m : new ScxUpgradeImpl(v);
    }

    String value();

}
