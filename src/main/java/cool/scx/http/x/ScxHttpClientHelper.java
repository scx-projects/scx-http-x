package cool.scx.http.x;

import cool.scx.http.ScxHttpClient;
import cool.scx.http.ScxHttpClientRequest;
import cool.scx.http.ScxHttpClientResponse;
import cool.scx.http.media.multi_part.MultiPart;
import cool.scx.http.uri.ScxURI;

import static cool.scx.http.method.HttpMethod.*;

/// ScxHttpClientHelper
/// todo 这个类有必要吗?
///
/// @author scx567888
/// @version 0.0.1
public final class ScxHttpClientHelper {

    public static final ScxHttpClient DEFAULT_HTTP_CLIENT = new HttpClient();

    public static ScxHttpClientRequest request() {
        return DEFAULT_HTTP_CLIENT.request();
    }

    public static ScxHttpClientResponse get(ScxURI uri) {
        return DEFAULT_HTTP_CLIENT.request().method(GET).uri(uri).send();
    }

    public static ScxHttpClientResponse post(ScxURI uri, MultiPart content) {
        return DEFAULT_HTTP_CLIENT.request().method(POST).uri(uri).send(content);
    }

    public static ScxHttpClientResponse put(ScxURI uri, MultiPart content) {
        return DEFAULT_HTTP_CLIENT.request().method(PUT).uri(uri).send(content);
    }

    public static ScxHttpClientResponse delete(ScxURI uri, MultiPart content) {
        return DEFAULT_HTTP_CLIENT.request().method(DELETE).uri(uri).send(content);
    }

    public static ScxHttpClientResponse get(String uri) {
        return DEFAULT_HTTP_CLIENT.request().method(GET).uri(uri).send();
    }

    public static ScxHttpClientResponse post(String uri, MultiPart content) {
        return DEFAULT_HTTP_CLIENT.request().method(POST).uri(uri).send(content);
    }

    public static ScxHttpClientResponse put(String uri, MultiPart content) {
        return DEFAULT_HTTP_CLIENT.request().method(PUT).uri(uri).send(content);
    }

    public static ScxHttpClientResponse delete(String uri, MultiPart content) {
        return DEFAULT_HTTP_CLIENT.request().method(DELETE).uri(uri).send(content);
    }

}
