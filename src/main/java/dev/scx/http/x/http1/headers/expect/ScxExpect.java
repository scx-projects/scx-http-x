package dev.scx.http.x.http1.headers.expect;

/// ScxExpect
///
/// @author scx567888
/// @version 0.0.1
public interface ScxExpect {

    static ScxExpect of(String v) {
        // 优先使用 Expect
        var m = Expect.find(v);
        return m != null ? m : new ScxExpectImpl(v);
    }

    String value();

}
