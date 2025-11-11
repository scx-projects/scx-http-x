package cool.scx.http.x.http1.headers;

import cool.scx.function.Function2Void;
import cool.scx.http.headers.ScxHttpHeaderName;
import cool.scx.http.headers.ScxHttpHeaders;
import cool.scx.http.headers.ScxHttpHeadersWritable;
import cool.scx.http.parameters.ParameterEntry;
import cool.scx.http.x.http1.headers.connection.Connection;
import cool.scx.http.x.http1.headers.connection.ScxConnection;
import cool.scx.http.x.http1.headers.expect.ScxExpect;
import cool.scx.http.x.http1.headers.transfer_encoding.ScxTransferEncoding;
import cool.scx.http.x.http1.headers.upgrade.ScxUpgrade;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cool.scx.http.headers.HttpHeaderName.*;

/// Http1Headers
///
/// 相比 默认的 ScxHttpHeaders, 多了一些 Http1 独有的头.
///
/// @author scx567888
/// @version 0.0.1
public final class Http1Headers implements ScxHttpHeadersWritable {

    private final ScxHttpHeadersWritable h;

    public Http1Headers(ScxHttpHeaders h) {
        this.h = ScxHttpHeaders.of(h);
    }

    public Http1Headers() {
        this.h = ScxHttpHeaders.of();
    }

    @Override
    public Http1Headers set(ScxHttpHeaderName name, String... value) {
        h.set(name, value);
        return this;
    }

    @Override
    public Http1Headers add(ScxHttpHeaderName name, String... value) {
        h.add(name, value);
        return this;
    }

    @Override
    public Http1Headers remove(ScxHttpHeaderName name) {
        h.remove(name);
        return this;
    }

    @Override
    public Http1Headers clear() {
        h.clear();
        return this;
    }

    @Override
    public long size() {
        return h.size();
    }

    @Override
    public Set<ScxHttpHeaderName> names() {
        return h.names();
    }

    @Override
    public String get(ScxHttpHeaderName name) {
        return h.get(name);
    }

    @Override
    public List<String> getAll(ScxHttpHeaderName name) {
        return h.getAll(name);
    }

    @Override
    public boolean contains(ScxHttpHeaderName name) {
        return h.contains(name);
    }

    @Override
    public boolean isEmpty() {
        return h.isEmpty();
    }

    @Override
    public Map<ScxHttpHeaderName, List<String>> toMultiValueMap() {
        return h.toMultiValueMap();
    }

    @Override
    public Map<ScxHttpHeaderName, String> toMap() {
        return h.toMap();
    }

    @Override
    public <X extends Throwable> void forEach(Function2Void<? super ScxHttpHeaderName, String, X> action) throws X {
        h.forEach(action);
    }

    @Override
    public <X extends Throwable> void forEachParameter(Function2Void<? super ScxHttpHeaderName, List<String>, X> action) throws X {
        h.forEachParameter(action);
    }

    @Override
    public Iterator<ParameterEntry<ScxHttpHeaderName, String>> iterator() {
        return h.iterator();
    }

    @Override
    public String toString() {
        return h.toString();
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
