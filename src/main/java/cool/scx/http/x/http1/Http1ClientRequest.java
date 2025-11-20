package cool.scx.http.x.http1;

import cool.scx.http.ScxHttpClientRequest;
import cool.scx.http.sender.ScxHttpSenderStatus;
import cool.scx.http.x.http1.request_line.RequestTargetForm;

/// Http1ClientRequest
///
/// @author scx567888
/// @version 0.0.1
public interface Http1ClientRequest extends ScxHttpClientRequest {

    RequestTargetForm requestTargetForm();

    Http1ClientRequest requestTargetForm(RequestTargetForm requestTargetForm);

    /// 内部方法 只应该由 Http1Writer 调用
    void _setSenderStatus(ScxHttpSenderStatus senderStatus);

}
