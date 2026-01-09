package dev.scx.http.x;

import dev.scx.function.Function1Void;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;

/// 在不暴漏 ScxHttpServer 语义的情况下, 提供 访问服务器配置的接口
public interface HttpServerContext {

    HttpServerOptions options();

    Function1Void<ScxHttpServerRequest, ?> requestHandler();

    ScxHttpServerErrorHandler errorHandler();

}
