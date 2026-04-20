package dev.scx.http.x.http1.request_line.request_target;

import dev.scx.http.uri.ScxURI;

/// RequestTarget
///
/// @author scx567888
/// @version 0.0.1
public sealed interface RequestTarget permits AbsoluteForm, AsteriskForm, AuthorityForm, OriginForm {

    ScxURI toScxURI();

    String encode();

}
