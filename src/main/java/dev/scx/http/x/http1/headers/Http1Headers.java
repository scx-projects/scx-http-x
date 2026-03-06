package dev.scx.http.x.http1.headers;

import dev.scx.http.headers.ScxHttpHeaderName;
import dev.scx.http.headers.ScxHttpHeaders;
import dev.scx.http.headers.ScxHttpHeadersImpl;
import dev.scx.http.x.http1.headers.connection.Connection;
import dev.scx.http.x.http1.headers.connection.ScxConnection;
import dev.scx.http.x.http1.headers.expect.ScxExpect;
import dev.scx.http.x.http1.headers.transfer_encoding.ScxTransferEncoding;
import dev.scx.http.x.http1.headers.upgrade.ScxUpgrade;

import static dev.scx.http.headers.HttpHeaderName.*;

/// Http1Headers
///
/// 相比 默认的 ScxHttpHeaders, 多了一些 Http1 独有的头.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1Headers extends ScxHttpHeadersImpl {

    public Http1Headers(ScxHttpHeaders oldHeaders) {
        super(oldHeaders);
    }

    public Http1Headers() {
        super();
    }

    @Override
    public Http1Headers set(ScxHttpHeaderName name, String... value) {
        return (Http1Headers) super.set(name, value);
    }

    @Override
    public Http1Headers add(ScxHttpHeaderName name, String... value) {
        return (Http1Headers) super.add(name, value);
    }

    @Override
    public Http1Headers remove(ScxHttpHeaderName name) {
        return (Http1Headers) super.remove(name);
    }

    @Override
    public Http1Headers clear() {
        return (Http1Headers) super.clear();
    }

    public ScxConnection connection() {
        var c = get(CONNECTION);
        return c != null ? ScxConnection.of(c) : null;
    }

    public Http1Headers connection(Connection connection) {
        return set(CONNECTION, connection.value());
    }

    public ScxTransferEncoding transferEncoding() {
        var c = get(TRANSFER_ENCODING);
        return c != null ? ScxTransferEncoding.of(c) : null;
    }

    public Http1Headers transferEncoding(ScxTransferEncoding transferEncoding) {
        return set(TRANSFER_ENCODING, transferEncoding.value());
    }

    public ScxExpect expect() {
        var c = get(EXPECT);
        return c != null ? ScxExpect.of(c) : null;
    }

    public Http1Headers expect(ScxExpect expect) {
        return set(EXPECT, expect.value());
    }

    public ScxUpgrade upgrade() {
        var c = get(UPGRADE);
        return c != null ? ScxUpgrade.of(c) : null;
    }

    public Http1Headers upgrade(ScxUpgrade upgrade) {
        return set(UPGRADE, upgrade.value());
    }

}
