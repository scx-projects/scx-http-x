package cool.scx.http.x.http1.headers.upgrade;

/// ScxUpgradeImpl
///
/// @author scx567888
/// @version 0.0.1
record ScxUpgradeImpl(String value) implements ScxUpgrade {

    ScxUpgradeImpl {
        value = value.toLowerCase();
    }

    @Override
    public String toString() {
        return value;
    }

}
