package cool.scx.http.x.http1;

import cool.scx.http.ScxHttpClientRequest;
import cool.scx.http.x.http1.request_line.RequestTargetForm;

/// Http1ClientRequest
///
/// @author scx567888
/// @version 0.0.1
public interface Http1ClientRequest extends ScxHttpClientRequest {

    RequestTargetForm requestTargetForm();

    Http1ClientRequest requestTargetForm(RequestTargetForm requestTargetForm);

}
