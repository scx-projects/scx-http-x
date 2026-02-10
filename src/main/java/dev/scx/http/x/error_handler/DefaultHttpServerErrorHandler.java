package dev.scx.http.x.error_handler;

import dev.scx.exception.ScxWrappedException;
import dev.scx.http.ScxHttpServerRequest;
import dev.scx.http.error_handler.ErrorPhase;
import dev.scx.http.error_handler.ScxHttpServerErrorHandler;
import dev.scx.http.exception.ScxHttpException;
import dev.scx.http.media_type.ScxMediaType;
import dev.scx.http.sender.IllegalSenderStateException;
import dev.scx.http.sender.ScxHttpReceiveException;
import dev.scx.http.sender.ScxHttpSendException;
import dev.scx.http.status_code.ScxHttpStatusCode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger;

import static dev.scx.http.media_type.MediaType.TEXT_HTML;
import static dev.scx.http.status_code.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static dev.scx.http.status_code.ScxHttpStatusCodeHelper.getReasonPhrase;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.nio.charset.StandardCharsets.UTF_8;

/// 默认错误处理器
///
/// @author scx567888
/// @version 0.0.1
public final class DefaultHttpServerErrorHandler implements ScxHttpServerErrorHandler {

    public static final DefaultHttpServerErrorHandler DEFAULT_HTTP_SERVER_ERROR_HANDLER = new DefaultHttpServerErrorHandler(true);

    /// 默认 html 模板
    public static final String HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>%s</title>
        </head>
        <body>
            <h1>%s - %s</h1>
            <hr>
            <pre>%s</pre>
        </body>
        </html>
        """;

    private static final Logger LOGGER = getLogger(DefaultHttpServerErrorHandler.class.getName());

    private final boolean useDevelopmentErrorPage;

    public DefaultHttpServerErrorHandler(boolean useDevelopmentErrorPage) {
        this.useDevelopmentErrorPage = useDevelopmentErrorPage;
    }

    public static String getErrorPhaseString(ErrorPhase errorPhase) {
        return switch (errorPhase) {
            case SYSTEM -> "系统处理器";
            case USER -> "用户处理器";
        };
    }

    /// 获取 jdk 内部默认实现的堆栈跟踪字符串
    public static String getStackTraceString(Throwable throwable) {
        var stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.getBuffer().toString();
    }

    public static void sendToClient(ScxHttpStatusCode statusCode, String info, ScxHttpServerRequest request) throws IllegalSenderStateException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        var reasonPhrase = getReasonPhrase(statusCode, "unknown");
        // 不管 accept, 统一返回 html
        var htmlStr = String.format(HTML_TEMPLATE, reasonPhrase, statusCode.value(), reasonPhrase, info);
        request.response()
            .contentType(ScxMediaType.of(TEXT_HTML).charset(UTF_8))
            .statusCode(statusCode)
            .send(htmlStr);
    }

    @Override
    public void handle(Throwable throwable, ScxHttpServerRequest request, ErrorPhase errorPhase) throws IllegalSenderStateException, ScxHttpSendException, ScxWrappedException, ScxHttpReceiveException {
        ScxHttpStatusCode statusCode = INTERNAL_SERVER_ERROR;// 默认状态码
        String info = ""; // 默认异常信息
        // Http 异常无需打印
        if (throwable instanceof ScxHttpException h) {
            statusCode = h.statusCode();
        }

        // 500 异常, 需要打印
        if (statusCode == INTERNAL_SERVER_ERROR) {
            LOGGER.log(ERROR, getErrorPhaseString(errorPhase) + " 发生异常 !!!", throwable);
        }

        // 如果启用了 开发者页面
        if (useDevelopmentErrorPage) {
            info = getStackTraceString(throwable);
        }

        // 发送给客户端
        sendToClient(statusCode, info, request);
    }

}
