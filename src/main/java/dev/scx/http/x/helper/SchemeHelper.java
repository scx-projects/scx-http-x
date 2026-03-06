package dev.scx.http.x.helper;

/// SchemeHelper
///
/// @author scx567888
/// @version 0.0.1
public final class SchemeHelper {

    public static boolean isTLS(String scheme) {
        scheme = scheme.toLowerCase();
        return switch (scheme) {
            case "http", "ws" -> false;
            case "https", "wss" -> true;
            default -> throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        };
    }

    public static int getDefaultPort(String scheme) throws IllegalArgumentException {
        scheme = scheme.toLowerCase();
        return switch (scheme) {
            case "http", "ws" -> 80;
            case "https", "wss" -> 443;
            default -> throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        };
    }

}
